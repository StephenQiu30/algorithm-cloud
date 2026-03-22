package com.stephen.cloud.ai.model.enums;

import lombok.Getter;

@Getter
public enum KnowledgeParseStatusEnum {

    PENDING(0),
    PROCESSING(1),
    DONE(2),
    FAILED(3);

    private final int value;

    KnowledgeParseStatusEnum(int value) {
        this.value = value;
    }
}
