package com.stephen.cloud.ai.knowledge.retrieval;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
class RagConversationScopeTest {
 @Test void clientIdCannotCrossUserOrKnowledgeBase() {
  String a=RagConversationScope.resolve("known-session", 10L, 1L);
  assertEquals(a,RagConversationScope.resolve("known-session",10L,1L));
  assertNotEquals(a,RagConversationScope.resolve("known-session",10L,2L));
  assertNotEquals(a,RagConversationScope.resolve("known-session",11L,1L));
  assertNotEquals(a,RagConversationScope.resolve(a,10L,2L));
  assertEquals(a,RagConversationScope.resolve(a,10L,1L));
 }
 @Test void anonymousMissingAndOversizedIdentifiersDoNotShareMemory() {
  assertThrows(IllegalArgumentException.class,()->RagConversationScope.resolve("x",10L,null));
  assertThrows(IllegalArgumentException.class,()->RagConversationScope.resolve("x".repeat(257),10L,1L));
  assertNotEquals(RagConversationScope.resolve(null,10L,1L),RagConversationScope.resolve(null,10L,1L));
 }
 @Test void teachingStateIsBoundedAndKeptOutOfRetrievalText() {
  String context=TeachingContextFormatter.format(Map.of("algorithmId","merge","originalArray",List.of(3,1,2),"currentArray",List.of(1,3,2),"currentStep",4,"unrecognized","ignore"));
  assertTrue(context.contains("currentStep"));assertFalse(context.contains("unrecognized"));
  assertEquals("merge sorting 排序算法：为什么？",TeachingContextFormatter.retrievalQuestion("为什么？",context));
  assertThrows(IllegalArgumentException.class,()->TeachingContextFormatter.format(Map.of("algorithmId","merge","originalArray",List.of(1.5),"currentArray",List.of(2))));
  assertThrows(IllegalArgumentException.class,()->TeachingContextFormatter.validateRequest("x",1L,100));
 }
}
