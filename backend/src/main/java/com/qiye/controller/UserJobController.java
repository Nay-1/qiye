package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.Result;
import com.qiye.entity.Job;
import com.qiye.entity.UserJob;
import com.qiye.mapper.JobMapper;
import com.qiye.mapper.UserJobMapper;
import com.qiye.security.SecurityUtils;
import com.qiye.service.TrainingTaskService;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 员工岗位分配
 */
@RestController
@RequestMapping("/user-job")
@RequiredArgsConstructor
public class UserJobController {

    private final UserJobMapper userJobMapper;
    private final JobMapper jobMapper;
    private final TrainingTaskService trainingTaskService;

    /** 员工查看自己的岗位 */
    @GetMapping("/mine")
    public Result<List<UserJob>> mine() {
        return listByUser(SecurityUtils.getUserId());
    }

    /** 管理员/培训负责人查看某员工岗位 */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<List<UserJob>> listByUser(@PathVariable Long userId) {
        List<UserJob> list = userJobMapper.selectList(
                new LambdaQueryWrapper<UserJob>().eq(UserJob::getUserId, userId));
        fillJobName(list);
        return Result.ok(list);
    }

    /** 分配岗位（全量替换，并同步生成培训任务） */
    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @Transactional
    public Result<Void> assign(@RequestBody AssignReq req) {
        userJobMapper.delete(new LambdaQueryWrapper<UserJob>().eq(UserJob::getUserId, req.getUserId()));
        if (req.getJobIds() != null) {
            for (Long jobId : req.getJobIds()) {
                UserJob uj = new UserJob();
                uj.setUserId(req.getUserId());
                uj.setJobId(jobId);
                uj.setIsPrimary(jobId.equals(req.getPrimaryJobId()));
                userJobMapper.insert(uj);
            }
        }
        // 岗位变化 → 同步任务
        trainingTaskService.syncForUser(req.getUserId());
        return Result.ok();
    }

    private void fillJobName(List<UserJob> list) {
        Map<Long, Job> jobMap = jobMapper.selectList(null).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        list.forEach(uj -> {
            Job j = jobMap.get(uj.getJobId());
            uj.setJobName(j == null ? "" : j.getName());
        });
    }

    @Data
    public static class AssignReq {
        @NotNull(message = "用户ID不能为空")
        private Long userId;
        @NotNull(message = "主岗位不能为空")
        private Long primaryJobId;
        private List<Long> jobIds;
    }
}
