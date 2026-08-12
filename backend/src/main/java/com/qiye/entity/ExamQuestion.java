package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("exam_question")
public class ExamQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private Long questionId;
    private Integer score;
    private Integer sort;

    @TableField(exist = false)
    private Question question;    // 管理端/员工端详情
}
