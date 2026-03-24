package com.stephen.cloud.api.file.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件上传业务类型枚举
 *
 * @author StephenQiu30
 */
@Getter
@AllArgsConstructor
public enum FileUploadBizEnum {

    /**
     * 用户头像
     */
    USER_AVATAR("用户头像", "user_avatar"),

    /**
     * 帖子封面
     */
    POST_COVER("帖子封面", "post_cover"),

    /**
     * 帖子上传图片
     */
    POST_IMAGE_COVER("帖子上传图片", "post_image_cover"),

    /**
     * 知识库文档
     */
    KNOWLEDGE("知识库文档", "knowledge");

    private final String text;

    private final String value;

    /**
     * 获取值列表
     *
     * @return {@link List<String>}
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value value
     * @return {@link FileUploadBizEnum}
     */
    public static FileUploadBizEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        return Arrays.stream(FileUploadBizEnum.values())
                .filter(anEnum -> anEnum.value.equals(value))
                .findFirst()
                .orElse(null);
    }

}
