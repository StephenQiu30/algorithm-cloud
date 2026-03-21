package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import org.springframework.beans.BeanUtils;

/**
 * AI 对话记录转换器
 *
 * @author StephenQiu30
 */
public class AiChatRecordConvert {

    /**
     * 对象转视图
     *
     * @param aiChatRecord AI 对话记录实体
     * @return AI 对话记录视图
     */
    public static AiChatRecordVO objToVo(AiChatRecord aiChatRecord) {
        if (aiChatRecord == null) {
            return null;
        }
        AiChatRecordVO aiChatRecordVO = new AiChatRecordVO();
        BeanUtils.copyProperties(aiChatRecord, aiChatRecordVO);
        return aiChatRecordVO;
    }
}
