package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.ai.model.vo.RAGAnswerVO;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import reactor.core.publisher.Flux;

public interface RAGService {

    RAGAnswerVO ask(String question, Long knowledgeBaseId, Long userId, Integer topK);

    Flux<String> askStream(String question, Long knowledgeBaseId, Long userId, Integer topK);

    void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId, String sources, Long responseTime);

    Page<RAGHistoryVO> listHistoryByPage(long current, long size, Long knowledgeBaseId, Long userId);
}
