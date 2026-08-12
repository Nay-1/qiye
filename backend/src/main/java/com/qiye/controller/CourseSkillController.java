package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.Result;
import com.qiye.entity.CourseSkill;
import com.qiye.entity.Skill;
import com.qiye.mapper.CourseSkillMapper;
import com.qiye.mapper.SkillMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程技能关联（声明本课程培养哪个技能）
 */
@RestController
@RequestMapping("/course-skill")
@RequiredArgsConstructor
public class CourseSkillController {

    private final CourseSkillMapper courseSkillMapper;
    private final SkillMapper skillMapper;

    @GetMapping("/course/{courseId}")
    public Result<List<CourseSkill>> listByCourse(@PathVariable Long courseId) {
        List<CourseSkill> list = courseSkillMapper.selectList(
                new LambdaQueryWrapper<CourseSkill>().eq(CourseSkill::getCourseId, courseId));
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        list.forEach(cs -> {
            Skill s = skillMap.get(cs.getSkillId());
            cs.setSkillName(s == null ? "" : s.getName());
        });
        return Result.ok(list);
    }

    @PostMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> saveByCourse(@PathVariable Long courseId, @RequestBody @Valid SaveReq req) {
        courseSkillMapper.delete(new LambdaQueryWrapper<CourseSkill>()
                .eq(CourseSkill::getCourseId, courseId));
        if (req.getItems() != null) {
            for (Item item : req.getItems()) {
                CourseSkill cs = new CourseSkill();
                cs.setCourseId(courseId);
                cs.setSkillId(item.getSkillId());
                cs.setWeight(item.getWeight() == null ? 1 : item.getWeight());
                cs.setRequired(Boolean.TRUE.equals(item.getRequired()));
                courseSkillMapper.insert(cs);
            }
        }
        return Result.ok();
    }

    @Data
    public static class SaveReq {
        private List<Item> items;
    }

    @Data
    public static class Item {
        @NotNull(message = "技能ID不能为空")
        private Long skillId;
        private Integer weight;
        private Boolean required;
    }
}
