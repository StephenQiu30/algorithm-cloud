package com.stephen.cloud.file.service;

import com.stephen.cloud.api.file.model.dto.FileUploadLogDTO;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;

/**
 * 文件上传记录服务
 *
 * @author StephenQiu30
 */
public interface FileUploadRecordService {

    /**
     * 异步记录文件上传日志
     *
     * @param logDTO 文件上传日志 DTO
     */
    void recordFileUploadAsync(FileUploadLogDTO logDTO);

    /**
     * 创建文件上传记录（直接通过请求对象）
     *
     * @param request 创建请求
     */
    void createRecord(FileUploadRecordAddRequest request);
}
