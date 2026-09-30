package com.stephen.cloud.ai.knowledge.retrieval;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.HashSet;
import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.*;

/** DB inventory is the atomic publication boundary for both retrieval channels. */
@Component
public class PublishedChunkFilter {
    @Resource private DocumentChunkMapper mapper;
    public List<Document> filter(List<Document> docs) {
        if (docs == null || docs.isEmpty()) return List.of();
        List<String> ids = docs.stream().map(Document::getId).distinct().toList();
        var rows = mapper.selectList(new LambdaQueryWrapper<DocumentChunk>().in(DocumentChunk::getVectorId, ids));
        var active = new HashSet<String>();
        if (rows != null) rows.forEach(row -> active.add(row.getVectorId()));
        return docs.stream().filter(doc -> active.contains(doc.getId())).toList();
    }
}
