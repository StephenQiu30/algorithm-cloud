package com.stephen.cloud.log.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordQueryRequest;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordUpdateStatusRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.log.mapper.EmailRecordMapper;
import com.stephen.cloud.log.model.entity.EmailRecord;
import com.stephen.cloud.log.service.EmailRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 邮件记录服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class EmailRecordServiceImpl extends ServiceImpl<EmailRecordMapper, EmailRecord>
        implements EmailRecordService {

    @Override
    public boolean addRecord(EmailRecordAddRequest request) {
        return addRecordReturnId(request) != null;
    }

    @Override
    public Long addRecordReturnId(EmailRecordAddRequest request) {
        if (request == null) {
            log.warn("邮件记录创建请求为空");
            return null;
        }
        EmailRecord emailRecord = new EmailRecord();
        BeanUtils.copyProperties(request, emailRecord);
        String msgId = emailRecord.getMsgId();
        if (StringUtils.isBlank(msgId)) {
            msgId = UUID.randomUUID().toString();
            emailRecord.setMsgId(msgId);
        }

        LambdaQueryWrapper<EmailRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EmailRecord::getMsgId, msgId);

        try {
            EmailRecord existingRecord = this.getOne(queryWrapper);
            if (existingRecord != null) {
                log.info("邮件记录已存在，执行更新操作, msgId: {}", msgId);
                emailRecord.setId(existingRecord.getId());
                this.updateById(emailRecord);
                return existingRecord.getId();
            } else {
                boolean saved = this.save(emailRecord);
                if (saved) {
                    return emailRecord.getId();
                }
            }
        } catch (DuplicateKeyException e) {
            log.warn("邮件记录主键/唯一索引冲突，尝试获取已存在的记录，msgId: {}", msgId);
            EmailRecord existingRecord = this.getOne(queryWrapper);
            if (existingRecord != null) {
                return existingRecord.getId();
            }
        } catch (Exception e) {
            log.error("邮件记录保存失败, msgId: {}, error: {}", msgId, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean updateRecordStatus(EmailRecordUpdateStatusRequest request) {
        Long id = request.getId();
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "邮件记录ID不能为空");
        EmailRecord oldRecord = this.getById(id);
        ThrowUtils.throwIf(oldRecord == null, ErrorCode.NOT_FOUND_ERROR, "邮件记录不存在");

        EmailRecord updateRecord = new EmailRecord();
        updateRecord.setId(id);
        updateRecord.setStatus(request.getStatus());
        updateRecord.setErrorMessage(request.getErrorMessage());
        return this.updateById(updateRecord);
    }

    @Override
    public LambdaQueryWrapper<EmailRecord> getQueryWrapper(EmailRecordQueryRequest queryRequest) {
        LambdaQueryWrapper<EmailRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        String msgId = queryRequest.getMsgId();
        String bizId = queryRequest.getBizId();
        String bizType = queryRequest.getBizType();
        String toEmail = queryRequest.getToEmail();
        String status = queryRequest.getStatus();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), EmailRecord::getId, id);
        queryWrapper.eq(StringUtils.isNotBlank(msgId), EmailRecord::getMsgId, msgId);
        queryWrapper.eq(StringUtils.isNotBlank(bizId), EmailRecord::getBizId, bizId);
        queryWrapper.eq(StringUtils.isNotBlank(bizType), EmailRecord::getBizType, bizType);
        queryWrapper.like(StringUtils.isNotBlank(toEmail), EmailRecord::getToEmail, toEmail);
        queryWrapper.eq(ObjectUtil.isNotNull(status), EmailRecord::getStatus, status);

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, EmailRecord::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, EmailRecord::getUpdateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }
}
