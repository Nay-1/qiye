package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.entity.Exam;
import com.qiye.entity.ExamAnswer;
import com.qiye.entity.ExamAttempt;
import com.qiye.entity.ExamQuestion;
import com.qiye.entity.Question;
import com.qiye.mapper.ExamAnswerMapper;
import com.qiye.mapper.ExamAttemptMapper;
import com.qiye.mapper.ExamMapper;
import com.qiye.mapper.ExamQuestionMapper;
import com.qiye.mapper.QuestionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 在线考试：进入 → 答题 → 提交 → 自动评分
 */
@Service
@RequiredArgsConstructor
public class ExamAttemptService {

    private final ExamMapper examMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final ExamAttemptMapper attemptMapper;
    private final ExamAnswerMapper answerMapper;
    private final QuestionMapper questionMapper;
    private final UserSkillService userSkillService;

    /** 进入考试：创建考试记录，返回试卷（不含答案） */
    @Transactional
    public Map<String, Object> start(Long userId, Long examId) {
        Exam exam = examMapper.selectById(examId);
        if (exam == null) {
            throw new BizException("考试不存在");
        }
        if (!"PUBLISHED".equals(exam.getStatus())) {
            throw new BizException("该考试当前不可参加");
        }
        Long submitted = attemptMapper.selectCount(new LambdaQueryWrapper<ExamAttempt>()
                .eq(ExamAttempt::getUserId, userId)
                .eq(ExamAttempt::getExamId, examId)
                .eq(ExamAttempt::getStatus, "SUBMITTED"));
        if (submitted >= exam.getAttempts()) {
            throw new BizException("已达到该考试的最大次数（" + exam.getAttempts() + " 次）");
        }
        ExamAttempt a = new ExamAttempt();
        a.setUserId(userId);
        a.setExamId(examId);
        a.setAttemptNo(submitted.intValue() + 1);
        a.setStatus("IN_PROGRESS");
        a.setStartedAt(LocalDateTime.now());
        attemptMapper.insert(a);

        List<ExamQuestion> eqs = examQuestionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getExamId, examId)
                        .orderByAsc(ExamQuestion::getSort));
        List<Long> qids = eqs.stream().map(ExamQuestion::getQuestionId).toList();
        Map<Long, Question> qMap = qids.isEmpty() ? Map.of()
                : questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        Map<String, Object> paper = new HashMap<>();
        paper.put("attemptId", a.getId());
        paper.put("examId", examId);
        paper.put("title", exam.getTitle());
        paper.put("duration", exam.getDuration());
        paper.put("passScore", exam.getPassScore());
        paper.put("attemptNo", a.getAttemptNo());
        paper.put("questions", eqs.stream().map(eq -> {
            Question q = qMap.get(eq.getQuestionId());
            Map<String, Object> m = new HashMap<>();
            m.put("questionId", q.getId());
            m.put("type", q.getType());
            m.put("content", q.getContent());
            m.put("options", q.getOptions());
            m.put("score", eq.getScore());
            m.put("sort", eq.getSort());
            return m;
        }).toList());
        return paper;
    }

    /** 提交并自动评分 */
    @Transactional
    public Map<String, Object> submit(Long userId, Long attemptId, List<AnswerItem> answers) {
        ExamAttempt a = attemptMapper.selectById(attemptId);
        if (a == null || !a.getUserId().equals(userId)) {
            throw new BizException("考试记录不存在");
        }
        if ("SUBMITTED".equals(a.getStatus())) {
            throw new BizException("该考试已提交，不能重复提交");
        }
        Exam exam = examMapper.selectById(a.getExamId());

        Map<Long, Integer> scoreMap = examQuestionMapper.selectList(
                        new LambdaQueryWrapper<ExamQuestion>().eq(ExamQuestion::getExamId, a.getExamId()))
                .stream().collect(Collectors.toMap(ExamQuestion::getQuestionId, ExamQuestion::getScore));

        Map<Long, String> answerMap = answers == null ? Map.of()
                : answers.stream().collect(Collectors.toMap(AnswerItem::getQuestionId, AnswerItem::getUserAnswer, (x, y) -> x));

        List<Long> qids = scoreMap.keySet().stream().toList();
        Map<Long, Question> qMap = qids.isEmpty() ? Map.of()
                : questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        for (Long qid : scoreMap.keySet()) {
            Question q = qMap.get(qid);
            boolean correct = checkAnswer(q, answerMap.get(qid));
            BigDecimal score = correct ? BigDecimal.valueOf(scoreMap.get(qid)) : BigDecimal.ZERO;
            total = total.add(score);

            ExamAnswer ea = new ExamAnswer();
            ea.setAttemptId(attemptId);
            ea.setQuestionId(qid);
            ea.setUserAnswer(answerMap.get(qid));
            ea.setCorrect(correct);
            ea.setScore(score);
            answerMapper.insert(ea);
        }

        a.setTotalScore(total);
        a.setStatus("SUBMITTED");
        a.setSubmittedAt(LocalDateTime.now());
        attemptMapper.updateById(a);

        Map<String, Object> resp = new HashMap<>();
        resp.put("attemptId", attemptId);
        resp.put("totalScore", total);
        resp.put("passScore", exam == null ? 60 : exam.getPassScore());
        resp.put("passed", exam != null && total.compareTo(BigDecimal.valueOf(exam.getPassScore())) >= 0);

        // 提交后自动重算技能画像
        userSkillService.recalc(userId);
        return resp;
    }

    /** 我的考试记录 */
    public List<ExamAttempt> listByUser(Long userId) {
        return attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>()
                .eq(ExamAttempt::getUserId, userId)
                .orderByDesc(ExamAttempt::getId));
    }

    /** 只读取一条考试记录（用于归属校验，不暴露题目答案） */
    public ExamAttempt find(Long attemptId) {
        return attemptMapper.selectById(attemptId);
    }

    /** 某考试的全部已提交记录（管理端查看成绩） */
    public List<ExamAttempt> listByExam(Long examId) {
        return attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>()
                .eq(ExamAttempt::getExamId, examId)
                .eq(ExamAttempt::getStatus, "SUBMITTED")
                .orderByDesc(ExamAttempt::getSubmittedAt));
    }

    /** 成绩详情（含每题答案与解析） */
    public Map<String, Object> detail(Long attemptId) {
        ExamAttempt a = attemptMapper.selectById(attemptId);
        if (a == null) {
            throw new BizException("考试记录不存在");
        }
        Exam exam = examMapper.selectById(a.getExamId());
        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getAttemptId, attemptId));
        List<Long> qids = answers.stream().map(ExamAnswer::getQuestionId).toList();
        Map<Long, Question> qMap = qids.isEmpty() ? Map.of()
                : questionMapper.selectBatchIds(qids).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        answers.forEach(an -> an.setQuestion(qMap.get(an.getQuestionId())));

        Map<String, Object> resp = new HashMap<>();
        resp.put("attemptId", a.getId());
        resp.put("examTitle", exam == null ? "" : exam.getTitle());
        resp.put("totalScore", a.getTotalScore());
        resp.put("passScore", exam == null ? 60 : exam.getPassScore());
        resp.put("status", a.getStatus());
        resp.put("startedAt", a.getStartedAt());
        resp.put("submittedAt", a.getSubmittedAt());
        resp.put("passed", a.getTotalScore() != null
                && exam != null
                && a.getTotalScore().compareTo(BigDecimal.valueOf(exam.getPassScore())) >= 0);
        resp.put("answers", answers);
        return resp;
    }

    private boolean checkAnswer(Question q, String userAnswer) {
        if (q == null) {
            return false;
        }
        String correct = q.getAnswer() == null ? "" : q.getAnswer().trim();
        String answer = userAnswer == null ? "" : userAnswer.trim();
        if ("MULTIPLE".equals(q.getType())) {
            TreeSet<String> c = new TreeSet<>(List.of(correct.split(",")));
            TreeSet<String> a = new TreeSet<>(answer.isEmpty() ? List.of() : List.of(answer.split(",")));
            return c.equals(a);
        }
        return correct.equalsIgnoreCase(answer);
    }

    public static class AnswerItem {
        private Long questionId;
        private String userAnswer;

        public Long getQuestionId() { return questionId; }
        public void setQuestionId(Long questionId) { this.questionId = questionId; }
        public String getUserAnswer() { return userAnswer; }
        public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }
    }
}
