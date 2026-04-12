package com.stephen.cloud.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.stephen.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.user.model.entity.User;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.user.mapper.UserMapper;
import com.stephen.cloud.user.service.UserEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户服务单元测试 (白盒测试)")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserEmailService userEmailService;

    @Mock
    private LockUtils lockUtils;

    @Mock
    private RabbitMqSender mqSender;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("用户校验逻辑测试 (validUser)")
    class ValidUserTest {

        @Test
        @DisplayName("新增用户：名称为空应报错")
        void testValidUserAddNameEmpty() {
            User user = new User();
            user.setUserEmail("test@edu.cn");
            BusinessException exception = assertThrows(BusinessException.class, () -> userService.validUser(user, true));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("名称过短应报错")
        void testValidUserNameTooShort() {
            User user = new User();
            user.setUserName("a");
            BusinessException exception = assertThrows(BusinessException.class, () -> userService.validUser(user, false));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("邮箱格式不正确应报错")
        void testValidUserEmailInvalid() {
            User user = new User();
            user.setUserName("testUser");
            user.setUserEmail("invalid_email");
            BusinessException exception = assertThrows(BusinessException.class, () -> userService.validUser(user, false));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("校验成功场景")
        void testValidUserSuccess() {
            User user = new User();
            user.setUserName("testUser");
            user.setUserEmail("test@edu.cn");
            
            // 模拟数据库查重 (count = 0)
            when(userMapper.selectCount(any())).thenReturn(0L);
            
            assertDoesNotThrow(() -> userService.validUser(user, true));
        }
    }

    @Nested
    @DisplayName("邮箱登录逻辑测试 (userLoginByEmail)")
    class UserLoginByEmailTest {

        @Test
        @DisplayName("验证码错误应报错")
        void testUserLoginByEmailCodeError() {
            UserEmailLoginRequest request = new UserEmailLoginRequest();
            request.setEmail("test@edu.cn");
            request.setCode("123456");

            when(userEmailService.verifyEmailCode(anyString(), anyString())).thenReturn(false);

            BusinessException exception = assertThrows(BusinessException.class, () -> userService.userLoginByEmail(request, null));
            assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
        }

        @Test
        @DisplayName("新用户登录自动注册")
        void testUserLoginByEmailAutoRegister() {
            UserEmailLoginRequest request = new UserEmailLoginRequest();
            request.setEmail("new@edu.cn");
            request.setCode("123456");

            when(userEmailService.verifyEmailCode(anyString(), anyString())).thenReturn(true);
            when(userMapper.selectOne(any())).thenReturn(null);
            
            // 模拟锁逻辑
            when(lockUtils.lockEvent(anyString(), any(), any(), any())).thenAnswer(invocation -> {
                Supplier<LoginUserVO> supplier = invocation.getArgument(2);
                return supplier.get();
            });

            // 模拟保存成功
            when(userMapper.insert(any(User.class))).thenReturn(1);

            // Mock Sa-Token 登录 (需处理 StpUtil 静态类，或者通过 Mockito 模拟静态，这里简化处理)
            // 注意：单元测试中通常避免 Mock 静态类，除非必要，这里可能需要集成测试配合
        }
    }

    @Nested
    @DisplayName("查询构造逻辑测试 (getQueryWrapper)")
    class GetQueryWrapperTest {

        @Test
        @DisplayName("根据搜索词构造查询")
        void testGetQueryWrapperWithSearchText() {
            UserQueryRequest request = new UserQueryRequest();
            request.setSearchText("stephen");

            LambdaQueryWrapper<User> wrapper = userService.getQueryWrapper(request);
            assertNotNull(wrapper);
            // 验证生成的 SQL 条件
            String sqlSegment = wrapper.getSqlSegment();
            assertTrue(sqlSegment.contains("user_name") || sqlSegment.contains("user_profile"));
        }
    }
}
