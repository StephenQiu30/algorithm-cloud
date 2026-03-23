package com.stephen.cloud.api.knowledge.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum KnowledgeDocumentTypeEnum {
    PDF("PDF文档", "pdf"),
    DOCX("Word文档", "docx"),
    TXT("文本文件", "txt"),
    MD("Markdown文件", "md"),
    MARKDOWN("Markdown文件", "markdown"),
    CSV("CSV文件", "csv"),
    XLSX("Excel文件", "xlsx"),
    XLS("Excel文件", "xls");

    private final String text;
    private final String value;

    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    public static KnowledgeDocumentTypeEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (KnowledgeDocumentTypeEnum anEnum : KnowledgeDocumentTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}

