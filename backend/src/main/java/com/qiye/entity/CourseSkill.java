package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("course_skill")
public class CourseSkill {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long skillId;
    private Integer weight;
    private Boolean required;

    @TableField(exist = false)
    private String skillName;
    @TableField(exist = false)
    private String courseName;
}
