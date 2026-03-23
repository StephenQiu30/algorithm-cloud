package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.KnowledgeDocument;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentEditRequest;
import com.stephen.cloud.api.knowledge.model.dto.knowledgedocument.KnowledgeDocumentUpdateRequest;
import com.stephen.cloud.api.knowledge.model.vo.KnowledgeDocumentVO;
import org.springframework.beans.BeanUtils;

/**
 * 知识文档转换器
 *
 * @author StephenQiu30
 */
public class KnowledgeDocumentConvert {

    public static final KnowledgeDocumentConvert INSTANCE = new KnowledgeDocumentConvert();

    /**
     * 文档对象转视图
     *
     * @param entity 文档实体
     * @return 文档视图
     */
    public KnowledgeDocumentVO entityToDocumentVo(KnowledgeDocument entity) {
        if (entity == null) {
            return null;
        }
        KnowledgeDocumentVO vo = new KnowledgeDocumentVO();
        BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 新增请求转文档对象
     *
     * @param addRequest 新增请求
     * @return 知识文档实体
     */
    public KnowledgeDocument documentAddRequestToObj(KnowledgeDocumentAddRequest addRequest) {
        if (addRequest == null) {
            return null;
        }
        KnowledgeDocument doc = new KnowledgeDocument();
        BeanUtils.copyProperties(addRequest, doc);
        return doc;
    }

    /**
     * 更新请求转文档对象
     *
     * @param updateRequest 更新请求
     * @return 知识文档实体
     */
    public KnowledgeDocument documentUpdateRequestToObj(KnowledgeDocumentUpdateRequest updateRequest) {
        if (updateRequest == null) {
            return null;
        }
        KnowledgeDocument doc = new KnowledgeDocument();
        BeanUtils.copyProperties(updateRequest, doc);
        return doc;
    }

    /**
     * 编辑请求转文档对象
     *
     * @param editRequest 编辑请求
     * @return 知识文档实体
     */
    public KnowledgeDocument documentEditRequestToObj(KnowledgeDocumentEditRequest editRequest) {
        if (editRequest == null) {
            return null;
        }
        KnowledgeDocument doc = new KnowledgeDocument();
        BeanUtils.copyProperties(editRequest, doc);
        return doc;
    }
}
