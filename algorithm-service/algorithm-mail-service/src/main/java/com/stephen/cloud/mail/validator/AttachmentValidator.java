package com.stephen.cloud.mail.validator;

import com.stephen.cloud.api.mail.model.dto.EmailAttachment;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * 附件验证器
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class AttachmentValidator {

    /**
     * 最大附件大小（10MB）
     */
    private static final long MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024;

    /**
     * 允许的附件类型
     */
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "image/gif",
            "text/plain",
            "application/zip");

    /**
     * 危险文件扩展名
     */
    private static final List<String> DANGEROUS_EXTENSIONS = Arrays.asList(
            ".exe", ".bat", ".cmd", ".sh", ".ps1", ".vbs", ".js", ".jar");

    /**
     * 验证附件
     *
     * @param attachment 附件
     */
    public void validate(EmailAttachment attachment) {
        if (attachment == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "附件不能为空");
        }

        // 验证文件名
        String filename = attachment.getFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "附件文件名不能为空");
        }

        // 验证危险扩展名
        String lowerFilename = filename.toLowerCase();
        for (String ext : DANGEROUS_EXTENSIONS) {
            if (lowerFilename.endsWith(ext)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许的附件类型: " + ext);
            }
        }

        // 验证内容类型
        String contentType = attachment.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("不支持的附件类型: {}, 文件名: {}", contentType, filename);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的附件类型: " + contentType);
        }

        // 验证大小
        Long size = attachment.getSize();
        if (size != null && size > MAX_ATTACHMENT_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    String.format("附件大小超过限制 (最大 %d MB)", MAX_ATTACHMENT_SIZE / 1024 / 1024));
        }

        // 验证 Base64 内容
        String content = attachment.getContent();
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "附件内容不能为空");
        }

        try {
            Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "附件内容格式错误（需要 Base64 编码）");
        }
    }

    /**
     * 验证附件列表
     *
     * @param attachments 附件列表
     */
    public void validateAll(List<EmailAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        if (attachments.size() > 5) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "附件数量不能超过 5 个");
        }

        for (EmailAttachment attachment : attachments) {
            validate(attachment);
        }
    }
}
