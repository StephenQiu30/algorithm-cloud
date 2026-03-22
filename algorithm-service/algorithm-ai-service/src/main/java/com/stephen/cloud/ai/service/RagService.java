package com.stephen.cloud.ai.service;

import com.stephen.cloud.api.knowledge.model.dto.RagChatRequest;
import com.stephen.cloud.api.knowledge.model.vo.RagChatResponseVO;

public interface RagService {

    RagChatResponseVO ragChat(RagChatRequest request, Long userId);
}
