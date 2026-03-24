package com.stephen.cloud.ai.knowledge.reader;

import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DocumentReaderFactory {

    public DocumentReader getReader(String fileExtension, Resource resource) {
        if (fileExtension == null || fileExtension.isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件扩展名不能为空");
        }
        return switch (fileExtension.toLowerCase()) {
            case "pdf" -> new PagePdfDocumentReader(resource);
            case "md", "markdown" -> new MarkdownDocumentReader(resource, MarkdownDocumentReaderConfig.builder().build());
            case "txt" -> new TextReader(resource);
            case "doc", "docx", "ppt", "pptx", "html" -> new TikaDocumentReader(resource);
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的文件格式: " + fileExtension);
        };
    }
}
