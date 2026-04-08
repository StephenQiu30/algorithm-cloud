package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseAddRequest;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseEditRequest;
import com.stephen.cloud.api.ai.model.dto.knowledgebase.KnowledgeBaseUpdateRequest;
import com.stephen.cloud.api.ai.model.vo.KnowledgeBaseVO;
import org.springframework.beans.BeanUtils;

/**
 * 知识库转换器
 * <p>
 * 提供 KnowledgeBase、KnowledgeBaseVO 及各类请求对象之间的相互转换
 * </p>
 *
 * @author StephenQiu30
 */
public class KnowledgeBaseConvert {

    public static final KnowledgeBaseConvert INSTANCE = new KnowledgeBaseConvert();

    public KnowledgeBaseVO objToVo(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return null;
        }
        KnowledgeBaseVO knowledgeBaseVO = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, knowledgeBaseVO);
        return knowledgeBaseVO;
    }

    public KnowledgeBase addRequestToObj(KnowledgeBaseAddRequest addRequest) {
        if (addRequest == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(addRequest, knowledgeBase);
        return knowledgeBase;
    }

    public KnowledgeBase updateRequestToObj(KnowledgeBaseUpdateRequest updateRequest) {
        if (updateRequest == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(updateRequest, knowledgeBase);
        return knowledgeBase;
    }

    public KnowledgeBase editRequestToObj(KnowledgeBaseEditRequest editRequest) {
        if (editRequest == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(editRequest, knowledgeBase);
        return knowledgeBase;
    }
}
