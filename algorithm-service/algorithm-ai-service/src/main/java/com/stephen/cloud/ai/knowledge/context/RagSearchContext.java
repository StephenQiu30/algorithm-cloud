package com.stephen.cloud.ai.knowledge.context;

import com.stephen.cloud.api.knowledge.model.vo.ChunkSourceVO;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索上下文：利用 ThreadLocal 记录单次请求中工具检索出的原始分片，
 * 以便在 Agentic RAG 模式下捕获引用源。
 *
 * @author StephenQiu30
 */
public class RagSearchContext {

    private static final ThreadLocal<List<ChunkSourceVO>> SEARCH_RESULTS = ThreadLocal.withInitial(ArrayList::new);

    /**
     * 添加检索结果
     */
    public static void addSources(List<ChunkSourceVO> sources) {
        if (sources != null) {
            SEARCH_RESULTS.get().addAll(sources);
        }
    }

    /**
     * 获取并清除结果（由 RagServiceImpl 在请求结束前调用）
     */
    public static List<ChunkSourceVO> getAndClearSources() {
        List<ChunkSourceVO> result = new ArrayList<>(SEARCH_RESULTS.get());
        SEARCH_RESULTS.remove();
        return result;
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        SEARCH_RESULTS.remove();
    }
}
