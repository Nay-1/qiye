package com.qiye.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiye.common.BizException;
import com.qiye.common.PageResult;
import com.qiye.entity.Dept;
import com.qiye.entity.Role;
import com.qiye.entity.SysUser;
import com.qiye.mapper.DeptMapper;
import com.qiye.mapper.RoleMapper;
import com.qiye.mapper.SysUserMapper;
import com.qiye.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper sysUserMapper;
    private final DeptMapper deptMapper;
    private final RoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<SysUser> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getName, keyword));
        }
        qw.orderByAsc(SysUser::getId);
        Page<SysUser> p = sysUserMapper.selectPage(new Page<>(page, size), qw);
        fillExt(p.getRecords());
        return new PageResult<>(p.getTotal(), p.getRecords());
    }

    public SysUser detail(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        fillExt(List.of(user));
        return user;
    }

    public void create(SysUser user, String rawPassword) {
        checkUsernameUnique(user.getUsername(), null);
        user.setId(null);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEnabled(user.getEnabled() == null ? true : user.getEnabled());
        sysUserMapper.insert(user);
    }

    public void update(SysUser user) {
        if (user.getId() == null) {
            throw new BizException("用户ID不能为空");
        }
        checkUsernameUnique(user.getUsername(), user.getId());
        user.setPassword(null);   // 密码不走此接口
        sysUserMapper.updateById(user);
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = requireUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    public void toggleStatus(Long id) {
        SysUser user = requireUser(id);
        if (user.getId().equals(SecurityUtils.getUserId())) {
            throw new BizException("不能禁用当前登录账号");
        }
        user.setEnabled(!Boolean.TRUE.equals(user.getEnabled()));
        sysUserMapper.updateById(user);
    }

    public void delete(Long id) {
        SysUser user = requireUser(id);
        if (user.getId().equals(SecurityUtils.getUserId())) {
            throw new BizException("不能删除当前登录账号");
        }
        sysUserMapper.deleteById(id);
    }

    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username);
        if (excludeId != null) {
            qw.ne(SysUser::getId, excludeId);
        }
        if (sysUserMapper.selectCount(qw) > 0) {
            throw new BizException("用户名已存在");
        }
    }

    private void fillExt(List<SysUser> users) {
        if (users.isEmpty()) {
            return;
        }
        Map<Long, Dept> deptMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Dept::getId, Function.identity()));
        Map<Long, Role> roleMap = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(Role::getId, Function.identity()));
        for (SysUser u : users) {
            Dept d = deptMap.get(u.getDeptId());
            Role r = roleMap.get(u.getRoleId());
            u.setDeptName(d == null ? "" : d.getName());
            u.setRoleName(r == null ? "" : r.getName());
            u.setRoleCode(r == null ? "" : r.getCode());
        }
    }
}
