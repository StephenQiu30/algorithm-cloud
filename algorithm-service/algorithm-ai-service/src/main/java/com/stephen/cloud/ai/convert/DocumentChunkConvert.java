package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.ai.model.vo.ChunkVO;
import org.springframework.beans.BeanUtils;

public class DocumentChunkConvert {

    public static final DocumentChunkConvert INSTANCE = new DocumentChunkConvert();

    public ChunkVO objToVo(DocumentChunk chunk) {
        if (chunk == null) {
            return null;
        }
        ChunkVO chunkVO = new ChunkVO();
        BeanUtils.copyProperties(chunk, chunkVO);
        chunkVO.setId(String.valueOf(chunk.getId()));
        return chunkVO;
    }
}
