package com.stephen.cloud.api.post.model.vo;

import com.stephen.cloud.api.user.model.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 帖子视图对象（API传输用）
 *
 * @author StephenQiu30
 */
@Data
@Schema(description = "帖子视图对象")
public class PostVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @Schema(description = "帖子ID")
    private Long id;

    /**
     * 标题
     */
    @Schema(description = "标题")
    private String title;

    /**
     * 内容
     */
    @Schema(description = "内容")
    private String content;

    /**
     * 封面
     */
    @Schema(description = "封面")
    private String cover;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数")
    private Integer thumbNum;

    /**
     * 收藏数
     */
    @Schema(description = "收藏数")
    private Integer favourNum;

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private Long userId;

    /**
     * 内容类型(0-帖子 1-算法知识库)
     */
    @Schema(description = "内容类型(0-帖子 1-算法知识库)")
    private Integer contentType;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Date createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private Date updateTime;

    /**
     * 标签列表
     */
    @Schema(description = "标签列表")
    private List<String> tags;

    /**
     * 用户视图
     */
    @Schema(description = "用户视图")
    private UserVO userVO;

    /**
     * 是否已点赞
     */
    @Schema(description = "是否已点赞")
    private Boolean hasThumb;

    /**
     * 是否已收藏
     */
    @Schema(description = "是否已收藏")
    private Boolean hasFavour;

    /**
     * 审核状态（0-待审核，1-通过，2-拒绝）
     */
    @Schema(description = "审核状态")
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    @Schema(description = "审核信息")
    private String reviewMessage;

}
