package com.stephen.cloud.user.service.impl;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Spy
    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        if (TableInfoHelper.getTableInfo(User.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        }
    }

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
            doReturn(null).when(userService).getOne(any(LambdaQueryWrapper.class));
            doAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return true;
            }).when(userService).save(any(User.class));
            doReturn(true).when(userService).updateById(any(User.class));
            doNothing().when(userService).recordLoginLogAsync(any());

            when(lockUtils.lockEvent(anyString(), any(), any(), any())).thenAnswer(invocation -> {
                Supplier<LoginUserVO> supplier = invocation.getArgument(2);
                return supplier.get();
            });

            SaTokenInfo tokenInfo = new SaTokenInfo();
            tokenInfo.setTokenValue("mock-token");
            SaSession saSession = new SaSession("mock-session");

            LoginUserVO loginUserVO;
            try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
                stpUtil.when(() -> StpUtil.login(1L)).thenAnswer(invocation -> null);
                stpUtil.when(StpUtil::getTokenInfo).thenReturn(tokenInfo);
                stpUtil.when(StpUtil::getSession).thenReturn(saSession);
                loginUserVO = userService.userLoginByEmail(request, null);
                stpUtil.verify(() -> StpUtil.login(1L));
            }

            assertNotNull(loginUserVO);
            assertEquals("new", loginUserVO.getUserName());
            assertEquals("mock-token", loginUserVO.getToken());
            verify(userEmailService).deleteEmailCode("new@edu.cn");
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
            String sqlSegment = wrapper.getSqlSegment();
            assertTrue(sqlSegment.contains("user_name") || sqlSegment.contains("user_profile"));
        }
    }
}
