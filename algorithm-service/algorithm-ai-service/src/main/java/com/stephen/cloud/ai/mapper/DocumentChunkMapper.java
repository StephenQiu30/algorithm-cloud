package com.stephen.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;

import java.util.List;

/**
 * 文档分片 Mapper
 *
 * @author StephenQiu30
 * @Entity com.stephen.cloud.ai.model.entity.DocumentChunk
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    @Insert("""
            <script>
            INSERT INTO document_chunk
            (document_id, knowledge_base_id, chunk_index, content, word_count, token_count, vector_id)
            VALUES
            <foreach collection="chunks" item="item" separator=",">
                (#{item.documentId}, #{item.knowledgeBaseId}, #{item.chunkIndex}, #{item.content},
                 #{item.wordCount}, #{item.tokenCount}, #{item.vectorId})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("chunks") List<DocumentChunk> chunks);
}
