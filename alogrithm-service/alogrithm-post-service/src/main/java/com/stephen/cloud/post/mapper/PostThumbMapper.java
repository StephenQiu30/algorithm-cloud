package com.stephen.cloud.post.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostThumb;
import org.apache.ibatis.annotations.Param;

/**
 * 帖子点赞数据库操作
 *
 * @author StephenQiu30
 */
public interface PostThumbMapper extends BaseMapper<PostThumb> {

    /**
     * 分页获取用户点赞的帖子列表
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @param thumbUserId  点赞用户ID
     * @return {@link Page<Post>}
     */
    Page<Post> listThumbPostByPage(IPage<Post> page,
                                   @Param(Constants.WRAPPER) Wrapper<Post> queryWrapper,
                                   @Param("thumbUserId") long thumbUserId);
}
