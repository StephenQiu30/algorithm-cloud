package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.chunk.DocumentChunkUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 文档分片转换器
 *
 * @author StephenQiu30
 */
public class DocumentChunkConvert {

    public static final DocumentChunkConvert INSTANCE = new DocumentChunkConvert();

    /**
     * 新增请求转对象
     *
     * @param addRequest 新增请求
     * @return 文档分片实体
     */
    public DocumentChunk addRequestToObj(DocumentChunkAddRequest addRequest) {
        if (addRequest == null) {
            return null;
        }
        DocumentChunk chunk = new DocumentChunk();
        BeanUtils.copyProperties(addRequest, chunk);
        return chunk;
    }

    /**
     * 更新请求转对象
     *
     * @param updateRequest 更新请求
     * @return 文档分片实体
     */
    public DocumentChunk updateRequestToObj(DocumentChunkUpdateRequest updateRequest) {
        if (updateRequest == null) {
            return null;
        }
        DocumentChunk chunk = new DocumentChunk();
        BeanUtils.copyProperties(updateRequest, chunk);
        return chunk;
    }
}
