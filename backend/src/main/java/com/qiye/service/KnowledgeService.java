package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiye.ai.EmbeddingClient;
import com.qiye.common.BizException;
import com.qiye.common.PageResult;
import com.qiye.config.AppProperties;
import com.qiye.entity.Dept;
import com.qiye.entity.KnowledgeChunk;
import com.qiye.entity.KnowledgeFile;
import com.qiye.mapper.DeptMapper;
import com.qiye.mapper.KnowledgeChunkMapper;
import com.qiye.mapper.KnowledgeFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 知识库：上传 → 解析 → 切片 → 向量化 → 检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private static final Set<String> ALLOWED = Set.of("pdf", "docx", "txt", "md");
    private static final int CHUNK_SIZE = 500;

    private final KnowledgeFileMapper fileMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final EmbeddingClient embeddingClient;
    private final AppProperties appProps;
    private final DeptMapper deptMapper;

    @Transactional
    public KnowledgeFile upload(MultipartFile file, Long deptId, Long userId) {
        String filename = file.getOriginalFilename() == null ? "未命名" : file.getOriginalFilename();
        String ext = extOf(filename);
        if (!ALLOWED.contains(ext)) {
            throw new BizException("仅支持 PDF / Word(.docx) / TXT 文件");
        }
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new BizException("文件不能超过 20MB");
        }
        String text;
        try {
            text = parse(file, ext);
        } catch (Exception e) {
            log.error("解析文档失败", e);
            throw new BizException("文档解析失败：" + e.getMessage());
        }
        if (!StringUtils.hasText(text)) {
            throw new BizException("未能从文档中解析出文本内容");
        }

        // 存储原文件
        Path dir = Path.of(appProps.getUploadDir());
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path path = dir.resolve(stored);
        try {
            file.transferTo(path);
        } catch (IOException e) {
            throw new BizException("文件保存失败");
        }

        KnowledgeFile kf = new KnowledgeFile();
        kf.setName(filename);
        kf.setPath(path.toString());
        kf.setFileType(ext);
        kf.setSize(file.getSize());
        kf.setDeptId(deptId);
        kf.setStatus("READY");
        kf.setCreatedBy(userId);
        fileMapper.insert(kf);

        // 切片 + 向量化
        List<String> chunks = split(text);
        boolean embedded = embeddingClient.enabled();
        int seq = 1;
        for (String c : chunks) {
            if (embedded) {
                try {
                    float[] vec = embeddingClient.embed(c);
                    chunkMapper.insertVector(kf.getId(), seq, c, toVectorStr(vec));
                    seq++;
                    continue;
                } catch (Exception e) {
                    log.warn("向量化失败，降级为文本存储", e);
                    embedded = false;
                }
            }
            KnowledgeChunk kc = new KnowledgeChunk();
            kc.setFileId(kf.getId());
            kc.setSeq(seq);
            kc.setContent(c);
            chunkMapper.insert(kc);
            seq++;
        }
        kf.setChunkCount(chunks.size());
        fileMapper.updateById(kf);
        return kf;
    }

    public PageResult<KnowledgeFile> page(int page, int size, String keyword, Long deptId, boolean isAdmin) {
        LambdaQueryWrapper<KnowledgeFile> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(KnowledgeFile::getName, keyword);
        }
        if (!isAdmin) {
            // 员工仅可见全部门文档与本部门文档
            qw.and(w -> w.isNull(KnowledgeFile::getDeptId).or().eq(KnowledgeFile::getDeptId, deptId));
        }
        qw.orderByDesc(KnowledgeFile::getId);
        Page<KnowledgeFile> p = fileMapper.selectPage(new Page<>(page, size), qw);
        Map<Long, Dept> deptMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Dept::getId, Function.identity()));
        p.getRecords().forEach(kf -> {
            Dept d = deptMap.get(kf.getDeptId());
            kf.setDeptName(d == null ? null : d.getName());
        });
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    public KnowledgeFile detail(Long id) {
        KnowledgeFile kf = fileMapper.selectById(id);
        if (kf == null) {
            throw new BizException("文档不存在");
        }
        List<KnowledgeChunk> first = chunkMapper.selectList(
                new LambdaQueryWrapper<KnowledgeChunk>()
                        .eq(KnowledgeChunk::getFileId, id)
                        .orderByAsc(KnowledgeChunk::getSeq)
                        .last("LIMIT 1"));
        if (!first.isEmpty()) {
            String content = first.get(0).getContent();
            kf.setPreview(content == null ? "" : (content.length() > 200 ? content.substring(0, 200) + "…" : content));
        }
        return kf;
    }

    public void delete(Long id) {
        fileMapper.deleteById(id);   // chunk 级联删除
    }

    /** 检索：优先向量（pgvector 余弦），无向量化能力时降级关键词 */
    public List<Map<String, Object>> search(String query, boolean isAdmin, Long deptId, int topK) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        if (embeddingClient.enabled()) {
            try {
                float[] vec = embeddingClient.embed(query);
                return chunkMapper.searchSimilarity(toVectorStr(vec), deptId, isAdmin, topK);
            } catch (Exception e) {
                log.warn("向量检索失败，降级为关键词检索", e);
            }
        }
        return chunkMapper.searchLike(toLikePattern(query), deptId, isAdmin, topK);
    }

    /** 查询词 → 正则 OR 模式（去掉疑问助词后按词匹配） */
    private String toLikePattern(String query) {
        String cleaned = query.replaceAll("[？?。！!，,、\\s的怎么办如何什么是怎么样请问]", " ");
        StringBuilder sb = new StringBuilder();
        for (String w : cleaned.split("\\s+")) {
            if (!w.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(escapeRegex(w));
            }
        }
        return sb.length() == 0 ? escapeRegex(query) : sb.toString();
    }

    /** POSIX 正则转义（PostgreSQL ~* 使用） */
    private String escapeRegex(String s) {
        return s.replaceAll("([.\\\\+*?\\[\\]^$(){}=!<>|\\-])", "\\\\$1");
    }

    private String parse(MultipartFile file, String ext) throws Exception {
        try (InputStream in = file.getInputStream()) {
            return switch (ext) {
                case "pdf" -> parsePdf(in);
                case "docx" -> parseDocx(in);
                case "txt", "md" -> new String(in.readAllBytes(), StandardCharsets.UTF_8);
                default -> "";
            };
        }
    }

    private String parsePdf(InputStream in) throws IOException {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        }
    }

    private String parseDocx(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append('\n');
            }
        }
        return sb.toString();
    }

    /** 简单切片：按约 500 字符窗口，尽量在句号处断开 */
    private List<String> split(String text) {
        String clean = text.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s+", " ").trim();
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < clean.length()) {
            int end = Math.min(start + CHUNK_SIZE, clean.length());
            if (end < clean.length()) {
                int cut = clean.lastIndexOf('。', end);
                if (cut > start + 100) {
                    end = cut + 1;
                }
            }
            String seg = clean.substring(start, end).trim();
            if (!seg.isEmpty()) {
                result.add(seg);
            }
            start = end;
        }
        return result;
    }

    private String toVectorStr(float[] v) {
        // pgvector 文本格式为 [a,b,c]（方括号），且不能用科学计数法
        StringBuilder sb = new StringBuilder("[");
        for (float f : v) {
            sb.append(BigDecimal.valueOf(f).toPlainString()).append(',');
        }
        sb.setLength(sb.length() - 1);
        sb.append(']');
        return sb.toString();
    }

    private String extOf(String filename) {
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase();
    }
}
