package com.stephen.cloud.notification.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.api.notification.model.dto.NotificationAddRequest;
import com.stephen.cloud.api.notification.model.dto.NotificationQueryRequest;
import com.stephen.cloud.api.notification.model.vo.NotificationVO;
import com.stephen.cloud.notification.model.entity.Notification;

import java.util.List;

/**
 * 通知服务
 * <p>
 * 该服务负责系统中所有通知的完整生命周期管理。
 * 核心功能包括：
 * 1. <b>分发引擎</b>：解析多种目标源（全员、角色、特定 ID），实现智能通知路由。
 * 2. <b>合规性校验</b>：统一的通知内容及业务规则验证。
 * 3. <b>实时推送</b>：集成 WebSocket 与 MQ，确保通知毫秒级触达。
 * 4. <b>状态管理</b>：高性能的未读统计与已读/删除批处理操作。
 * </p>
 *
 * @author StephenQiu30
 */
public interface NotificationService extends IService<Notification> {

    /**
     * 创建单条基础通知
     * <p>
     * 适用于后端业务模块直接调用的场景（如点赞、评论触发的通知）。
     * </p>
     *
     * @param notification 通知实体信息
     * @return 数据库生成的 ID
     */
    Long addNotification(Notification notification);

    /**
     * 验证通知数据的完整性与业务规则
     *
     * @param notification 待校验实体
     * @param add          是否为新增操作（为 true 时会检查必填字段）
     */
    void validNotification(Notification notification, boolean add);

    /**
     * 生成通用查询封装
     * <p>
     * 支持根据用户 ID、通知类型、已读状态及时间范围进行多维度检索。
     * </p>
     *
     * @param notificationQueryRequest 封装的查询请求
     * @return LambdaQueryWrapper 查询条件
     */
    LambdaQueryWrapper<Notification> getQueryWrapper(NotificationQueryRequest notificationQueryRequest);

    /**
     * 将实体转换为视图对象
     *
     * @param notification 数据库实体
     * @return 脱敏后的 VO 对象
     */
    NotificationVO getNotificationVO(Notification notification);

    /**
     * 批量转换分页数据为视图分页
     * <p>
     * 内部采用批量查询方式高效获取关联的 User 信息并进行数据脱敏。
     * </p>
     *
     * @param notificationPage 原始实体分页
     * @return 填充了关联信息的视图分页
     */
    Page<NotificationVO> getNotificationVOPage(Page<Notification> notificationPage);

    /**
     * 标记指定通知为已读状态
     *
     * @param notificationId 通知的唯一 ID
     * @param operatorId     当前操作用户 ID
     * @param isAdmin        操作人是否具备管理员权限（具备则可跨用户标记）
     * @return 操作是否成功
     */
    boolean markRead(Long notificationId, Long operatorId, boolean isAdmin);

    /**
     * 快速标记用户名下所有通知为已读
     *
     * @param userId 目标用户 ID
     * @return 是否成功（若无未读通知亦返回 true）
     */
    boolean markAllRead(Long userId);

    /**
     * 实时统计用户的未读通知总数
     *
     * @param userId 目标用户 ID
     * @return 未读总数
     */
    long getUnreadCount(Long userId);

    /**
     * 批量持久化删除通知
     *
     * @param ids        待删除 ID 列表
     * @param operatorId 当前操作人
     * @param isAdmin    管理员权限标识
     * @return 实际成功删除的数量
     */
    int batchDeleteNotification(List<Long> ids, Long operatorId, boolean isAdmin);

    /**
     * 批量将多个通知设为已读
     *
     * @param ids        目标 ID 列表
     * @param operatorId 当前操作人
     * @param isAdmin    管理员权限标识
     * @return 实际成功更新的数量
     */
    int batchMarkRead(List<Long> ids, Long operatorId, boolean isAdmin);

    /**
     * 管理员智能分发入口 (统一分发引擎)
     * <p>
     * 支持多重分发逻辑：
     * 1. <b>广播模式</b>：设置 target 为 "all"，通知 0 号全局用户。
     * 2. <b>定向组模式</b>：target 为 "@role:VAL"，系统按角色动态拉取用户组。
     * 3. <b>精确定向模式</b>：target 为逗号分隔的 ID 序列。
     * 4. <b>后验消息一致性</b>：仅在事务 Commit 后发送 WebSocket/MQ。
     * </p>
     *
     * @param request 封装的分发请求
     * @return 成功创建的通知 ID 集合
     */
    List<Long> addNotification(NotificationAddRequest request);
}
