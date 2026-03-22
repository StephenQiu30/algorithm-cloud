package com.stephen.cloud.ai.knowledge.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;
import java.util.stream.Collectors;

public class TextChunker {

    public static List<String> splitWithOverlap(String text, int chunkSize, int overlap) {
        if (StringUtils.isBlank(text)) {
            return List.of();
        }

        // 使用 Spring AI 的 TokenTextSplitter 进行智能分片 (语义化更强)
        // chunkSize 代表 Token 数量，overlap 为重叠 Token 数
        TokenTextSplitter splitter = new TokenTextSplitter(chunkSize, overlap, 5, 10000, true);
        
        List<Document> chunks = splitter.split(new Document(text));
        
        return chunks.stream()
                .map(Document::getText)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
    }
}
