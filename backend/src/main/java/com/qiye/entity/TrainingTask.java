package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("training_task")
public class TrainingTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long jobId;
    private Long skillId;
    private Long courseId;
    private String status;    // PENDING/IN_PROGRESS/COMPLETED
    private String source;    // SYSTEM/AI
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String jobName;
    @TableField(exist = false)
    private String skillName;
    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String courseLevel;
    @TableField(exist = false)
    private String userName;
}
