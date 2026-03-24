package com.stephen.cloud.ai.convert;

import com.stephen.cloud.ai.model.entity.Document;
import com.stephen.cloud.api.ai.model.vo.DocumentVO;
import org.springframework.beans.BeanUtils;

public class DocumentConvert {

    public static final DocumentConvert INSTANCE = new DocumentConvert();

    public DocumentVO objToVo(Document document) {
        if (document == null) {
            return null;
        }
        DocumentVO documentVO = new DocumentVO();
        BeanUtils.copyProperties(document, documentVO);
        return documentVO;
    }
}
