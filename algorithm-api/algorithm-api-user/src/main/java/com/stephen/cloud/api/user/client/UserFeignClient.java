package com.stephen.cloud.api.user.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.user.model.dto.UserEditRequest;
import com.stephen.cloud.api.user.model.dto.UserQueryRequest;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户服务 Feign 客户端
 * 提供跨微服务调用的用户相关接口
 *
 * @author StephenQiu30
 */
@FeignClient(name = "algorithm-user-service", path = "/api/user", contextId = "userFeignClient")
public interface UserFeignClient {

    /**
     * 根据 ID 获取用户脱敏信息
     *
     * @param id 用户 ID
     * @return 包含用户脱敏信息的响应负载
     */
    @GetMapping("/get/vo")
    BaseResponse<UserVO> getUserVOById(@RequestParam("id") Long id);

    /**
     * 批量获取用户脱敏信息
     *
     * @param ids 用户 ID 列表
     * @return 包含用户脱敏信息列表的响应负载
     */
    @GetMapping("/get/vo/batch")
    BaseResponse<List<UserVO>> getUserVOByIds(@RequestParam("ids") List<Long> ids);

    /**
     * 获取当前系统登录用户
     *
     * @return 当前登录用户脱敏信息的响应负载
     */
    @GetMapping("/get/login")
    BaseResponse<LoginUserVO> getLoginUser();

    /**
     * 校验当前登录用户是否为管理员
     *
     * @return 角色校验结果的响应负载
     */
    @GetMapping("/is/admin")
    BaseResponse<Boolean> isAdmin();

    /**
     * 分页查询用户列表（用于同步）
     *
     * @param userQueryRequest 查询请求
     * @return 用户列表
     */
    @PostMapping("/list/page/vo")
    BaseResponse<Page<UserVO>> listUserByPage(
            @RequestBody UserQueryRequest userQueryRequest);
}
