package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.common.Result;
import com.qiye.entity.Dept;
import com.qiye.entity.Job;
import com.qiye.entity.JobSkill;
import com.qiye.mapper.CourseMapper;
import com.qiye.mapper.DeptMapper;
import com.qiye.mapper.JobMapper;
import com.qiye.mapper.JobSkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobController {

    private final JobMapper jobMapper;
    private final JobSkillMapper jobSkillMapper;
    private final DeptMapper deptMapper;
    private final CourseMapper courseMapper;

    @GetMapping("/list")
    public Result<List<Job>> list() {
        List<Job> jobs = jobMapper.selectList(new LambdaQueryWrapper<Job>().orderByAsc(Job::getId));
        Map<Long, Dept> deptMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Dept::getId, Function.identity()));
        for (Job j : jobs) {
            Dept d = deptMap.get(j.getDeptId());
            j.setDeptName(d == null ? "" : d.getName());
            j.setSkillCount(Math.toIntExact(jobSkillMapper.selectCount(
                    new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, j.getId()))));
        }
        return Result.ok(jobs);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> create(@RequestBody Job job) {
        checkUniqueName(job.getName(), null);
        job.setId(null);
        jobMapper.insert(job);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> update(@RequestBody Job job) {
        if (job.getId() == null) {
            throw new BizException("岗位ID不能为空");
        }
        checkUniqueName(job.getName(), job.getId());
        jobMapper.updateById(job);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        Long courseCount = courseMapper.selectCount(
                new LambdaQueryWrapper<com.qiye.entity.Course>().eq(com.qiye.entity.Course::getJobId, id));
        if (courseCount > 0) {
            throw new BizException("该岗位下存在课程，无法删除");
        }
        jobMapper.deleteById(id);   // job_skill / user_job 级联删除
        return Result.ok();
    }

    private void checkUniqueName(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            throw new BizException("岗位名称不能为空");
        }
        LambdaQueryWrapper<Job> qw = new LambdaQueryWrapper<Job>().eq(Job::getName, name);
        if (excludeId != null) {
            qw.ne(Job::getId, excludeId);
        }
        if (jobMapper.selectCount(qw) > 0) {
            throw new BizException("岗位名称已存在");
        }
    }
}
