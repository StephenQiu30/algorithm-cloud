package com.stephen.cloud.api.knowledge.model.dto;

import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;

import java.io.Serializable;
import java.util.List;

/**
 * 知识库检索响应 DTO：返回给 LLM 的引用数据。
 *
 * @param sources 检索到的知识分片列表
 * @author StephenQiu30
 */
public record KnowledgeSearchResponse(List<ChunkSourceVO> sources) implements Serializable {}
