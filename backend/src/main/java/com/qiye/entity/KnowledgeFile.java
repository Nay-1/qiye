package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_file")
public class KnowledgeFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String path;
    private String fileType;
    private Long size;
    private Long deptId;          // null = 全部门可见
    private String status;        // PROCESSING/READY/FAILED
    private Integer chunkCount;
    private Long createdBy;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String preview;
}
