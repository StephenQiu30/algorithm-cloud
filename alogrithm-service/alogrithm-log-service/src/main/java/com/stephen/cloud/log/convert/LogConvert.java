package com.stephen.cloud.log.convert;

import com.stephen.cloud.api.log.model.vo.*;
import com.stephen.cloud.log.model.entity.*;
import org.springframework.beans.BeanUtils;

/**
 * 日志转换器
 *
 * @author StephenQiu30
 */
public class LogConvert {

    /**
     * 操作日志转视图
     *
     * @param operationLog 操作日志
     * @return 视图对象
     */
    public static OperationLogVO operationLogToVO(OperationLog operationLog) {
        if (operationLog == null) {
            return null;
        }
        OperationLogVO vo = new OperationLogVO();
        BeanUtils.copyProperties(operationLog, vo);
        return vo;
    }

    /**
     * 接口访问日志转视图
     *
     * @param apiAccessLog 接口访问日志
     * @return 视图对象
     */
    public static ApiAccessLogVO apiAccessLogToVO(ApiAccessLog apiAccessLog) {
        if (apiAccessLog == null) {
            return null;
        }
        ApiAccessLogVO vo = new ApiAccessLogVO();
        BeanUtils.copyProperties(apiAccessLog, vo);
        return vo;
    }

    /**
     * 用户登录日志转视图
     *
     * @param userLoginLog 用户登录日志
     * @return 视图对象
     */
    public static UserLoginLogVO userLoginLogToVO(UserLoginLog userLoginLog) {
        if (userLoginLog == null) {
            return null;
        }
        UserLoginLogVO vo = new UserLoginLogVO();
        BeanUtils.copyProperties(userLoginLog, vo);
        return vo;
    }

    /**
     * 邮件记录转视图
     *
     * @param emailRecord 邮件记录
     * @return 视图对象
     */
    public static EmailRecordVO emailRecordToVO(EmailRecord emailRecord) {
        if (emailRecord == null) {
            return null;
        }
        EmailRecordVO vo = new EmailRecordVO();
        BeanUtils.copyProperties(emailRecord, vo);
        return vo;
    }

    /**
     * 文件上传记录转视图
     *
     * @param fileUploadRecord 文件上传记录
     * @return 视图对象
     */
    public static FileUploadRecordVO fileUploadRecordToVO(FileUploadRecord fileUploadRecord) {
        if (fileUploadRecord == null) {
            return null;
        }
        FileUploadRecordVO vo = new FileUploadRecordVO();
        BeanUtils.copyProperties(fileUploadRecord, vo);
        return vo;
    }
}
