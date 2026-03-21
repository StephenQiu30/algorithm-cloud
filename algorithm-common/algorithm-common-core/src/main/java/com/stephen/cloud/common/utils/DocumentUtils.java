package com.stephen.cloud.common.utils;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.OutputStream;

/**
 * 文档转换工具类
 * 提供 Markdown 文本到 PDF 和 Word 的基础转换
 * 秉持“不过度设计”原则，当前实现为基础文本导出
 *
 * @author StephenQiu30
 */
@Slf4j
public class DocumentUtils {

    /**
     * 将 Markdown 文本导出为 PDF
     *
     * @param markdown 文本内容
     * @param out      输出流
     */
    public static void exportToPdf(String markdown, OutputStream out) {
        // 设置页边距: 左, 右, 上, 下
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // 基础字体设置 (如需支持中文需额外处理，此处保留基础转换逻辑)
            String[] lines = markdown.split("\n");
            for (String line : lines) {
                String content = line.trim();
                if (content.isEmpty()) {
                    document.add(new Paragraph(" "));
                    continue;
                }

                Paragraph paragraph;
                if (content.startsWith("# ")) {
                    paragraph = new Paragraph(content.substring(2),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22));
                    paragraph.setSpacingBefore(10);
                    paragraph.setSpacingAfter(10);
                } else if (content.startsWith("## ")) {
                    paragraph = new Paragraph(content.substring(3),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
                    paragraph.setSpacingBefore(8);
                    paragraph.setSpacingAfter(8);
                } else if (content.startsWith("### ")) {
                    paragraph = new Paragraph(content.substring(4),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
                    paragraph.setSpacingBefore(6);
                    paragraph.setSpacingAfter(6);
                } else {
                    paragraph = new Paragraph(content);
                    paragraph.setSpacingAfter(5);
                }
                document.add(paragraph);
            }
        } catch (Exception e) {
            log.error("Export to PDF failed", e);
            throw new RuntimeException("PDF 导出失败", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /**
     * 将 Markdown 文本导出为 Word (.docx)
     *
     * @param markdown 文本内容
     * @param out      输出流
     */
    public static void exportToWord(String markdown, OutputStream out) {
        try (XWPFDocument document = new XWPFDocument()) {
            String[] lines = markdown.split("\n");
            for (String line : lines) {
                String content = line.trim();
                if (content.isEmpty()) {
                    document.createParagraph();
                    continue;
                }

                XWPFParagraph paragraph = document.createParagraph();
                XWPFRun run = paragraph.createRun();

                if (content.startsWith("# ")) {
                    run.setText(content.substring(2));
                    run.setBold(true);
                    run.setFontSize(22);
                    paragraph.setSpacingBefore(200); // 1/20 point
                    paragraph.setSpacingAfter(200);
                } else if (content.startsWith("## ")) {
                    run.setText(content.substring(3));
                    run.setBold(true);
                    run.setFontSize(18);
                    paragraph.setSpacingBefore(160);
                    paragraph.setSpacingAfter(160);
                } else if (content.startsWith("### ")) {
                    run.setText(content.substring(4));
                    run.setBold(true);
                    run.setFontSize(16);
                    paragraph.setSpacingBefore(120);
                    paragraph.setSpacingAfter(120);
                } else {
                    run.setText(line);
                    paragraph.setSpacingAfter(100);
                }
            }
            document.write(out);
        } catch (Exception e) {
            log.error("Export to Word failed", e);
            throw new RuntimeException("Word 导出失败", e);
        }
    }
}
