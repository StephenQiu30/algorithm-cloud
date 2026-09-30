package com.stephen.cloud.ai.knowledge.retrieval;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Client IDs are labels, never authority to access another user's memory. */
public final class RagConversationScope {
    private RagConversationScope() {}
    public static String resolve(String clientId, Long kbId, Long userId) {
        if (userId == null || userId <= 0 || kbId == null || kbId <= 0) throw new IllegalArgumentException("登录用户和知识库 ID 必须有效");
        if (clientId != null && clientId.length() > 256) throw new IllegalArgumentException("会话 ID 过长");
        String label = clientId == null || clientId.isBlank() ? UUID.randomUUID().toString() : clientId.trim();
        String prefix = "rag:v2:" + userId + ":" + kbId + ":";
        if (label.startsWith(prefix) && label.substring(prefix.length()).matches("[0-9a-f]{64}")) return label;
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(label.getBytes(StandardCharsets.UTF_8)));
            return "rag:v2:" + userId + ":" + kbId + ":" + hash;
        } catch (NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
}
