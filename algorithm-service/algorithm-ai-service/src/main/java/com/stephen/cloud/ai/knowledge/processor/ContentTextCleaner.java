package com.stephen.cloud.ai.knowledge.processor;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 非结构化教学文本离线清洗：去 BOM、统一换行、压缩空白、合并多余空行，降低噪声对分块与检索的干扰。
 * <p>
 * 适用于 PDF/Word/Markdown 等经 {@link com.stephen.cloud.ai.knowledge.parser.DocumentTextExtractor} 抽取后的正文，
 * 使算法描述、复杂度列表等在 {@link com.stephen.cloud.ai.knowledge.util.TextChunker} 中不被无关格式打断。
 * </p>
 *
 * @author StephenQiu30
 */
@Component
public class ContentTextCleaner implements DocumentTransformer {

    public String clean(String raw) {
        if (StringUtils.isBlank(raw)) {
            return "";
        }
        String t = raw;
        if (t.startsWith("\uFEFF")) {
            t = t.substring(1);
        }
        t = t.replace("\r\n", "\n").replace('\r', '\n');
        t = t.replaceAll("[\t \u00A0]+", " ");
        t = t.replaceAll("\n{3,}", "\n\n");
        return t.trim();
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        List<Document> out = new ArrayList<>(documents.size());
        for (Document doc : documents) {
            String c = clean(doc.getText());
            if (StringUtils.isBlank(c)) {
                continue;
            }
            Map<String, Object> meta = doc.getMetadata() != null
                    ? new HashMap<>(doc.getMetadata())
                    : new HashMap<>();
            out.add(new Document(c, meta));
        }
        return out;
    }
}
