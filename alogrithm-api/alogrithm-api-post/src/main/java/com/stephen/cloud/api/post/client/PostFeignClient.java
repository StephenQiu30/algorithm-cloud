package com.stephen.cloud.api.post.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 帖子服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "stephen-post-service", path = "/api/post", contextId = "postFeignClient")
public interface PostFeignClient {

    /**
     * 根据 ID 获取帖子 VO
     *
     * @param id 帖子 ID
     * @return 帖子信息
     */
    @GetMapping("/get/vo")
    BaseResponse<PostVO> getPostVOById(@RequestParam("id") Long id);

    /**
     * 批量获取帖子 VO
     *
     * @param ids 帖子 ID 列表
     * @return 帖子信息列表
     */
    @PostMapping("/get/vo/batch")
    BaseResponse<List<PostVO>> getPostVOByIds(@RequestBody List<Long> ids);

    /**
     * 增加帖子浏览量
     *
     * @param postId 帖子 ID
     * @return 是否成功
     */
    @PostMapping("/view/increment")
    BaseResponse<Boolean> incrementViewCount(@RequestParam("postId") Long postId);

    /**
     * 检查帖子是否存在
     *
     * @param postId 帖子 ID
     * @return 是否存在
     */
    @GetMapping("/exists")
    BaseResponse<Boolean> existsById(@RequestParam("postId") Long postId);

    /**
     * 获取用户的帖子数量
     *
     * @param userId 用户 ID
     * @return 帖子数量
     */
    @GetMapping("/count/user")
    BaseResponse<Long> countByUserId(@RequestParam("userId") Long userId);

    /**
     * 分页查询帖子（用于同步）
     *
     * @param postQueryRequest 查询请求
     * @return 帖子分页列表
     */
    @PostMapping("/list/page")
    BaseResponse<Page<PostVO>> listPostByPage(
            @RequestBody PostQueryRequest postQueryRequest);
}
