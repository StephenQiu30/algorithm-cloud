package com.stephen.cloud.post.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.post.model.dto.post.PostAddRequest;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.enums.PostContentTypeEnum;
import com.stephen.cloud.api.post.model.enums.PostReviewStatusEnum;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.DeleteRequest;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.post.convert.PostConvert;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post/knowledge")
@Slf4j
@Tag(name = "AlgorithmKnowledgeController", description = "算法知识库")
public class AlgorithmKnowledgeController {

    @Resource
    private PostService postService;

    @PostMapping("/add")
    @OperationLog(module = "算法知识库", action = "创建条目")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "创建知识库条目（管理员）")
    public BaseResponse<Long> add(@RequestBody PostAddRequest postAddRequest, HttpServletRequest request) {
        Post post = PostConvert.addRequestToObj(postAddRequest);
        postService.validPost(post, true);
        post.setContentType(PostContentTypeEnum.ALGO_KB.getValue());
        post.setReviewStatus(PostReviewStatusEnum.PASS.getValue());
        post.setReviewMessage("");
        Long userId = SecurityUtils.getLoginUserId();
        post.setUserId(userId);
        post.setFavourNum(0);
        post.setThumbNum(0);
        boolean result = postService.save(post);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        if (result) {
            postService.syncToEs(post.getId());
        }
        return ResultUtils.success(post.getId());
    }

    @PostMapping("/delete")
    @OperationLog(module = "算法知识库", action = "删除条目")
    @Operation(summary = "删除知识库条目")
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long userId = SecurityUtils.getLoginUserId();
        long id = deleteRequest.getId();
        Post oldPost = postService.getById(id);
        if (oldPost == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        ThrowUtils.throwIf(!PostContentTypeEnum.ALGO_KB.getValue().equals(oldPost.getContentType()),
                ErrorCode.NOT_FOUND_ERROR);
        if (!oldPost.getUserId().equals(userId) && !SecurityUtils.isAdmin()) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = postService.removeById(id);
        if (b) {
            postService.syncToEs(id);
        }
        return ResultUtils.success(b);
    }

    @GetMapping("/get/vo")
    @Operation(summary = "知识库详情")
    public BaseResponse<PostVO> getVo(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        Post post = postService.getById(id);
        ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!PostContentTypeEnum.ALGO_KB.getValue().equals(post.getContentType()),
                ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(postService.getPostVO(post, request));
    }

    @PostMapping("/list/page/vo")
    @Operation(summary = "知识库公开列表")
    public BaseResponse<Page<PostVO>> listVoByPage(@RequestBody PostQueryRequest postQueryRequest,
            HttpServletRequest request) {
        if (postQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = postQueryRequest.getCurrent();
        long size = postQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        postQueryRequest.setReviewStatus(PostReviewStatusEnum.PASS.getValue());
        postQueryRequest.setContentType(PostContentTypeEnum.ALGO_KB.getValue());
        Page<Post> postPage = postService.page(new Page<>(current, size),
                postService.getQueryWrapper(postQueryRequest));
        return ResultUtils.success(postService.getPostVOPage(postPage, request));
    }
}
