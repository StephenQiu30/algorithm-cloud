package com.stephen.cloud.post.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.api.post.model.vo.PostVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.web.exception.GlobalExceptionHandler;
import com.stephen.cloud.post.model.entity.Post;
import com.stephen.cloud.post.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("帖子控制层测试")
class PostControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PostController controller = new PostController();
        ReflectionTestUtils.setField(controller, "postService", createPostServiceStub());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("黑盒测试：分页获取帖子列表 - 成功")
    void testListPostVOByPageSuccess() throws Exception {
        PostQueryRequest queryRequest = new PostQueryRequest();
        queryRequest.setCurrent(1);
        queryRequest.setPageSize(10);

        mockMvc.perform(post("/post/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].title").value("mock-post"));
    }

    @Test
    @DisplayName("黑盒测试：分页获取帖子列表 - Size 过大报错")
    void testListPostVOByPageSizeTooLarge() throws Exception {
        PostQueryRequest queryRequest = new PostQueryRequest();
        queryRequest.setPageSize(100);

        mockMvc.perform(post("/post/list/page/vo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAMS_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PARAMS_ERROR.getMessage()));
    }

    private PostService createPostServiceStub() {
        return (PostService) Proxy.newProxyInstance(
                PostService.class.getClassLoader(),
                new Class[]{PostService.class},
                (proxy, method, args) -> {
                    if ("getQueryWrapper".equals(method.getName())) {
                        return new LambdaQueryWrapper<Post>();
                    }
                    if ("page".equals(method.getName())) {
                        return new Page<Post>(1, 10);
                    }
                    if ("getPostVOPage".equals(method.getName())) {
                        Page<PostVO> postVOPage = new Page<>(1, 10, 1);
                        PostVO postVO = new PostVO();
                        postVO.setTitle("mock-post");
                        postVOPage.setRecords(List.of(postVO));
                        return postVOPage;
                    }
                    if ("toString".equals(method.getName())) {
                        return "PostServiceStub";
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    return getDefaultValue(method.getReturnType());
                });
    }

    private Object getDefaultValue(Class<?> returnType) {
        if (Void.TYPE.equals(returnType)) {
            return null;
        }
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (Boolean.TYPE.equals(returnType)) {
            return false;
        }
        if (Character.TYPE.equals(returnType)) {
            return '\0';
        }
        if (Byte.TYPE.equals(returnType)) {
            return (byte) 0;
        }
        if (Short.TYPE.equals(returnType)) {
            return (short) 0;
        }
        if (Integer.TYPE.equals(returnType)) {
            return 0;
        }
        if (Long.TYPE.equals(returnType)) {
            return 0L;
        }
        if (Float.TYPE.equals(returnType)) {
            return 0F;
        }
        if (Double.TYPE.equals(returnType)) {
            return 0D;
        }
        return null;
    }
}
