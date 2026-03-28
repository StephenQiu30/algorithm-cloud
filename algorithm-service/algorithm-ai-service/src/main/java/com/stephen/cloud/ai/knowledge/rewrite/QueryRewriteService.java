package com.stephen.cloud.ai.knowledge.rewrite;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

public interface QueryRewriteService {

    default RewriteResult rewrite(String question) {
        return rewrite(question, List.of());
    }

    RewriteResult rewrite(String question, List<Message> history);
}
