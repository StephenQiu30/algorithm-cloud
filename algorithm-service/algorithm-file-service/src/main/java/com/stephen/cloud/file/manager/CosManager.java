package com.stephen.cloud.file.manager;

import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.file.config.properties.CosProperties;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 腾讯云 COS 对象存储管理器
 * 提供文件上传和删除功能
 *
 * @author StephenQiu30
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "file.storage", name = "type", havingValue = "cos")
public class CosManager {

    @Resource
    private CosProperties cosProperties;

    @Resource
    private COSClient cosClient;

    /**
     * 上传文件到腾讯云 COS
     *
     * @param file 待上传的文件
     * @param path 上传的路径（目录），如 "images"、"documents"
     * @return 文件在 COS 的完整访问 URL
     * @throws BusinessException 文件为空或上传失败时抛出
     */
    public String uploadToCos(MultipartFile file, String path) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        // 获取文件的原始名称和后缀
        String originalName = file.getOriginalFilename();
        String suffix = FileNameUtil.getSuffix(originalName);
        long fileSize = file.getSize();

        // 生成唯一文件名（UUID + 后缀）
        String fileName = IdUtil.simpleUUID() + "." + suffix;
        // 拼接路径，确保没有重复的斜杠
        String filePath = (path.endsWith("/") ? path : path + "/") + fileName;
        // 移除开头的斜杠（COS Key 通常不以 / 开头）
        if (filePath.startsWith("/")) {
            filePath = filePath.substring(1);
        }

        try (InputStream inputStream = file.getInputStream()) {
            // 设置文件元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(fileSize);

            // 构建上传请求
            PutObjectRequest putRequest = new PutObjectRequest(
                    cosProperties.getBucket(),
                    filePath,
                    inputStream,
                    metadata);

            // 执行上传
            cosClient.putObject(putRequest);

            log.info("文件上传成功: {}", filePath);

            // 动态生成完整的访问URL
            String url = String.format("https://%s.cos.%s.myqcloud.com/%s",
                    cosProperties.getBucket(),
                    cosProperties.getRegion(),
                    filePath);
            return url;
        } catch (IOException | CosClientException e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 通过文件的 URL 从 COS 中删除文件
     *
     * @param url 文件URL
     */
    public void deleteByUrl(String url) {
        ThrowUtils.throwIf(StringUtils.isEmpty(url), ErrorCode.PARAMS_ERROR, "被删除地址为空");

        // 提取 Key：简单的做法是根据域名后的路径提取
        // URL 格式通常为
        // https://bucket-1250000000.cos.ap-guangzhou.myqcloud.com/path/to/file
        String bucket = cosProperties.getBucket();
        String region = cosProperties.getRegion();
        String host = String.format("%s.cos.%s.myqcloud.com/", bucket, region);

        int hostIndex = url.indexOf(host);
        if (hostIndex == -1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL不属于当前COS存储桶");
        }

        String key = url.substring(hostIndex + host.length());
        try {
            cosClient.deleteObject(bucket, key);
            log.info("文件删除成功: {}", key);
        } catch (CosClientException e) {
            log.error("文件删除失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败: " + e.getMessage());
        }
    }

}
