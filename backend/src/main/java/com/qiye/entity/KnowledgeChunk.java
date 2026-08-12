package com.qiye.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("knowledge_chunk")
public class KnowledgeChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long fileId;
    private Integer seq;
    private String content;
    // embedding 列通过原生 SQL 写入/查询，不映射到实体
}
