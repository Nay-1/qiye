package com.qiye.controller;

import com.qiye.common.Result;
import com.qiye.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 数据统计分析（管理员/培训负责人）
 */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(statsService.overview());
    }

    @GetMapping("/study")
    public Result<Map<String, Object>> study() {
        return Result.ok(statsService.study());
    }

    @GetMapping("/exam")
    public Result<Map<String, Object>> exam() {
        return Result.ok(statsService.exam());
    }

    @GetMapping("/skill")
    public Result<Map<String, Object>> skill() {
        return Result.ok(statsService.skill());
    }

    @GetMapping("/dept")
    public Result<List<Map<String, Object>>> dept() {
        return Result.ok(statsService.dept());
    }

    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> ranking() {
        return Result.ok(statsService.ranking());
    }
}
