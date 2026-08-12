package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("job_skill")
public class JobSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private Long skillId;
    private String targetLevel;   // 初级/中级/高级
    private Integer weight;

    @TableField(exist = false)
    private String skillName;
}
