package com.stephen.cloud.log.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordQueryRequest;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.log.mapper.FileUploadRecordMapper;
import com.stephen.cloud.log.model.entity.FileUploadRecord;
import com.stephen.cloud.log.service.FileUploadRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 文件上传记录服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class FileUploadRecordServiceImpl extends ServiceImpl<FileUploadRecordMapper, FileUploadRecord>
        implements FileUploadRecordService {

    @Override
    public boolean addRecord(FileUploadRecordAddRequest request) {
        if (request == null) {
            log.warn("文件上传记录创建请求为空");
            return false;
        }
        FileUploadRecord fileUploadRecord = new FileUploadRecord();
        BeanUtils.copyProperties(request, fileUploadRecord);
        return this.save(fileUploadRecord);
    }

    @Override
    public LambdaQueryWrapper<FileUploadRecord> getQueryWrapper(FileUploadRecordQueryRequest queryRequest) {
        LambdaQueryWrapper<FileUploadRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (queryRequest == null) {
            return queryWrapper;
        }
        Long id = queryRequest.getId();
        Long userId = queryRequest.getUserId();
        String bizType = queryRequest.getBizType();
        String fileName = queryRequest.getFileName();
        String status = queryRequest.getStatus();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        queryWrapper.eq(ObjectUtil.isNotNull(id), FileUploadRecord::getId, id);
        queryWrapper.eq(ObjectUtil.isNotNull(userId), FileUploadRecord::getUserId, userId);
        queryWrapper.eq(StringUtils.isNotBlank(bizType), FileUploadRecord::getBizType, bizType);
        queryWrapper.like(StringUtils.isNotBlank(fileName), FileUploadRecord::getFileName, fileName);
        queryWrapper.eq(ObjectUtil.isNotNull(status), FileUploadRecord::getStatus, status);

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, FileUploadRecord::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, FileUploadRecord::getUpdateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }
}
