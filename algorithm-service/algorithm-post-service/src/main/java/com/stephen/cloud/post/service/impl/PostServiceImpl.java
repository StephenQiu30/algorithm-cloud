package com.stephen.cloud.post.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.dto.review.PostReviewRequest;
import com.stephen.cloud.api.post.model.enums.PostReviewStatusEnum;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.api.search.model.entity.PostEsDTO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncBatchMessage;
import com.stephen.cloud.common.rabbitmq.model.EsSyncMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.post.convert.PostConvert;
import com.stephen.cloud.post.mapper.PostFavourMapper;
import com.stephen.cloud.common.rabbitmq.model.event.PostReviewEvent;
import com.stephen.cloud.post.mapper.PostMapper;
import com.stephen.cloud.post.mapper.PostThumbMapper;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.model.entity.PostFavour;
import com.stephen.cloud.post.model.entity.PostThumb;
import com.stephen.cloud.post.service.PostService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * 帖子服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    // removed NotificationMqUtils as RabbitMqSender is already present here

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private PostThumbMapper postThumbMapper;

    @Resource
    private PostFavourMapper postFavourMapper;

    @Resource
    private RabbitMqSender mqSender;

    /**
     * 校验帖子信息的合法性
     * 包含对标题、内容长度及业务规则的校验
     *
     * @param post 待校验的帖子实体
     * @param add  是否为新增操作（新增时必填项校验更严）
     */
    @Override
    public void validPost(Post post, boolean add) {
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String title = post.getTitle();
        String content = post.getContent();
        
        // 1. 基础必填项校验
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
            ThrowUtils.throwIf(StringUtils.isBlank(content), ErrorCode.PARAMS_ERROR, "内容不能为空");
        }
        
        // 2. 标题长度校验，防止 UI 渲染问题
        ThrowUtils.throwIf(StringUtils.isNotBlank(title) && title.length() > 80, ErrorCode.PARAMS_ERROR, "标题过长");
    }

    /**
     * 构建帖子查询条件
     *
     * @param postQueryRequest 查询请求
     * @return {@link LambdaQueryWrapper<Post>}
     */
    @Override
    public LambdaQueryWrapper<Post> getQueryWrapper(PostQueryRequest postQueryRequest) {
        LambdaQueryWrapper<Post> queryWrapper = new LambdaQueryWrapper<>();
        if (postQueryRequest == null) {
            return queryWrapper;
        }
        String searchText = postQueryRequest.getSearchText();
        String sortField = postQueryRequest.getSortField();
        String sortOrder = postQueryRequest.getSortOrder();
        Long id = postQueryRequest.getId();
        String title = postQueryRequest.getTitle();
        String content = postQueryRequest.getContent();
        List<String> tagList = postQueryRequest.getTags();
        Long userId = postQueryRequest.getUserId();
        Long notId = postQueryRequest.getNotId();

        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw.like(Post::getTitle, searchText).or().like(Post::getContent, searchText));
        }
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), Post::getId, id);
        queryWrapper.like(StringUtils.isNotBlank(title), Post::getTitle, title);
        queryWrapper.like(StringUtils.isNotBlank(content), Post::getContent, content);

        if (CollUtil.isNotEmpty(tagList)) {
            for (String tag : tagList) {
                queryWrapper.like(Post::getTags, "\"" + tag + "\"");
            }
        }

        queryWrapper.ne(ObjectUtils.isNotEmpty(notId), Post::getId, notId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), Post::getUserId, userId);
        queryWrapper.eq(ObjectUtils.isNotEmpty(postQueryRequest.getReviewStatus()), Post::getReviewStatus,
                postQueryRequest.getReviewStatus());

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, Post::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, Post::getUpdateTime);
                default -> {
                }
            }
        }
        return queryWrapper;
    }

    /**
     * 获取帖子视图对象
     *
     * @param post    帖子实体
     * @param request 请求
     * @return {@link PostVO}
     */
    @Override
    public PostVO getPostVO(Post post, HttpServletRequest request) {
        PostVO postVO = PostConvert.objToVo(post);
        long postId = post.getId();

        // 关联查询用户信息（通过 Feign 远程调用用户微服务）
        Long postUserId = post.getUserId();
        UserVO userVO = null;
        if (postUserId != null && postUserId > 0) {
            userVO = userFeignClient.getUserVOById(postUserId).getData();
        }
        postVO.setUserVO(userVO);

        // 若用户已登录，则并行获取当前用户对该帖子的点赞及收藏状态，用于前端高亮展示
        Long loginUserId = null;
        if (StpUtil.isLogin()) {
            loginUserId = StpUtil.getLoginIdAsLong();
        }
        if (loginUserId != null) {
            // 获取点赞状态
            QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>();
            thumbQueryWrapper.eq("post_id", postId);
            thumbQueryWrapper.eq("user_id", loginUserId);
            PostThumb postThumb = postThumbMapper.selectOne(thumbQueryWrapper);
            postVO.setHasThumb(postThumb != null);

            // 获取收藏状态
            QueryWrapper<PostFavour> favourQueryWrapper = new QueryWrapper<>();
            favourQueryWrapper.eq("post_id", postId);
            favourQueryWrapper.eq("user_id", loginUserId);
            PostFavour postFavour = postFavourMapper.selectOne(favourQueryWrapper);
            postVO.setHasFavour(postFavour != null);
        }
        return postVO;
    }

    /**
     * 分页获取帖子视图对象
     *
     * @param postPage 帖子分页数据
     * @param request  请求
     * @return {@link Page<PostVO>}
     */
    @Override
    public Page<PostVO> getPostVOPage(Page<Post> postPage, HttpServletRequest request) {
        List<Post> postList = postPage.getRecords();
        Page<PostVO> postVOPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        if (CollUtil.isEmpty(postList)) {
            return postVOPage;
        }

        Long loginUserId = null;
        if (StpUtil.isLogin()) {
            loginUserId = StpUtil.getLoginIdAsLong();
        }

        Long finalLoginUserId = loginUserId;
        // 使用 CompletableFuture 并行异步获取用户信息、点赞状态和收藏状态，提高接口响应速度
        // 1. 异步获取用户信息
        CompletableFuture<Map<Long, UserVO>> userMapFuture = CompletableFuture.supplyAsync(() -> {
            Set<Long> userIdSet = postList.stream().map(Post::getUserId).collect(Collectors.toSet());
            if (CollUtil.isEmpty(userIdSet)) {
                return new HashMap<>();
            }
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData();
            if (CollUtil.isEmpty(userVOList)) {
                return new HashMap<>();
            }
            return userVOList.stream().collect(Collectors.toMap(UserVO::getId, userVO -> userVO, (a, b) -> a));
        });

        // 异步获取点赞状态
        CompletableFuture<Map<Long, Boolean>> thumbMapFuture = CompletableFuture.supplyAsync(() -> {
            Map<Long, Boolean> postIdHasThumbMap = new HashMap<>();
            if (finalLoginUserId != null) {
                Set<Long> postIdSet = postList.stream().map(Post::getId).collect(Collectors.toSet());
                QueryWrapper<PostThumb> thumbQueryWrapper = new QueryWrapper<>();
                thumbQueryWrapper.in("post_id", postIdSet);
                thumbQueryWrapper.eq("user_id", finalLoginUserId);
                postThumbMapper.selectList(thumbQueryWrapper)
                        .forEach(postThumb -> postIdHasThumbMap.put(postThumb.getPostId(), true));
            }
            return postIdHasThumbMap;
        });

        // 异步获取收藏状态
        CompletableFuture<Map<Long, Boolean>> favourMapFuture = CompletableFuture.supplyAsync(() -> {
            Map<Long, Boolean> postIdHasFavourMap = new HashMap<>();
            if (finalLoginUserId != null) {
                Set<Long> postIdSet = postList.stream().map(Post::getId).collect(Collectors.toSet());
                QueryWrapper<PostFavour> favourQueryWrapper = new QueryWrapper<>();
                favourQueryWrapper.in("post_id", postIdSet);
                favourQueryWrapper.eq("user_id", finalLoginUserId);
                postFavourMapper.selectList(favourQueryWrapper)
                        .forEach(postFavour -> postIdHasFavourMap.put(postFavour.getPostId(), true));
            }
            return postIdHasFavourMap;
        });

        try {
            // 等待所有异步任务完成
            CompletableFuture.allOf(userMapFuture, thumbMapFuture, favourMapFuture).join();
            // 获取异步执行结果
            Map<Long, UserVO> userVOMap = userMapFuture.get();
            Map<Long, Boolean> postIdHasThumbMap = thumbMapFuture.get();
            Map<Long, Boolean> postIdHasFavourMap = favourMapFuture.get();

            // 填充信息
            List<PostVO> postVOList = postList.stream().map(post -> {
                PostVO postVO = PostConvert.objToVo(post);
                postVO.setUserVO(userVOMap.get(post.getUserId()));
                postVO.setHasThumb(postIdHasThumbMap.getOrDefault(post.getId(), false));
                postVO.setHasFavour(postIdHasFavourMap.getOrDefault(post.getId(), false));
                return postVO;
            }).toList();
            postVOPage.setRecords(postVOList);
        } catch (InterruptedException | ExecutionException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "获取帖子信息失败");
        }
        return postVOPage;
    }

    /**
     * 同步单个帖子到 ES
     *
     * @param postId 帖子 ID
     */
    @Override
    public void syncToEs(Long postId) {
        if (postId == null || postId <= 0) {
            return;
        }
        Post post = this.getById(postId);
        // 如果帖子不存在（MyBatis-Plus 逻辑删除后 getById 会返回 null），则从 ES 中删除
        if (post == null) {
            log.info("[PostServiceImpl] 帖子不存在或已被逻辑删除，从 ES 中删除: id={}", postId);
            EsSyncMessage message = new EsSyncMessage(
                    EsSyncDataTypeEnum.POST.getValue(), "delete", postId, null, System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, postId + ":" + System.currentTimeMillis(), message);
            return;
        }
        // 仅在审核通过且未删除时同步（作为安全保护，理论上 getById 已经过滤了 isDelete=1）
        if (!PostReviewStatusEnum.PASS.getValue().equals(post.getReviewStatus()) || post.getIsDelete() == 1) {
            EsSyncMessage message = new EsSyncMessage(
                    EsSyncDataTypeEnum.POST.getValue(), "delete", postId, null, System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, postId + ":" + System.currentTimeMillis(), message);
            log.info("[PostServiceImpl] 帖子未审核通过或已被删除，向 ES 发送删除指令: id={}", postId);
            return;
        }
        try {
            UserVO userVO = userFeignClient.getUserVOById(post.getUserId()).getData();
            syncToEs(post, userVO);
        } catch (Exception e) {
            log.error("【ES同步失败】单个帖子 ES 同步消息发送失败, postId: {}", postId, e);
        }
    }

    /**
     * 同步单个帖子到 ES（使用已查询的数据，避免重复查询）
     *
     * @param post   帖子实体
     * @param userVO 用户信息
     */
    @Override
    public void syncToEs(Post post, UserVO userVO) {
        if (post == null || post.getId() == null) {
            return;
        }
        Long postId = post.getId();
        // 仅在审核通过且未删除时同步
        if (!PostReviewStatusEnum.PASS.getValue().equals(post.getReviewStatus()) || post.getIsDelete() == 1) {
            EsSyncMessage deleteMessage = new EsSyncMessage(
                    EsSyncDataTypeEnum.POST.getValue(), "delete", postId, null, System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, postId + ":" + System.currentTimeMillis(), deleteMessage);
            log.info("[PostServiceImpl] 帖子符合删除条件，向 ES 发送删除指令: id={}", postId);
            return;
        }
        try {
            PostEsDTO postEsDTO = PostConvert.objToEsDTO(post, userVO);
            EsSyncMessage message = new EsSyncMessage(
                    EsSyncDataTypeEnum.POST.getValue(), "upsert", postId, JSONUtil.toJsonStr(postEsDTO),
                    System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, postId + ":" + System.currentTimeMillis(), message);
            log.info("[PostServiceImpl] 单个帖子 ES 同步消息已发送, postId: {}", postId);
        } catch (Exception e) {
            log.error("【ES同步失败】单个帖子 ES 同步消息发送失败, postId: {}", postId, e);
        }
    }

    /**
     * 同步数据到 ES
     *
     * @param syncType      同步方式（全量或增量）
     * @param minUpdateTime 最小更新时间 (仅在增量同步时生效)
     */
    @Override
    public void syncToEs(EsSyncTypeEnum syncType, Date minUpdateTime) {
        log.info("[PostServiceImpl] 开始同步帖子数据到 ES, 方式: {}, 起始时间: {}", syncType, minUpdateTime);

        long pageSize = 500;
        Long lastId = 0L;

        while (true) {
            QueryWrapper<Post> queryWrapper = new QueryWrapper<>();
            // 使用 ID 滚动分页，避免大偏移量导致的性能问题
            queryWrapper.gt("id", lastId);
            queryWrapper.ge(minUpdateTime != null, "update_time", minUpdateTime);
            queryWrapper.eq("is_delete", 0);
            // 只同步审核通过的帖子
            queryWrapper.eq("review_status", PostReviewStatusEnum.PASS.getValue());
            queryWrapper.orderByAsc("id");
            queryWrapper.last("limit " + pageSize);

            List<Post> postList = this.list(queryWrapper);
            if (CollUtil.isEmpty(postList)) {
                break;
            }

            // 批量获取用户信息
            Set<Long> userIdSet = postList.stream().map(Post::getUserId).collect(Collectors.toSet());
            // 守卫：避免空集合触发 Feign 调用
            if (CollUtil.isEmpty(userIdSet)) {
                continue;
            }
            List<UserVO> userVOList = userFeignClient.getUserVOByIds(new ArrayList<>(userIdSet)).getData();
            Map<Long, UserVO> userVOMap = CollUtil.isEmpty(userVOList) ? new HashMap<>()
                    : userVOList.stream().collect(Collectors.toMap(UserVO::getId, userVO -> userVO, (a, b) -> a));

            List<PostEsDTO> esDTOList = postList.stream()
                    .map(post -> PostConvert.objToEsDTO(post, userVOMap.get(post.getUserId())))
                    .toList();

            EsSyncBatchMessage batchMessage = new EsSyncBatchMessage();
            batchMessage.setDataType(EsSyncDataTypeEnum.POST.getValue());
            batchMessage.setOperation("upsert");
            batchMessage.setDataContentList(esDTOList.stream().map(JSONUtil::toJsonStr)
                    .collect(Collectors.toList()));
            batchMessage.setTimestamp(System.currentTimeMillis());

            mqSender.send(MqBizTypeEnum.ES_SYNC_BATCH, batchMessage);

            log.info("[PostServiceImpl] 已发送 {} 条帖子同步消息, lastId: {}", esDTOList.size(), lastId);

            if (postList.size() < pageSize) {
                break;
            }
            lastId = postList.get(postList.size() - 1).getId();
        }
        log.info("[PostServiceImpl] 帖子数据同步指令处理完成");
    }

    /**
     * 执行帖子审核流程
     * 包含更新帖子状态、触发 ES 同步、发送审核结果异步通知等逻辑
     *
     * @param postReviewRequest 审核请求（包含 ID、状态、审核信息）
     * @return 审核是否成功
     */
    @Override
    public boolean doPostReview(PostReviewRequest postReviewRequest) {
        if (postReviewRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long id = postReviewRequest.getId();
        Integer reviewStatus = postReviewRequest.getReviewStatus();
        PostReviewStatusEnum reviewStatusEnum = PostReviewStatusEnum.getEnumByValue(reviewStatus);
        
        // 基础参数校验：审核状态不能仍为“待审核”
        ThrowUtils.throwIf(
                id == null || id <= 0 || reviewStatusEnum == null
                        || PostReviewStatusEnum.REVIEWING.equals(reviewStatusEnum),
                ErrorCode.PARAMS_ERROR);

        // 1. 判断审核对象是否存在
        Post oldPost = this.getById(id);
        ThrowUtils.throwIf(oldPost == null, ErrorCode.NOT_FOUND_ERROR);

        // 2. 构造更新对象，仅更新审核相关字段，确保数据安全性
        Post updatePost = new Post();
        updatePost.setId(id);
        updatePost.setReviewStatus(reviewStatus);
        updatePost.setReviewMessage(postReviewRequest.getReviewMessage());
        boolean result = this.updateById(updatePost);

        // 3. 审核完成后，统一触发 ES 同步指令及审核通知流程
        if (result) {
            // 同步数据到 ES 环境
            this.syncToEs(id);
            // 发送审核结果通知（异步解耦）
            try {
                if (oldPost != null) {
                    PostReviewEvent reviewEvent = PostReviewEvent.builder()
                            .postId(id)
                            .authorId(oldPost.getUserId())
                            .postTitle(oldPost.getTitle())
                            .status(reviewStatus)
                            .message(postReviewRequest.getReviewMessage())
                            .build();
                    mqSender.send(MqBizTypeEnum.POST_REVIEW_EVENT,
                            "post_review:" + id + ":" + System.currentTimeMillis(),
                            reviewEvent);
                }
            } catch (Exception e) {
                log.error("[PostServiceImpl] 发送审核通知失败, postId: {}", id, e);
            }
        }
        return result;
    }
}
