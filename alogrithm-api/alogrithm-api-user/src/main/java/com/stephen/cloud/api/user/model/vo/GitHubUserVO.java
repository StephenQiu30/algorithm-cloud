package com.stephen.cloud.api.user.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * GitHub用户信息
 *
 * @author stephen
 */
@Data
@Schema(description = "GitHub 用户信息")
public class GitHubUserVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * GitHub用户ID
     */
    @Schema(description = "GitHub用户ID")
    private String id;

    /**
     * GitHub用户名
     */
    @Schema(description = "GitHub用户名")
    private String login;

    /**
     * GitHub头像URL
     */
    @Schema(description = "GitHub头像URL")
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /**
     * GitHub主页URL
     */
    @Schema(description = "GitHub主页URL")
    @JsonProperty("html_url")
    private String htmlUrl;

    /**
     * GitHub用户昵称
     */
    @Schema(description = "GitHub用户昵称")
    private String name;

    /**
     * GitHub用户邮箱
     */
    @Schema(description = "GitHub用户邮箱")
    private String email;
}
