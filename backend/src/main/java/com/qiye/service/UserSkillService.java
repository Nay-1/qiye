package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.entity.ExamAttempt;
import com.qiye.entity.ExamAnswer;
import com.qiye.entity.ExamQuestion;
import com.qiye.entity.JobSkill;
import com.qiye.entity.QuestionSkill;
import com.qiye.entity.Skill;
import com.qiye.entity.SysUser;
import com.qiye.entity.UserJob;
import com.qiye.entity.UserSkill;
import com.qiye.mapper.ExamAnswerMapper;
import com.qiye.mapper.ExamAttemptMapper;
import com.qiye.mapper.ExamQuestionMapper;
import com.qiye.mapper.JobSkillMapper;
import com.qiye.mapper.QuestionSkillMapper;
import com.qiye.mapper.SkillMapper;
import com.qiye.mapper.SysUserMapper;
import com.qiye.mapper.UserJobMapper;
import com.qiye.mapper.UserSkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 员工技能画像：按"最近一次提交"聚合题目-技能得分
 *
 * 得分口径（PRD 5.7）：
 *   技能得分 = 该技能最近一次考核提交的实得分 / 该技能最近一次考核提交的满分 × 100
 * 等级换算：score≥80 高级；60≤score<80 中级；score<60 初级
 * 达成率 = score / 目标等级达标线 × 100%（封顶 100）；达标线 初级60 中级60 高级80
 * 多岗位合并：target_level 取各岗位最高等级
 */
@Service
@RequiredArgsConstructor
public class UserSkillService {

    private static final BigDecimal B100 = BigDecimal.valueOf(100);
    private static final BigDecimal B80 = BigDecimal.valueOf(80);
    private static final BigDecimal B60 = BigDecimal.valueOf(60);

    private final UserSkillMapper userSkillMapper;
    private final UserJobMapper userJobMapper;
    private final JobSkillMapper jobSkillMapper;
    private final ExamAttemptMapper attemptMapper;
    private final ExamAnswerMapper answerMapper;
    private final QuestionSkillMapper questionSkillMapper;
    private final ExamQuestionMapper examQuestionMapper;
    private final SkillMapper skillMapper;
    private final SysUserMapper sysUserMapper;

    /** 重算员工画像（考试提交后自动触发） */
    @Transactional
    public void recalc(Long userId) {
        Map<Long, String> targetMap = computeTargetLevels(userId);

        // 已提交的考试记录
        List<ExamAttempt> attempts = attemptMapper.selectList(new LambdaQueryWrapper<ExamAttempt>()
                .eq(ExamAttempt::getUserId, userId)
                .eq(ExamAttempt::getStatus, "SUBMITTED"));
        if (attempts.isEmpty()) {
            return;
        }
        Set<Long> attemptIds = attempts.stream().map(ExamAttempt::getId).collect(Collectors.toSet());
        Map<Long, Long> attemptExamMap = attempts.stream()
                .collect(Collectors.toMap(ExamAttempt::getId, ExamAttempt::getExamId));

        // 题目 → 技能映射（一题可绑多技能）
        Map<Long, List<Long>> qSkills = questionSkillMapper.selectList(null).stream()
                .collect(Collectors.groupingBy(QuestionSkill::getQuestionId,
                        Collectors.mapping(QuestionSkill::getSkillId, Collectors.toList())));

        // 满分映射 examId:questionId → score
        Map<String, BigDecimal> fullMap = new HashMap<>();
        for (ExamQuestion eq : examQuestionMapper.selectList(null)) {
            fullMap.put(eq.getExamId() + ":" + eq.getQuestionId(), BigDecimal.valueOf(eq.getScore()));
        }

        // skill → attemptId → [得分合计, 满分合计]
        Map<Long, Map<Long, BigDecimal[]>> agg = new HashMap<>();
        List<ExamAnswer> answers = answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                .in(ExamAnswer::getAttemptId, attemptIds));
        for (ExamAnswer a : answers) {
            List<Long> skillIds = qSkills.get(a.getQuestionId());
            if (skillIds == null) {
                continue;
            }
            Long examId = attemptExamMap.get(a.getAttemptId());
            if (examId == null) {
                continue;
            }
            BigDecimal full = fullMap.getOrDefault(examId + ":" + a.getQuestionId(), BigDecimal.ZERO);
            BigDecimal score = a.getScore() == null ? BigDecimal.ZERO : a.getScore();
            for (Long sid : skillIds) {
                BigDecimal[] arr = agg.computeIfAbsent(sid, k -> new HashMap<>())
                        .computeIfAbsent(a.getAttemptId(), k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
                arr[0] = arr[0].add(score);
                arr[1] = arr[1].add(full);
            }
        }

        for (Map.Entry<Long, Map<Long, BigDecimal[]>> entry : agg.entrySet()) {
            Long skillId = entry.getKey();
            Map.Entry<Long, BigDecimal[]> best = entry.getValue().entrySet().stream()
                    .max(Comparator.comparingLong(Map.Entry::getKey))   // 最近一次提交
                    .orElseThrow();
            BigDecimal full = best.getValue()[1];
            BigDecimal score = full.signum() == 0 ? BigDecimal.ZERO
                    : best.getValue()[0].multiply(B100).divide(full, 1, RoundingMode.HALF_UP);
            upsertSkill(userId, skillId, levelOf(score), targetMap.get(skillId), score);
        }
    }

