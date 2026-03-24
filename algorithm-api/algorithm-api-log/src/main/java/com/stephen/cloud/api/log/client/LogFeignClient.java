package com.stephen.cloud.api.log.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogAddRequest;
import com.stephen.cloud.api.log.model.dto.access.ApiAccessLogQueryRequest;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.email.EmailRecordQueryRequest;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordQueryRequest;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogAddRequest;
import com.stephen.cloud.api.log.model.dto.login.UserLoginLogQueryRequest;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogAddRequest;
import com.stephen.cloud.api.log.model.dto.operation.OperationLogQueryRequest;
import com.stephen.cloud.api.log.model.vo.*;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 日志服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "algorithm-log-service", path = "/api/log", contextId = "logFeignClient")
public interface LogFeignClient {

    /**
     * 创建用户登录日志
     *
     * @param request 登录日志创建请求
     * @return 是否创建成功
     */
    @PostMapping("/login/add")
    BaseResponse<Boolean> addUserLoginLog(@RequestBody UserLoginLogAddRequest request);

    /**
     * 创建操作日志
     *
     * @param request 操作日志创建请求
     * @return 是否创建成功
     */
    @PostMapping("/operation/add")
    BaseResponse<Boolean> addOperationLog(@RequestBody OperationLogAddRequest request);

    /**
     * 创建API访问日志
     *
     * @param request API访问日志创建请求
     * @return 是否创建成功
     */
    @PostMapping("/access/add")
    BaseResponse<Boolean> addApiAccessLog(@RequestBody ApiAccessLogAddRequest request);

    /**
     * 创建邮件记录
     *
     * @param request 邮件记录创建请求
     * @return 是否创建成功
     */
    @PostMapping("/email/add")
    BaseResponse<Boolean> addEmailRecord(@RequestBody EmailRecordAddRequest request);

    /**
     * 创建文件上传记录
     *
     * @param request 文件上传记录创建请求
     * @return 是否创建成功
     */
    @PostMapping("/file/upload/add")
    BaseResponse<Boolean> addFileUploadRecord(@RequestBody FileUploadRecordAddRequest request);

    /**
     * 分页查询用户登录日志
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/login/list/page")
    BaseResponse<Page<UserLoginLogVO>> listUserLoginLogByPage(@RequestBody UserLoginLogQueryRequest request);

    /**
     * 分页查询操作日志
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/operation/list/page")
    BaseResponse<Page<OperationLogVO>> listOperationLogByPage(@RequestBody OperationLogQueryRequest request);

    /**
     * 分页查询API访问日志
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/access/list/page")
    BaseResponse<Page<ApiAccessLogVO>> listApiAccessLogByPage(@RequestBody ApiAccessLogQueryRequest request);

    /**
     * 分页查询邮件记录
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/email/list/page")
    BaseResponse<Page<EmailRecordVO>> listEmailRecordByPage(@RequestBody EmailRecordQueryRequest request);

    /**
     * 分页查询文件上传记录
     *
     * @param request 查询请求
     * @return 分页结果
     */
    @PostMapping("/file/upload/list/page")
    BaseResponse<Page<FileUploadRecordVO>> listFileUploadRecordByPage(
            @RequestBody FileUploadRecordQueryRequest request);
}
