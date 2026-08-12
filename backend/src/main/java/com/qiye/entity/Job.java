package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("job")
public class Job {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long deptId;
    private String description;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private Integer skillCount;
}
