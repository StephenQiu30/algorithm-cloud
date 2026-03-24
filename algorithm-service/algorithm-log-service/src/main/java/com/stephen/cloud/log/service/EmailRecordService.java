package com.stephen.cloud.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordQueryRequest;
import com.stephen.cloud.log.model.entity.EmailRecord;

/**
 * 邮件记录服务
 *
 * @author StephenQiu30
 */
public interface EmailRecordService extends IService<EmailRecord> {

    /**
     * 添加邮件记录
     *
     * @param request 记录创建请求
     * @return 是否添加成功
     */
    boolean addRecord(EmailRecordAddRequest request);


    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param queryRequest 邮件记录查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<EmailRecord> getQueryWrapper(EmailRecordQueryRequest queryRequest);
}
