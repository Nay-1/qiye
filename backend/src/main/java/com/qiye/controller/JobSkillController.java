package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.common.Result;
import com.qiye.entity.JobSkill;
import com.qiye.entity.Skill;
import com.qiye.mapper.JobSkillMapper;
import com.qiye.mapper.SkillMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 岗位技能配置：为岗位定义目标等级与权重（核心）
 */
@RestController
@RequestMapping("/job-skill")
@RequiredArgsConstructor
public class JobSkillController {

    private final JobSkillMapper jobSkillMapper;
    private final SkillMapper skillMapper;

    /** 查看某岗位的技能要求（登录可见，员工可看自己岗位要求） */
    @GetMapping("/job/{jobId}")
    public Result<List<JobSkill>> listByJob(@PathVariable Long jobId) {
        List<JobSkill> list = jobSkillMapper.selectList(
                new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, jobId));
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        list.forEach(js -> {
            Skill s = skillMap.get(js.getSkillId());
            js.setSkillName(s == null ? "" : s.getName());
        });
        return Result.ok(list);
    }

    /** 整体保存岗位技能（全量替换） */
    @PostMapping("/job/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> saveByJob(@PathVariable Long jobId, @RequestBody @Valid SaveReq req) {
        jobSkillMapper.delete(new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getJobId, jobId));
        int weight = 1;
        for (Item item : req.getItems()) {
            JobSkill js = new JobSkill();
            js.setJobId(jobId);
            js.setSkillId(item.getSkillId());
            js.setTargetLevel(item.getTargetLevel());
            js.setWeight(item.getWeight() == null ? weight++ : item.getWeight());
            jobSkillMapper.insert(js);
        }
        return Result.ok();
    }

    @Data
    public static class SaveReq {
        @NotNull(message = "技能列表不能为空")
        private List<Item> items;
    }

    @Data
    public static class Item {
        @NotNull(message = "技能ID不能为空")
        private Long skillId;
        @NotBlank(message = "目标等级不能为空")
        private String targetLevel;   // 初级/中级/高级
        private Integer weight;
    }
}
