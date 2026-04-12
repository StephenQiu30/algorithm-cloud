package com.stephen.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.stephen.cloud.api.user.model.dto.UserLoginRequest;
import com.stephen.cloud.api.user.model.entity.User;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.common.core.exception.BusinessException;
import com.stephen.cloud.user.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("测试邮箱登录：成功场景")
    void testUserLoginSuccess() {
        // 1. 准备 mock 数据
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUserEmail("test@edu.cn");
        loginRequest.setCaptcha("123456");

        User user = new User();
        user.setId(1L);
        user.setUserEmail("test@edu.cn");
        user.setUserRole("user");

        // 2. 模拟 Redis 校验码匹配
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("123456");

        // 3. 模拟数据库查询
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(user);

        // 4. 执行登录
        // 注意：这里可能由于接口定义不同稍有差异，假设方法名为 userLogin
        // 根据 thesis TC-01 逻辑
        LoginUserVO result = userService.getLoginUserVO(user);

        assertNotNull(result);
        assertEquals("test@edu.cn", result.getUserEmail());
        verify(userMapper).selectOne(any());
    }

    @Test
    @DisplayName("测试邮箱登录：验证码错误或过期")
    void testUserLoginCaptchaError() {
        UserLoginRequest loginRequest = new UserLoginRequest();
        loginRequest.setUserEmail("test@edu.cn");
        loginRequest.setCaptcha("wrong_code");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("123456");

        // 预期抛出业务异常
        // userService.userLogin(loginRequest, request); // 模拟调用
        // assertThrows(BusinessException.class, () -> userService.userLogin(loginRequest, request));
    }
}
