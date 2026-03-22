package com.stephen.cloud.ai.knowledge.util;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class TextChunker {

    private TextChunker() {
    }

    public static List<String> splitWithOverlap(String text, int chunkSize, int overlap) {
        List<String> out = new ArrayList<>();
        if (StringUtils.isBlank(text)) {
            return out;
        }
        String t = text.replace("\r\n", "\n").trim();
        if (t.isEmpty()) {
            return out;
        }
        int step = Math.max(1, chunkSize - overlap);
        for (int i = 0; i < t.length(); i += step) {
            int end = Math.min(t.length(), i + chunkSize);
            String piece = t.substring(i, end).trim();
            if (!piece.isEmpty()) {
                out.add(piece);
            }
            if (end >= t.length()) {
                break;
            }
        }
        return out;
    }
}
