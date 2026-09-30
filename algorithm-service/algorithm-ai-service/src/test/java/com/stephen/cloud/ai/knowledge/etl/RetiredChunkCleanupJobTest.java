package com.stephen.cloud.ai.knowledge.etl;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class RetiredChunkCleanupJobTest {
 @Test void failedCleanupKeepsDurableRecordsUntilRetrySucceeds() {
  var mapper=mock(DocumentChunkMapper.class);var vectors=mock(VectorStore.class);var publisher=mock(KeywordIndexPublisher.class);var job=new RetiredChunkCleanupJob();
  ReflectionTestUtils.setField(job,"mapper",mapper);ReflectionTestUtils.setField(job,"vectorStore",vectors);ReflectionTestUtils.setField(job,"keywordIndexPublisher",publisher);
  var row=new DocumentChunk();row.setId(1L);row.setVectorId("old-build");when(mapper.selectRetiredForCleanup()).thenReturn(List.of(row));
  doThrow(new IllegalStateException("ES unavailable")).doNothing().when(publisher).delete(anyList());
  job.retryCleanup();verify(mapper,never()).deleteRetired(anyList());
  job.retryCleanup();verify(mapper).deleteRetired(List.of(1L));
 }
}
