package com.qiye.controller;

import com.qiye.common.Result;
import com.qiye.security.SecurityUtils;
import com.qiye.service.UserSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 员工技能画像
 */
@RestController
@RequestMapping("/user-skill")
@RequiredArgsConstructor
public class UserSkillController {

    private final UserSkillService userSkillService;

    /** 我的画像（员工） */
    @GetMapping("/mine")
    public Result<Map<String, Object>> mine() {
        return Result.ok(userSkillService.profile(SecurityUtils.getUserId()));
    }

    /** 查看某员工画像（管理员/培训负责人） */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    public Result<Map<String, Object>> byUser(@PathVariable Long userId) {
        return Result.ok(userSkillService.profile(userId));
    }

    /** 手动重算我的画像（考试提交后已自动重算） */
    @PostMapping("/recalc")
    public Result<Void> recalc() {
        userSkillService.recalc(SecurityUtils.getUserId());
        return Result.ok();
    }
}
