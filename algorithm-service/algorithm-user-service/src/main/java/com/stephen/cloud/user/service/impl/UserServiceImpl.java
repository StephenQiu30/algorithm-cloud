package com.stephen.cloud.user.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogAddRequest;
import com.stephen.cloud.api.log.model.enums.LoginStatusEnum;
import com.stephen.cloud.api.log.model.enums.LoginTypeEnum;
import com.stephen.cloud.api.search.model.entity.UserEsDTO;
import com.stephen.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.model.enums.EmailVerifiedEnum;
import com.stephen.cloud.api.user.model.enums.UserRoleEnum;
import com.stephen.cloud.api.user.model.vo.GitHubUserVO;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.cache.constants.CacheConstant;
import com.stephen.cloud.common.cache.model.TimeModel;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.constants.CommonConstant;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.mysql.utils.SqlUtils;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncDataTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncBatchMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.common.utils.IpUtils;
import com.stephen.cloud.common.utils.RegexUtils;
import com.stephen.cloud.user.constant.UserLoginConstants;
import com.stephen.cloud.user.convert.UserConvert;
import com.stephen.cloud.user.mapper.UserMapper;
import com.stephen.cloud.user.model.dto.UserLoginLogRecordRequest;
import com.stephen.cloud.user.model.entity.User;
import com.stephen.cloud.user.service.GitHubOAuthService;
import com.stephen.cloud.user.service.GitHubService;
import com.stephen.cloud.user.service.UserEmailService;
import com.stephen.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private GitHubService gitHubService;

    @Resource
    private UserEmailService userEmailService;

    @Resource
    private CacheUtils cacheUtils;

    @Resource
    private GitHubOAuthService gitHubOAuthService;

    @Resource
    private LogFeignClient logFeignClient;

    @Resource
    private RabbitMqSender mqSender;

    @Resource
    private LockUtils lockUtils;

    /**
     * 校验用户信息合法性
     * 包含对用户名、邮箱、手机号等格式及业务规则的校验
     *
     * @param user 待校验的用户对象
     * @param add  是否为新增操作（新增时必填字段校验更严格）
     */
    @Override
    public void validUser(User user, boolean add) {
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String userName = user.getUserName();
        String userEmail = user.getUserEmail();
        String userPhone = user.getUserPhone();
        String userProfile = user.getUserProfile();

        // 基础必填校验
        if (add) {
            ThrowUtils.throwIf(StringUtils.isBlank(userName), ErrorCode.PARAMS_ERROR, "用户名称不能为空");
            ThrowUtils.throwIf(StringUtils.isBlank(userEmail), ErrorCode.PARAMS_ERROR, "用户邮箱不能为空");
        }

        // 昵称长度校验
        if (StringUtils.isNotBlank(userName)) {
            ThrowUtils.throwIf(userName.length() < 2 || userName.length() > 30, ErrorCode.PARAMS_ERROR, "用户昵称过短或过长");
        }

        // 邮箱格式及唯一性校验
        if (StringUtils.isNotBlank(userEmail)) {
            ThrowUtils.throwIf(!RegexUtils.checkEmail(userEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUserEmail, userEmail);
            queryWrapper.ne(user.getId() != null, User::getId, user.getId());
            long count = this.count(queryWrapper);
            ThrowUtils.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "该邮箱已被占用");
        }

        // 手机号格式校验
        if (StringUtils.isNotBlank(userPhone)) {
            ThrowUtils.throwIf(!RegexUtils.checkPhone(userPhone), ErrorCode.PARAMS_ERROR, "用户手机号格式有误");
        }

        // 个人简介长度校验
        if (StringUtils.isNotBlank(userProfile)) {
            ThrowUtils.throwIf(userProfile.length() > 500, ErrorCode.PARAMS_ERROR, "用户简介过长");
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request request
     * @return {@link User}
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserId();
        User currentUser = this.getById(userId);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request request
     * @return {@link User}
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        Long userId = SecurityUtils.getLoginUserIdPermitNull();
        if (userId == null) {
            return null;
        }
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request request
     * @return boolean 是否为管理员
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        return SecurityUtils.isAdmin();
    }

    @Override
    public boolean isAdmin(User user) {
        return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request request
     * @return boolean 是否退出成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        StpUtil.checkLogin();
        StpUtil.logout();
        return true;
    }

    /**
     * 获取登录用户视图类
     *
     * @param user user
     * @return {@link LoginUserVO}
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        loginUserVO.setToken(StpUtil.getTokenInfo().getTokenValue());
        return loginUserVO;
    }

    /**
     * 获取用户 VO 封装类
     *
     * @param user    user
     * @param request request
     * @return {@link UserVO}
     */
    @Override
    public UserVO getUserVO(User user, HttpServletRequest request) {
        return UserConvert.objToVo(user);
    }

    /**
     * 获取用户 VO 视图类列表
     *
     * @param userList 用户列表
     * @param request  HTTP 请求
     * @return 用户视图类列表
     */
    @Override
    public List<UserVO> getUserVO(List<User> userList, HttpServletRequest request) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(user -> getUserVO(user, request)).collect(Collectors.toList());
    }

    /**
     * 分页获取用户视图类
     *
     * @param userPage 用户分页数据
     * @param request  HTTP 请求
     * @return 用户视图类分页对象
     */
    @Override
    public Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request) {
        List<User> userList = userPage.getRecords();
        Page<UserVO> userVOPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        if (CollUtil.isEmpty(userList)) {
            return userVOPage;
        }
        List<UserVO> userVOList = userList.stream().map(UserConvert::objToVo).collect(Collectors.toList());
        userVOPage.setRecords(userVOList);

        return userVOPage;
    }

    /**
     * 获取查询封装类
     *
     * @param userQueryRequest userQueryRequest
     * @return {@link LambdaQueryWrapper<User>}
     */
    @Override
    public LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        Long notId = userQueryRequest.getNotId();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        String userEmail = userQueryRequest.getUserEmail();
        String userPhone = userQueryRequest.getUserPhone();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        String searchText = userQueryRequest.getSearchText();

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(id != null, User::getId, id)
                .ne(ObjectUtils.isNotEmpty(notId), User::getId, notId)
                .eq(StringUtils.isNotBlank(userRole), User::getUserRole, userRole)
                .like(StringUtils.isNotBlank(userName), User::getUserName, userName)
                .like(StringUtils.isNotBlank(userEmail), User::getUserEmail, userEmail)
                .like(StringUtils.isNotBlank(userPhone), User::getUserPhone, userPhone);

        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like(User::getUserName, searchText)
                    .or()
                    .like(User::getUserProfile, searchText));
        }

        if (SqlUtils.validSortField(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            switch (sortField) {
                case "createTime" -> queryWrapper.orderBy(true, isAsc, User::getCreateTime);
                case "updateTime" -> queryWrapper.orderBy(true, isAsc, User::getUpdateTime);
                case "userName" -> queryWrapper.orderBy(true, isAsc, User::getUserName);
                default -> {
                }
            }
        }

        return queryWrapper;
    }

    /**
     * 登录成功后的统一后续处理：更新登录时间/IP、Sa-Token 登录、记录登录日志、写入 Session 并返回 LoginUserVO。
     *
     * @param user      已确定或新创建的用户
     * @param request   HTTP 请求，可为 null（如微信扫码时无 request）
     * @param loginType 登录类型
     * @param account   登录账号标识（用于日志）
     * @return 脱敏的登录用户视图，含 token
     */
    private LoginUserVO doAfterLoginSuccess(User user, HttpServletRequest request,
                                            LoginTypeEnum loginType, String account) {
        user.setLastLoginTime(new Date());
        if (request != null) {
            user.setLastLoginIp(IpUtils.getClientIp(request));
        }
        this.updateById(user);
        StpUtil.login(user.getId());
        UserLoginLogRecordRequest logRecordRequest = new UserLoginLogRecordRequest();
        logRecordRequest.setUser(user);
        logRecordRequest.setLoginType(loginType);
        logRecordRequest.setAccount(account);
        logRecordRequest.setHttpRequest(request);
        recordLoginLogAsync(logRecordRequest);
        LoginUserVO loginUserVO = getLoginUserVO(user);
        UserVO userVO = UserConvert.objToVo(user);
        StpUtil.getSession().set(UserConstant.USER_LOGIN_STATE, userVO);
        return loginUserVO;
    }

    /**
     * GitHub 登录：校验 state、用 code 换 token 并拉取用户信息，按 GitHub ID 或邮箱关联/创建用户后执行登录后续流程。
     *
     * @param code    授权码
     * @param state   防 CSRF 的 state
     * @param request HTTP 请求
     * @return {@link LoginUserVO}
     */
    @Override
    public LoginUserVO userLoginByGitHub(String code, String state, HttpServletRequest request) {
        gitHubOAuthService.validateAndConsumeState(state);
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "授权码不能为空");
        String accessToken = gitHubService.getAccessToken(code);
        ThrowUtils.throwIf(StringUtils.isBlank(accessToken), ErrorCode.OPERATION_ERROR, "获取 GitHub Access Token 失败");
        GitHubUserVO gitHubUserVO = gitHubService.getUserInfo(accessToken);
        ThrowUtils.throwIf(gitHubUserVO == null || StringUtils.isBlank(gitHubUserVO.getId()),
                ErrorCode.OPERATION_ERROR, "获取 GitHub 用户信息失败");

        String githubId = gitHubUserVO.getId();
        User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));
        // 未按 GitHub ID 命中时，尝试按邮箱关联已有账号并绑定 GitHub 信息
        if (user == null && StringUtils.isNotBlank(gitHubUserVO.getEmail())) {
            user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserEmail, gitHubUserVO.getEmail()));
            if (user != null) {
                user.setGithubId(githubId);
                user.setGithubLogin(gitHubUserVO.getLogin());
                user.setGithubUrl(gitHubUserVO.getHtmlUrl());
                if (StringUtils.isNotBlank(gitHubUserVO.getAvatarUrl()) && StringUtils.isBlank(user.getUserAvatar())) {
                    user.setUserAvatar(gitHubUserVO.getAvatarUrl());
                }
                boolean result = this.updateById(user);
                ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "关联 GitHub 账号失败");
            }
        }

        // 分布式锁内再查并创建，防止并发重复注册
        String lockKey = CacheConstant.USER_REGISTER_GITHUB + githubId;
        return lockUtils.lockEvent(lockKey,
                new TimeModel(UserLoginConstants.REGISTER_LOCK_SECONDS, UserLoginConstants.REGISTER_LOCK_TIME_UNIT),
                () -> {
                    User lockedUser = this.getOne(new LambdaQueryWrapper<User>().eq(User::getGithubId, githubId));
                    if (lockedUser == null) {
                        lockedUser = new User();
                        lockedUser.setGithubId(githubId);
                        lockedUser.setUserName(StringUtils.firstNonBlank(gitHubUserVO.getName(), gitHubUserVO.getLogin()));
                        lockedUser.setUserAvatar(gitHubUserVO.getAvatarUrl());
                        lockedUser.setGithubLogin(gitHubUserVO.getLogin());
                        lockedUser.setGithubUrl(gitHubUserVO.getHtmlUrl());
                        if (StringUtils.isNotBlank(gitHubUserVO.getEmail())) {
                            lockedUser.setUserEmail(gitHubUserVO.getEmail());
                            lockedUser.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                        }
                        lockedUser.setUserRole(UserRoleEnum.USER.getValue());
                        boolean result = this.save(lockedUser);
                        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "GitHub 注册失败");
                    }
                    return doAfterLoginSuccess(lockedUser, request, LoginTypeEnum.GITHUB, gitHubUserVO.getLogin());
                },
                () -> {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
                });
    }

    /**
     * 邮箱验证码登录：校验验证码后，按邮箱查用户，不存在则自动注册，再执行登录后续流程。
     *
     * @param userEmailLoginRequest 邮箱与验证码
     * @param request               HTTP 请求
     * @return {@link LoginUserVO}
     */
    @Override
    public LoginUserVO userLoginByEmail(UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userEmailLoginRequest == null, ErrorCode.PARAMS_ERROR, "请求不能为空");
        String email = StringUtils.trimToEmpty(userEmailLoginRequest.getEmail());
        String code = userEmailLoginRequest.getCode();
        ThrowUtils.throwIf(StringUtils.isAnyBlank(email, code), ErrorCode.PARAMS_ERROR, "邮箱或验证码不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(email), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
        boolean verifyResult = userEmailService.verifyEmailCode(email, code);
        ThrowUtils.throwIf(!verifyResult, ErrorCode.PARAMS_ERROR, "验证码错误或已过期");

        String lockKey = CacheConstant.USER_REGISTER_EMAIL + email;
        return lockUtils.lockEvent(lockKey,
                new TimeModel(UserLoginConstants.REGISTER_LOCK_SECONDS, UserLoginConstants.REGISTER_LOCK_TIME_UNIT),
                () -> {
                    User user = this.getOne(new LambdaQueryWrapper<User>().eq(User::getUserEmail, email));
                    if (user == null) {
                        // 自动注册
                        user = new User();
                        user.setUserEmail(email);
                        user.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                        user.setUserName(email.split("@")[0]);
                        user.setUserRole(UserRoleEnum.USER.getValue());
                        boolean result = this.save(user);
                        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "用户注册失败");
                    } else {
                        // 已有用户则确保邮箱验证状态为已验证
                        if (!EmailVerifiedEnum.VERIFIED.getValue().equals(user.getEmailVerified())) {
                            user.setEmailVerified(EmailVerifiedEnum.VERIFIED.getValue());
                            this.updateById(user);
                        }
                    }
                    userEmailService.deleteEmailCode(email);
                    return doAfterLoginSuccess(user, request, LoginTypeEnum.EMAIL, email);
                },
                () -> {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "登录人数过多，请稍后再试");
                });
    }

    /**
     * 异步记录登录日志
     *
     * @param logRecordRequest 登录日志记录请求
     */
    @Async
    public void recordLoginLogAsync(UserLoginLogRecordRequest logRecordRequest) {
        try {
            UserLoginLogAddRequest request = new UserLoginLogAddRequest();
            request.setUserId(logRecordRequest.getUser().getId());
            request.setAccount(logRecordRequest.getAccount());
            request.setLoginType(logRecordRequest.getLoginType().getValue());
            request.setStatus(LoginStatusEnum.SUCCESS.getValue());

            // 提取客户端信息
            HttpServletRequest httpRequest = logRecordRequest.getHttpRequest();
            if (httpRequest != null) {
                String clientIp = IpUtils.getClientIp(httpRequest);
                request.setClientIp(clientIp);
                request.setLocation(IpUtils.getRegion(clientIp));
                request.setUserAgent(httpRequest.getHeader("User-Agent"));
            }

            logFeignClient.addUserLoginLog(request);
        } catch (Exception e) {
            log.error("记录登录日志失败", e);
        }
    }

    @Override
    public String getGitHubAuthorizeUrl() {
        return gitHubOAuthService.buildAuthorizeUrl();
    }

    /**
     * 同步数据到 ES
     *
     * @param syncType      同步方式
     * @param minUpdateTime 最小更新时间 (增量同步时)
     */
    @Override
    public void syncToEs(EsSyncTypeEnum syncType, Date minUpdateTime) {
        log.info("[UserServiceImpl] 开始同步用户数据到 ES, 方式: {}, 起始时间: {}", syncType, minUpdateTime);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge(minUpdateTime != null, "update_time", minUpdateTime);
        queryWrapper.eq("is_delete", 0);

        long pageSize = 500;
        long current = 1;
        while (true) {
            Page<User> page = this.page(new Page<>(current, pageSize), queryWrapper);
            List<User> userList = page.getRecords();
            if (CollUtil.isEmpty(userList)) {
                break;
            }

            List<UserEsDTO> esDTOList = userList.stream()
                    .map(UserConvert::objToEsDTO)
                    .collect(Collectors.toList());

            EsSyncBatchMessage batchMessage = new EsSyncBatchMessage();
            batchMessage.setDataType(EsSyncDataTypeEnum.USER.getValue());
            batchMessage.setOperation("upsert");
            batchMessage.setDataContentList(esDTOList.stream().map(cn.hutool.json.JSONUtil::toJsonStr)
                    .collect(Collectors.toList()));
            batchMessage.setTimestamp(System.currentTimeMillis());

            mqSender.send(MqBizTypeEnum.ES_SYNC_BATCH, batchMessage);

            log.info("[UserServiceImpl] 已发送 {} 条用户同步消息, 当前页: {}", esDTOList.size(), current);

            if (userList.size() < pageSize) {
                break;
            }
            current++;
        }
        log.info("[UserServiceImpl] 用户数据同步指令处理完成");
    }
}
