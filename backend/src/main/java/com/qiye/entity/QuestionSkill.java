package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("question_skill")
public class QuestionSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long skillId;
    private Integer scoreWeight;

    @TableField(exist = false)
    private String skillName;
}
