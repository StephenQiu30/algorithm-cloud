package com.stephen.cloud.ai.knowledge.retrieval;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import static org.junit.jupiter.api.Assertions.*;
class ElasticsearchFilterExpressionConverterTest {
 @Test void knowledgeBaseAndCourseReleaseAreRequiredConjuncts() {
  var b=new FilterExpressionBuilder();var result=new ElasticsearchFilterExpressionConverter().convert(b.and(b.eq("knowledgeBaseId",42L),b.and(b.eq("version","sorting-123456abcdef"),b.eq("bizTag","sorting:merge"))).build());
  String json=result.toString();assertTrue(json.contains("knowledgeBaseId"));assertTrue(json.contains("metadata.version.keyword"));assertTrue(json.contains("sorting-123456abcdef"));assertFalse(json.contains("Key["));assertEquals(2,result.bool().filter().size());
 }
 @Test void unsupportedExpressionsCannotSilentlyRemoveTheBoundary() {
  var invalid=new Filter.Expression(Filter.ExpressionType.GT,new Filter.Key("knowledgeBaseId"),new Filter.Value(1));
  assertThrows(IllegalArgumentException.class,()->new ElasticsearchFilterExpressionConverter().convert(invalid));
 }
}
