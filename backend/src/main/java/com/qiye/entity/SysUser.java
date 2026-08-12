package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;

    @JsonIgnore
    private String password;

    private String name;
    private Long deptId;
    private Long roleId;
    private Boolean enabled;
    private LocalDateTime createdAt;

    // 非表字段：联查展示
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String roleName;
    @TableField(exist = false)
    private String roleCode;
}
