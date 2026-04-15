package com.stephen.cloud.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stephen.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.web.exception.GlobalExceptionHandler;
import com.stephen.cloud.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Proxy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("用户控制层测试")
class UserControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", createUserServiceStub());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("黑盒测试：邮箱登录接口 - 成功")
    void testUserLoginByEmailSuccess() throws Exception {
        UserEmailLoginRequest loginRequest = new UserEmailLoginRequest();
        loginRequest.setEmail("test@edu.cn");
        loginRequest.setCode("123456");

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCode.PARAMS_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("邮箱或验证码不能为空"));
    }

    private UserService createUserServiceStub() {
        return (UserService) Proxy.newProxyInstance(
                UserService.class.getClassLoader(),
                new Class[]{UserService.class},
                (proxy, method, args) -> {
                    if ("userLoginByEmail".equals(method.getName())) {
                        UserEmailLoginRequest request = (UserEmailLoginRequest) args[0];
                        if (request == null || request.getCode() == null || request.getCode().isBlank()
                                || request.getEmail() == null || !request.getEmail().endsWith("@edu.cn")) {
                            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱或验证码不能为空");
                        }
                        LoginUserVO loginUserVO = new LoginUserVO();
                        loginUserVO.setUserName("testUser");
                        loginUserVO.setToken("mock_token");
                        return loginUserVO;
                    }
                    if ("toString".equals(method.getName())) {
                        return "UserServiceStub";
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
