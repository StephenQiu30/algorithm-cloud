package com.stephen.cloud.api.notification.client;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.notification.model.dto.NotificationQueryRequest;
import com.stephen.cloud.api.notification.model.vo.NotificationVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 通知服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "algorithm-notification-service", path = "/api/notification", contextId = "notificationFeignClient")
public interface NotificationFeignClient {

    /**
     * 根据 ID 获取通知 VO
     *
     * @param id 通知 ID
     * @return 通知信息
     */
    @GetMapping("/get/vo")
    BaseResponse<NotificationVO> getNotificationVOById(@RequestParam("id") Long id);

    /**
     * 分页查询通知列表（用于同步）
     *
     * @param notificationQueryRequest 查询请求
     * @return 通知列表
     */
    @PostMapping("/list/page/vo")
    BaseResponse<Page<NotificationVO>> listNotificationByPage(
            @RequestBody NotificationQueryRequest notificationQueryRequest);
}
