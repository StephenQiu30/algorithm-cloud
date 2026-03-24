package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.ai.model.dto.rag.BatchRecallRequest;
import com.stephen.cloud.api.ai.model.dto.rag.RecallAnalysisRequest;
import com.stephen.cloud.api.ai.model.vo.BatchRecallVO;
import com.stephen.cloud.api.ai.model.vo.RAGAnswerVO;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import com.stephen.cloud.api.ai.model.vo.RecallAnalysisVO;
import reactor.core.publisher.Flux;

public interface RAGService {

    RAGAnswerVO ask(String question, Long knowledgeBaseId, Long userId, Integer topK);

    Flux<String> askStream(String question, Long knowledgeBaseId, Long userId, Integer topK);

    void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId, String sources, Long responseTime);

    Page<RAGHistoryVO> listHistoryByPage(long current, long size, Long knowledgeBaseId, Long userId);

    RecallAnalysisVO analyzeRecall(RecallAnalysisRequest request);

    BatchRecallVO batchAnalyzeRecall(BatchRecallRequest request);
}
