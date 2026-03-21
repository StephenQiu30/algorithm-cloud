package com.stephen.cloud.post.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostFavour;

/**
 * 帖子收藏服务
 *
 * @author StephenQiu30
 */
public interface PostFavourService extends IService<PostFavour> {

    /**
     * 收藏/取消收藏
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 收藏变化数（1表示收藏，-1表示取消收藏）
     */
    int doPostFavour(long postId, long userId);

    /**
     * 收藏/取消收藏（内部事务处理）
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 收藏状态
     */
    int doPostFavourInner(long userId, long postId);

    /**
     * 分页获取用户收藏的帖子列表
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @param favourUserId 收藏用户ID
     * @return {@link Page<Post>}
     */
    Page<Post> listFavourPostByPage(IPage<Post> page, Wrapper<Post> queryWrapper, long favourUserId);
}
