package com.qiye.controller;

import com.qiye.common.Result;
import com.qiye.entity.Question;
import com.qiye.security.SecurityUtils;
import com.qiye.service.AiAssistantService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 智能培训助手
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    /** 功能1：AI 培训问答（RAG） */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody ChatReq req) {
        return Result.ok(aiAssistantService.chat(req.getQuestion(), SecurityUtils.getLoginUser()));
    }

    /** 功能2：AI 生成试题（草稿，审核入库时强制绑定 question_skill） */
    @PostMapping("/generate-questions")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<List<Question>> generateQuestions(@RequestBody GenerateReq req) {
        return Result.ok(aiAssistantService.generateQuestions(req.getTopic(), req.getCount(), req.getType()));
    }

    /** 功能3：AI 学习建议（业务规则判定薄弱技能 + LLM 文案） */
    @PostMapping("/study-advice")
    public Result<List<Map<String, Object>>> studyAdvice() {
        var lu = SecurityUtils.getLoginUser();
        return Result.ok(aiAssistantService.studyAdvice(lu.getId(), lu.getName()));
    }

    @Data
    public static class ChatReq {
        @NotBlank(message = "问题不能为空")
        private String question;
    }

    @Data
    public static class GenerateReq {
        @NotBlank(message = "主题不能为空")
        private String topic;
        private Integer count;
        private String type;
    }
}
