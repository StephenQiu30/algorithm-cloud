package com.stephen.cloud.ai.knowledge.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档文本提取器 (Extract Phase)
 * <p>
 * 遵循 Spring AI 官方 ETL 规范，集成多种 DocumentReader 实现。
 * 支持格式：PDF, Word, PPT, TXT, Markdown (MD) 等。
 * </p>
 *
 * @author StephenQiu30
 */
@Slf4j
public class DocumentTextExtractor {

    /**
     * 根据文件类型选择合适的 Reader 读取文档内容
     *
     * @param path      文件物理路径
     * @param lowerName 小写文件名（用于判断后缀）
     * @return 解析后的 Spring AI Document 列表
     * @throws IOException IO 异常
     */
    public static List<Document> readDocuments(Path path, String lowerName) throws IOException {
        if (path == null || StringUtils.isBlank(lowerName)) {
            return List.of();
        }

        Resource resource = new FileSystemResource(path.toFile());
        if (!resource.exists()) {
            log.warn("文件资源不存在: {}", path);
            return List.of();
        }

        try {
            // 1. Markdown 处理
            if (lowerName.endsWith(".md") || lowerName.endsWith(".markdown")) {
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withIncludeCodeBlock(true)
                        .withIncludeBlockquote(true)
                        .withHorizontalRuleCreateDocument(true)
                        .build();
                return new MarkdownDocumentReader(resource, config).read();
            }

            // 2. PDF 处理（推荐使用 PagePdfDocumentReader 以保持页面/段落结构）
            if (lowerName.endsWith(".pdf")) {
                PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                        .withPageTopMargin(0)
                        .withPageBottomMargin(0)
                        .build();
                return new PagePdfDocumentReader(resource, config).read();
            }

            // 3. 其他格式（Word/Excel/PPT/TXT 等）使用 Tika 自动识别并提取
            log.info("使用 Tika 解析通用格式文档: {}", lowerName);
            return new TikaDocumentReader(resource).read();

        } catch (Exception e) {
            log.error("解析文档失败: {}, 错误信息: {}", lowerName, e.getMessage());
            // 如果特定解析器失败，尝试使用 Tika 作为最终兜底
            try {
                return new TikaDocumentReader(resource).read();
            } catch (Exception fatal) {
                log.error("所有解析器均失败: {}", fatal.getMessage());
                return List.of();
            }
        }
    }

    /**
     * 提取并合并所有文档文本内容
     *
     * @param path      文件物理路径
     * @param lowerName 小写文件名
     * @return 合并后的完整文本
     * @throws IOException IO 异常
     */
    public static String extract(Path path, String lowerName) throws IOException {
        return readDocuments(path, lowerName).stream()
                .map(Document::getText)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.joining("\n\n"));
    }
}
