package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.ai.model.vo.ChunkVO;
import com.stephen.cloud.api.search.model.entity.ChunkEsDTO;
import org.springframework.beans.BeanUtils;

/**
 * 文档分片转换器
 * <p>
 * 提供 DocumentChunk、ChunkVO、ChunkEsDTO 之间的相互转换
 * </p>
 *
 * @author StephenQiu30
 */
public class DocumentChunkConvert {

    public static final DocumentChunkConvert INSTANCE = new DocumentChunkConvert();

    public static ChunkVO objToVo(DocumentChunk chunk) {
        if (chunk == null) {
            return null;
        }
        ChunkVO chunkVO = new ChunkVO();
        BeanUtils.copyProperties(chunk, chunkVO);
        chunkVO.setId(String.valueOf(chunk.getId()));
        chunkVO.setChunkId(chunk.getVectorId());
        return chunkVO;
    }

    /**
     * 对象转 ES DTO
     *
     * @param chunk 分片实体
     * @return ES DTO
     */
    public static ChunkEsDTO objToEsDTO(DocumentChunk chunk) {
        if (chunk == null) {
            return null;
        }
        ChunkEsDTO chunkEsDTO = new ChunkEsDTO();
        BeanUtils.copyProperties(chunk, chunkEsDTO);
        chunkEsDTO.setChunkId(chunk.getVectorId());
        return chunkEsDTO;
    }

}
