package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "question", autoResultMap = true)
public class Question {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String type;          // SINGLE/MULTIPLE/JUDGE
    private String content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<OptionItem> options;

    private String answer;
    private String analysis;
    private Long createdBy;
    private String source;        // MANUAL/AI
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<Long> skillIds;               // 提交时绑定
    @TableField(exist = false)
    private List<QuestionSkill> skills;        // 详情展示
    @TableField(exist = false)
    private String skillNames;                 // 逗号拼接

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private String key;
        private String text;
    }
}
