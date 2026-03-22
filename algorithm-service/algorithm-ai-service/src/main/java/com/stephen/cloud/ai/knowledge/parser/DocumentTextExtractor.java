package com.stephen.cloud.ai.knowledge.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档文本提取器
 * <p>
 * 集成 Spring AI Tika 实现全格式支持 (PDF, Word, PPT, TXT, MD 等)，
 * 并针对 CSV 等结构化数据进行优化提取。
 * </p>
 */
public final class DocumentTextExtractor {

    private DocumentTextExtractor() {
    }

    /**
     * 提取文档文本
     *
     * @param path      文件路径
     * @param lowerName 小写文件名（用于判断后缀）
     * @return 提取出的文本内容
     * @throws IOException IO 异常
     */
    public static String extract(Path path, String lowerName) throws IOException {
        if (StringUtils.isBlank(lowerName)) {
            return "";
        }

        // 针对 CSV 进行结构化提取优化 (每行转为 key:value 格式利于检索)
        if (lowerName.endsWith(".csv")) {
            return extractCsv(path);
        }

        // 默认使用 TikaDocumentReader 处理全格式 (PDF, DOCX, PPTX, HTML, MD 等)
        try {
            TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(path.toFile()));
            return reader.read().stream()
                    .map(org.springframework.ai.document.Document::getText)
                    .collect(Collectors.joining("\n\n"));
        } catch (Exception e) {
            // Tika 失败时尝试回退到物理读取 (仅限纯文本类型)
            return StringUtils.trimToEmpty(Files.readString(path, StandardCharsets.UTF_8));
        }
    }

    /**
     * CSV 结构化提取
     */
    private static String extractCsv(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
                CSVParser csvParser = new CSVParser(reader, format)) {
            
            List<String> headers = csvParser.getHeaderNames();
            for (CSVRecord record : csvParser) {
                for (String header : headers) {
                    sb.append(header).append(": ").append(record.get(header)).append("; ");
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            // 如果 CSV 无表头或格式异常，回退到普通读取
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        return sb.toString();
    }
}
