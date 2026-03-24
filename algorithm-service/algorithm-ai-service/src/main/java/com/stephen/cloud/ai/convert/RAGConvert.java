package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.RAGHistory;
import com.stephen.cloud.api.ai.model.vo.RAGHistoryVO;
import org.springframework.beans.BeanUtils;

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
