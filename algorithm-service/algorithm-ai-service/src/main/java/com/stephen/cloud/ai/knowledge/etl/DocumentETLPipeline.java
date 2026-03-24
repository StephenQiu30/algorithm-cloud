package com.stephen.cloud.ai.knowledge.etl;

import com.stephen.cloud.ai.knowledge.reader.DocumentReaderFactory;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class DocumentETLPipeline {

    @Resource
    private DocumentReaderFactory documentReaderFactory;

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private VectorStore vectorStore;

    @Resource
    private ResourceLoader resourceLoader;

    @Resource
    private DocumentChunkMapper documentChunkMapper;

    public int process(String filePath, String fileExtension, Map<String, Object> metadata) {
        String location = filePath;
        if (!(filePath.startsWith("http://") || filePath.startsWith("https://")
                || filePath.startsWith("file:") || filePath.startsWith("classpath:"))) {
            location = "file:" + filePath;
        }
        org.springframework.core.io.Resource resource = resourceLoader.getResource(location);
        DocumentReader reader = documentReaderFactory.getReader(fileExtension, resource);
        List<Document> documents = reader.get();
        for (Document document : documents) {
            document.getMetadata().putAll(metadata);
        }
        List<Document> chunks = tokenTextSplitter.apply(documents);
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            chunk.getMetadata().put("chunkIndex", i);
            String chunkId = metadata.get("documentId") + "_" + i + "_" + UUID.randomUUID();
            chunk.getMetadata().put("chunkId", chunkId);
        }
        vectorStore.add(chunks);

        // 持久化分片到数据库
        Long documentId = metadata.get("documentId") == null ? null : Long.valueOf(String.valueOf(metadata.get("documentId")));
        Long knowledgeBaseId = metadata.get("knowledgeBaseId") == null ? null : Long.valueOf(String.valueOf(metadata.get("knowledgeBaseId")));
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            DocumentChunk entity = new DocumentChunk();
            entity.setDocumentId(documentId);
            entity.setKnowledgeBaseId(knowledgeBaseId);
            entity.setChunkIndex(i);
            entity.setContent(chunk.getText());
            entity.setWordCount(chunk.getText() == null ? 0 : chunk.getText().length());
            entity.setVectorId(chunk.getId());
            chunkEntities.add(entity);
        }
        for (DocumentChunk entity : chunkEntities) {
            documentChunkMapper.insert(entity);
        }

        return chunks.size();
    }
}

