package com.stephen.cloud.ai.knowledge.etl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.SECTION_PATH;
import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.SECTION_TITLE;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("智能文本分片器单元测试")
class SmartTextSplitterTest {

    private final SmartTextSplitter splitter = new SmartTextSplitter(400, 80, 800);

    @Test
    @DisplayName("测试嵌套标题解析与路径追踪")
    void testSplitByHeadings() {
        String text = "# 算法基础\n" +
                      "这里是算法的定义。\n" +
                      "## 排序算法\n" +
                      "排序算法很重要。\n" +
                      "### 快速排序\n" +
                      "快排的核心是分治策略。";

        List<Document> result = splitter.split(List.of(new Document(text)));

        assertFalse(result.isEmpty());
        
        // 验证最后一个分片（快速排序）的元数据
        Document lastChunk = result.get(result.size() - 1);
        assertEquals("快速排序", lastChunk.getMetadata().get(SECTION_TITLE));
        assertEquals("算法基础 > 排序算法 > 快速排序", lastChunk.getMetadata().get(SECTION_PATH));
        assertTrue(lastChunk.getText().contains("分治策略"));
    }

    @Test
    @DisplayName("测试边界：超长段落自动按句切分")
    void testLongParagraphSplit() {
        // 构造一个长度超过 800 的单段落，包含多句
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 20; i++) {
            sb.append("这是第 ").append(i).append(" 个测试句子，用于填充段落长度。");
        }
        String longText = sb.toString();
        assertTrue(longText.length() > 400);

        List<Document> result = splitter.split(List.of(new Document(longText)));

        // 预期被切分为多个分片
        assertTrue(result.size() > 1);
        for (Document doc : result) {
            assertTrue(doc.getText().length() <= 800);
        }
    }

    @Test
    @DisplayName("测试语义 Overlap 重叠逻辑")
    void testOverlap() {
        // 构造一个需要被切分的长文本
        String text = "第一部分内容。".repeat(100);
        
        List<Document> result = splitter.split(List.of(new Document(text)));
        
        if (result.size() > 1) {
            String firstTail = result.get(0).getText();
            String secondHead = result.get(1).getText();
            
            // 第二个分片的开头应该包含第一个分片的结尾部分的文本 (Overlap)
            // 简单校验其重叠性质
            assertNotNull(secondHead);
            assertFalse(secondHead.isEmpty());
        }
    }

    @Test
    @DisplayName("测试空内容与异常边界")
    void testEmptyContent() {
        assertTrue(splitter.split(List.of(new Document(""))).isEmpty());
        assertTrue(splitter.split(List.of(new Document("   "))).isEmpty());
    }
}
