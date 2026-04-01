package com.stephen.cloud.ai.knowledge.retrieval;

/**
 * RAG 元数据字段常量接口
 * <p>
 * 集中定义所有在 Document metadata 中使用的 key，
 * 消除各 Service / Converter 里的硬编码字符串，便于重构和全局查找引用。
 * 使用接口定义常量，字段默认为 public static final。
 * </p>
 *
 * @author StephenQiu30
 */
public interface RagMetadataKeys {

    // ==================== 分片标识 ====================
    String CHUNK_ID = "chunkId";
    String VECTOR_ID = "vectorId";
    String CHUNK_INDEX = "chunkIndex";

    // ==================== 文档 / 知识库 ====================
    String DOCUMENT_ID = "documentId";
    String DOCUMENT_NAME = "documentName";
    String KNOWLEDGE_BASE_ID = "knowledgeBaseId";

    // ==================== 章节结构 ====================
    String SECTION_TITLE = "sectionTitle";
    String SECTION_PATH = "sectionPath";

    // ==================== 得分 ====================
    String VECTOR_SCORE = "vectorScore";
    String KEYWORD_SCORE = "keywordScore";
    String KEYWORD_RANK = "keywordRank";
    String FUSION_SCORE = "fusionScore";
    String RERANK_SCORE = "rerankScore";
    String DISTANCE = "distance";
    String SCORE = "score";

    // ==================== 检索来源 / 匹配信息 ====================
    String SOURCE_TYPE = "sourceType";
    String MATCH_REASON = "matchReason";
    String ES_ID = "esId";

    // ==================== 业务标签 ====================
    String VERSION = "version";
    String BIZ_TAG = "bizTag";
}
