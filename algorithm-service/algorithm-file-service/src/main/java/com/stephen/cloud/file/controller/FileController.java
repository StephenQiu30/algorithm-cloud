package com.stephen.cloud.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import com.stephen.cloud.api.file.model.dto.FileUploadLogDTO;
import com.stephen.cloud.api.file.model.dto.FileUploadRequest;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileUploadVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.file.config.properties.FileStorageProperties;
import com.stephen.cloud.file.service.FileStorageService;
import com.stephen.cloud.file.service.FileUploadRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

/**
 * 文件接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/file")
@Slf4j
@Tag(name = "FileController", description = "文件管理")
public class FileController {

    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private FileUploadRecordService fileUploadRecordService;

    @Resource
    private FileStorageProperties fileStorageProperties;

    /**
     * 文件上传 (通用)
     * <p>
     * 提供统一的文件上传能力。会根据 biz 参数进行文件校验（大小、类型等）。
     * 上传成功或失败后会异步记录日志。
     *
     * @param multipartFile     文件对象
     * @param fileUploadRequest 文件上传请求（含 biz）
     * @param request           HTTP 请求
     * @return 文件访问 URL 及元数据
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "文件上传", description = "统一样式的文件上传接口，支持按业务类型进行校验")
    @OperationLog(module = "文件管理", action = "上传文件")
    public BaseResponse<FileUploadVO> uploadFile(@RequestPart("file") @Parameter(description = "文件对象", required = true) MultipartFile multipartFile,
                                                 FileUploadRequest fileUploadRequest,
                                                 HttpServletRequest request) {
        // 校验文件不为空
        ThrowUtils.throwIf(multipartFile == null || multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 校验业务参数不为空
        ThrowUtils.throwIf(fileUploadRequest == null, ErrorCode.PARAMS_ERROR);
        String biz = fileUploadRequest.getBiz();
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.getEnumByValue(biz);
        ThrowUtils.throwIf(fileUploadBizEnum == null, ErrorCode.PARAMS_ERROR, "文件上传业务类型有误");

        // 校验文件类型及大小
        validFile(multipartFile, fileUploadBizEnum);

        // 权限校验（由于是文件服务，需要保证已登录）
        ThrowUtils.throwIf(!StpUtil.isLogin(), ErrorCode.NOT_LOGIN_ERROR);
        Long loginUserId = StpUtil.getLoginIdAsLong();

        // 文件目录：根据业务、用户来划分
        String path = String.format("/%s/%s/%s", fileStorageProperties.getPathPrefix(), fileUploadBizEnum.getValue(),
                loginUserId);

        // 上传文件
        String fileUrl;
        String clientIp = request.getRemoteAddr();
        String originalFilename = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        String contentType = multipartFile.getContentType();

        FileUploadLogDTO logDTO = FileUploadLogDTO.builder()
                .userId(loginUserId)
                .bizType(fileUploadBizEnum.getValue())
                .fileName(originalFilename)
                .fileSize(fileSize)
                .contentType(contentType)
                .objectKey(path)
                .clientIp(clientIp)
                .build();

        try {
            fileUrl = fileStorageService.upload(multipartFile, path);
            // 异步记录文件上传成功日志
            logDTO.setFileUrl(fileUrl);
            logDTO.setStatus("SUCCESS");
            fileUploadRecordService.recordFileUploadAsync(logDTO);
        } catch (Exception e) {
            log.error("文件上传异常, biz: {}, userId: {}", biz, loginUserId, e);
            // 异步记录文件上传失败日志
            logDTO.setStatus("FAILED");
            logDTO.setErrorMessage(e.getMessage());
            fileUploadRecordService.recordFileUploadAsync(logDTO);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "文件上传失败: " + e.getMessage());
        }

        // 返回结果封装为 VO
        FileUploadVO fileUploadVO = FileUploadVO.builder()
                .url(fileUrl)
                .fileName(multipartFile.getOriginalFilename())
                .build();

        return ResultUtils.success(fileUploadVO);
    }

    /**
     * 校验文件
     *
     * @param multipartFile     multipartFile
     * @param fileUploadBizEnum 业务类型
     */
    private void validFile(MultipartFile multipartFile, FileUploadBizEnum fileUploadBizEnum) {
        // 文件大小
        long fileSize = multipartFile.getSize();

        // 获取原始文件名
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }

        // 文件后缀（统一转为小写）
        String fileSuffix = FileUtil.getSuffix(originalFilename).toLowerCase();

        // 根据不同业务类型进行校验
        if (FileUploadBizEnum.USER_AVATAR.equals(fileUploadBizEnum)) {
            // 用户头像：最大 5MB
            long FIVE_MB = 5 * 1024L * 1024L;
            ThrowUtils.throwIf(fileSize > FIVE_MB, ErrorCode.PARAMS_ERROR, "用户头像文件大小不能超过 5M");
            // 允许的图片格式
            ThrowUtils.throwIf(!Arrays.asList("jpeg", "jpg", "svg", "png", "webp").contains(fileSuffix),
                    ErrorCode.PARAMS_ERROR, "用户头像仅支持 jpeg、jpg、svg、png、webp 格式");
        } else if (FileUploadBizEnum.POST_COVER.equals(fileUploadBizEnum)) {
            // 帖子封面：最大 10MB
            long TEN_MB = 10 * 1024L * 1024L;
            ThrowUtils.throwIf(fileSize > TEN_MB, ErrorCode.PARAMS_ERROR, "帖子封面文件大小不能超过 10M");
            // 允许的图片格式
            ThrowUtils.throwIf(!Arrays.asList("jpeg", "jpg", "png", "webp").contains(fileSuffix),
                    ErrorCode.PARAMS_ERROR, "帖子封面仅支持 jpeg、jpg、png、webp 格式");
        } else if (FileUploadBizEnum.POST_IMAGE_COVER.equals(fileUploadBizEnum)) {
            // 帖子上传图片：最大 10MB
            long TEN_MB = 10 * 1024L * 1024L;
            ThrowUtils.throwIf(fileSize > TEN_MB, ErrorCode.PARAMS_ERROR, "帖子上传图片文件大小不能超过 10M");
            // 允许的图片格式
            ThrowUtils.throwIf(!Arrays.asList("jpeg", "jpg", "png", "webp", "gif").contains(fileSuffix),
                    ErrorCode.PARAMS_ERROR, "帖子上传图片仅支持 jpeg、jpg、png、webp、gif 格式");
        } else if (FileUploadBizEnum.KNOWLEDGE.equals(fileUploadBizEnum)) {
            // 知识库文档：最大 100MB
            long HUNDRED_MB = 100 * 1024L * 1024L;
            ThrowUtils.throwIf(fileSize > HUNDRED_MB, ErrorCode.PARAMS_ERROR, "知识库文档大小不能超过 100M");
            // 允许的文档格式
            ThrowUtils.throwIf(!Arrays.asList("pdf", "docx", "doc", "txt", "md").contains(fileSuffix),
                    ErrorCode.PARAMS_ERROR, "知识库文档仅支持 pdf、docx、doc、txt、md 格式");
        } else if (FileUploadBizEnum.ALGORITHM_IMAGE.equals(fileUploadBizEnum)) {
            // 算法图片：最大 10MB
            long TEN_MB = 10 * 1024L * 1024L;
            ThrowUtils.throwIf(fileSize > TEN_MB, ErrorCode.PARAMS_ERROR, "算法图片文件大小不能超过 10M");
            // 允许的图片格式
            ThrowUtils.throwIf(!Arrays.asList("jpeg", "jpg", "png", "webp").contains(fileSuffix),
                    ErrorCode.PARAMS_ERROR, "算法图片仅支持 jpeg、jpg、png、webp 格式");
        }
    }
}
