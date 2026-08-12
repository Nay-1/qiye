package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("exam")
public class Exam {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Long courseId;
    private Integer duration;     // 分钟
    private Integer attempts;     // 允许次数
    private Integer passScore;    // 及格线
    private String status;        // DRAFT/PUBLISHED/CLOSED
    private Long createdBy;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private Integer questionCount;
    @TableField(exist = false)
    private Integer totalScore;
    @TableField(exist = false)
    private List<ExamQuestion> questionList;
}
