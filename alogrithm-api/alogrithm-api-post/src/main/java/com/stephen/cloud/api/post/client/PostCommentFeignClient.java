package com.stephen.cloud.api.post.client;

import com.stephen.cloud.api.post.model.vo.PostCommentVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 帖子评论服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "stephen-post-service", path = "/api/post/comment", contextId = "postCommentFeignClient")
public interface PostCommentFeignClient {

    /**
     * 根据 ID 获取评论 VO
     *
     * @param id 评论 ID
     * @return 评论信息
     */
    @GetMapping("/get/vo")
    BaseResponse<PostCommentVO> getCommentVOById(@RequestParam("id") Long id);

    /**
     * 批量获取评论 VO
     *
     * @param ids 评论 ID 列表
     * @return 评论信息列表
     */
    @PostMapping("/get/vo/batch")
    BaseResponse<List<PostCommentVO>> getCommentVOByIds(@RequestBody List<Long> ids);

    /**
     * 获取帖子的评论数量
     *
     * @param postId 帖子 ID
     * @return 评论数量
     */
    @GetMapping("/count/post")
    BaseResponse<Long> countByPostId(@RequestParam("postId") Long postId);

    /**
     * 获取用户的评论数量
     *
     * @param userId 用户 ID
     * @return 评论数量
     */
    @GetMapping("/count/user")
    BaseResponse<Long> countByUserId(@RequestParam("userId") Long userId);
}
