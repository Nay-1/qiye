package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String level;
    private Long jobId;
    private String description;
    private String cover;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String jobName;
    @TableField(exist = false)
    private Integer chapterCount;
    @TableField(exist = false)
    private List<CourseChapter> chapters;
    @TableField(exist = false)
    private List<CourseSkill> skills;
}
