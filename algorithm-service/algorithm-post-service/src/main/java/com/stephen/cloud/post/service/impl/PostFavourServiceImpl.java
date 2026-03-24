package com.stephen.cloud.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.event.FavourEvent;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.post.mapper.PostFavourMapper;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostFavour;
import com.stephen.cloud.post.service.PostFavourService;
import com.stephen.cloud.post.service.PostService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帖子收藏服务实现类
 * 提供对帖子收藏和取消收藏的业务功能，并确保高并发下的数据一致性。
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class PostFavourServiceImpl extends ServiceImpl<PostFavourMapper, PostFavour> implements PostFavourService {

    @Resource
    private PostService postService;

    @Resource
    @Lazy
    private PostFavourService postFavourService;

    @Resource
    private LockUtils lockUtils;

    @Resource
    private RabbitMqSender mqSender;

    @Resource
    private UserFeignClient userFeignClient;

    /**
     * 执行收藏/取消收藏操作
     * 使用分布式锁（Redis/Redisson）确保每个用户对同一帖子的操作是串行的。
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 收藏变化数（1表示收藏，-1表示取消收藏，0表示无变化）
     */
    @Override
    public int doPostFavour(long postId, long userId) {
        // 1. 数据合法性检查：判断帖子是否存在
        Post post = postService.getById(postId);
        if (post == null) {
            log.error("doPostFavour error: post not found, postId: {}", postId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }

        // 2. 串行化处理：使用分布式锁，锁定用户操作维度
        String lockKey = "post:favour:" + userId;
        log.info("Attempting to acquire lock for user: {}, post: {}", userId, postId);

        return lockUtils.lockEventWithRetry(
                lockKey,
                3,
                100L,
                () -> postFavourService.doPostFavourInner(userId, postId),
                () -> {
                    log.warn("Acquire lock failed for userId: {}, system busy", userId);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
                });
    }

    /**
     * 封装了事务的收藏业务逻辑（内部方法，不直接对外暴露，由 doPostFavour 调用以支持事务回滚）
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 收藏变化数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostFavourInner(long userId, long postId) {
        PostFavour postFavour = new PostFavour();
        postFavour.setUserId(userId);
        postFavour.setPostId(postId);
        QueryWrapper<PostFavour> favourQueryWrapper = new QueryWrapper<>(postFavour);
        PostFavour oldPostFavour = this.getOne(favourQueryWrapper);
        boolean result;
        if (oldPostFavour != null) {
            // 取消收藏
            result = this.removeById(oldPostFavour.getId());
            if (result) {
                result = postService.update().eq("id", postId).gt("favour_num", 0).setSql("favour_num = favour_num - 1")
                        .update();
                if (result) {
                    // 事务提交后再同步 ES，确保消费端读到的是已提交数据
                    mqSender.sendTransactional(MqBizTypeEnum.ES_SYNC_SINGLE, postId);
                }
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消收藏失败");
            }
        } else {
            // 收藏
            result = this.save(postFavour);
            if (result) {
                // 收藏数 + 1
                result = postService.update().eq("id", postId).setSql("favour_num = favour_num + 1").update();
                if (result) {
                    // 获取必要数据并发送事务型消息
                    mqSender.sendTransactional(MqBizTypeEnum.ES_SYNC_SINGLE, postId);

                    // 获取收藏信息发送通知
                    final Long favourId = postFavour.getId();
                    Post post = postService.getById(postId);
                    if (post != null) {
                        UserVO userVO = userFeignClient.getUserVOById(post.getUserId()).getData();
                        if (userVO != null) {
                            FavourEvent favourEvent = FavourEvent.builder()
                                    .favourId(favourId)
                                    .postId(postId)
                                    .postAuthorId(post.getUserId())
                                    .postTitle(post.getTitle())
                                    .favourUserId(userId)
                                    .favourUserName(userVO.getUserName())
                                    .build();
                            mqSender.sendTransactional(MqBizTypeEnum.FAVOUR_EVENT, "favour:" + favourId, favourEvent);
                        }
                    }
                }
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "收藏失败");
            }
        }
    }

    /**
     * 分页获取用户收藏的帖子列表
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @param favourUserId 收藏用户ID
     * @return {@link Page<Post>}
     */
    @Override
    public Page<Post> listFavourPostByPage(IPage<Post> page, Wrapper<Post> queryWrapper, long favourUserId) {
        if (favourUserId <= 0) {
            return new Page<>();
        }
        return baseMapper.listFavourPostByPage(page, queryWrapper, favourUserId);
    }
}
