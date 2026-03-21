package com.stephen.cloud.file.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 *
 * @author StephenQiu30
 */
public interface FileStorageService {

    /**
     * 上传文件到指定的存储路径
     *
     * @param multipartFile 待上传的文件对象
     * @param path          存储路径 (不包含文件名，或包含完整路径)
     * @return 上传成功后的文件访问 URL 地址
     */
    String upload(MultipartFile multipartFile, String path);

    /**
     * 删除文件
     *
     * @param url 文件地址
     */
    void delete(String url);
}