    /** 员工画像（含达成率与薄弱识别） */
    public Map<String, Object> profile(Long userId) {
        List<UserSkill> skills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId));
        fillSkillName(skills);
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        List<Map<String, Object>> list = new ArrayList<>();
        long weakCount = 0;
        for (UserSkill us : skills) {
            Skill sk = skillMap.get(us.getSkillId());
            BigDecimal rate = rateOf(us.getScore(), us.getTargetLevel());
            boolean reached = rate != null && rate.compareTo(B100) >= 0;
            boolean weak = !reached && us.getScore() != null;
            if (weak) {
                weakCount++;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("skillId", us.getSkillId());
            m.put("skillName", sk == null ? "" : sk.getName());
            m.put("currentLevel", us.getCurrentLevel());
            m.put("targetLevel", us.getTargetLevel() == null ? "--" : us.getTargetLevel());
            m.put("score", us.getScore());
            m.put("rate", rate);
            m.put("reached", reached);
            m.put("weak", weak);
            m.put("updatedAt", us.getUpdatedAt());
            list.add(m);
        }
        SysUser user = sysUserMapper.selectById(userId);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("userId", userId);
        resp.put("userName", user == null ? "" : user.getName());
        resp.put("skills", list);
        resp.put("weakCount", weakCount);
        resp.put("skillCount", list.size());

        // 补充：岗位要求但尚未考核的技能
        Map<Long, String> targetMap = computeTargetLevels(userId);
        Set<Long> existing = list.stream()
                .map(m -> (Long) m.get("skillId"))
                .collect(Collectors.toSet());
        for (Map.Entry<Long, String> e : targetMap.entrySet()) {
            if (existing.contains(e.getKey())) {
                continue;
            }
            Skill sk = skillMap.get(e.getKey());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("skillId", e.getKey());
            m.put("skillName", sk == null ? "" : sk.getName());
            m.put("currentLevel", "未考核");
            m.put("targetLevel", e.getValue());
            m.put("score", null);
            m.put("rate", null);
            m.put("reached", false);
            m.put("weak", false);
            m.put("updatedAt", null);
            list.add(m);
        }
        return resp;
    }

    /** 达成率：score / 达标线 × 100，封顶 100 */
    public BigDecimal rateOf(BigDecimal score, String targetLevel) {
        if (score == null) {
            return null;
        }
        BigDecimal line = lineOf(targetLevel);
        if (line == null) {
            return null;
        }
        BigDecimal rate = score.multiply(B100).divide(line, 1, RoundingMode.HALF_UP);
        return rate.min(B100);
    }

    /** 多岗位 target 合并：取最高等级 */
    private Map<Long, String> computeTargetLevels(Long userId) {
        Map<Long, String> map = new HashMap<>();
        List<UserJob> userJobs = userJobMapper.selectList(
                new LambdaQueryWrapper<UserJob>().eq(UserJob::getUserId, userId));
        for (UserJob uj : userJobs) {
            List<JobSkill> jsList = jobSkillMapper.selectList(
                    new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, uj.getJobId()));
            for (JobSkill js : jsList) {
                map.merge(js.getSkillId(), js.getTargetLevel(), this::higherLevel);
            }
        }
        return map;
    }

    private String higherLevel(String a, String b) {
        return order(a) >= order(b) ? a : b;
    }

    private int order(String level) {
        return switch (level == null ? "" : level) {
            case "初级" -> 1;
            case "中级" -> 2;
            case "高级" -> 3;
            default -> 0;
        };
    }

    private BigDecimal lineOf(String targetLevel) {
        return switch (targetLevel == null ? "" : targetLevel) {
            case "初级" -> B60;
            case "中级" -> B60;
            case "高级" -> B80;
            default -> null;
        };
    }

    private String levelOf(BigDecimal score) {
        if (score.compareTo(B80) >= 0) {
            return "高级";
        }
        if (score.compareTo(B60) >= 0) {
            return "中级";
        }
        return "初级";
    }

    private void upsertSkill(Long userId, Long skillId, String level, String target, BigDecimal score) {
        UserSkill us = userSkillMapper.selectOne(new LambdaQueryWrapper<UserSkill>()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getSkillId, skillId));
        if (us == null) {
            us = new UserSkill();
            us.setUserId(userId);
            us.setSkillId(skillId);
            us.setCurrentLevel(level);
            us.setTargetLevel(target);
            us.setScore(score);
            us.setUpdatedAt(java.time.LocalDateTime.now());
            userSkillMapper.insert(us);
        } else {
            us.setCurrentLevel(level);
            us.setTargetLevel(target);
            us.setScore(score);
            us.setUpdatedAt(java.time.LocalDateTime.now());
            userSkillMapper.updateById(us);
        }
    }

    private void fillSkillName(List<UserSkill> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        list.forEach(us -> {
            Skill s = skillMap.get(us.getSkillId());
            us.setSkillName(s == null ? "" : s.getName());
        });
    }
}
