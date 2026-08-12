package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dept")
public class Dept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Long userCount;
}
