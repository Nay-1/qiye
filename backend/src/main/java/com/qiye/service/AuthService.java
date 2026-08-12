package com.qiye.service;

import com.qiye.common.BizException;
import com.qiye.entity.Dept;
import com.qiye.entity.Role;
import com.qiye.entity.SysUser;
import com.qiye.mapper.DeptMapper;
import com.qiye.mapper.RoleMapper;
import com.qiye.mapper.SysUserMapper;
import com.qiye.security.JwtUtil;
import com.qiye.security.LoginUser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;
    private final DeptMapper deptMapper;

    public LoginResp login(String username, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            LoginUser lu = (LoginUser) auth.getPrincipal();
            String token = jwtUtil.createToken(lu);
            return new LoginResp(token, buildUserInfo(lu));
        } catch (BadCredentialsException e) {
            throw new BizException("用户名或密码错误");
        } catch (DisabledException e) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
    }

    public UserInfo currentUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BizException(401, "用户不存在");
        }
        LoginUser lu = new LoginUser();
        lu.setId(user.getId());
        lu.setUsername(user.getUsername());
        lu.setName(user.getName());
        lu.setDeptId(user.getDeptId());
        lu.setRoleId(user.getRoleId());
        return buildUserInfo(lu);
    }

    private UserInfo buildUserInfo(LoginUser lu) {
        UserInfo info = new UserInfo();
        info.setId(lu.getId());
        info.setUsername(lu.getUsername());
        info.setName(lu.getName());
        info.setDeptId(lu.getDeptId());
        info.setRoleId(lu.getRoleId());
        info.setRoleCode(lu.getRoleCode());
        Dept dept = lu.getDeptId() == null ? null : deptMapper.selectById(lu.getDeptId());
        Role role = roleMapper.selectById(lu.getRoleId());
        info.setDeptName(dept == null ? "" : dept.getName());
        info.setRoleName(role == null ? "" : role.getName());
        return info;
    }

    @Data
    public static class LoginResp {
        private String token;
        private UserInfo user;

        public LoginResp(String token, UserInfo user) {
            this.token = token;
            this.user = user;
        }
    }

    @Data
    public static class UserInfo {
        private Long id;
        private String username;
        private String name;
        private Long deptId;
        private String deptName;
        private Long roleId;
        private String roleName;
        private String roleCode;
    }
}
