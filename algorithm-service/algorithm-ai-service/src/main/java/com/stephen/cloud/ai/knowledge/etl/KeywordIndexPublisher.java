package com.stephen.cloud.ai.knowledge.etl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import com.stephen.cloud.ai.config.RagRetrievalProperties;
import com.stephen.cloud.ai.convert.DocumentChunkConvert;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import com.stephen.cloud.api.search.constant.EsIndexConstant;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import java.util.List;

/** COMPLETED requires an acknowledged, searchable keyword index, not an MQ enqueue. */
@Component
public class KeywordIndexPublisher {
    @Resource private ElasticsearchClient client;
    @Resource private RagRetrievalProperties properties;
    private String index() {return StringUtils.defaultIfBlank(properties.getKeywordIndexName(), StringUtils.defaultIfBlank(properties.getIndexName(), EsIndexConstant.CHUNK_INDEX));}
    public void publish(List<DocumentChunk> chunks) {
        for (int offset = 0; offset < chunks.size(); offset += 100) {
            BulkRequest.Builder request = new BulkRequest.Builder().refresh(Refresh.WaitFor);
            chunks.subList(offset, Math.min(offset + 100, chunks.size())).forEach(chunk -> request.operations(op -> op.index(i -> i.index(index()).id(String.valueOf(chunk.getId())).document(DocumentChunkConvert.objToEsDTO(chunk)))));
            try {if (client.bulk(request.build()).errors()) throw new IllegalStateException("关键词索引包含失败项");}
            catch (java.io.IOException e) {throw new IllegalStateException("关键词索引未就绪", e);}
        }
    }
    public void delete(List<DocumentChunk> chunks) {
        if (chunks.isEmpty()) return;
        for (int offset = 0; offset < chunks.size(); offset += 100) {
            BulkRequest.Builder request = new BulkRequest.Builder();
            chunks.subList(offset, Math.min(offset + 100, chunks.size())).forEach(chunk -> request.operations(op -> op.delete(d -> d.index(index()).id(String.valueOf(chunk.getId())))));
            try {if (client.bulk(request.build()).errors()) throw new IllegalStateException("索引清理包含失败项");}
            catch (java.io.IOException e) {throw new IllegalStateException("索引清理失败", e);}
        }
    }
}
