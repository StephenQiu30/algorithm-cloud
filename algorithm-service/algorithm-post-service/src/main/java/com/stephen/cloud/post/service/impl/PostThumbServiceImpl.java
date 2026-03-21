package com.stephen.cloud.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.rabbitmq.model.event.LikeEvent;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.post.mapper.PostThumbMapper;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostThumb;
import com.stephen.cloud.post.service.PostService;
import com.stephen.cloud.post.service.PostThumbService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帖子点赞服务实现类
 * 提供对帖子点赞和取消点赞的业务功能，并确保高并发下的数据一致性。
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class PostThumbServiceImpl extends ServiceImpl<PostThumbMapper, PostThumb> implements PostThumbService {

    @Resource
    private PostService postService;

    @Resource
    private LockUtils lockUtils;

    @Resource
    private RabbitMqSender mqSender;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    @org.springframework.context.annotation.Lazy
    private PostThumbService postThumbService;

    /**
     * 执行点赞/取消点赞操作
     * 使用分布式锁（Redis/Redisson）确保每个用户对同一帖子的操作是串行的。
     *
     * @param postId 帖子ID
     * @param userId 用户ID
     * @return 点赞变化数（1表示点赞，-1表示取消点赞，0表示无变化）
     */
    @Override
    public int doPostThumb(long postId, long userId) {
        // 1. 数据合法性检查：判断帖子是否存在
        Post post = postService.getById(postId);
        if (post == null) {
            log.error("doPostThumb error: post not found, postId: {}", postId);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "帖子不存在");
        }

        // 2. 串行化处理：使用分布式锁，锁定用户操作维度
        String lockKey = "post:thumb:" + userId;
        log.info("Attempting to acquire lock for user: {}, post: {}", userId, postId);

        return lockUtils.lockEventWithRetry(lockKey, 3, 100L, () -> postThumbService.doPostThumbInner(userId, postId),
                () -> {
                    log.warn("Acquire lock failed for userId: {}, system busy", userId);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
                });
    }

    /**
     * 封装了事务的点赞业务逻辑
     *
     * @param userId 用户ID
     * @param postId 帖子ID
     * @return 点赞变化数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int doPostThumbInner(long userId, long postId) {
        PostThumb postThumb = new PostThumb();
        postThumb.setUserId(userId);
        postThumb.setPostId(postId);
        QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>(postThumb);
        PostThumb oldPostThumb = this.getOne(thumbQueryWrapper);
        boolean result;
        if (oldPostThumb != null) {
            // 取消点赞
            result = this.removeById(oldPostThumb.getId());
            if (result) {
                result = postService.update().eq("id", postId).gt("thumb_num", 0).setSql("thumb_num = thumb_num - 1")
                        .update();
                if (result) {
                    // 事务提交后再同步 ES，确保消费端读到的是已提交数据
                    mqSender.sendTransactional(MqBizTypeEnum.ES_SYNC_SINGLE, postId);
                }
                return result ? -1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "取消点赞失败");
            }
        } else {
            // 点赞
            result = this.save(postThumb);
            if (result) {
                result = postService.update().eq("id", postId).setSql("thumb_num = thumb_num + 1").update();
                if (result) {
                    // 获取必要数据并发送事务型消息
                    mqSender.sendTransactional(MqBizTypeEnum.ES_SYNC_SINGLE, postId);

                    // 获取点赞信息发送通知（如果需要更复杂的逻辑，可以考虑在 Consumer 中处理，这里简化为构建事件并发送）
                    final Long thumbId = postThumb.getId();
                    Post post = postService.getById(postId);
                    if (post != null) {
                        UserVO userVO = userFeignClient.getUserVOById(post.getUserId()).getData();
                        if (userVO != null) {
                            LikeEvent likeEvent = LikeEvent.builder()
                                    .likeId(thumbId)
                                    .postId(postId)
                                    .postAuthorId(post.getUserId())
                                    .postTitle(post.getTitle())
                                    .likeUserId(userId)
                                    .likeUserName(userVO.getUserName())
                                    .build();
                            mqSender.sendTransactional(MqBizTypeEnum.LIKE_EVENT, "like:" + thumbId, likeEvent);
                        }
                    }
                }
                return result ? 1 : 0;
            } else {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "点赞失败");
            }
        }
    }

    /**
     * 分页获取用户点赞的帖子列表
     *
     * @param page         分页参数
     * @param queryWrapper 查询条件
     * @param thumbUserId  点赞用户ID
     * @return {@link Page<Post>}
     */
    @Override
    public Page<Post> listThumbPostByPage(IPage<Post> page, Wrapper<Post> queryWrapper, long thumbUserId) {
        if (thumbUserId <= 0) {
            return new Page<>();
        }
        return baseMapper.listThumbPostByPage(page, queryWrapper, thumbUserId);
    }
}
