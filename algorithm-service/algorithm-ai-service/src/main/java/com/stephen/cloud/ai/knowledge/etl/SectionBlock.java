package com.stephen.cloud.ai.knowledge.etl;

/**
 * 文档逻辑分段
 * <p>
 * 用于承载标题切分后的段落内容及其章节元数据。
 * </p>
 */
record SectionBlock(String content, String sectionTitle, String sectionPath) {
}
