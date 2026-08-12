package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_job")
public class UserJob {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long jobId;
    private Boolean isPrimary;

    @TableField(exist = false)
    private String jobName;
}
