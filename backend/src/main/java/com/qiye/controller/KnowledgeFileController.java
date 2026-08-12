package com.qiye.controller;

import com.qiye.common.PageResult;
import com.qiye.common.Result;
import com.qiye.entity.KnowledgeFile;
import com.qiye.security.LoginUser;
import com.qiye.security.SecurityUtils;
import com.qiye.service.KnowledgeService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 企业知识库
 */
@RestController
@RequestMapping("/knowledge")
@RequiredArgsConstructor
public class KnowledgeFileController {

    private final KnowledgeService knowledgeService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<KnowledgeFile> upload(@RequestParam("file") MultipartFile file,
                                        @RequestParam(required = false) Long deptId) {
        return Result.ok(knowledgeService.upload(file, deptId, SecurityUtils.getUserId()));
    }

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<PageResult<KnowledgeFile>> page(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size,
                                                  @RequestParam(required = false) String keyword) {
        LoginUser lu = SecurityUtils.getLoginUser();
        return Result.ok(knowledgeService.page(page, size, keyword,
                lu.getDeptId(), isAdminOrTrainer(lu)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<KnowledgeFile> detail(@PathVariable Long id) {
        return Result.ok(knowledgeService.detail(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok();
    }

    /** RAG 检索（员工不直接调用，通过 AI 助手 service 层使用） */
    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<List<Map<String, Object>>> search(@RequestBody SearchReq req) {
        LoginUser lu = SecurityUtils.getLoginUser();
        int topK = req.getTopK() == null ? 5 : req.getTopK();
        return Result.ok(knowledgeService.search(req.getQuery(),
                isAdminOrTrainer(lu), lu.getDeptId(), Math.min(topK, 20)));
    }

    private boolean isAdminOrTrainer(LoginUser lu) {
        return "ADMIN".equals(lu.getRoleCode()) || "TRAINER".equals(lu.getRoleCode());
    }

    @Data
    public static class SearchReq {
        @NotBlank(message = "查询内容不能为空")
        private String query;
        private Integer topK;
    }
}
