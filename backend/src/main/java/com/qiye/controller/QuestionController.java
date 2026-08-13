package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiye.common.BizException;
import com.qiye.common.PageResult;
import com.qiye.common.Result;
import com.qiye.entity.Question;
import com.qiye.entity.QuestionSkill;
import com.qiye.entity.Skill;
import com.qiye.mapper.QuestionMapper;
import com.qiye.mapper.QuestionSkillMapper;
import com.qiye.mapper.SkillMapper;
import com.qiye.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 题库管理
 */
@RestController
@RequestMapping("/question")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionMapper questionMapper;
    private final QuestionSkillMapper questionSkillMapper;
    private final SkillMapper skillMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<PageResult<Question>> page(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) Long skillId) {
        LambdaQueryWrapper<Question> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(Question::getContent, keyword);
        }
        if (StringUtils.hasText(type)) {
            qw.eq(Question::getType, type);
        }
        qw.orderByAsc(Question::getId);
        Page<Question> p = questionMapper.selectPage(new Page<>(page, size), qw);
        List<Question> list = p.getRecords();
        if (skillId != null) {
            List<Long> qids = questionSkillMapper.selectList(
                            new LambdaQueryWrapper<QuestionSkill>().eq(QuestionSkill::getSkillId, skillId))
                    .stream().map(QuestionSkill::getQuestionId).toList();
            list = list.stream().filter(q -> qids.contains(q.getId())).toList();
        }
        fillSkill(list);
        return Result.ok(new PageResult<>(p.getTotal(), list));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Question> detail(@PathVariable Long id) {
        Question q = questionMapper.selectById(id);
        if (q == null) {
            throw new BizException("题目不存在");
        }
        fillSkill(List.of(q));
        return Result.ok(q);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @Transactional
    public Result<Void> create(@RequestBody @Valid Question req) {
        if (req.getSkillIds() == null || req.getSkillIds().isEmpty()) {
            throw new BizException("请至少绑定一个考核技能（否则无法进入技能画像计算）");
        }
        req.setId(null);
        req.setSource("MANUAL");
        req.setCreatedBy(SecurityUtils.getUserId());
        questionMapper.insert(req);
        bindSkills(req.getId(), req.getSkillIds());
        return Result.ok();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @Transactional
    public Result<Void> update(@RequestBody @Valid Question req) {
        if (req.getId() == null) {
            throw new BizException("题目ID不能为空");
        }
        if (req.getSkillIds() == null || req.getSkillIds().isEmpty()) {
            throw new BizException("请至少绑定一个考核技能（否则无法进入技能画像计算）");
        }
        questionMapper.updateById(req);
        questionSkillMapper.delete(new LambdaQueryWrapper<QuestionSkill>()
                .eq(QuestionSkill::getQuestionId, req.getId()));
        bindSkills(req.getId(), req.getSkillIds());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Void> delete(@PathVariable Long id) {
        questionMapper.deleteById(id);   // question_skill 级联
        return Result.ok();
    }

    private void bindSkills(Long questionId, List<Long> skillIds) {
        for (Long skillId : skillIds) {
            QuestionSkill qs = new QuestionSkill();
            qs.setQuestionId(questionId);
            qs.setSkillId(skillId);
            qs.setScoreWeight(1);
            questionSkillMapper.insert(qs);
        }
    }

    private void fillSkill(List<Question> list) {
        if (list.isEmpty()) {
            return;
        }
        Map<Long, Skill> skillMap = skillMapper.selectList(null).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
        for (Question q : list) {
            List<QuestionSkill> skills = questionSkillMapper.selectList(
                    new LambdaQueryWrapper<QuestionSkill>().eq(QuestionSkill::getQuestionId, q.getId()));
            skills.forEach(s -> {
                Skill sk = skillMap.get(s.getSkillId());
                s.setSkillName(sk == null ? "" : sk.getName());
            });
            q.setSkills(skills);
            q.setSkillIds(skills.stream().map(QuestionSkill::getSkillId).toList());
            q.setSkillNames(skills.stream().map(QuestionSkill::getSkillName)
                    .collect(Collectors.joining("、")));
        }
    }
}
