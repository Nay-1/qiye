package com.qiye.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.entity.Role;
import com.qiye.entity.SysUser;
import com.qiye.mapper.RoleMapper;
import com.qiye.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从数据库加载用户
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper sysUserMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        Role role = roleMapper.selectById(user.getRoleId());
        LoginUser lu = new LoginUser();
        lu.setId(user.getId());
        lu.setUsername(user.getUsername());
        lu.setPassword(user.getPassword());
        lu.setName(user.getName());
        lu.setDeptId(user.getDeptId());
        lu.setRoleId(user.getRoleId());
        lu.setRoleCode(role == null ? "EMPLOYEE" : role.getCode());
        lu.setEnabled(user.getEnabled());
        return lu;
    }
}
