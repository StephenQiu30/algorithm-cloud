package com.stephen.cloud.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.user.model.dto.UserEmailLoginRequest;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.rabbitmq.enums.EsSyncTypeEnum;
import com.stephen.cloud.user.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;

/**
 * 用户服务接口
 * <p>
 * 提供用户管理、登录认证、数据校验、ES 同步等核心业务能力。
 * 登录相关方法统一在登录成功后调用 {@link #doAfterLoginSuccess} 执行后置处理。
 * </p>
 *
 * @author StephenQiu30
 */
public interface UserService extends IService<User> {

    /**
     * 校验用户数据合法性
     * <p>
     * 包含对用户名、邮箱、手机号、个人简介等字段的格式及业务规则校验。
     * 新增时校验必填字段唯一性，更新时允许部分字段为空。
     *
     * @param user 待校验的用户对象
     * @param add  是否为新增操作（新增时必填字段校验更严格）
     */
    void validUser(User user, boolean add);

    /**
     * 获取当前登录用户
     * <p>
     * 从 Sa-Token 会话中提取用户 ID，再查询数据库返回完整用户对象。
     * 用户未登录时抛出业务异常。
     *
     * @param request HTTP 请求（从中提取会话信息）
     * @return 当前登录的用户实体
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     * <p>
     * 与 {@link #getLoginUser} 的区别在于：未登录时返回 null 而非抛出异常。
     *
     * @param request HTTP 请求
     * @return 登录用户实体，若未登录则为 null
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 判断当前登录用户是否为管理员
     *
     * @param request HTTP 请求
     * @return true 表示当前用户拥有管理员角色
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 判断指定用户是否为管理员
     *
     * @param user 待判断的用户对象
     * @return true 表示该用户拥有管理员角色
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     * <p>
     * 使当前会话失效并清除服务端登录状态。
     *
     * @param request HTTP 请求
     * @return true 表示注销成功
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户视图
     * <p>
     * 在普通 {@link UserVO} 基础上额外包含当前登录态 Token，适用于登录接口返回值。
     *
     * @param user 已确认存在的用户对象
     * @return 包含 Token 的登录用户视图
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户视图
     * <p>
     * 对敏感字段（密码、密钥等）进行过滤，仅返回业务所需的公开字段。
     *
     * @param user    用户实体
     * @param request HTTP 请求
     * @return 脱敏后的用户视图
     */
    UserVO getUserVO(User user, HttpServletRequest request);

    /**
     * 批量获取脱敏用户视图
     *
     * @param userList 用户实体列表
     * @param request  HTTP 请求
     * @return 脱敏后的用户视图列表
     */
    List<UserVO> getUserVO(List<User> userList, HttpServletRequest request);

    /**
     * 分页获取用户视图
     *
     * @param userPage 用户分页数据
     * @param request  HTTP 请求
     * @return 脱敏后的用户视图分页对象
     */
    Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request);

    /**
     * GitHub 授权登录
     * <p>
     * 使用 GitHub 授权码完成用户登录或自动注册流程。
     * 若该 GitHub 账号首次登录，系统将自动创建本地用户记录。
     *
     * @param code    GitHub 授权回调码
     * @param state   防止 CSRF 攻击的随机状态码
     * @param request HTTP 请求
     * @return 登录成功后的用户视图（含 Token）
     */
    LoginUserVO userLoginByGitHub(String code, String state, HttpServletRequest request);

    /**
     * 获取 GitHub 授权跳转 URL
     * <p>
     * 返回跳转至 GitHub 授权页面的完整 URL，前端引导用户完成授权。
     *
     * @return GitHub OAuth 授权页面地址
     */
    String getGitHubAuthorizeUrl();

    /**
     * 邮箱验证码登录
     * <p>
     * 校验邮箱验证码后完成登录。若该邮箱首次登录，系统将自动创建本地用户记录。
     *
     * @param userEmailLoginRequest 包含邮箱和验证码的登录请求
     * @param request               HTTP 请求
     * @return 登录成功后的用户视图（含 Token）
     */
    LoginUserVO userLoginByEmail(UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request);

    /**
     * 根据查询请求构建 MyBatis Plus 查询条件封装
     *
     * @param userQueryRequest 用户查询请求对象（支持按用户名、邮箱、角色等条件过滤）
     * @return 封装好的查询条件，可直接用于 {@link IService#page}
     */
    LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 同步数据到 Elasticsearch
     * <p>
     * 支持全量同步和增量同步两种模式。增量同步时仅同步 updateTime >= minUpdateTime 的记录。
     *
     * @param syncType      同步方式（全量或增量）
     * @param minUpdateTime 增量同步的起始时间（仅增量模式生效）
     */
    void syncToEs(EsSyncTypeEnum syncType, Date minUpdateTime);
}
