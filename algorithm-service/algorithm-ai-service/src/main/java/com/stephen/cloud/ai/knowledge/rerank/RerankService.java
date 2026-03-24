package com.stephen.cloud.ai.knowledge.rerank;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

public interface RerankService {

    List<Document> rerank(List<Document> fusedDocs, List<String> mustTerms, Map<String, String> metadataFilters, int finalTopK);
}
