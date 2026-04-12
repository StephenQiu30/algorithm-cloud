package com.stephen.cloud.post.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.dto.review.PostReviewRequest;
import com.stephen.cloud.api.post.model.enums.PostReviewStatusEnum;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.post.mapper.PostMapper;
import com.stephen.cloud.post.model.entity.Post;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("帖子服务单元测试 (白盒测试)")
class PostServiceImplTest {

    @Mock
    private PostMapper postMapper;

    @Mock
    private RabbitMqSender mqSender;

    @InjectMocks
    private PostServiceImpl postService;

    @Nested
    @DisplayName("帖子校验逻辑测试 (validPost)")
    class ValidPostTest {

        @Test
        @DisplayName("新增帖子：标题为空应报错")
        void testValidPostAddTitleEmpty() {
            Post post = new Post();
            post.setContent("content");
            BusinessException exception = assertThrows(BusinessException.class, () -> postService.validPost(post, true));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("标题过长应报错")
        void testValidPostTitleTooLong() {
            Post post = new Post();
            post.setTitle("a".repeat(81));
            BusinessException exception = assertThrows(BusinessException.class, () -> postService.validPost(post, false));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("内容为空应报错")
        void testValidPostContentEmpty() {
            Post post = new Post();
            post.setTitle("title");
            BusinessException exception = assertThrows(BusinessException.class, () -> postService.validPost(post, true));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("校验成功场景")
        void testValidPostSuccess() {
            Post post = new Post();
            post.setTitle("title");
            post.setContent("content");
            assertDoesNotThrow(() -> postService.validPost(post, true));
        }
    }

    @Nested
    @DisplayName("帖子审核逻辑测试 (doPostReview)")
    class PostReviewTest {

        @Test
        @DisplayName("审核状态非法：仍为待审核应报错")
        void testDoPostReviewStatusInvalid() {
            PostReviewRequest request = new PostReviewRequest();
            request.setId(1L);
            request.setReviewStatus(PostReviewStatusEnum.REVIEWING.getValue());

            BusinessException exception = assertThrows(BusinessException.class, () -> postService.doPostReview(request));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("帖子不存在应报错")
        void testDoPostReviewNotFound() {
            PostReviewRequest request = new PostReviewRequest();
            request.setId(1L);
            request.setReviewStatus(PostReviewStatusEnum.PASS.getValue());

            when(postMapper.selectById(1L)).thenReturn(null);

            BusinessException exception = assertThrows(BusinessException.class, () -> postService.doPostReview(request));
            assertEquals(ErrorCode.NOT_FOUND_ERROR.getCode(), exception.getCode());
        }
    }

    @Nested
    @DisplayName("查询构造逻辑测试 (getQueryWrapper)")
    class GetQueryWrapperTest {

        @Test
        @DisplayName("构造包含标签的查询")
        void testGetQueryWrapperWithTags() {
            PostQueryRequest request = new PostQueryRequest();
            request.setTags(java.util.List.of("java", "python"));

            LambdaQueryWrapper<Post> wrapper = postService.getQueryWrapper(request);
            assertNotNull(wrapper);
            String sqlSegment = wrapper.getSqlSegment();
            assertTrue(sqlSegment.contains("tags"));
        }
    }
}
