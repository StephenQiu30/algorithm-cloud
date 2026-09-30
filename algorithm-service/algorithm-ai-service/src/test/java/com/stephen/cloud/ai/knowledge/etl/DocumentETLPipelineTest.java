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

    @Mock private KeywordIndexPublisher keywordIndexPublisher;
    @Mock private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

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

        verify(vectorStoreService, never()).deleteByDocumentId(1001L);
        verify(documentChunkMapper, never()).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(documentChunkMapper, never()).batchInsert(anyList());
        verify(mqSender, never()).send(any(com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum.class), any());
        verify(vectorStore).delete(anyList());
    }

    @Test
    void shouldPreserveOldBuildWhenReadFails() {
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(documentReaderFactory.getReader(anyString(), eq(resource))).thenReturn(documentReader);
        when(documentReader.get()).thenThrow(new IllegalStateException("read failed"));
        assertThrows(IllegalStateException.class, () -> documentETLPipeline.process("/tmp/test.md", "md", Map.of("documentId", 1001L, "knowledgeBaseId", 2002L)));
        verifyNoInteractions(vectorStore, keywordIndexPublisher, documentChunkMapper, transactionTemplate);
    }

    @Test
    void keywordFailureMustNotPublishOrDeleteOldChunks() {
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(documentReaderFactory.getReader(anyString(), eq(resource))).thenReturn(documentReader);
        when(documentReader.get()).thenReturn(List.of(new Document("有效课程文档".repeat(30))));
        doThrow(new IllegalStateException("keyword unavailable")).when(keywordIndexPublisher).publish(anyList());
        assertThrows(IllegalStateException.class, () -> documentETLPipeline.process("/tmp/test.md", "md", Map.of("documentId", 1001L, "knowledgeBaseId", 2002L)));
        verifyNoInteractions(transactionTemplate);
        verify(documentChunkMapper, never()).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(documentChunkMapper, never()).batchInsert(anyList());
        verify(vectorStore).delete(argThat((List<String> ids) -> ids.stream().allMatch(id -> id.startsWith("1001_") && !id.equals("1001_0"))));
    }

    @Test
    void databaseFailureRollsBackOnlyIsolatedNewBuild() {
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(documentReaderFactory.getReader(anyString(), eq(resource))).thenReturn(documentReader);
        when(documentReader.get()).thenReturn(List.of(new Document("有效课程文档".repeat(30))));
        when(transactionTemplate.execute(any())).thenThrow(new IllegalStateException("db transaction failed"));
        assertThrows(IllegalStateException.class, () -> documentETLPipeline.process("/tmp/test.md", "md", Map.of("documentId", 1001L, "knowledgeBaseId", 2002L)));
        verify(keywordIndexPublisher).publish(anyList());
        verify(keywordIndexPublisher).delete(anyList());
        verify(vectorStore).delete(argThat((List<String> ids) -> ids.stream().noneMatch("1001_0"::equals)));
    }

    @Test
    void shouldPublishOnlyAfterBothIndexesAreReadyAndThenRetireOldBuild() {
        when(resourceLoader.getResource(anyString())).thenReturn(resource);
        when(documentReaderFactory.getReader(anyString(), eq(resource))).thenReturn(documentReader);
        when(documentReader.get()).thenReturn(List.of(new Document("有效课程文档".repeat(30))));
        var old = new com.stephen.cloud.ai.model.entity.DocumentChunk();old.setId(99L);old.setVectorId("1001_0");
        when(documentChunkMapper.lockDocument(1001L, 2002L, "course-v2", "/tmp/test.md")).thenReturn(1001L);
        when(documentChunkMapper.selectList(any())).thenReturn(List.of(old));
        when(documentChunkMapper.markDocumentPublished(eq(1001L), anyInt())).thenReturn(1);
        when(documentChunkMapper.batchInsert(anyList())).thenAnswer(i -> ((List<?>) i.getArgument(0)).size());
        when(transactionTemplate.execute(any())).thenAnswer(i -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = i.getArgument(0);
            return callback.doInTransaction(mock(org.springframework.transaction.TransactionStatus.class));
        });
        org.junit.jupiter.api.Assertions.assertTrue(documentETLPipeline.process("/tmp/test.md", "md", Map.of("documentId",1001L,"knowledgeBaseId",2002L,"version","course-v2")) > 0);
        var order=inOrder(vectorStore,keywordIndexPublisher,documentChunkMapper);
        order.verify(vectorStore).add(anyList());order.verify(keywordIndexPublisher).publish(anyList());
        order.verify(documentChunkMapper).lockDocument(1001L, 2002L, "course-v2", "/tmp/test.md");order.verify(documentChunkMapper).selectList(any());
        order.verify(documentChunkMapper).delete(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));order.verify(documentChunkMapper).batchInsert(anyList());
        order.verify(documentChunkMapper).markDocumentPublished(eq(1001L), anyInt());
        order.verify(vectorStore).delete(List.of("1001_0"));order.verify(keywordIndexPublisher).delete(List.of(old));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
