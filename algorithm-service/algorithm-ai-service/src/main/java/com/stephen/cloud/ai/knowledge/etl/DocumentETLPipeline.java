package com.stephen.cloud.ai.knowledge.etl;

import com.stephen.cloud.ai.knowledge.reader.DocumentReaderFactory;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

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

    public int process(String filePath, String fileExtension, Map<String, Object> metadata) {
        org.springframework.core.io.Resource resource = resourceLoader.getResource("file:" + filePath);
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
        return chunks.size();
    }
}
