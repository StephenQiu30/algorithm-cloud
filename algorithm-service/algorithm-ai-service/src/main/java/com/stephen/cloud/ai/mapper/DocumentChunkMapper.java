package com.stephen.cloud.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文档分片 Mapper
 *
 * @author StephenQiu30
 * @Entity com.stephen.cloud.ai.model.entity.DocumentChunk
 */
@Mapper
public interface DocumentChunkMapper extends BaseMapper<DocumentChunk> {

    @Select("SELECT id FROM document WHERE id = #{documentId} AND knowledge_base_id = #{knowledgeBaseId} AND is_delete = 0 AND version <=> #{version,jdbcType=VARCHAR} AND file_path = #{filePath} FOR UPDATE")
    Long lockDocument(@Param("documentId") Long documentId, @Param("knowledgeBaseId") Long knowledgeBaseId,
                      @Param("version") String version, @Param("filePath") String filePath);

    @Update("UPDATE document SET status = 'COMPLETED', chunk_count = #{count}, error_message = NULL, process_end_time = CURRENT_TIMESTAMP WHERE id = #{documentId} AND is_delete = 0")
    int markDocumentPublished(@Param("documentId") Long documentId, @Param("count") int count);

    @Select("SELECT id, vector_id FROM document_chunk WHERE is_delete = 1 ORDER BY update_time LIMIT 100")
    List<DocumentChunk> selectRetiredForCleanup();

    @Delete("<script>DELETE FROM document_chunk WHERE is_delete = 1 AND id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    int deleteRetired(@Param("ids") List<Long> ids);

    @Insert("""
            <script>
            INSERT INTO document_chunk
            (id, document_id, knowledge_base_id, document_name, chunk_index, content,
             section_title, section_path, word_count, token_count, vector_id, metadata_json)
            VALUES
            <foreach collection="chunks" item="item" separator=",">
                (#{item.id}, #{item.documentId}, #{item.knowledgeBaseId}, #{item.documentName}, #{item.chunkIndex}, #{item.content},
                 #{item.sectionTitle}, #{item.sectionPath}, #{item.wordCount}, #{item.tokenCount}, #{item.vectorId}, #{item.metadataJson})
            </foreach>
            </script>
            """)
    int batchInsert(@Param("chunks") List<DocumentChunk> chunks);
}
