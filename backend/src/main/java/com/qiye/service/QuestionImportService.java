package com.qiye.service;

import com.qiye.ai.LlmClient;
import com.qiye.common.BizException;
import com.qiye.entity.Question;
import com.qiye.entity.QuestionSkill;
import com.qiye.mapper.QuestionMapper;
import com.qiye.mapper.QuestionSkillMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 试题批量导入：Excel / Word(docx) / PDF / TXT / Markdown
 * <ul>
 *   <li>xlsx：按列结构化解析（表头：题型/题干/选项A-D/答案/解析）</li>
 *   <li>docx/pdf/txt/md：提取文本 → 约定格式解析；AI 模式（或 AUTO 兜底）调用 LLM 抽取</li>
 * </ul>
 * 逐行校验，错误行跳过并记录原因，其余正常入库（绑定统一技能，scoreWeight=1）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionImportService {

    private static final Set<String> ALLOWED = Set.of("xlsx", "docx", "pdf", "txt", "md");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private static final Pattern OPTION = Pattern.compile("^([A-Ha-h])[.、)）:]\\s*(.*)");
    private static final Pattern ANSWER = Pattern.compile("^答案[：:]\\s*(.*)");
    private static final Pattern ANALYSIS = Pattern.compile("^解析[：:]\\s*(.*)");
    private static final Pattern STEM_NO = Pattern.compile("^\\d+[.、)）]\\s*");

    private final QuestionMapper questionMapper;
    private final QuestionSkillMapper questionSkillMapper;
    private final AiAssistantService aiAssistantService;
    private final LlmClient llmClient;

    @Transactional
    public ImportResult importFile(MultipartFile file, Long skillId, String mode, Long userId) {
        String filename = file.getOriginalFilename() == null ? "未命名" : file.getOriginalFilename();
        String ext = extOf(filename);
        if (!ALLOWED.contains(ext)) {
            throw new BizException("仅支持 Excel(.xlsx) / Word(.docx) / PDF / TXT / Markdown 文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BizException("文件不能超过 5MB");
        }

        String source = "MANUAL";
        List<Question> parsed;
        List<String> errors = new ArrayList<>();
        boolean aiMode = "AI".equalsIgnoreCase(mode);
        try {
            if ("xlsx".equals(ext)) {
                parsed = parseExcel(file);
            } else {
                String text = extractText(file, ext);
                if (aiMode) {
                    parsed = aiExtract(text);
                    source = "AI";
                } else {
                    parsed = parseText(text, errors);
                    if (parsed.isEmpty() && "AUTO".equalsIgnoreCase(mode) && llmClient.enabled()) {
                        try {
                            parsed = aiExtract(text);
                            source = "AI";
                        } catch (Exception e) {
                            log.warn("AI 抽取失败，已跳过：{}", e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            log.error("解析文档失败", e);
            throw new BizException("文档解析失败：" + e.getMessage());
        }

        if (parsed.isEmpty() && errors.isEmpty()) {
            throw new BizException("未能从文件中解析出任何题目，请检查文件格式或改用 AI 识别");
        }

        int success = 0;
        for (Question q : parsed) {
            String err = validate(q, errors.size() + success + 1);
            if (err != null) {
                errors.add(err);
                continue;
            }
            q.setId(null);
            q.setSource(source);
            q.setCreatedBy(userId);
            questionMapper.insert(q);
            bindSkill(q.getId(), skillId);
            success++;
        }
        return new ImportResult(parsed.size(), success, errors);
    }

    // ========== 解析分发 ==========

    private String extractText(MultipartFile file, String ext) throws Exception {
        try (InputStream in = file.getInputStream()) {
            return switch (ext) {
                case "docx" -> parseDocx(in);
                case "pdf" -> parsePdf(in);
                default -> new String(in.readAllBytes(), StandardCharsets.UTF_8);  // txt / md
            };
        }
    }

    private String parsePdf(InputStream in) throws Exception {
        try (PDDocument doc = Loader.loadPDF(in.readAllBytes())) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String parseDocx(InputStream in) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append('\n');
            }
        }
        return sb.toString();
    }

    /** Excel：首行为表头（题型/题干/选项A/选项B/选项C/选项D/答案/解析），其余每行一题 */
    private List<Question> parseExcel(MultipartFile file) throws Exception {
        List<Question> result = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) {
                    continue;   // 表头
                }
                String type = cellStr(row, 0);
                String content = cellStr(row, 1);
                if (!StringUtils.hasText(content) && !StringUtils.hasText(type)) {
                    continue;   // 空行
                }
                Question q = new Question();
                q.setType(normalizeType(type));
                q.setContent(content);
                List<Question.OptionItem> options = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    String text = cellStr(row, 2 + i);
                    if (StringUtils.hasText(text)) {
                        options.add(new Question.OptionItem(String.valueOf((char) ('A' + i)), text));
                    }
                }
                q.setOptions(options);
                q.setAnswer(cellStr(row, 6));
                q.setAnalysis(cellStr(row, 7));
                result.add(q);
            }
        }
        return result;
    }

    private String cellStr(Row row, int idx) {
        Cell c = row.getCell(idx);
        return c == null ? "" : new DataFormatter().formatCellValue(c).trim();
    }

    /** 题型中英文归一化 */
    private String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return "";
        }
        String t = type.trim().toUpperCase();
        return switch (t) {
            case "SINGLE", "单选", "单选题" -> "SINGLE";
            case "MULTIPLE", "多选", "多选题" -> "MULTIPLE";
            case "JUDGE", "判断", "判断题" -> "JUDGE";
            default -> t;
        };
    }

    // ========== 约定格式解析（docx/pdf/txt/md 通用） ==========

    /** 逐行扫描：选项/答案/解析行归入当前题块，其他普通行触发新题块 */
    List<Question> parseText(String text, List<String> errors) {
        List<Question> result = new ArrayList<>();
        List<String> block = new ArrayList<>();
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                if (!block.isEmpty()) {
                    parseBlock(block, result, errors);
                    block.clear();
                }
                continue;
            }
            if (OPTION.matcher(line).find() || ANSWER.matcher(line).find() || ANALYSIS.matcher(line).find()) {
                block.add(line);
            } else {
                if (!block.isEmpty()) {
                    parseBlock(block, result, errors);
                    block.clear();
                }
                block.add(line);
            }
        }
        if (!block.isEmpty()) {
            parseBlock(block, result, errors);
        }
        return result;
    }

    private void parseBlock(List<String> block, List<Question> result, List<String> errors) {
        if (block.isEmpty()) {
            return;
        }
        Question q = new Question();
        String stem = STEM_NO.matcher(block.get(0)).replaceFirst("");
        q.setContent(stem.trim());

        List<Question.OptionItem> options = new ArrayList<>();
        String answerRaw = "";
        List<String> analyses = new ArrayList<>();
        for (int i = 1; i < block.size(); i++) {
            String line = block.get(i);
            Matcher om = OPTION.matcher(line);
            Matcher am = ANSWER.matcher(line);
            Matcher ym = ANALYSIS.matcher(line);
            if (om.find()) {
                options.add(new Question.OptionItem(om.group(1).toUpperCase(), om.group(2).trim()));
            } else if (am.find()) {
                answerRaw = am.group(1).trim();
            } else if (ym.find()) {
                analyses.add(ym.group(1).trim());
            }
        }
        q.setOptions(options);
        q.setAnalysis(String.join("\n", analyses));
        q.setType(inferType(stem, answerRaw, options));
        q.setAnswer(normalizeAnswer(q.getType(), answerRaw));
        if (!StringUtils.hasText(answerRaw) && options.isEmpty()) {
            return;   // 纯说明文本块，不是题目，静默忽略
        }
        result.add(q);
    }

    /** 题型判定：题干标注优先，否则按答案/选项推断 */
    private String inferType(String stem, String answerRaw, List<Question.OptionItem> options) {
        String s = stem == null ? "" : stem;
        if (s.contains("单选")) {
            return "SINGLE";
        }
        if (s.contains("多选")) {
            return "MULTIPLE";
        }
        if (s.contains("判断")) {
            return "JUDGE";
        }
        if (answerRaw != null && isJudgeAnswer(answerRaw)) {
            return "JUDGE";
        }
        if (answerRaw != null && answerRaw.contains(",")) {
            return "MULTIPLE";
        }
        return "SINGLE";
    }

    private boolean isJudgeAnswer(String s) {
        String t = s.trim().toUpperCase();
        return t.equals("TRUE") || t.equals("FALSE") || t.equals("对") || t.equals("错")
                || t.equals("正确") || t.equals("错误") || t.equals("√") || t.equals("×");
    }

    /** 判断题答案归一化为 TRUE/FALSE；多选排序后逗号连接 */
    private String normalizeAnswer(String type, String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String t = raw.trim().toUpperCase();
        if ("JUDGE".equals(type)) {
            return switch (t) {
                case "TRUE", "对", "正确", "√", "T" -> "TRUE";
                default -> "FALSE";
            };
        }
        if ("MULTIPLE".equals(type)) {
            return java.util.Arrays.stream(raw.split("[,，]"))
                    .map(String::trim).filter(StringUtils::hasText)
                    .map(String::toUpperCase)
                    .sorted().collect(java.util.stream.Collectors.joining(","));
        }
        return raw.trim();
    }

    // ========== 校验 ==========

    /** 返回 null 表示通过，否则返回错误信息（带题目序号） */
    private String validate(Question q, int index) {
        String pre = "第" + index + "题：";
        if (!StringUtils.hasText(q.getContent())) {
            return pre + "题干为空";
        }
        String type = q.getType();
        if (!Set.of("SINGLE", "MULTIPLE", "JUDGE").contains(type)) {
            return pre + "题型不合法（应为 单选/多选/判断）";
        }
        if (!StringUtils.hasText(q.getAnswer())) {
            return pre + "缺少正确答案";
        }
        if ("JUDGE".equals(type)) {
            q.setAnswer(normalizeAnswer(type, q.getAnswer()));
            if (!"TRUE".equals(q.getAnswer()) && !"FALSE".equals(q.getAnswer())) {
                return pre + "判断题答案应为 对/错（或 TRUE/FALSE）";
            }
            q.setOptions(new ArrayList<>());
            return null;
        }
        if (q.getOptions().size() < 2) {
            return pre + "至少需要两个选项";
        }
        Set<String> keys = q.getOptions().stream()
                .map(o -> o.getKey().trim().toUpperCase()).collect(java.util.stream.Collectors.toSet());
        for (String k : q.getAnswer().split(",")) {
            if (!keys.contains(k.trim().toUpperCase())) {
                return pre + "答案 " + k + " 不在选项中";
            }
        }
        return null;
    }

    // ========== AI 识别 ==========

    private List<Question> aiExtract(String text) throws Exception {
        String system = "你是企业培训平台的出题专家。请严格按照 JSON 数组格式输出题目，不要输出任何其他文字。"
                + "每个题目对象包含字段：type(SINGLE/MULTIPLE/JUDGE)、content(题干)、options(选项数组，每题约4项，元素为{key,text}，判断题留空数组)、answer(正确答案，多选用逗号分隔如\"A,B\")、analysis(答案解析)。";
        String user = "请从以下文档内容中提取整理出题目，按上述 JSON 格式输出：\n" + text;
        String content = llmClient.chat(system, user, 0.3);
        return aiAssistantService.parseQuestionsJson(content);
    }

    private void bindSkill(Long questionId, Long skillId) {
        QuestionSkill qs = new QuestionSkill();
        qs.setQuestionId(questionId);
        qs.setSkillId(skillId);
        qs.setScoreWeight(1);
        questionSkillMapper.insert(qs);
    }

    private String extOf(String filename) {
        int i = filename.lastIndexOf('.');
        return i < 0 ? "" : filename.substring(i + 1).toLowerCase();
    }

    @Data
    @AllArgsConstructor
    public static class ImportResult {
        private int total;
        private int success;
        private List<String> errors;
    }
}
