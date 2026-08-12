package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.Result;
import com.qiye.entity.Role;
import com.qiye.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleMapper roleMapper;

    @GetMapping("/list")
    public Result<List<Role>> list() {
        return Result.ok(roleMapper.selectList(
                new LambdaQueryWrapper<Role>().orderByAsc(Role::getId)));
    }
}
