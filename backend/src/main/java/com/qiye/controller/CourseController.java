package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiye.common.BizException;
import com.qiye.common.PageResult;
import com.qiye.common.Result;
import com.qiye.entity.Course;
import com.qiye.entity.CourseChapter;
import com.qiye.entity.CourseSkill;
import com.qiye.entity.Job;
import com.qiye.entity.Skill;
import com.qiye.entity.StudyRecord;
import com.qiye.mapper.CourseChapterMapper;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.CourseSkillMapper;
import com.qiye.mapper.JobMapper;
import com.qiye.mapper.SkillMapper;
import com.qiye.mapper.StudyRecordMapper;
import com.qiye.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseMapper courseMapper;
    private final CourseChapterMapper chapterMapper;
    private final CourseSkillMapper courseSkillMapper;
    private final JobMapper jobMapper;
    private final SkillMapper skillMapper;
    private final StudyRecordMapper studyRecordMapper;

    @GetMapping("/page")
    public Result<PageResult<Course>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) Long jobId) {
        LambdaQueryWrapper<Course> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Course::getName, keyword);
        }
        if (StringUtils.hasText(category)) {
            qw.eq(Course::getCategory, category);
        }
        if (jobId != null) {
            qw.eq(Course::getJobId, jobId);
        }
        qw.orderByAsc(Course::getId);
        Page<Course> p = courseMapper.selectPage(new Page<>(page, size), qw);
        fillExt(p.getRecords(), false);
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    /** 课程详情：含章节 + 技能关联 + 当前用户章节学习状态 */
    @GetMapping("/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BizException("课程不存在");
        }
        fillExt(List.of(course), true);
        // 当前用户学习记录挂到章节上
        Long userId = SecurityUtils.getUserId();
        List<CourseChapter> chapters = course.getChapters();
        List<StudyRecord> records = studyRecordMapper.selectList(
                new LambdaQueryWrapper<StudyRecord>().eq(StudyRecord::getUserId, userId)
                        .eq(StudyRecord::getCourseId, id));
        Map<Long, StudyRecord> recMap = records.stream()
                .collect(Collectors.toMap(StudyRecord::getChapterId, Function.identity()));
        if (chapters != null) {
            for (CourseChapter ch : chapters) {
                ch.setStudyRecord(recMap.get(ch.getId()));
            }
        }
        return Result.ok(course);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> create(@RequestBody Course course) {
        course.setId(null);
        courseMapper.insert(course);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> update(@RequestBody Course course) {
        if (course.getId() == null) {
            throw new BizException("课程ID不能为空");
        }
        courseMapper.updateById(course);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        courseMapper.deleteById(id);   // chapter/course_skill/task 级联
        return Result.ok();
    }

    private void fillExt(List<Course> courses, boolean full) {
        if (courses.isEmpty()) {
            return;
        }
        Map<Long, Job> jobMap = jobMapper.selectList(null).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        for (Course c : courses) {
            Job j = jobMap.get(c.getJobId());
            c.setJobName(j == null ? "" : j.getName());
            c.setChapterCount(Math.toIntExact(chapterMapper.selectCount(
                    new LambdaQueryWrapper<CourseChapter>().eq(CourseChapter::getCourseId, c.getId()))));
            if (full) {
                c.setChapters(chapterMapper.selectList(
                        new LambdaQueryWrapper<CourseChapter>()
                                .eq(CourseChapter::getCourseId, c.getId())
                                .orderByAsc(CourseChapter::getSeq)));
                List<CourseSkill> skills = courseSkillMapper.selectList(
                        new LambdaQueryWrapper<CourseSkill>().eq(CourseSkill::getCourseId, c.getId()));
                skills.forEach(s -> {
                    Skill sk = skillMap.get(s.getSkillId());
                    s.setSkillName(sk == null ? "" : sk.getName());
                });
                c.setSkills(skills);
            }
        }
    }
}
