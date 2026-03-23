package com.stephen.cloud.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 知识库 / RAG 相关配置（前缀 {@code app.knowledge}），与
 * {@link KnowledgeVectorStoreConfig}、{@link com.stephen.cloud.ai.service.impl.VectorStoreServiceImpl}、
 * {@link com.stephen.cloud.ai.knowledge.util.TextChunker} 等共用。
 *
 * @author StephenQiu30
 */
@Data
@ConfigurationProperties(prefix = "app.knowledge")
public class KnowledgeProperties {

    /** Elasticsearch 向量索引名。 */
    private String vectorIndex = "algorithm-knowledge-vectors";

    /** 嵌入向量维度，须与所用 embedding 模型一致。 */
    private int embeddingDimension = 1536;

    /** 分片目标 token 数（Spring AI TokenTextSplitter）。 */
    private int chunkSize = 800;

    /** 分片重叠 token 数，用于相邻切片语义衔接。 */
    private int chunkOverlap = 100;

    /** 默认检索返回条数（RAG 与 {@link com.stephen.cloud.ai.service.KnowledgeRetrievalService}）。 */
    private int defaultTopK = 5;

    /**
     * 相似度下限，低于该值的检索结果将被过滤（具体语义由向量存储实现决定）。
     */
    private double similarityThreshold = 0.5;

    /**
     * 临时下载缓存根目录。
     */
    private String cacheDir = "./data/algorithm-kb-cache";

    /**
     * 本地永久文档库目录：解析后的文档或从 COS 下载后的镜像将永久存放于此，
     * 以便节省资源和提高二次解析速度。
     */
    private String localLibraryDir = "./data/algorithm-kb-library";

    /** 入库时写入 {@code embedding_vector} 表的嵌入模型名称。 */
    private String embeddingModelName = "text-embedding-v2";

    /**
     * SSE 等流式场景超时（毫秒）；当前 RAG 主路径为同步 HTTP，本项供后续扩展使用。
     */
    private Long sseTimeout = 60000L;

    /**
     * 是否启用 ES 双路召回：kNN + BM25，经 RRF 融合（关闭则仅 kNN）。
     */
    private boolean hybridSearchEnabled = true;

    /**
     * BM25 一路拉取条数，应不小于 topK，略大有利于 RRF 融合。
     */
    private int hybridBm25FetchSize = 30;

    /**
     * RRF 平滑常数 k，越大则排名靠后的命中权重衰减越快。
     */
    private int rrfRankConstant = 60;

    /**
     * 段落合并字符预算：相邻短段落先合并再按 token 切分，减轻「半句一切」对教学段落的影响。
     */
    private int chunkParagraphMergeCharBudget = 2500;

    /** RAG 检索 topK 上限（{@link com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade}）。 */
    private int ragTopKMax = 20;

    /** 诊断检索 topK 上限（{@link com.stephen.cloud.ai.manager.KnowledgeChunkSearchFacade}）。 */
    private int retrievalTopKMax = 50;

    /**
     * BM25 检索时的最小匹配百分比（例如 "50%"）。
     */
    private String bm25MinimumShouldMatch = "50%";

    /**
     * 向量入库时的批量大小，防止单次请求过大。
     */
    private int vectorBatchSize = 100;

}
