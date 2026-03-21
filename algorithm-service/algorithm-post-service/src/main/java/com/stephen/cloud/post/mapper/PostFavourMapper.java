package com.stephen.cloud.post.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostFavour;
import org.apache.ibatis.annotations.Param;

/**
 * 帖子收藏数据库操作
 *
 * @author StephenQiu30
 */
public interface PostFavourMapper extends BaseMapper<PostFavour> {

    /**
     * 分页获取用户收藏的帖子列表
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @param favourUserId 收藏用户ID
     * @return {@link Page<Post>}
     */
    Page<Post> listFavourPostByPage(IPage<Post> page,
                                    @Param(Constants.WRAPPER) Wrapper<Post> queryWrapper,
                                    @Param("favourUserId") long favourUserId);
}
