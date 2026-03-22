package com.stephen.cloud.ai.knowledge.parser;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DocumentTextExtractor {

    private DocumentTextExtractor() {
    }

    public static String extract(Path path, String lowerName) throws IOException {
        if (lowerName.endsWith(".pdf")) {
            try (PDDocument doc = Loader.loadPDF(path.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return StringUtils.trimToEmpty(stripper.getText(doc));
            }
        }
        if (lowerName.endsWith(".docx")) {
            try (XWPFDocument doc = new XWPFDocument(Files.newInputStream(path));
                    XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
                return StringUtils.trimToEmpty(ex.getText());
            }
        }
        return StringUtils.trimToEmpty(Files.readString(path, StandardCharsets.UTF_8));
    }
}
