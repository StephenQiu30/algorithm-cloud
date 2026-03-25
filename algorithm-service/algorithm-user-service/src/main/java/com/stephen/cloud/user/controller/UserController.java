package com.stephen.cloud.user.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.user.model.dto.*;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.common.log.annotation.OperationLog;
import com.stephen.cloud.common.rabbitmq.enums.MqBizTypeEnum;
import com.stephen.cloud.common.rabbitmq.model.EsSyncMessage;
import com.stephen.cloud.common.rabbitmq.producer.RabbitMqSender;
import com.stephen.cloud.common.utils.IpUtils;
import com.stephen.cloud.user.convert.UserConvert;
import com.stephen.cloud.user.model.entity.User;
import com.stephen.cloud.user.service.UserEmailService;
import com.stephen.cloud.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户管理接口
 * <p>
 * 提供用户注册登录（GitHub/邮箱）、个人信息管理、权限控制等功能。
 * 支持 GitHub OAuth2.0 授权登录及邮箱验证码登录两种方式。
 * 用户敏感信息在返回前均经过脱敏处理。
 * </p>
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/user")
@Slf4j
@Tag(name = "UserController", description = "用户管理")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserEmailService userEmailService;

    @Resource
    private RabbitMqSender mqSender;

    /**
     * GitHub 登录
     * <p>
     * 通过 GitHub 授权码进行登录或注册。
     *
     * @param gitHubLoginRequest GitHub 登录请求参数
     * @param request            HTTP 请求
     * @return 登录成功的用户信息
     */
    @PostMapping("/login/github")
    @Operation(summary = "GitHub 登录", description = "通过 GitHub 授权码进行登录或注册")
    @OperationLog(module = "用户认证", action = "GitHub登录")
    public BaseResponse<LoginUserVO> userLoginByGitHub(@RequestBody GitHubLoginRequest gitHubLoginRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(gitHubLoginRequest == null || StringUtils.isBlank(gitHubLoginRequest.getCode())
                || StringUtils.isBlank(gitHubLoginRequest.getState()), ErrorCode.PARAMS_ERROR, "code 或 state 不能为空");
        String code = gitHubLoginRequest.getCode();
        String state = gitHubLoginRequest.getState();
        LoginUserVO loginUserVO = userService.userLoginByGitHub(code, state, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户邮箱登录
     *
     * @param userEmailLoginRequest 邮箱登录请求参数
     * @param request               HTTP 请求
     * @return 登录成功的用户信息
     */
    @PostMapping("/login/email")
    @Operation(summary = "用户邮箱登录", description = "使用邮箱 and 验证码进行登录")
    @OperationLog(module = "用户认证", action = "邮箱登录")
    public BaseResponse<LoginUserVO> userLoginByEmail(
            @Validated @RequestBody UserEmailLoginRequest userEmailLoginRequest,
            HttpServletRequest request) {
        LoginUserVO loginUserVO = userService.userLoginByEmail(userEmailLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 发送邮箱验证码
     * <p>
     * 向指定邮箱发送登录或注册所需的验证码。
     *
     * @param request     发送邮箱验证码请求
     * @param httpRequest HTTP 请求
     * @return 验证码过期时间（秒）
     */
    @PostMapping("/login/email/code")
    @Operation(summary = "发送邮箱验证码", description = "向指定邮箱发送登录或注册所需的验证码")
    @OperationLog(module = "用户认证", action = "发送邮箱验证码")
    public BaseResponse<Integer> sendEmailLoginCode(@Validated @RequestBody UserEmailCodeSendRequest request,
                                                    HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求不能为空");
        String clientIp = IpUtils.getClientIp(httpRequest);
        Integer expireSeconds = userEmailService.sendEmailCode(request.getEmail(), clientIp);
        return ResultUtils.success(expireSeconds);
    }

    /**
     * 获取 GitHub 授权 URL
     * <p>
     * 获取跳转到 GitHub 授权页面的 URL。
     *
     * @return GitHub 授权 URL
     */
    @GetMapping("/login/github")
    @Operation(summary = "获取 GitHub 授权 URL", description = "获取跳转到 GitHub 授权页面的 URL")
    public BaseResponse<String> getGitHubAuthorizeUrl() {
        String authorizeUrl = userService.getGitHubAuthorizeUrl();
        return ResultUtils.success(authorizeUrl);
    }

    /**
     * GitHub 登录回调接口
     * 接收 GitHub 授权码并完成用户登录或注册
     *
     * @param request     GitHub 回调参数 (code, state)
     * @param httpRequest HTTP 请求
     * @return 登录成功的用户信息
     */
    @GetMapping("/login/github/callback")
    @Operation(summary = "GitHub 登录回调", description = "用于接收 GitHub 授权重定向后的回调请求")
    public BaseResponse<LoginUserVO> gitHubLoginCallback(@ModelAttribute GitHubCallbackRequest request,
                                                         HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求不能为空");
        ThrowUtils.throwIf(StringUtils.isAnyBlank(request.getCode(), request.getState()), ErrorCode.PARAMS_ERROR, "code 或 state 不能为空");
        LoginUserVO loginUserVO = userService.userLoginByGitHub(request.getCode(), request.getState(), httpRequest);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 用户注销
     * <p>
     * 退出当前登录状态。
     *
     * @param request HTTP 请求
     * @return 是否成功退出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "退出当前登录状态")
    @OperationLog(module = "用户认证", action = "用户注销")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR, "请求不能为空");
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 获取当前登录用户
     * <p>
     * 获取系统当前登录的用户信息。
     *
     * @param request HTTP 请求
     * @return 当前登录的用户信息
     */
    @GetMapping("/get/login")
    @Operation(summary = "获取当前登录用户", description = "获取系统当前登录的用户信息")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    // endregion

    /**
     * 创建用户
     *
     * @param userAddRequest userAddRequest
     * @param request        request
     * @return BaseResponse<Long>
     */
    @PostMapping("/add")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @OperationLog(module = "用户管理", action = "创建用户")
    public BaseResponse<Long> addUser(@Validated @RequestBody UserAddRequest userAddRequest,
                                      HttpServletRequest request) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = UserConvert.addRequestToObj(userAddRequest);
        // 数据校验
        userService.validUser(user, true);
        // 写入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        // 返回新写入的数据 id
        long newTagId = user.getId();

        // 发送 ES 同步消息（失败不影响用户创建）
        try {
            UserVO userVO = UserConvert.objToVo(user);
            EsSyncMessage message = new EsSyncMessage("user", "upsert", newTagId,
                    JSONUtil.toJsonStr(userVO), System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, newTagId + ":" + System.currentTimeMillis(), message);
            log.info("用户创建 ES 同步消息已发送, userId: {}", newTagId);
        } catch (Exception e) {
            log.error("【ES同步失败】用户创建 ES 同步消息发送失败, userId: {}", newTagId, e);
        }

        return ResultUtils.success(newTagId);
    }

    /**
     * 删除用户接口
     * 仅该用户本人或系统管理员有权执行删除操作
     *
     * @param deleteRequest 删除请求参数（包含目标 ID）
     * @param request       HTTP 请求
     * @return 是否删除成功的布尔结果
     */
    @PostMapping("/delete")
    @Operation(summary = "删除用户", description = "删除指定 ID 的用户，需具备相应权限")
    @OperationLog(module = "用户管理", action = "删除用户")
    public BaseResponse<Boolean> deleteUser(@Validated @RequestBody DeleteRequest deleteRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        User oldUser = userService.getById(id);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        ThrowUtils.throwIf(!oldUser.getId().equals(user.getId()) && !userService.isAdmin(request),
                ErrorCode.NO_AUTH_ERROR);
        // 操作数据库
        boolean result = userService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 发送 ES 同步消息（失败不影响用户删除）
        try {
            // 注：删除操作需要在目标端进行判断，所以直接发送包含 ID 的空对象或是特殊标记，此处为了简单且保留一致性，建议在重构模型时改进
            // 目前依然发送，利用 RabbitMqSender 但需要 Consumer 处理
            EsSyncMessage message = new EsSyncMessage("user", "delete", id, null, System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, id + ":" + System.currentTimeMillis(), message);
            log.info("用户删除 ES 同步消息已发送, userId: {}", id);
        } catch (Exception e) {
            log.error("【ES同步失败】用户删除 ES 同步消息发送失败, userId: {}", id, e);
        }

        return ResultUtils.success(true);
    }

    /**
     * 更新用户
     *
     * @param userUpdateRequest userUpdateRequest
     * @param request           request
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/update")
    @OperationLog(module = "用户管理", action = "更新用户")
    public BaseResponse<Boolean> updateUser(@Validated @RequestBody UserUpdateRequest userUpdateRequest,
                                            HttpServletRequest request) {
        ThrowUtils.throwIf(userUpdateRequest == null || userUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User user = UserConvert.updateRequestToObj(userUpdateRequest);
        userService.validUser(user, false);
        long id = userUpdateRequest.getId();
        User oldUser = userService.getById(id);
        ThrowUtils.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        try {
            User updatedUser = userService.getById(id);
            UserVO userVO = UserConvert.objToVo(updatedUser);
            EsSyncMessage message = new EsSyncMessage("user", "upsert", id, JSONUtil.toJsonStr(userVO),
                    System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, id + ":" + System.currentTimeMillis(), message);
            log.info("用户更新 ES 同步消息已发送, userId: {}", id);
        } catch (Exception e) {
            log.error("【ES同步失败】用户更新 ES 同步消息发送失败, userId: {}", id, e);
        }

        return ResultUtils.success(true);
    }

    /**
     * 是否管理员（Feign 调用）
     *
     * @param request request
     * @return 是否管理员
     */
    @GetMapping("/is/admin")
    @Operation(summary = "是否管理员", description = "返回当前登录用户是否为管理员")
    public BaseResponse<Boolean> isAdmin(HttpServletRequest request) {
        try {
            return ResultUtils.success(userService.isAdmin(request));
        } catch (Exception e) {
            log.warn("获取管理员标记失败，按非管理员处理", e);
            return ResultUtils.success(false);
        }
    }

    /**
     * 根据 ID 获取用户详情（管理员权限）
     * 返回用户原始实体信息，包含敏感字段
     *
     * @param id      用户 ID
     * @param request HTTP 请求
     * @return 用户完整实体对象
     */
    @GetMapping("/get")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "根据 ID 获取用户详情", description = "管理员获取指定用户的原始实体信息")
    public BaseResponse<User> getUserById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 ID 获取脱敏用户信息
     *
     * @param id      用户 ID
     * @param request HTTP 请求
     * @return 封装后的用户脱敏视图对象
     */
    @GetMapping("/get/vo")
    @Operation(summary = "根据 ID 获取脱敏信息", description = "获取指定用户的脱敏后的视图数据")
    public BaseResponse<UserVO> getUserVOById(@RequestParam("id") long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user, request));
    }

    /**
     * 批量根据 id 获取用户包装类（Feign 调用）
     *
     * @param ids     用户id列表
     * @param request request
     * @return 查询得到的用户包装类列表
     */
    @GetMapping("/get/vo/batch")
    public BaseResponse<List<UserVO>> getUserVOByIds(@RequestParam("ids") List<Long> ids) {
        ThrowUtils.throwIf(ids == null || ids.isEmpty(), ErrorCode.PARAMS_ERROR);
        List<User> userList = userService.listByIds(ids);
        // 批量接口主要用于内部 Feign 调用（如 ES 同步），不依赖 HttpServletRequest 上下文
        List<UserVO> userVOList = userList.stream()
                .map(UserConvert::objToVo)
                .collect(Collectors.toList());
        return ResultUtils.success(userVOList);
    }

    /**
     * 分页获取用户列表（仅管理员）
     *
     * @param userQueryRequest userQueryRequest
     * @param request          request
     * @return BaseResponse<Page < User>>
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<User>> listUserByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                   HttpServletRequest request) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        return ResultUtils.success(userPage);
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest 用户查询请求
     * @param request          request
     * @return BaseResponse<Page < UserVO>>
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest,
                                                       HttpServletRequest request) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<User> userPage = userService.page(new Page<>(current, size),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVO(userPage.getRecords(), request);
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 编辑个人信息
     *
     * @param userEditRequest userEditRequest
     * @param request         request
     * @return BaseResponse<Boolean>
     */
    @PostMapping("/edit")
    @OperationLog(module = "用户管理", action = "编辑个人信息")
    public BaseResponse<Boolean> editUser(@Validated @RequestBody UserEditRequest userEditRequest,
                                          HttpServletRequest request) {
        ThrowUtils.throwIf(userEditRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        User user = UserConvert.editRequestToObj(userEditRequest);
        user.setId(loginUser.getId());
        userService.validUser(user, false);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        try {
            User updatedUser = userService.getById(user.getId());
            UserVO userVO = UserConvert.objToVo(updatedUser);
            EsSyncMessage message = new EsSyncMessage("user", "upsert", user.getId(),
                    JSONUtil.toJsonStr(userVO), System.currentTimeMillis());
            mqSender.send(MqBizTypeEnum.ES_SYNC_SINGLE, user.getId() + ":" + System.currentTimeMillis(), message);
            log.info("用户更新个人信息 ES 同步消息已发送, userId: {}", user.getId());
        } catch (Exception e) {
            log.error("【ES同步失败】用户更新个人信息 ES 同步消息发送失败, userId: {}", user.getId(), e);
        }

        return ResultUtils.success(true);
    }

}
