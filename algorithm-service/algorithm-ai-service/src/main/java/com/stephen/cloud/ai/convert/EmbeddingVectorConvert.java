package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.api.knowledge.model.dto.vector.EmbeddingVectorAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.vector.EmbeddingVectorUpdateRequest;
import org.springframework.beans.BeanUtils;

/**
 * 向量元数据转换器
 *
 * @author StephenQiu30
 */
public class EmbeddingVectorConvert {

    public static final EmbeddingVectorConvert INSTANCE = new EmbeddingVectorConvert();

    /**
     * 新增请求转对象
     *
     * @param addRequest 新增请求
     * @return 向量元数据实体
     */
    public EmbeddingVector addRequestToObj(EmbeddingVectorAddRequest addRequest) {
        if (addRequest == null) {
            return null;
        }
        EmbeddingVector vector = new EmbeddingVector();
        BeanUtils.copyProperties(addRequest, vector);
        return vector;
    }

    /**
     * 更新请求转对象
     *
     * @param updateRequest 更新请求
     * @return 向量元数据实体
     */
    public EmbeddingVector updateRequestToObj(EmbeddingVectorUpdateRequest updateRequest) {
        if (updateRequest == null) {
            return null;
        }
        EmbeddingVector vector = new EmbeddingVector();
        BeanUtils.copyProperties(updateRequest, vector);
        return vector;
    }
}
