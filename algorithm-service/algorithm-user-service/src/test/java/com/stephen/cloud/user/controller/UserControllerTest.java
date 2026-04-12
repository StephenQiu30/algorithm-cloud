package com.stephen.cloud.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stephen.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.user.service.UserService;
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
@DisplayName("用户控制层集成测试 (黑盒测试)")
class UserControllerTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Resource
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("黑盒测试：邮箱登录接口 - 成功")
    void testUserLoginByEmailSuccess() throws Exception {
        UserEmailLoginRequest loginRequest = new UserEmailLoginRequest();
        loginRequest.setEmail("test@edu.cn");
        loginRequest.setCode("123456");

        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setUserName("testUser");
        loginUserVO.setToken("mock_token");

        when(userService.userLoginByEmail(any(), any())).thenReturn(loginUserVO);

        mockMvc.perform(post("/user/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.userName").value("testUser"))
                .andExpect(jsonPath("$.data.token").value("mock_token"));
    }

    @Test
    @DisplayName("黑盒测试：邮箱登录接口 - 参数非法")
    void testUserLoginByEmailInvalidParams() throws Exception {
        UserEmailLoginRequest loginRequest = new UserEmailLoginRequest();
        loginRequest.setEmail("invalid-email");
        loginRequest.setCode("");

        mockMvc.perform(post("/user/login/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()); // 假设框架会拦截并返回 400
    }
}
