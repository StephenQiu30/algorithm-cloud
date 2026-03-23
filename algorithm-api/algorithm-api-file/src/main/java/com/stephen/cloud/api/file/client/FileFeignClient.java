package com.stephen.cloud.api.file.client;

import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.api.file.model.vo.FileUploadVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "algorithm-file-service", path = "/api/file", contextId = "fileFeignClient")
public interface FileFeignClient {

    /**
     * 文件上传
     *
     * @param file 业务文件
     * @param biz  业务类型
     * @return 文件上传结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    BaseResponse<FileUploadVO> uploadFile(@RequestPart("file") MultipartFile file, @RequestParam("biz") String biz);
}
