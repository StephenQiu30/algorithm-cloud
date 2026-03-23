package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordAddRequest;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordEditRequest;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordUpdateRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import org.springframework.beans.BeanUtils;

/**
 * AI 对话记录转换器
 *
 * @author StephenQiu30
 */
public class AiChatRecordConvert {

    public static final AiChatRecordConvert INSTANCE = new AiChatRecordConvert();

    /**
     * 对象转视图
     *
     * @param aiChatRecord AI 对话记录实体
     * @return AI 对话记录视图
     */
    public AiChatRecordVO objToVo(AiChatRecord aiChatRecord) {
        if (aiChatRecord == null) {
            return null;
        }
        AiChatRecordVO aiChatRecordVO = new AiChatRecordVO();
        BeanUtils.copyProperties(aiChatRecord, aiChatRecordVO);
        return aiChatRecordVO;
    }

    /**
     * 新增请求转对象
     *
     * @param addRequest 新增请求
     * @return AI 对话记录实体
     */
    public AiChatRecord addRequestToObj(AiChatRecordAddRequest addRequest) {
        if (addRequest == null) {
            return null;
        }
        AiChatRecord aiChatRecord = new AiChatRecord();
        BeanUtils.copyProperties(addRequest, aiChatRecord);
        return aiChatRecord;
    }

    /**
     * 更新请求转对象
     *
     * @param updateRequest 更新请求
     * @return AI 对话记录实体
     */
    public AiChatRecord updateRequestToObj(AiChatRecordUpdateRequest updateRequest) {
        if (updateRequest == null) {
            return null;
        }
        AiChatRecord aiChatRecord = new AiChatRecord();
        BeanUtils.copyProperties(updateRequest, aiChatRecord);
        return aiChatRecord;
    }

    /**
     * 编辑请求转对象
     *
     * @param editRequest 编辑请求
     * @return AI 对话记录实体
     */
    public AiChatRecord editRequestToObj(AiChatRecordEditRequest editRequest) {
        if (editRequest == null) {
            return null;
        }
        AiChatRecord aiChatRecord = new AiChatRecord();
        BeanUtils.copyProperties(editRequest, aiChatRecord);
        return aiChatRecord;
    }
}
