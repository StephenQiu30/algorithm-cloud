package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import org.springframework.beans.BeanUtils;

/**
 * RAG 对话历史转换器
 * <p>
 * 提供 RAGHistory、RAGHistoryVO 之间的相互转换
 * </p>
 *
 * @author StephenQiu30
 */
public class RAGConvert {

    public static final RAGConvert INSTANCE = new RAGConvert();

    public RAGHistoryVO objToVo(RAGHistory ragHistory) {
        if (ragHistory == null) {
            return null;
        }
        RAGHistoryVO ragHistoryVO = new RAGHistoryVO();
        BeanUtils.copyProperties(ragHistory, ragHistoryVO);
        return ragHistoryVO;
    }
}
