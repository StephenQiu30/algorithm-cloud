package com.stephen.cloud.log.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordQueryRequest;
import com.stephen.cloud.log.model.entity.FileUploadRecord;

/**
 * 文件上传记录服务
 *
 * @author StephenQiu30
 */
public interface FileUploadRecordService extends IService<FileUploadRecord> {

    /**
     * 创建文件上传记录
     *
     * @param request 记录创建请求
     * @return 是否创建成功
     */
    boolean addRecord(FileUploadRecordAddRequest request);

    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param queryRequest 文件上传记录查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<FileUploadRecord> getQueryWrapper(FileUploadRecordQueryRequest queryRequest);
}
