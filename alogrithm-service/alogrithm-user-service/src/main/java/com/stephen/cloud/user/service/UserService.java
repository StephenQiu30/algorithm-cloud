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
 * 用户服务
 *
 * @author StephenQiu30
 */
public interface UserService extends IService<User> {

    /**
     * 校验用户数据
     *
     * @param user 用户实体
     * @param add  是否为新增操作 (新增时校验账号唯一性，更新时校验 ID)
     */
    void validUser(User user, boolean add);

    /**
     * 获取当前登录用户
     *
     * @param request request
     * @return {@link User}
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request request
     * @return {@link User}
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request request
     * @return boolean 是否为管理员
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user user
     * @return boolean 是否为管理员
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request request
     * @return boolean 用户注销
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return {@link LoginUserVO}
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user    user
     * @param request request
     * @return {@link UserVO}
     */
    UserVO getUserVO(User user, HttpServletRequest request);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList userList
     * @return {@link List<UserVO>}
     */
    List<UserVO> getUserVO(List<User> userList, HttpServletRequest request);

    /**
     * 分页获取用户视图类
     *
     * @param userPage userPage
     * @param request  request
     * @return {@link Page {@link UserVO} }
     */
    Page<UserVO> getUserVOPage(Page<User> userPage, HttpServletRequest request);

    /**
     * GitHub 登录
     *
     * @param code    授权码
     * @param state   状态码
     * @param request HTTP请求
     * @return {@link LoginUserVO}
     */
    LoginUserVO userLoginByGitHub(String code, String state, HttpServletRequest request);

    /**
     * 获取 GitHub 授权 URL（包含 state）
     *
     * @return 授权 URL
     */
    String getGitHubAuthorizeUrl();

    /**
     * 邮箱登录
     *
     * @param userEmailLoginRequest 邮箱登录请求
     * @param request               HTTP请求
     * @return {@link LoginUserVO}
     */
    LoginUserVO userLoginByEmail(UserEmailLoginRequest userEmailLoginRequest, HttpServletRequest request);

    /**
     * 根据查询请求构建 MyBatis Plus 的查询条件封装
     *
     * @param userQueryRequest 用户查询请求对象
     * @return LambdaQueryWrapper 查询条件封装
     */
    LambdaQueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 同步数据到 ES
     *
     * @param syncType      同步方式
     * @param minUpdateTime 最小更新时间 (增量同步时)
     */
    void syncToEs(EsSyncTypeEnum syncType, Date minUpdateTime);
}
