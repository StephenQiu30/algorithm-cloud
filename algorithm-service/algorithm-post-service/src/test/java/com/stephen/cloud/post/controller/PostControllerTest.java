package com.stephen.cloud.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stephen.cloud.api.post.model.dto.post.PostAddRequest;
import com.stephen.cloud.api.post.model.dto.post.PostQueryRequest;
import com.stephen.cloud.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("帖子控制层集成测试 (黑盒测试)")
class PostControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private PostService postService;

    @Resource
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("黑盒测试：分页获取帖子列表 - 成功")
    void testListPostVOByPageSuccess() throws Exception {
        PostQueryRequest queryRequest = new PostQueryRequest();
        queryRequest.setCurrent(1);
        queryRequest.setPageSize(10);

        // 模拟 Service 返回结果逻辑 (省略具体分页对象构造，因为关键是接口能通)
        when(postService.page(any(), any()))
                .thenReturn(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>());

        mockMvc.perform(post("/post/list/page/vo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("黑盒测试：分页获取帖子列表 - Size 过大报错")
    void testListPostVOByPageSizeTooLarge() throws Exception {
        PostQueryRequest queryRequest = new PostQueryRequest();
        queryRequest.setPageSize(100);

        mockMvc.perform(post("/post/list/page/vo")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(queryRequest)))
                .andExpect(status().isInternalServerError());
    }
}
