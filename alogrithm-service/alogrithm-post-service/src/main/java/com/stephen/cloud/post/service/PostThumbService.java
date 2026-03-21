package com.stephen.cloud.post.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostThumb;

/**
 * 帖子点赞服务
 *
 * @author StephenQiu30
 */
public interface PostThumbService extends IService<PostThumb> {

    /**
     * 点赞/取消点赞
     *
     * @param postId 帖子 ID
     * @param userId 用户 ID
     * @return 点赞变化数（1表示点赞，-1表示取消点赞）
     */
    int doPostThumb(long postId, long userId);

    /**
     * 点赞/取消点赞（内部事务处理）
     *
     * @param userId 用户 ID
     * @param postId 帖子 ID
     * @return 点赞状态
     */
    int doPostThumbInner(long userId, long postId);

    /**
     * 分页获取用户点赞的帖子列表
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @param thumbUserId  点赞用户ID
     * @return {@link Page<Post>}
     */
    Page<Post> listThumbPostByPage(IPage<Post> page, Wrapper<Post> queryWrapper, long thumbUserId);
}
