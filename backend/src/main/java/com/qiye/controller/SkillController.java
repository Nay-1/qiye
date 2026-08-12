package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.common.Result;
import com.qiye.entity.JobSkill;
import com.qiye.entity.Skill;
import com.qiye.entity.UserSkill;
import com.qiye.mapper.JobSkillMapper;
import com.qiye.mapper.SkillMapper;
import com.qiye.mapper.UserSkillMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {

    private final SkillMapper skillMapper;
    private final JobSkillMapper jobSkillMapper;
    private final UserSkillMapper userSkillMapper;

    @GetMapping("/list")
    public Result<List<Skill>> list() {
        List<Skill> skills = skillMapper.selectList(new LambdaQueryWrapper<Skill>().orderByAsc(Skill::getId));
        for (Skill s : skills) {
            s.setJobCount(Math.toIntExact(jobSkillMapper.selectCount(
                    new LambdaQueryWrapper<JobSkill>().eq(JobSkill::getSkillId, s.getId()))));
        }
        return Result.ok(skills);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> create(@RequestBody Skill skill) {
        checkUniqueName(skill.getName(), null);
        skill.setId(null);
        skillMapper.insert(skill);
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> update(@RequestBody Skill skill) {
        if (skill.getId() == null) {
            throw new BizException("技能ID不能为空");
        }
        checkUniqueName(skill.getName(), skill.getId());
        skillMapper.updateById(skill);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        Long userSkillCount = userSkillMapper.selectCount(
                new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getSkillId, id));
        if (userSkillCount > 0) {
            throw new BizException("该技能已被员工画像引用，无法删除");
        }
        skillMapper.deleteById(id);   // job_skill/course_skill/question_skill 级联删除
        return Result.ok();
    }

    private void checkUniqueName(String name, Long excludeId) {
        if (!StringUtils.hasText(name)) {
            throw new BizException("技能名称不能为空");
        }
        LambdaQueryWrapper<Skill> qw = new LambdaQueryWrapper<Skill>().eq(Skill::getName, name);
        if (excludeId != null) {
            qw.ne(Skill::getId, excludeId);
        }
        if (skillMapper.selectCount(qw) > 0) {
            throw new BizException("技能名称已存在");
        }
    }
}
