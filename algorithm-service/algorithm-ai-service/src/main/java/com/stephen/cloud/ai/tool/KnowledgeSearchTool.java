package com.stephen.cloud.ai.tool;

import com.stephen.cloud.ai.config.KnowledgeProperties;
import com.stephen.cloud.ai.knowledge.context.RagSearchContext;
import com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade;
import com.stephen.cloud.api.ai.model.enums.AiToolEnum;
import com.stephen.cloud.api.knowledge.model.dto.search.KnowledgeSearchRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeSearchVO;
import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class KnowledgeSearchTool {

    /**
     * 定义 Spring AI 检索函数 Bean。
     * 该 Bean 会被自动注入到 ChatClient 的 Tool Registry 中。
     *
     * @param searchFacade        检索门面：负责汇聚多种检索策略
     * @param knowledgeProperties 配置属性：获取默认检索阈值 and TopK
     * @return 函数接口实现
     */
    @Bean(AiToolEnum.ALGORITHM_KNOWLEDGE_SEARCH_VALUE)
    @Description("搜索排序算法相关的私有知识库，以获取精准的算法描述、代码实现和复杂度分析。")
    public Function<KnowledgeSearchRequest, KnowledgeSearchVO> algorithmKnowledgeSearch(
            KnowledgeChunkSearchFacade searchFacade,
            KnowledgeProperties knowledgeProperties) {
        return request -> {
            log.info("Tool algorithmKnowledgeSearch calling: query={}, kbId={}", request.getQuery(), request.getKnowledgeBaseId());
            List<ChunkSourceVO> sources = searchFacade.searchChunks(
                    request.getKnowledgeBaseId(),
                    request.getQuery(),
                    knowledgeProperties.getDefaultTopK(),
                    knowledgeProperties.getRagTopKMax()
            );
            // 记录检索过程中的原始内容，以便在 RAG 最终响应中通过 RagSearchContext 返回引用
            RagSearchContext.addSources(sources);
            return new KnowledgeSearchVO(sources);
        };
    }
}
