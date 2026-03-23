package com.stephen.cloud.ai.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.api.ai.model.enums.AiToolEnum;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

/**
 * 语义化知识检索工具：基于 Spring AI Function Calling 实现。
 * 将传统的 RAG 检索逻辑封装为 LLM 可自主调度的工具。
 *
 * @author StephenQiu30
 */
@Configuration
public class KnowledgeSearchTool {

    /**
     * 检索请求 DTO
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonClassDescription("算法知识库检索请求")
    public static class Request {
        @JsonProperty(required = true)
        @JsonPropertyDescription("需要检索的问题或核心算法关键词")
        private String query;

        @JsonProperty(required = true)
        @JsonPropertyDescription("当前对话关联的知识库 ID")
        private Long knowledgeBaseId;
    }

    /**
     * 检索响应 DTO
     *
     * @param sources 检索到的知识分片列表
     */
    public record Response(List<ChunkSourceVO> sources) {}

    /**
     * 定义 Spring AI 检索函数 Bean
     */
    @Bean(AiToolEnum.ALGORITHM_KNOWLEDGE_SEARCH_VALUE)
    @Description("搜索排序算法相关的私有知识库，以获取精准的算法描述、代码实现和复杂度分析。")
    public Function<Request, Response> algorithmKnowledgeSearch(
            KnowledgeChunkSearchFacade searchFacade,
            KnowledgeProperties knowledgeProperties) {
        return request -> {
            List<ChunkSourceVO> sources = searchFacade.searchChunks(
                    request.getKnowledgeBaseId(),
                    request.getQuery(),
                    knowledgeProperties.getDefaultTopK(),
                    knowledgeProperties.getRagTopKMax()
            );
            return new Response(sources);
        };
    }
}
