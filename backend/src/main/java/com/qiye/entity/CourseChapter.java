package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_chapter")
public class CourseChapter {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private String title;
    private String content;
    private Integer seq;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private StudyRecord studyRecord;   // 当前用户对该章节的学习记录
}
