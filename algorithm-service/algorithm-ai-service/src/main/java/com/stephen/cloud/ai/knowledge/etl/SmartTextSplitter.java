package com.stephen.cloud.ai.knowledge.etl;

import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.SECTION_PATH;
import static com.stephen.cloud.ai.knowledge.retrieval.RagMetadataKeys.SECTION_TITLE;

/**
 * 智能文本分片器
 * <p>
 * 参考阿里云百炼平台知识库分片策略，基于文档结构（Markdown 标题、段落边界）进行语义级切分，
 * 保证每个分片的语义完整性。对于超长段落，自动回退到按句切分。
 * </p>
 *
 * @author StephenQiu30
 */
public class SmartTextSplitter {

    /**
     * Markdown 标题正则 (# ~ ######)
     */
    private static final Pattern HEADING_PATTERN = Pattern.compile("^#{1,6}\\s+.+$", Pattern.MULTILINE);

    /**
     * 段落分隔符（连续两个换行）
     */
    private static final Pattern PARAGRAPH_SEPARATOR = Pattern.compile("\\n\\s*\\n");

    /**
     * 句子分隔正则（中英文句号、问号、感叹号）
     * 中文标点后直接断句；英文标点后必须跟空格或换行才视为句子结束，避免误切 "e.g."、"v1.0"、"3.14" 等
     */
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("(?<=[。！？])\\s*|(?<=[.!?])(?=\\s)");

    private final int chunkSize;
    private final int overlapSize;
    private final int maxChunkSize;

    public SmartTextSplitter(int chunkSize, int overlapSize, int maxChunkSize) {
        this.chunkSize = chunkSize <= 0 ? 400 : chunkSize;
        this.overlapSize = overlapSize < 0 ? 80 : overlapSize;
        this.maxChunkSize = maxChunkSize <= 0 ? 800 : maxChunkSize;
    }

