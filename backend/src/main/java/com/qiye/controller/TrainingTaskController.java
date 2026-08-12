package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiye.common.PageResult;
import com.qiye.common.Result;
import com.qiye.entity.Course;
import com.qiye.entity.Job;
import com.qiye.entity.Skill;
import com.qiye.entity.SysUser;
import com.qiye.entity.TrainingTask;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.JobMapper;
import com.qiye.mapper.SkillMapper;
import com.qiye.mapper.SysUserMapper;
import com.qiye.mapper.TrainingTaskMapper;
import com.qiye.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 培训任务
 */
@RestController
@RequestMapping("/training-task")
@RequiredArgsConstructor
public class TrainingTaskController {

    private final TrainingTaskMapper taskMapper;
    private final JobMapper jobMapper;
    private final SkillMapper skillMapper;
    private final CourseMapper courseMapper;
    private final SysUserMapper sysUserMapper;

    /** 我的学习任务 */
    @GetMapping("/my")
    public Result<List<TrainingTask>> my() {
        List<TrainingTask> list = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>()
                        .eq(TrainingTask::getUserId, SecurityUtils.getUserId())
                        .orderByAsc(TrainingTask::getId));
        fillExt(list);
        return Result.ok(list);
    }

    /** 查看某员工任务 */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<List<TrainingTask>> byUser(@PathVariable Long userId) {
        List<TrainingTask> list = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>().eq(TrainingTask::getUserId, userId));
        fillExt(list);
        return Result.ok(list);
    }

    /** 全部任务分页 */
    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<PageResult<TrainingTask>> page(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) Long userId,
                                                 @RequestParam(required = false) String status) {
        LambdaQueryWrapper<TrainingTask> qw = new LambdaQueryWrapper<>();
        if (userId != null) {
            qw.eq(TrainingTask::getUserId, userId);
        }
        if (status != null && !status.isBlank()) {
            qw.eq(TrainingTask::getStatus, status);
        }
        qw.orderByAsc(TrainingTask::getId);
        Page<TrainingTask> p = taskMapper.selectPage(new Page<>(page, size), qw);
        fillExt(p.getRecords());
        return Result.ok(new PageResult<>(p.getTotal(), p.getRecords()));
    }

    private void fillExt(List<TrainingTask> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, Job> jobMap = jobMapper.selectList(null).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        Map<Long, Course> courseMap = courseMapper.selectList(null).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<Long, SysUser> userMap = sysUserMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        for (TrainingTask t : list) {
            Job j = jobMap.get(t.getJobId());
            Skill s = skillMap.get(t.getSkillId());
            Course c = courseMap.get(t.getCourseId());
            SysUser u = userMap.get(t.getUserId());
            t.setJobName(j == null ? "" : j.getName());
            t.setSkillName(s == null ? "" : s.getName());
            t.setCourseName(c == null ? "" : c.getName());
            t.setCourseLevel(c == null ? "" : c.getLevel());
            t.setUserName(u == null ? "" : u.getName());
        }
    }
}
