package com.qiye.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qiye.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface KnowledgeChunkMapper extends BaseMapper<KnowledgeChunk> {

    /** 写入带向量的切片 */
    @Insert("""
            INSERT INTO knowledge_chunk (file_id, seq, content, embedding)
            VALUES (#{fileId}, #{seq}, #{content}, CAST(#{vec} AS vector))
            """)
    int insertVector(@Param("fileId") Long fileId,
                     @Param("seq") int seq,
                     @Param("content") String content,
                     @Param("vec") String vec);

    /**
     * pgvector 余弦相似度检索（c.embedding <=> query）
     * 权限：admin/trainer 全量；员工仅本部门（dept_id IS NULL OR = 本部门）
     */
    @Select("""
            SELECT c.id, c.file_id AS "fileId", c.content, f.name AS "fileName",
                   f.dept_id AS "deptId", 1 - (c.embedding <=> CAST(#{vec} AS vector)) AS similarity
            FROM knowledge_chunk c
            JOIN knowledge_file f ON c.file_id = f.id
            WHERE c.embedding IS NOT NULL
              AND (#{isAdmin} OR f.dept_id IS NULL OR f.dept_id = #{deptId})
            ORDER BY c.embedding <=> CAST(#{vec} AS vector)
            LIMIT #{topK}
            """)
    List<Map<String, Object>> searchSimilarity(@Param("vec") String vec,
                                               @Param("deptId") Long deptId,
                                               @Param("isAdmin") boolean isAdmin,
                                               @Param("topK") int topK);

    /**
     * 降级：关键词（正则 OR）检索（未配置向量化 API 时使用）
     */
    @Select("""
            SELECT c.id, c.file_id AS "fileId", c.content, f.name AS "fileName",
                   f.dept_id AS "deptId", 0.0 AS similarity
            FROM knowledge_chunk c
            JOIN knowledge_file f ON c.file_id = f.id
            WHERE c.content ~* #{pattern}
              AND (#{isAdmin} OR f.dept_id IS NULL OR f.dept_id = #{deptId})
            ORDER BY c.id
            LIMIT #{topK}
            """)
    List<Map<String, Object>> searchLike(@Param("pattern") String pattern,
                                         @Param("deptId") Long deptId,
                                         @Param("isAdmin") boolean isAdmin,
                                         @Param("topK") int topK);
}
