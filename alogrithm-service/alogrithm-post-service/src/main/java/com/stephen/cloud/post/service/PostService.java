package com.stephen.cloud.post.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.dto.review.PostReviewRequest;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.post.model.entity.Post;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;

/**
 * 帖子服务
 *
 * @author StephenQiu30
 */
public interface PostService extends IService<Post> {

    /**
     * 校验帖子信息
     *
     * @param post 帖子实体
     * @param add  是否为新增操作 (新增时校验必填项，更新时校验 ID)
     */
    void validPost(Post post, boolean add);

    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param postQueryRequest 帖子查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest);

    /**
     * 获取帖子封装
     *
     * @param post    帖子实体
     * @param request 请求
     */
    PostVO getPostVO(Post post, HttpServletRequest request);

    /**
     * 分页获取帖子封装
     *
     * @param postPage 帖子分页数据
     * @param request  请求
     */
    Page<PostVO> getPostVOPage(Page<Post> postPage, HttpServletRequest request);

    /**
     * 同步单个帖子到 ES
     *
     * @param postId 帖子 ID
     */
    void syncToEs(Long postId);

    /**
     * 同步单个帖子到 ES（使用已查询的数据，避免重复查询）
     *
     * @param post   帖子实体
     * @param userVO 用户信息
     */
    void syncToEs(Post post, UserVO userVO);

    /**
     * 同步数据到 ES
     *
     * @param syncType      同步方式
     * @param minUpdateTime 最小更新时间 (增量同步时)
     */
    void syncToEs(EsSyncTypeEnum syncType, Date minUpdateTime);

    /**
     * 审核帖子
     *
     * @param postReviewRequest 审核请求
     * @return 是否审核成功
     */
    boolean doPostReview(PostReviewRequest postReviewRequest);
}