    /**
     * 对文档列表进行智能分片
     */
    public List<Document> split(List<Document> documents) {
        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                continue;
            }
            Map<String, Object> metadata = doc.getMetadata();
            result.addAll(splitDocument(text, metadata));
        }
        return result;
    }

    /**
     * 核心切分逻辑：
     * 1. 按 Markdown 标题分段
     * 2. 段内按段落（双换行）拆分
     * 3. 超长段落按句拆分
     * 4. 相邻分片在 Section 内部添加 overlap（不跨 Section，避免语义噪声）
     */
    private List<Document> splitDocument(String text, Map<String, Object> metadata) {
        List<SectionBlock> sections = splitByHeadings(text);
        List<Document> allChunks = new ArrayList<>();

        for (SectionBlock section : sections) {
            List<String> sectionChunkTexts = splitSectionToChunks(section.content());
            List<Document> sectionChunks = new ArrayList<>();
            for (String sectionChunk : sectionChunkTexts) {
                if (sectionChunk.isBlank()) {
                    continue;
                }
                Map<String, Object> chunkMetadata = new LinkedHashMap<>(metadata);
                if (section.sectionTitle() != null && !section.sectionTitle().isBlank()) {
                    chunkMetadata.put(SECTION_TITLE, section.sectionTitle());
                }
                if (section.sectionPath() != null && !section.sectionPath().isBlank()) {
                    chunkMetadata.put(SECTION_PATH, section.sectionPath());
                }
                sectionChunks.add(new Document(sectionChunk.strip(), Map.copyOf(chunkMetadata)));
            }

            // Overlap 在 Section 内部添加，不跨 Section
            if (overlapSize > 0 && sectionChunks.size() > 1) {
                sectionChunks = addOverlap(sectionChunks);
            }
            allChunks.addAll(sectionChunks);
        }
        return allChunks;
    }

    /**
     * 按 Markdown 标题 (#) 切分文本为逻辑段。
     * 每个段以标题行开头（如果有），包含到下一个标题前的所有内容。
     */
    private List<SectionBlock> splitByHeadings(String text) {
        List<SectionBlock> sections = new ArrayList<>();
        var matcher = HEADING_PATTERN.matcher(text);
        List<int[]> headingPositions = new ArrayList<>();
        List<Integer> headingLevels = new ArrayList<>();
        List<String> headingTitles = new ArrayList<>();

        while (matcher.find()) {
            headingPositions.add(new int[]{matcher.start(), matcher.end()});
            String headingLine = matcher.group();
            headingLevels.add(resolveHeadingLevel(headingLine));
            headingTitles.add(resolveHeadingTitle(headingLine));
        }

        if (headingPositions.isEmpty()) {
            // 无标题，整篇视为一个段
            sections.add(new SectionBlock(text, null, null));
            return sections;
        }

        // 标题前的内容作为第一段（如果有）
        if (headingPositions.getFirst()[0] > 0) {
            String before = text.substring(0, headingPositions.getFirst()[0]).strip();
            if (!before.isEmpty()) {
                sections.add(new SectionBlock(before, null, null));
            }
        }

        List<String> headingStack = new ArrayList<>();
        // 按标题切分
        for (int i = 0; i < headingPositions.size(); i++) {
            int start = headingPositions.get(i)[0];
            int end = (i + 1 < headingPositions.size()) ? headingPositions.get(i + 1)[0] : text.length();
            String section = text.substring(start, end).strip();
            if (!section.isEmpty()) {
                Integer headingLevel = headingLevels.get(i);
                String headingTitle = headingTitles.get(i);
                adjustHeadingStack(headingStack, headingLevel, headingTitle);
                sections.add(new SectionBlock(section, headingTitle, joinHeadingPath(headingStack)));
            }
        }
        return sections;
    }

    /**
     * 对一个逻辑段按段落拆分并组装分片，确保每个分片不超过 maxChunkSize
     */
    private List<String> splitSectionToChunks(String section) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = PARAGRAPH_SEPARATOR.split(section);
        StringBuilder buffer = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 单个段落超过 maxChunkSize，按句切分
            if (trimmed.length() > maxChunkSize) {
                // 先 flush buffer
                if (!buffer.isEmpty()) {
                    chunks.add(buffer.toString().strip());
                    buffer.setLength(0);
                }
                // 按句拆分超长段落
                chunks.addAll(splitBySentence(trimmed));
                continue;
            }

            // 加入当前段落后是否超过 chunkSize
            if (buffer.length() + trimmed.length() + 1 > chunkSize && !buffer.isEmpty()) {
                chunks.add(buffer.toString().strip());
                buffer.setLength(0);
            }
            if (!buffer.isEmpty()) {
                buffer.append("\n\n");
            }
            buffer.append(trimmed);
        }

        if (!buffer.isEmpty()) {
            chunks.add(buffer.toString().strip());
        }
        return chunks;
    }

    /**
     * 按句子拆分超长文本
     */
    private List<String> splitBySentence(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_PATTERN.split(text);
        StringBuilder buffer = new StringBuilder();

        for (String sentence : sentences) {
            String trimmed = sentence.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (buffer.length() + trimmed.length() + 1 > chunkSize && !buffer.isEmpty()) {
                chunks.add(buffer.toString().strip());
                buffer.setLength(0);
            }
            if (!buffer.isEmpty()) {
                buffer.append(" ");
            }
            buffer.append(trimmed);
        }
        if (!buffer.isEmpty()) {
            chunks.add(buffer.toString().strip());
        }

        // 兜底：如果句子拆分后仍有超长分片，按字符硬切
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            if (chunk.length() > maxChunkSize) {
                result.addAll(hardSplit(chunk));
            } else {
                result.add(chunk);
            }
        }
        return result;
    }

    /**
     * 硬切分（智能回退）：优先在句号/换行符等自然边界处切分，
     * 仅在找不到自然边界时才按 maxChunkSize 硬截断，避免在词语中间断裂。
     */
    private List<String> hardSplit(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            if (start + maxChunkSize >= text.length()) {
                chunks.add(text.substring(start));
                break;
            }
            int end = start + maxChunkSize;
            // 在 [start, end] 范围内从后往前搜索自然断句点
            int splitAt = findNaturalBreakpoint(text, start, end);
            chunks.add(text.substring(start, splitAt));
            start = splitAt;
        }
        return chunks;
    }

    /**
     * 从 end 位置向前搜索自然断句点（句号、换行、分号等），
     * 搜索范围限制在 [start + maxChunkSize/2, end] 之间，
     * 避免回退过多导致分片过小。
     */
    private int findNaturalBreakpoint(String text, int start, int end) {
        // 自然断句字符优先级：换行 > 句号 > 分号 > 逗号
        char[] breakChars = {'\n', '。', '！', '？', '；', '.', '!', '?', ';', '，', ','};
        int minPos = start + (maxChunkSize / 2); // 至少保留一半内容
        for (char breakChar : breakChars) {
            int pos = text.lastIndexOf(breakChar, end - 1);
            if (pos >= minPos) {
                return pos + 1; // 包含断句符号
            }
        }
        // 未找到自然边界，按空格切分（适用于英文内容）
        int spacePos = text.lastIndexOf(' ', end - 1);
        if (spacePos >= minPos) {
            return spacePos + 1;
        }
        // 兜底：硬切
        return end;
    }

    /**
     * 添加 overlap：每个分片的开头包含上一个分片的尾部文本（仅在同一 Section 内部）
     */
    private List<Document> addOverlap(List<Document> chunks) {
        List<Document> overlappedChunks = new ArrayList<>();
        overlappedChunks.add(chunks.getFirst());
        for (int i = 1; i < chunks.size(); i++) {
            Document previousChunk = chunks.get(i - 1);
            Document currentChunk = chunks.get(i);
            String previousText = previousChunk.getText() == null ? "" : previousChunk.getText();
            String currentText = currentChunk.getText() == null ? "" : currentChunk.getText();
            int availableOverlapSize = Math.max(0, maxChunkSize - currentText.length() - 1);
            int finalOverlapSize = Math.min(overlapSize, availableOverlapSize);
            if (finalOverlapSize <= 0) {
                overlappedChunks.add(new Document(currentText, Map.copyOf(currentChunk.getMetadata())));
                continue;
            }
            String overlapText = previousText.length() <= finalOverlapSize
                    ? previousText
                    : previousText.substring(previousText.length() - finalOverlapSize);
            String combined = overlapText + "\n" + currentText;
            overlappedChunks.add(new Document(combined.strip(), Map.copyOf(currentChunk.getMetadata())));
        }
        return overlappedChunks;
    }

    private int resolveHeadingLevel(String headingLine) {
        int level = 0;
        while (level < headingLine.length() && headingLine.charAt(level) == '#') {
            level++;
        }
        return level;
    }

    private String resolveHeadingTitle(String headingLine) {
        return headingLine.replaceFirst("^#{1,6}\\s+", "").strip();
    }

    private void adjustHeadingStack(List<String> headingStack, Integer headingLevel, String headingTitle) {
        if (headingLevel == null || headingLevel <= 0) {
            return;
        }
        while (headingStack.size() >= headingLevel) {
            headingStack.remove(headingStack.size() - 1);
        }
        headingStack.add(headingTitle);
    }

    private String joinHeadingPath(List<String> headingStack) {
        if (headingStack.isEmpty()) {
            return null;
        }
        return String.join(" > ", headingStack);
    }

}
