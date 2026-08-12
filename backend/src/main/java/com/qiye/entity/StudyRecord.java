package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("study_record")
public class StudyRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    private Long chapterId;
    private Integer progress;         // 0~100
    private Integer studyDuration;    // 秒
    private String status;            // NOT_STARTED/IN_PROGRESS/COMPLETED
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String chapterTitle;
    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String userName;
}
