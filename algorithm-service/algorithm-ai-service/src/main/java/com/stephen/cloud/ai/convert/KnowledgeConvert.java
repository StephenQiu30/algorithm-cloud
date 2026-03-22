package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.KnowledgeBase;
import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeBaseAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeBaseEditRequest;
import com.stephen.cloud.api.knowledge.model.dto.KnowledgeBaseUpdateRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeBaseVO;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;
import org.springframework.beans.BeanUtils;

/**
 * 知识库转换器
 *
 * @author StephenQiu30
 */
public class KnowledgeConvert {

    /**
     * 对象转视图
     *
     * @param knowledgeBase 知识库实体
     * @return 知识库视图
     */
    public static KnowledgeBaseVO objToVo(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            return null;
        }
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        return vo;
    }

    /**
     * 新增请求转对象
     *
     * @param addRequest 新增请求
     * @return 知识库实体
     */
    public static KnowledgeBase addRequestToObj(KnowledgeBaseAddRequest addRequest) {
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
    public static KnowledgeBase updateRequestToObj(KnowledgeBaseUpdateRequest updateRequest) {
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
    public static KnowledgeBase editRequestToObj(KnowledgeBaseEditRequest editRequest) {
        if (editRequest == null) {
            return null;
        }
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        BeanUtils.copyProperties(editRequest, knowledgeBase);
        return knowledgeBase;
    }

    /**
     * 文档对象转视图
     *
     * @param entity 文档实体
     * @return 文档视图
     */
    public static KnowledgeDocumentVO entityToDocumentVo(KnowledgeDocument entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }
}
