package com.stephen.cloud.log.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.stephen.cloud.log.config.LogCleanupProperties;
import com.stephen.cloud.log.model.entity.ApiAccessLog;
import com.stephen.cloud.log.model.entity.OperationLog;
import com.stephen.cloud.log.model.entity.UserLoginLog;
import com.stephen.cloud.log.service.ApiAccessLogService;
import com.stephen.cloud.log.service.OperationLogService;
import com.stephen.cloud.log.service.UserLoginLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupJob {

    private static final int BATCH_SIZE = 1000;

    private final LogCleanupProperties properties;
    private final OperationLogService operationLogService;
    private final ApiAccessLogService apiAccessLogService;
    private final UserLoginLogService userLoginLogService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void clean() {
        if (!properties.isEnabled()) {
            return;
        }
        cleanOperationLog();
        cleanApiAccessLog();
        cleanUserLoginLog();
    }

    private void cleanOperationLog() {
        int days = properties.getOperationRetentionDays();
        if (days <= 0) {
            return;
        }
        Date threshold = toDate(LocalDateTime.now().minusDays(days));
        long total = batchRemove(operationLogService,
                () -> new LambdaQueryWrapper<OperationLog>()
                        .lt(OperationLog::getCreateTime, threshold)
                        .select(OperationLog::getId)
                        .last("LIMIT " + BATCH_SIZE));
        log.info("[LogCleanupJob] 操作日志清理完成, 共删除{}条, retentionDays={}", total, days);
    }

    private void cleanApiAccessLog() {
        int days = properties.getApiAccessRetentionDays();
        if (days <= 0) {
            return;
        }
        Date threshold = toDate(LocalDateTime.now().minusDays(days));
        long total = batchRemove(apiAccessLogService,
                () -> new LambdaQueryWrapper<ApiAccessLog>()
                        .lt(ApiAccessLog::getCreateTime, threshold)
                        .select(ApiAccessLog::getId)
                        .last("LIMIT " + BATCH_SIZE));
        log.info("[LogCleanupJob] API访问日志清理完成, 共删除{}条, retentionDays={}", total, days);
    }

    private void cleanUserLoginLog() {
        int days = properties.getLoginRetentionDays();
        if (days <= 0) {
            return;
        }
        Date threshold = toDate(LocalDateTime.now().minusDays(days));
        long total = batchRemove(userLoginLogService,
                () -> new LambdaQueryWrapper<UserLoginLog>()
                        .lt(UserLoginLog::getCreateTime, threshold)
                        .select(UserLoginLog::getId)
                        .last("LIMIT " + BATCH_SIZE));
        log.info("[LogCleanupJob] 登录日志清理完成, 共删除{}条, retentionDays={}", total, days);
    }

    @SuppressWarnings("unchecked")
    private <T> long batchRemove(IService<T> service, Supplier<LambdaQueryWrapper<T>> wrapperSupplier) {
        long total = 0;
        while (true) {
            List<Object> ids = service.listObjs(wrapperSupplier.get());
            if (ids == null || ids.isEmpty()) {
                break;
            }
            List<Serializable> idList = ids.stream()
                    .map(id -> (Serializable) id)
                    .toList();
            service.removeByIds(idList);
            total += idList.size();
            if (idList.size() < BATCH_SIZE) {
                break;
            }
        }
        return total;
    }

    private Date toDate(LocalDateTime time) {
        return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
    }
}
