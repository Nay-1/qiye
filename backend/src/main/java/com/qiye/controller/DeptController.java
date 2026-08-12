package com.qiye.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qiye.common.BizException;
import com.qiye.common.Result;
import com.qiye.entity.Dept;
import com.qiye.mapper.DeptMapper;
import com.qiye.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dept")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DeptController {

    private final DeptMapper deptMapper;
    private final SysUserMapper sysUserMapper;

    @GetMapping("/list")
    public Result<List<Dept>> list() {
        List<Dept> depts = deptMapper.selectList(
                new LambdaQueryWrapper<Dept>().orderByAsc(Dept::getId));
        for (Dept d : depts) {
            Long count = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<com.qiye.entity.SysUser>().eq(com.qiye.entity.SysUser::getDeptId, d.getId()));
            d.setUserCount(count);
        }
        return Result.ok(depts);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Dept dept) {
        checkUniqueName(dept.getName(), null);
        dept.setId(null);
        deptMapper.insert(dept);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Dept dept) {
        if (dept.getId() == null) {
            throw new BizException("部门ID不能为空");
        }
        checkUniqueName(dept.getName(), dept.getId());
        deptMapper.updateById(dept);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long count = sysUserMapper.selectCount(
                new LambdaQueryWrapper<com.qiye.entity.SysUser>().eq(com.qiye.entity.SysUser::getDeptId, id));
        if (count > 0) {
            throw new BizException("该部门下存在员工，无法删除");
        }
        deptMapper.deleteById(id);
        return Result.ok();
    }

    private void checkUniqueName(String name, Long excludeId) {
        LambdaQueryWrapper<Dept> qw = new LambdaQueryWrapper<Dept>().eq(Dept::getName, name);
        if (excludeId != null) {
            qw.ne(Dept::getId, excludeId);
        }
        if (deptMapper.selectCount(qw) > 0) {
            throw new BizException("部门名称已存在");
        }
    }
}
