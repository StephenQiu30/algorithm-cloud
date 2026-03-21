package com.stephen.cloud.ai.mq.handler;

import cn.hutool.core.bean.BeanUtil;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordDTO;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqDedupeLock;
import com.stephen.cloud.common.rabbitmq.consumer.RabbitMqHandler;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.RabbitMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RabbitMqDedupeLock(prefix = "mq:ai:chat", expire = 86400)
public class AiChatRecordHandler implements RabbitMqHandler<AiChatRecordDTO> {

    @Resource
    private AiChatRecordService aiChatRecordService;

    @Override
    public String getBizType() {
        return MqBizTypeEnum.AI_CHAT_RECORD.getValue();
    }

    @Override
    public void onMessage(AiChatRecordDTO dto, RabbitMessage rabbitMessage) throws Exception {
        log.info("[AiChatRecordHandler] 收到持久化请求, userId: {}, sessionId: {}",
                dto.getUserId(), dto.getSessionId());
        AiChatRecord aiChatRecord = new AiChatRecord();
        BeanUtil.copyProperties(dto, aiChatRecord);
        aiChatRecordService.save(aiChatRecord);
        log.info("[AiChatRecordHandler] 持久化成功, msgId: {}, recordId: {}",
                rabbitMessage.getMsgId(), aiChatRecord.getId());
    }

    @Override
    public Class<AiChatRecordDTO> getDataType() {
        return AiChatRecordDTO.class;
    }
}
