package com.stephen.cloud.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.api.ai.model.dto.document.DocumentQueryRequest;
import com.stephen.cloud.api.ai.model.vo.DocumentVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentService extends IService<Document> {

    Long uploadDocument(MultipartFile file, Long knowledgeBaseId, Long userId);

    void validDocument(Document document, boolean add);

    LambdaQueryWrapper<Document> getQueryWrapper(DocumentQueryRequest queryRequest);

    DocumentVO getDocumentVO(Document document, HttpServletRequest request);

    Page<DocumentVO> getDocumentVOPage(Page<Document> page, HttpServletRequest request);

    void sendDocumentProcessMessage(Long documentId);
}
