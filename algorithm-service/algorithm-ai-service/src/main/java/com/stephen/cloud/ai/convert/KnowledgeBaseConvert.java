package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseEditRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgebase.KnowledgeBaseUpdateRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import org.springframework.beans.BeanUtils;

/**
 * 知识库转换器
 *
 * @author StephenQiu30
 */
public class KnowledgeBaseConvert {

    public static final KnowledgeBaseConvert INSTANCE = new KnowledgeBaseConvert();

    /**
     * 对象转视图
     *
     * @param knowledgeBase 知识库实体
     * @return 知识库视图
     */
    public KnowledgeBaseVO objToVo(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return null;
        }
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        return vo;
    }

    /**
     * 知识库：新增请求转对象
     *
     * @param addRequest 新增请求
     * @return 知识库实体
     */
    public KnowledgeBase addRequestToObj(KnowledgeBaseAddRequest addRequest) {
        if (addRequest == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(addRequest, knowledgeBase);
        return knowledgeBase;
    }

    /**
     * 更新请求转对象
     *
     * @param updateRequest 更新请求
     * @return 知识库实体
     */
    public KnowledgeBase updateRequestToObj(KnowledgeBaseUpdateRequest updateRequest) {
        if (updateRequest == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(updateRequest, knowledgeBase);
        return knowledgeBase;
    }

    /**
     * 编辑请求转对象
     *
     * @param editRequest 编辑请求
     * @return 知识库实体
     */
    public KnowledgeBase editRequestToObj(KnowledgeBaseEditRequest editRequest) {
        if (editRequest == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(editRequest, knowledgeBase);
        return knowledgeBase;
    }
}
