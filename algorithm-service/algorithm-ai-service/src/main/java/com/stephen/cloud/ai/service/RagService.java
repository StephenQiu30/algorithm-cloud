package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.knowledge.model.dto.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;

public interface RagService {

    RagChatResponseVO ragChat(Long knowledgeBaseId, RagChatRequest request, Long userId);
}
