package com.stephen.cloud.api.post.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 帖子审核状态枚举
 *
 * @author StephenQiu
 */
@Getter
public enum PostReviewStatusEnum {

    /**
     * 待审核
     */
    REVIEWING("待审核", 0),
    /**
     * 审核通过
     */
    PASS("通过", 1),
    /**
     * 审核拒绝
     */
    REJECT("拒绝", 2);

    private final String text;

    private final Integer value;

    PostReviewStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 获取值列表
     *
     * @return List<Integer>
     */
    public static List<Integer> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value
     * @return PostReviewStatusEnum
     */
    public static PostReviewStatusEnum getEnumByValue(Integer value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (PostReviewStatusEnum anEnum : PostReviewStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

}
