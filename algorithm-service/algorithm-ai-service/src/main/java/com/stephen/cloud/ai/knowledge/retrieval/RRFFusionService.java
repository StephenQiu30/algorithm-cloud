package com.stephen.cloud.ai.knowledge.retrieval;

import org.springframework.ai.document.Document;

import java.util.List;

public interface RRFFusionService {

    List<Document> fuse(List<Document> vectorDocs, List<Document> keywordDocs, int finalTopK, int rrfK);
}
