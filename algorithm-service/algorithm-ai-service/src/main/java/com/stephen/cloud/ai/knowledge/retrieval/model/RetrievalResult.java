package com.stephen.cloud.ai.knowledge.retrieval.model;

import lombok.Data;
import org.springframework.ai.document.Document;

import java.util.List;

@Data
public class RetrievalResult {

    private List<Document> docs;

    private String rewriteQuery;

    private String retrievalMeta;

    private String retrievalStrategy;
}
