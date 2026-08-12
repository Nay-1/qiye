package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.entity.Course;
import com.qiye.entity.CourseChapter;
import com.qiye.entity.Dept;
import com.qiye.entity.Exam;
import com.qiye.entity.ExamAttempt;
import com.qiye.entity.KnowledgeFile;
import com.qiye.entity.Skill;
import com.qiye.entity.StudyRecord;
import com.qiye.entity.SysUser;
import com.qiye.entity.UserSkill;
import com.qiye.mapper.CourseChapterMapper;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.DeptMapper;
import com.qiye.mapper.ExamAttemptMapper;
import com.qiye.mapper.ExamMapper;
import com.qiye.mapper.KnowledgeFileMapper;
import com.qiye.mapper.SkillMapper;
import com.qiye.mapper.StudyRecordMapper;
import com.qiye.mapper.SysUserMapper;
import com.qiye.mapper.UserSkillMapper;
import com.qiye.entity.Role;
import com.qiye.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 数据统计分析（Java 聚合，供 ECharts 使用）
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final DeptMapper deptMapper;
    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final StudyRecordMapper studyRecordMapper;
    private final ExamMapper examMapper;
    private final ExamAttemptMapper attemptMapper;
    private final UserSkillMapper userSkillMapper;
    private final SkillMapper skillMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;

    /** 首页概览卡片 */
    public Map<String, Object> overview() {
        long empCount = countEmployees();
        long courseCount = courseMapper.selectCount(null);
        long examCount = examMapper.selectCount(new LambdaQueryWrapper<Exam>().eq(Exam::getStatus, "PUBLISHED"));
        long docCount = knowledgeFileMapper.selectCount(null);

        List<StudyRecord> records = studyRecordMapper.selectList(null);
        long studyUsers = records.stream().map(StudyRecord::getUserId).distinct().count();
        long totalSeconds = records.stream().mapToLong(StudyRecord::getStudyDuration).sum();

        List<ExamAttempt> attempts = attemptMapper.selectList(
                new LambdaQueryWrapper<ExamAttempt>().eq(ExamAttempt::getStatus, "SUBMITTED"));
        double avgScore = attempts.stream().mapToDouble(a -> a.getTotalScore() == null ? 0 : a.getTotalScore().doubleValue())
                .average().orElse(0);
        long passCount = attempts.stream().filter(this::isPassed).count();

        List<UserSkill> us = userSkillMapper.selectList(null);
        long weakCount = us.stream().filter(u -> u.getScore() != null && u.getTargetLevel() != null
                && !isReached(u)).count();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userCount", empCount);
        m.put("courseCount", courseCount);
        m.put("examCount", examCount);
        m.put("docCount", docCount);
        m.put("studyUserCount", studyUsers);
        m.put("studyRate", pct(studyUsers, empCount));
        m.put("totalStudyHours", round(totalSeconds / 3600.0));
        m.put("avgScore", round1(avgScore));
        m.put("passRate", pct(passCount, attempts.size()));
        m.put("attemptCount", attempts.size());
        m.put("weakSkillCount", weakCount);
        return m;
    }

    /** 学习情况：按课程学习人数 / 完成率 */
    public Map<String, Object> study() {
        List<Course> courses = courseMapper.selectList(null);
        List<StudyRecord> records = studyRecordMapper.selectList(null);
        Map<Long, List<StudyRecord>> byCourse = records.stream()
                .collect(Collectors.groupingBy(StudyRecord::getCourseId));

        List<Map<String, Object>> courseStats = new ArrayList<>();
        for (Course c : courses) {
            Long chapterTotal = chapterMapper.selectCount(
                    new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, c.getId()));
            List<StudyRecord> recs = byCourse.getOrDefault(c.getId(), List.of());
            long studyUsers = recs.stream().map(StudyRecord::getUserId).distinct().count();
            long completeUsers = 0;
            if (chapterTotal > 0) {
                // 课程章节全部完成的用户数
                Map<Long, Long> donePerUser = recs.stream()
                        .filter(r -> "COMPLETED".equals(r.getStatus()))
                        .collect(Collectors.groupingBy(StudyRecord::getUserId, Collectors.counting()));
                completeUsers = donePerUser.values().stream().filter(v -> v >= chapterTotal).count();
            }
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("courseName", c.getName());
            cm.put("chapterTotal", chapterTotal);
            cm.put("studyUsers", studyUsers);
            cm.put("completeUsers", completeUsers);
            cm.put("completeRate", pct(completeUsers, studyUsers));
            courseStats.add(cm);
        }
        long totalSeconds = records.stream().mapToLong(StudyRecord::getStudyDuration).sum();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courses", courseStats);
        m.put("totalStudyHours", round(totalSeconds / 3600.0));
        m.put("totalRecords", records.size());
        return m;
    }

    /** 考试分析：平均分/通过率/成绩趋势（最近7次提交） */
    public Map<String, Object> exam() {
        List<ExamAttempt> attempts = attemptMapper.selectList(
                new LambdaQueryWrapper<ExamAttempt>().eq(ExamAttempt::getStatus, "SUBMITTED"));
        double avgScore = attempts.stream()
                .mapToDouble(a -> a.getTotalScore() == null ? 0 : a.getTotalScore().doubleValue())
                .average().orElse(0);
        long passCount = attempts.stream().filter(this::isPassed).count();

        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        attempts.stream()
                .filter(a -> a.getSubmittedAt() != null)
                .sorted(Comparator.comparing(ExamAttempt::getSubmittedAt))
                .skip(Math.max(0, attempts.size() - 7L))
                .forEach(a -> {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("label", a.getSubmittedAt().format(fmt));
                    t.put("score", a.getTotalScore() == null ? 0 : a.getTotalScore());
                    t.put("examId", a.getExamId());
                    t.put("attemptId", a.getId());
                    trend.add(t);
                });

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("avgScore", round1(avgScore));
        m.put("passRate", pct(passCount, attempts.size()));
        m.put("attemptCount", attempts.size());
        m.put("passCount", passCount);
        m.put("trend", trend);
        return m;
    }

    /** 技能达成：达标/薄弱分布 + 薄弱技能 Top */
    public Map<String, Object> skill() {
        List<UserSkill> us = userSkillMapper.selectList(null);
        long reached = us.stream().filter(this::isReached).count();
        long weak = us.stream().filter(u -> u.getScore() != null && !isReached(u)).count();
        long assessed = reached + weak;

        // 薄弱技能按技能聚合 Top
        Map<Long, Long> weakBySkill = us.stream()
                .filter(u -> u.getScore() != null && !isReached(u))
                .collect(Collectors.groupingBy(UserSkill::getSkillId, Collectors.counting()));
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        List<Map<String, Object>> weakTop = weakBySkill.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    Skill s = skillMap.get(e.getKey());
                    m.put("skillName", s == null ? "" : s.getName());
                    m.put("count", e.getValue());
                    return m;
                }).toList();

        // 等级分布
        Map<String, Long> levelDist = us.stream().filter(u -> u.getCurrentLevel() != null)
                .collect(Collectors.groupingBy(UserSkill::getCurrentLevel, Collectors.counting()));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("assessedCount", assessed);
        m.put("reachedCount", reached);
        m.put("weakCount", weak);
        m.put("reachedRate", pct(reached, assessed));
        m.put("weakTop", weakTop);
        m.put("levelDist", levelDist);
        return m;
    }

    /** 部门培训完成率 */
    public List<Map<String, Object>> dept() {
        List<Dept> depts = deptMapper.selectList(null);
        List<SysUser> users = sysUserMapper.selectList(null);
        List<StudyRecord> records = studyRecordMapper.selectList(null);
        Map<Long, Long> recordUsers = records.stream()
                .collect(Collectors.groupingBy(StudyRecord::getUserId, Collectors.counting()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Dept d : depts) {
            List<SysUser> emp = users.stream().filter(u -> d.getId().equals(u.getDeptId())
                    && isEmployee(u)).toList();
            long studyUsers = emp.stream().filter(u -> recordUsers.containsKey(u.getId())).count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deptName", d.getName());
            m.put("empCount", emp.size());
            m.put("studyUsers", studyUsers);
            m.put("studyRate", pct(studyUsers, emp.size()));
            list.add(m);
        }
        return list;
    }

    /** 员工技能得分排名（有画像的用户） */
    public List<Map<String, Object>> ranking() {
        List<UserSkill> us = userSkillMapper.selectList(null);
        Map<Long, List<UserSkill>> byUser = us.stream()
                .filter(u -> u.getScore() != null)
                .collect(Collectors.groupingBy(UserSkill::getUserId));
        Map<Long, SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        Map<Long, Dept> deptMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Dept::getId, Function.identity()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<Long, List<UserSkill>> e : byUser.entrySet()) {
            double avg = e.getValue().stream()
                    .mapToDouble(u -> u.getScore().doubleValue()).average().orElse(0);
            SysUser u = userMap.get(e.getKey());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", e.getKey());
            m.put("userName", u == null ? "" : u.getName());
            Dept d = u == null ? null : deptMap.get(u.getDeptId());
            m.put("deptName", d == null ? "" : d.getName());
            m.put("avgScore", round1(avg));
            m.put("skillCount", e.getValue().size());
            list.add(m);
        }
        list.sort(Comparator.comparingDouble(
                (Map<String, Object> m) -> ((Number) m.get("avgScore")).doubleValue()).reversed());
        return list.stream().limit(10).toList();
    }

    private boolean isPassed(ExamAttempt a) {
        Exam exam = examMapper.selectById(a.getExamId());
        if (exam == null || exam.getPassScore() == null || a.getTotalScore() == null) {
            return false;
        }
        return a.getTotalScore().compareTo(BigDecimal.valueOf(exam.getPassScore())) >= 0;
    }

    private boolean isReached(UserSkill us) {
        if (us.getScore() == null || us.getTargetLevel() == null) {
            return false;
        }
        BigDecimal line = switch (us.getTargetLevel()) {
            case "初级" -> BigDecimal.valueOf(60);
            case "中级" -> BigDecimal.valueOf(60);
            case "高级" -> BigDecimal.valueOf(80);
            default -> null;
        };
        return line != null && us.getScore().compareTo(line) >= 0;
    }

    private long countEmployees() {
        Long roleId = roleMapper.selectList(new LambdaQueryWrapper<Role>().eq(Role::getCode, "EMPLOYEE"))
                .stream().findFirst().map(Role::getId).orElse(null);
        if (roleId == null) {
            return 0;
        }
        return sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, roleId));
    }

    private boolean isEmployee(SysUser u) {
        Role role = roleMapper.selectById(u.getRoleId());
        return role != null && "EMPLOYEE".equals(role.getCode());
    }

    private double pct(long a, long b) {
        if (b == 0) {
            return 0;
        }
        return round1(a * 100.0 / b);
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private double round1(double v) {
        return BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
