package com.qiye.controller;

import com.qiye.common.PageResult;
import com.qiye.common.Result;
import com.qiye.entity.SysUser;
import com.qiye.service.SysUserService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SysUserController {

    private final SysUserService sysUserService;

    @GetMapping("/page")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(sysUserService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<SysUser> detail(@PathVariable Long id) {
        return Result.ok(sysUserService.detail(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody CreateReq req) {
        SysUser u = new SysUser();
        u.setUsername(req.getUsername());
        u.setName(req.getName());
        u.setDeptId(req.getDeptId());
        u.setRoleId(req.getRoleId());
        sysUserService.create(u, req.getPassword());
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody SysUser user) {
        sysUserService.update(user);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody ResetPwdReq req) {
        sysUserService.resetPassword(id, req.getPassword());
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        sysUserService.toggleStatus(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.ok();
    }

    @Data
    public static class CreateReq {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "姓名不能为空")
        private String name;
        @NotBlank(message = "密码不能为空")
        private String password;
        private Long deptId;
        private Long roleId;
    }

    @Data
    public static class ResetPwdReq {
        @NotBlank(message = "新密码不能为空")
        private String password;
    }
}
