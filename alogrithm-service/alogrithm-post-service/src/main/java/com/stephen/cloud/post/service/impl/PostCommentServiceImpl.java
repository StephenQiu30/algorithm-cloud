package com.stephen.cloud.post.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.post.model.dto.comment.PostCommentQueryRequest;
import com.stephen.cloud.api.post.model.vo.PostCommentVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.post.convert.PostCommentConvert;
import com.stephen.cloud.post.mapper.PostCommentMapper;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostComment;
import com.stephen.cloud.post.service.PostCommentService;
import com.stephen.cloud.post.service.PostService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 帖子评论服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class PostCommentServiceImpl extends ServiceImpl<PostCommentMapper, PostComment> implements PostCommentService {

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    @Lazy
    private PostService postService;

    /**
     * 校验评论信息
     *
     * @param postComment postComment
     * @param add         add 是否是添加
     */
    @Override
    public void validPostComment(PostComment postComment, boolean add) {
        ThrowUtils.throwIf(postComment == null, ErrorCode.PARAMS_ERROR);
        String content = postComment.getContent();
        Long postId = postComment.getPostId();
        Long parentId = postComment.getParentId();

        // 创建时，参数不能为空
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
            ThrowUtils.throwIf(postId == null || postId <= 0, ErrorCode.PARAMS_ERROR, "帖子 id 不能为空");
        }
        // 有参数则校验
        if (StringUtils.isNotBlank(content) && content.length() > 2000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "评论内容过长");
        }
        // 校验帖子是否存在，确保评论有关联的实体
        if (postId != null && postId > 0) {
            Post post = postService.getById(postId);
            ThrowUtils.throwIf(post == null, ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }
        // 校验父评论是否存在，支持嵌套评论逻辑
        if (parentId != null && parentId > 0) {
            PostComment parentComment = this.getById(parentId);
            ThrowUtils.throwIf(parentComment == null, ErrorCode.NOT_FOUND_ERROR, "父评论不存在");
        }
    }

    /**
     * 获取查询包装类
     *
     * @param postCommentQueryRequest postCommentQueryRequest
     * @return {@link LambdaQueryWrapper<PostComment>}
     */
    @Override
    public LambdaQueryWrapper<PostComment> getQueryWrapper(PostCommentQueryRequest postCommentQueryRequest) {
        LambdaQueryWrapper<PostComment> queryWrapper = new LambdaQueryWrapper<>();
        if (postCommentQueryRequest == null) {
            return queryWrapper;
        }
        String sortField = postCommentQueryRequest.getSortField();
        String sortOrder = postCommentQueryRequest.getSortOrder();
        Long id = postCommentQueryRequest.getId();
        Long postId = postCommentQueryRequest.getPostId();
        Long userId = postCommentQueryRequest.getUserId();
        Long parentId = postCommentQueryRequest.getParentId();
        String content = postCommentQueryRequest.getContent();

        queryWrapper.like(StringUtils.isNotBlank(content), PostComment::getContent, content);
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), PostComment::getId, id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(postId), PostComment::getPostId, postId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), PostComment::getUserId, userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(parentId), PostComment::getParentId, parentId);

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, PostComment::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, PostComment::getUpdateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }

    /**
     * 获取评论视图类
     *
     * @param postComment postComment
     * @return {@link PostCommentVO}
     */
    @Override
    public PostCommentVO getPostCommentVO(PostComment postComment, HttpServletRequest request) {
        PostCommentVO postCommentVO = PostCommentConvert.objToVo(postComment);
        // 关联查询用户信息（通过 Feign 远程调用用户微服务，用于展示评论者信息）
        Long userId = postComment.getUserId();
        if (userId != null && userId > 0) {
            UserVO userVO = userFeignClient.getUserVOById(userId).getData();
            postCommentVO.setUserVO(userVO);
        }
        return postCommentVO;
    }

    /**
     * 分页获取评论视图类
     *
     * @param postCommentPage postCommentPage
     * @param request         request
     * @return {@link Page<PostCommentVO>}
     */
    @Override
    public Page<PostCommentVO> getPostCommentVOPage(Page<PostComment> postCommentPage, HttpServletRequest request) {
        List<PostComment> postCommentList = postCommentPage.getRecords();
        Page<PostCommentVO> postCommentVOPage = new Page<>(postCommentPage.getCurrent(), postCommentPage.getSize(),
                postCommentPage.getTotal());
        if (CollUtil.isEmpty(postCommentList)) {
            return postCommentVOPage;
        }
        // 批量获取用户信息，减少 RPC 调用次数，优化接口性能
        Set<Long> userIdSet = postCommentList.stream().map(PostComment::getUserId).collect(Collectors.toSet());
        Map<Long, UserVO> userIdUserVOMap = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData()
                .stream().collect(Collectors.toMap(UserVO::getId, userVO -> userVO));

        // 填充信息
        List<PostCommentVO> postCommentVOList = postCommentList.stream().map(postComment -> {
            PostCommentVO postCommentVO = PostCommentConvert.objToVo(postComment);
            Long userId = postComment.getUserId();
            if (userIdUserVOMap.containsKey(userId)) {
                postCommentVO.setUserVO(userIdUserVOMap.get(userId));
            }
            return postCommentVO;
        }).toList();
        postCommentVOPage.setRecords(postCommentVOList);
        return postCommentVOPage;
    }
}
