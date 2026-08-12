package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("exam_answer")
public class ExamAnswer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long attemptId;
    private Long questionId;
    private String userAnswer;
    private Boolean correct;
    private BigDecimal score;

    @TableField(exist = false)
    private Question question;    // 成绩详情：题干/正确答案/解析
}
