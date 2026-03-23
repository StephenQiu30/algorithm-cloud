package com.stephen.cloud.ai.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.ai.convert.EmbeddingVectorConvert;
import com.stephen.cloud.ai.model.entity.EmbeddingVector;
import com.stephen.cloud.ai.service.EmbeddingVectorService;
import com.stephen.cloud.api.knowledge.model.dto.vector.EmbeddingVectorAddRequest;
import com.stephen.cloud.api.knowledge.model.dto.vector.EmbeddingVectorQueryRequest;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 向量元数据接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/ai/vector")
@Slf4j
@Tag(name = "EmbeddingVectorController", description = "向量元数据管理接口")
public class EmbeddingVectorController {

    @Resource
    private EmbeddingVectorService embeddingVectorService;

    @GetMapping("/get/vo")
    @Operation(summary = "获取向量元数据详情")
    public BaseResponse<EmbeddingVector> getById(@RequestParam("id") long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        EmbeddingVector vector = embeddingVectorService.getById(id);
        ThrowUtils.throwIf(vector == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(vector);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除向量元数据")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "向量元数据模块", action = "删除向量元数据")
    public BaseResponse<Boolean> deleteEmbeddingVector(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = embeddingVectorService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete/by/document")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "按文档删除向量")
    public BaseResponse<Boolean> deleteByDocumentId(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        embeddingVectorService.deleteByDocumentId(deleteRequest.getId());
        return ResultUtils.success(true);
    }

    @PostMapping("/add")
    @Operation(summary = "创建向量元数据")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "向量元数据模块", action = "创建向量元数据")
    public BaseResponse<Long> addEmbeddingVector(@RequestBody EmbeddingVectorAddRequest addRequest) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        EmbeddingVector embeddingVector = EmbeddingVectorConvert.INSTANCE.addRequestToObj(addRequest);
        boolean result = embeddingVectorService.save(embeddingVector);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(embeddingVector.getId());
    }

    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取向量元数据")
    public BaseResponse<Page<EmbeddingVector>> listByPage(@RequestBody EmbeddingVectorQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        LambdaQueryWrapper<EmbeddingVector> qw = new LambdaQueryWrapper<>();
        Long chunkId = queryRequest.getChunkId();
        String embeddingModel = queryRequest.getEmbeddingModel();
        Integer dimension = queryRequest.getDimension();
        String esDocId = queryRequest.getEsDocId();

        qw.eq(chunkId != null && chunkId > 0, EmbeddingVector::getChunkId, chunkId);
        qw.eq(embeddingModel != null && !embeddingModel.isBlank(), EmbeddingVector::getEmbeddingModel, embeddingModel);
        qw.eq(dimension != null, EmbeddingVector::getDimension, dimension);
        qw.eq(esDocId != null && !esDocId.isBlank(), EmbeddingVector::getEsDocId, esDocId);

        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();
        boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);

        if (SqlUtils.validSortField(sortField)) {
            switch (sortField) {
                case "createTime" -> qw.orderBy(true, isAsc, EmbeddingVector::getCreateTime);
                case "dimension" -> qw.orderBy(true, isAsc, EmbeddingVector::getDimension);
                default -> qw.orderByDesc(EmbeddingVector::getCreateTime);
            }
        } else {
            qw.orderByDesc(EmbeddingVector::getCreateTime);
        }

        Page<EmbeddingVector> page = embeddingVectorService.page(new Page<>(current, size), qw);
        return ResultUtils.success(page);
    }
}

