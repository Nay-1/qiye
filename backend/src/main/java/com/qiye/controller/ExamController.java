package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiye.common.BizException;
import com.qiye.common.PageResult;
import com.qiye.common.Result;
import com.qiye.entity.Course;
import com.qiye.entity.Exam;
import com.qiye.entity.ExamQuestion;
import com.qiye.entity.Question;
import com.qiye.entity.QuestionSkill;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.ExamMapper;
import com.qiye.mapper.ExamQuestionMapper;
import com.qiye.mapper.QuestionMapper;
import com.qiye.mapper.QuestionSkillMapper;
import com.qiye.security.SecurityUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 考试管理（含组卷）
 */
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionSkillMapper questionSkillMapper;
    private final CourseMapper courseMapper;

    @GetMapping("/page")
    public Result<PageResult<Exam>> page(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String status) {
        LambdaQueryWrapper<Exam> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Exam::getTitle, keyword);
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Exam::getStatus, status);
        }
        qw.orderByAsc(Exam::getId);
        Page<Exam> p = examMapper.selectPage(new Page<>(page, size), qw);
        fillExt(p.getRecords());
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 管理端详情：含试卷题目（带答案） */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Exam> detail(@PathVariable Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BizException("考试不存在");
        }
        fillExt(List.of(exam));
        List<ExamQuestion> eqs = loadQuestions(id, true);
        exam.setQuestionList(eqs);
        return Result.ok(exam);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> create(@RequestBody Exam exam) {
        exam.setId(null);
        exam.setStatus("DRAFT");
        exam.setCreatedBy(SecurityUtils.getUserId());
        examMapper.insert(exam);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> update(@RequestBody Exam exam) {
        if (exam.getId() == null) {
            throw new BizException("考试ID不能为空");
        }
        examMapper.updateById(exam);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        examMapper.deleteById(id);
        return Result.ok();
    }

    /** 组卷（全量保存） */
    @PostMapping("/{id}/questions")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @Transactional
    public Result<Void> saveQuestions(@PathVariable Long id, @RequestBody SaveQuestionsReq req) {
        if (examMapper.selectById(id) == null) {
            throw new BizException("考试不存在");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BizException("请至少选择一道题目");
        }
        for (Item item : req.getItems()) {
            Long skillCount = questionSkillMapper.selectCount(
                    new LambdaQueryWrapper<QuestionSkill>().eq(QuestionSkill::getQuestionId, item.getQuestionId()));
            if (skillCount == 0) {
                Question q = questionMapper.selectById(item.getQuestionId());
                throw new BizException("题目「" + (q == null ? item.getQuestionId() : q.getContent()) + "」未绑定考核技能，无法组卷");
            }
        }
        examQuestionMapper.delete(new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, id));
        int sort = 1;
        for (Item item : req.getItems()) {
            ExamQuestion eq = new ExamQuestion();
            eq.setExamId(id);
            eq.setQuestionId(item.getQuestionId());
            eq.setScore(item.getScore() == null ? 10 : item.getScore());
            eq.setSort(sort++);
            examQuestionMapper.insert(eq);
        }
        return Result.ok();
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> publish(@PathVariable Long id) {
        Exam exam = requireExam(id);
        if (examQuestionMapper.selectCount(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getExamId, id)) == 0) {
            throw new BizException("考试未组卷，无法发布");
        }
        exam.setStatus("PUBLISHED");
        examMapper.updateById(exam);
        return Result.ok();
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> close(@PathVariable Long id) {
        Exam exam = requireExam(id);
        exam.setStatus("CLOSED");
        examMapper.updateById(exam);
        return Result.ok();
    }

    /** 加载试卷题目；withAnswer=true 返回题目含答案（管理端） */
    public List<ExamQuestion> loadQuestions(Long examId, boolean withAnswer) {
        List<ExamQuestion> eqs = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSort));
        List<Long> qids = eqs.stream().map(ExamQuestion::getQuestionId).toList();
        if (qids.isEmpty()) {
            return eqs;
        }
        Map<Long, Question> qMap = questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        eqs.forEach(eq -> {
            Question q = qMap.get(eq.getQuestionId());
            if (q != null) {
                if (!withAnswer) {
                    q.setAnswer(null);
                    q.setAnalysis(null);
                }
                eq.setQuestion(q);
            }
        });
        return eqs;
    }

    private void fillExt(List<Exam> exams) {
        if (exams.isEmpty()) {
            return;
        }
        Map<Long, Course> courseMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        for (Exam e : exams) {
            Course c = courseMap.get(e.getCourseId());
            e.setCourseName(c == null ? "" : c.getName());
            List<ExamQuestion> eqs = examQuestionMapper.selectList(
                    new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, e.getId()));
            e.setQuestionCount(eqs.size());
            e.setTotalScore(eqs.stream().mapToInt(ExamQuestion::getScore).sum());
        }
    }

    private Exam requireExam(Long id) {
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            throw new BizException("考试不存在");
        }
        return exam;
    }

    @Data
    public static class SaveQuestionsReq {
        private List<Item> items;
    }

    @Data
    public static class Item {
        @NotNull(message = "题目ID不能为空")
        private Long questionId;
        private Integer score;
    }
}
