package com.stephen.cloud.ai.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.RAGConvert;
import com.stephen.cloud.ai.mapper.RAGHistoryMapper;
import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.ai.service.RAGService;
import com.stephen.cloud.api.ai.model.vo.RAGAnswerVO;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import com.stephen.cloud.api.ai.model.vo.SourceVO;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class RAGServiceImpl implements RAGService {

    @Resource
    private VectorStore vectorStore;

    @Resource
    private ChatClient chatClient;

    @Resource
    private RAGHistoryMapper ragHistoryMapper;

    @Override
    public RAGAnswerVO ask(String question, Long knowledgeBaseId, Long userId, Integer topK) {
        long start = System.currentTimeMillis();
        List<Document> docs = filterByKnowledgeBase(vectorStore.similaritySearch(question), knowledgeBaseId, topK);
        String context = buildContext(docs);
        String prompt = buildPrompt(question, context);
        String answer = chatClient.prompt().user(prompt).call().content();
        List<SourceVO> sources = buildSources(docs);
        long responseTime = System.currentTimeMillis() - start;
        saveHistory(question, answer, knowledgeBaseId, userId, JSONUtil.toJsonStr(sources), responseTime);
        RAGAnswerVO vo = new RAGAnswerVO();
        vo.setAnswer(answer);
        vo.setSources(sources);
        vo.setResponseTime(responseTime);
        return vo;
    }

    @Override
    public Flux<String> askStream(String question, Long knowledgeBaseId, Long userId, Integer topK) {
        List<Document> docs = filterByKnowledgeBase(vectorStore.similaritySearch(question), knowledgeBaseId, topK);
        String context = buildContext(docs);
        String prompt = buildPrompt(question, context);
        return chatClient.prompt().user(prompt).stream().content();
    }

    @Override
    public void saveHistory(String question, String answer, Long knowledgeBaseId, Long userId, String sources, Long responseTime) {
        RAGHistory ragHistory = new RAGHistory();
        ragHistory.setQuestion(question);
        ragHistory.setAnswer(answer);
        ragHistory.setKnowledgeBaseId(knowledgeBaseId);
        ragHistory.setUserId(userId);
        ragHistory.setSources(sources);
        ragHistory.setResponseTime(responseTime);
        ragHistoryMapper.insert(ragHistory);
    }

    @Override
    public Page<RAGHistoryVO> listHistoryByPage(long current, long size, Long knowledgeBaseId, Long userId) {
        LambdaQueryWrapper<RAGHistory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(knowledgeBaseId != null && knowledgeBaseId > 0, RAGHistory::getKnowledgeBaseId, knowledgeBaseId)
                .eq(userId != null && userId > 0, RAGHistory::getUserId, userId)
                .orderByDesc(RAGHistory::getCreateTime);
        Page<RAGHistory> page = new Page<>(current, size);
        Page<RAGHistory> historyPage = ragHistoryMapper.selectPage(page, queryWrapper);
        Page<RAGHistoryVO> voPage = new Page<>(historyPage.getCurrent(), historyPage.getSize(), historyPage.getTotal());
        List<RAGHistoryVO> voList = historyPage.getRecords().stream().map(item -> {
            RAGHistoryVO vo = RAGConvert.INSTANCE.objToVo(item);
            String sourceJson = item.getSources();
            if (StringUtils.isNotBlank(sourceJson)) {
                vo.setSources(JSONUtil.toList(sourceJson, SourceVO.class));
            }
            return vo;
        }).toList();
        voPage.setRecords(voList);
        return voPage;
    }

    private String buildContext(List<Document> docs) {
        if (CollUtil.isEmpty(docs)) {
            return "";
        }
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            context.append("片段").append(i + 1).append(":\n").append(doc.getText()).append("\n\n");
        }
        return context.toString();
    }

    private String buildPrompt(String question, String context) {
        if (StringUtils.isBlank(context)) {
            return "你是知识库问答助手。用户问题是：" + question + "。当前知识库没有检索到相关内容，请明确告知用户。";
        }
        return "你是知识库问答助手。请基于以下上下文回答问题，若上下文不足请明确说明。\n\n上下文:\n"
                + context + "\n问题:\n" + question;
    }

    private List<SourceVO> buildSources(List<Document> docs) {
        List<SourceVO> sourceList = new ArrayList<>();
        if (CollUtil.isEmpty(docs)) {
            return sourceList;
        }
        for (Document doc : docs) {
            SourceVO sourceVO = new SourceVO();
            Object documentId = doc.getMetadata().get("documentId");
            Object documentName = doc.getMetadata().get("documentName");
            Object chunkIndex = doc.getMetadata().get("chunkIndex");
            Object score = doc.getMetadata().get("distance");
            if (documentId != null) {
                sourceVO.setDocumentId(Long.valueOf(String.valueOf(documentId)));
            }
            sourceVO.setDocumentName(documentName == null ? null : String.valueOf(documentName));
            if (chunkIndex != null) {
                sourceVO.setChunkIndex(Integer.valueOf(String.valueOf(chunkIndex)));
            }
            sourceVO.setContent(doc.getText());
            if (score != null) {
                sourceVO.setScore(Double.valueOf(String.valueOf(score)));
            }
            sourceList.add(sourceVO);
        }
        return sourceList;
    }

    private List<Document> filterByKnowledgeBase(List<Document> docs, Long knowledgeBaseId, Integer topK) {
        if (CollUtil.isEmpty(docs)) {
            return List.of();
        }
        int limit = topK == null || topK <= 0 ? 5 : topK;
        return docs.stream().filter(doc -> {
            if (knowledgeBaseId == null || knowledgeBaseId <= 0) {
                return true;
            }
            Object kbIdObj = doc.getMetadata().get("knowledgeBaseId");
            return kbIdObj != null && String.valueOf(knowledgeBaseId).equals(String.valueOf(kbIdObj));
        }).limit(limit).toList();
    }
}
