package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("exam_attempt")
public class ExamAttempt {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long examId;
    private Integer attemptNo;
    private BigDecimal totalScore;
    private String status;        // IN_PROGRESS/SUBMITTED
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    @TableField(exist = false)
    private String examTitle;
    @TableField(exist = false)
    private Integer passScore;
    @TableField(exist = false)
    private String userName;
    @TableField(exist = false)
    private Boolean passed;
}
