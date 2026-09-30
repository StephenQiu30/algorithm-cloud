package com.stephen.cloud.ai.knowledge.retrieval;
import org.junit.jupiter.api.Test;
import com.stephen.cloud.ai.mapper.DocumentChunkMapper;
import com.stephen.cloud.ai.model.entity.DocumentChunk;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
class PublishedChunkFilterTest {
 @Test void pendingAndRetiredBuildsCannotReachGeneration() {
  var mapper=mock(DocumentChunkMapper.class);var filter=new PublishedChunkFilter();ReflectionTestUtils.setField(filter,"mapper",mapper);
  var row=new DocumentChunk();row.setVectorId("active");when(mapper.selectList(any())).thenReturn(List.of(row));
  var active=new Document("active","current",Map.of());var pending=new Document("pending","new",Map.of());var retired=new Document("retired","old",Map.of());
  assertEquals(List.of(active),filter.filter(List.of(active,pending,retired)));
 }
}
