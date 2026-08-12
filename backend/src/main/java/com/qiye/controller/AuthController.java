package com.qiye.controller;

import com.qiye.common.Result;
import com.qiye.security.SecurityUtils;
import com.qiye.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthService.LoginResp> login(@RequestBody LoginReq req) {
        return Result.ok(authService.login(req.getUsername(), req.getPassword()));
    }

    @GetMapping("/me")
    public Result<AuthService.UserInfo> me() {
        return Result.ok(authService.currentUser(SecurityUtils.getUserId()));
    }

    @Data
    public static class LoginReq {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
