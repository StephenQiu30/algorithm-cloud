package com.stephen.cloud.file.service.impl;

import com.stephen.cloud.file.manager.CosManager;
import com.stephen.cloud.file.service.FileStorageService;
import org.springframework.web.multipart.MultipartFile;

/**
 * 腾讯云 COS 文件存储实现
 *
 * @author StephenQiu30
 */
public class CosFileStorageServiceImpl implements FileStorageService {

    private final CosManager cosManager;

    public CosFileStorageServiceImpl(CosManager cosManager) {
        this.cosManager = cosManager;
    }

    @Override
    public String upload(MultipartFile multipartFile, String path) {
        return cosManager.uploadToCos(multipartFile, path);
    }

    @Override
    public void delete(String url) {
        cosManager.deleteByUrl(url);
    }
}
