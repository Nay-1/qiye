package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("user_skill")
public class UserSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long skillId;
    private String currentLevel;   // 初级/中级/高级
    private String targetLevel;    // 岗位目标（多岗取最高）
    private BigDecimal score;      // 0~100
    private LocalDateTime updatedAt;

    // 非表字段
    @TableField(exist = false)
    private String skillName;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private BigDecimal rate;       // 达成率（0~100，封顶100）
    @TableField(exist = false)
    private Boolean reached;       // 是否达标
    @TableField(exist = false)
    private Boolean weak;          // 是否薄弱技能
}
