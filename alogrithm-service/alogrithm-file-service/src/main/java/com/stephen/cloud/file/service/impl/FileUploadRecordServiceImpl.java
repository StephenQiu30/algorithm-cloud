package com.stephen.cloud.file.service.impl;

import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;
import com.stephen.cloud.api.file.model.dto.FileUploadLogDTO;
import com.stephen.cloud.file.config.properties.FileStorageProperties;
import com.stephen.cloud.file.service.FileUploadRecordService;
import cn.hutool.core.io.file.FileNameUtil;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 文件上传记录服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class FileUploadRecordServiceImpl implements FileUploadRecordService {

    @Lazy
    @Resource
    private LogFeignClient logFeignClient;

    @Resource
    private FileStorageProperties fileStorageProperties;

    /**
     * 异步记录文件上传日志
     *
     * @param logDTO 文件上传日志 DTO
     */
    @Async
    @Override
    public void recordFileUploadAsync(FileUploadLogDTO logDTO) {
        if (logDTO == null) {
            return;
        }
        try {
            String fileName = logDTO.getFileName();
            if (StringUtils.isBlank(fileName)) {
                log.warn("文件名为空，跳过详细信息记录");
                return;
            }

            FileUploadRecordAddRequest request = new FileUploadRecordAddRequest();
            request.setUserId(logDTO.getUserId());
            request.setBizType(logDTO.getBizType());
            request.setFileName(fileName);
            request.setFileSize(logDTO.getFileSize());

            // 获取文件后缀
            request.setFileSuffix(FileNameUtil.getSuffix(fileName));

            request.setContentType(logDTO.getContentType());
            request.setStorageType(fileStorageProperties.getType());
            request.setObjectKey(logDTO.getObjectKey());
            request.setUrl(logDTO.getFileUrl());
            request.setClientIp(logDTO.getClientIp());
            request.setStatus(logDTO.getStatus());
            request.setErrorMessage(logDTO.getErrorMessage());

            createRecord(request);
        } catch (Exception e) {
            log.error("构建并记录文件上传日志失败", e);
        }
    }

    /**
     * 创建文件上传记录（直接通过请求对象）
     *
     * @param request 创建请求
     */
    @Override
    public void createRecord(FileUploadRecordAddRequest request) {
        if (request == null) {
            return;
        }
        try {
            logFeignClient.addFileUploadRecord(request);
            log.debug("文件上传记录已发送至日志中心: bizType={}, status={}", request.getBizType(), request.getStatus());
        } catch (Exception e) {
            log.error("调用日志服务记录文件上传失败", e);
        }
    }
}
