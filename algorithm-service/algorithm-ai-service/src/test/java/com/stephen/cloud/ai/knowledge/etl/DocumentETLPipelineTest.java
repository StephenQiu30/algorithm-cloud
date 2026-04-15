package com.stephen.cloud.ai.knowledge.etl;

import com.stephen.cloud.ai.config.DocumentProcessingProperties;
import com.stephen.cloud.ai.knowledge.reader.DocumentReaderFactory;
import com.stephen.cloud.ai.knowledge.retrieval.RagDocumentHelper;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.service.VectorStoreService;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("文档 ETL 管道单元测试")
class DocumentETLPipelineTest {

    @Mock
    private DocumentReaderFactory documentReaderFactory;

    @Mock
    private TokenTextSplitter tokenTextSplitter;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private DocumentChunkMapper documentChunkMapper;

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private RabbitMqSender mqSender;

    @Mock
    private Resource resource;

    @Mock
    private DocumentReader documentReader;

    @InjectMocks
    private DocumentETLPipeline documentETLPipeline;

    @BeforeEach
    void setUp() throws Exception {
        setField(documentETLPipeline, "documentProcessingProperties", new DocumentProcessingProperties());
        setField(documentETLPipeline, "ragDocumentHelper", new RagDocumentHelper());
    }

    @Test
    @DisplayName("Embedding 批次持续失败时应中止 ETL，且不写入 DB / ES")
    void shouldAbortPipelineWhenEmbeddingBatchFails() {
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(documentReaderFactory.getReader(anyString(), eq(resource))).thenReturn(documentReader);
        when(documentReader.get()).thenReturn(List.of(new Document("这是一个用于测试的文档内容。".repeat(20))));
        doThrow(new RuntimeException("embedding failed")).when(vectorStore).add(anyList());

        assertThrows(IllegalStateException.class, () -> documentETLPipeline.process(
                "/tmp/test.md",
                "md",
                Map.of("documentId", 1001L, "knowledgeBaseId", 2002L, "documentName", "test.md")));

        verify(vectorStoreService).deleteByDocumentId(1001L);
        verify(documentChunkMapper, never()).batchInsert(anyList());
        verify(mqSender, never()).send(any(com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum.class), any());
        verify(vectorStore).delete(anyList());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
