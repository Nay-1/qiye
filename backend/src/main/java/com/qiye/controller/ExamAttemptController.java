package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.common.Result;
import com.qiye.entity.Exam;
import com.qiye.entity.ExamAttempt;
import com.qiye.mapper.ExamMapper;
import com.qiye.mapper.SysUserMapper;
import com.qiye.security.SecurityUtils;
import com.qiye.service.ExamAttemptService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 在线考试答题接口
 */
@RestController
@RequestMapping("/exam-attempt")
@RequiredArgsConstructor
public class ExamAttemptController {

    private final ExamAttemptService attemptService;
    private final ExamMapper examMapper;
    private final SysUserMapper sysUserMapper;

    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestBody StartReq req) {
        return Result.ok(attemptService.start(SecurityUtils.getUserId(), req.getExamId()));
    }

    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestBody SubmitReq req) {
        return Result.ok(attemptService.submit(
                SecurityUtils.getUserId(), req.getAttemptId(), req.getAnswers()));
    }

    @GetMapping("/{attemptId}")
    public Result<Map<String, Object>> detail(@PathVariable Long attemptId) {
        // 员工仅可查看自己的答卷；管理员/培训负责人可查看任意
        String role = SecurityUtils.getRoleCode();
        if (!"ADMIN".equals(role) && !"TRAINER".equals(role)) {
            ExamAttempt a = attemptService.find(attemptId);
            if (a == null || !a.getUserId().equals(SecurityUtils.getUserId())) {
                throw new BizException("考试记录不存在");
            }
        }
        return Result.ok(attemptService.detail(attemptId));
    }

    /** 我的考试记录 */
    @GetMapping("/my")
    public Result<List<ExamAttempt>> my() {
        List<ExamAttempt> list = attemptService.listByUser(SecurityUtils.getUserId());
        fillExt(list);
        return Result.ok(list);
    }

    /** 管理员/培训负责人查看某员工考试记录 */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<List<ExamAttempt>> byUser(@PathVariable Long userId) {
        List<ExamAttempt> list = attemptService.listByUser(userId);
        fillExt(list);
        return Result.ok(list);
    }

    /** 管理员/培训负责人查看某考试的全部成绩 */
    @GetMapping("/exam/{examId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<List<ExamAttempt>> byExam(@PathVariable Long examId) {
        List<ExamAttempt> list = attemptService.listByExam(examId);
        fillExt(list);
        for (ExamAttempt a : list) {
            var u = sysUserMapper.selectById(a.getUserId());
            a.setUserName(u == null ? "" : u.getName());
        }
        return Result.ok(list);
    }

    private void fillExt(List<ExamAttempt> list) {
        for (ExamAttempt a : list) {
            Exam exam = examMapper.selectById(a.getExamId());
            if (exam != null) {
                a.setExamTitle(exam.getTitle());
                a.setPassScore(exam.getPassScore());
                a.setPassed(a.getTotalScore() != null && exam.getPassScore() != null
                        && a.getTotalScore().compareTo(java.math.BigDecimal.valueOf(exam.getPassScore())) >= 0);
            }
        }
    }

    @Data
    public static class StartReq {
        @NotNull(message = "考试ID不能为空")
        private Long examId;
    }

    @Data
    public static class SubmitReq {
        @NotNull(message = "考试记录ID不能为空")
        private Long attemptId;
        private List<ExamAttemptService.AnswerItem> answers;
    }
}
