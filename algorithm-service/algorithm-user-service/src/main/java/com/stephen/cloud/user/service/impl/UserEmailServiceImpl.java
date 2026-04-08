package com.stephen.cloud.user.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.stephen.cloud.api.mail.client.MailFeignClient;
import com.stephen.cloud.api.mail.model.dto.MailSendCodeRequest;
import com.stephen.cloud.common.cache.constants.CacheConstant;
import com.stephen.cloud.common.cache.model.TimeModel;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.common.cache.utils.lock.LockUtils;
import com.stephen.cloud.common.cache.utils.ratelimit.RateLimitUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.exception.BusinessException;
import com.stephen.cloud.common.utils.RegexUtils;
import com.stephen.cloud.user.config.EmailCodeProperties;
import com.stephen.cloud.user.service.UserEmailService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户邮箱服务实现
 * <p>
 * 提供邮箱验证码的发送、校验、限流等功能。
 * 支持基于 IP 和邮箱的发送频率限制，防止恶意刷验证码。
 * </p>
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class UserEmailServiceImpl implements UserEmailService {

    @Resource
    private MailFeignClient mailFeignClient;
    @Resource
    private EmailCodeProperties emailCodeProperties;
    @Resource
    private RateLimitUtils rateLimitUtils;
    @Resource
    private CacheUtils cacheUtils;
    @Resource
    private LockUtils lockUtils;

    @Override
    public Integer sendEmailCode(String email, String clientIp) {
        String normalizedEmail = StringUtils.trimToEmpty(email);
        ThrowUtils.throwIf(StringUtils.isBlank(normalizedEmail), ErrorCode.PARAMS_ERROR, "邮箱地址不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(normalizedEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
        ThrowUtils.throwIf(StringUtils.isBlank(clientIp), ErrorCode.PARAMS_ERROR, "客户端IP不能为空");

        String lockKey = "mail:send:code:" + normalizedEmail;
        Integer expireTime = lockUtils.lockEvent(lockKey, () -> {
            String emailLimitKey = CacheConstant.LOGIN_LIMIT_EMAIL + normalizedEmail;
            try {
                rateLimitUtils.doRateLimitAndExpire(
                        emailLimitKey,
                        new TimeModel((long) emailCodeProperties.getSendLimit(), TimeUnit.SECONDS),
                        1L, 1L,
                        new TimeModel((long) emailCodeProperties.getSendLimit(), TimeUnit.SECONDS));
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "验证码发送过于频繁，请稍后再试");
            }
            String ipLimitKey = CacheConstant.LOGIN_LIMIT_IP + clientIp;
            try {
                rateLimitUtils.doRateLimitAndExpire(
                        ipLimitKey,
                        new TimeModel(1L, TimeUnit.HOURS),
                        (long) emailCodeProperties.getIpLimit(), 1L,
                        new TimeModel(1L, TimeUnit.HOURS));
            } catch (BusinessException e) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "该 IP 请求验证码次数已达上限");
            }
            String code = RandomUtil.randomNumbers(emailCodeProperties.getLength());
            MailSendCodeRequest request = MailSendCodeRequest.builder()
                    .to(normalizedEmail)
                    .code(code)
                    .minutes(emailCodeProperties.getExpireTime() / 60)
                    .async(true)
                    .build();
            BaseResponse<Boolean> sendResponse = mailFeignClient.doSendVerificationCode(request);
            ThrowUtils.throwIf(sendResponse == null || !Boolean.TRUE.equals(sendResponse.getData()),
                    ErrorCode.OPERATION_ERROR, "发送验证码失败");
            String codeKey = CacheConstant.LOGIN_CODE_EMAIL + normalizedEmail;
            cacheUtils.putString(codeKey, code, emailCodeProperties.getExpireTime());
            log.info("[UserEmailServiceImpl] 验证码已发送, email: {}", normalizedEmail);
            return emailCodeProperties.getExpireTime();
        }, () -> {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, "请求处理中，请稍后刷新");
        });
        return expireTime;
    }

    @Override
    public boolean verifyEmailCode(String email, String code) {
        String normalizedEmail = StringUtils.trimToEmpty(email);
        ThrowUtils.throwIf(StringUtils.isBlank(normalizedEmail), ErrorCode.PARAMS_ERROR, "邮箱地址不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(normalizedEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
        ThrowUtils.throwIf(StringUtils.isBlank(code), ErrorCode.PARAMS_ERROR, "验证码不能为空");

        String codeKey = CacheConstant.LOGIN_CODE_EMAIL + normalizedEmail;
        String storedCode = cacheUtils.getString(codeKey);
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不存在或已过期");
        }
        if (!storedCode.equals(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "输入的验证码有误");
        }
        return true;
    }

    @Override
    public boolean deleteEmailCode(String email) {
        String normalizedEmail = StringUtils.trimToEmpty(email);
        ThrowUtils.throwIf(StringUtils.isBlank(normalizedEmail), ErrorCode.PARAMS_ERROR, "邮箱地址不能为空");
        ThrowUtils.throwIf(!RegexUtils.checkEmail(normalizedEmail), ErrorCode.PARAMS_ERROR, "用户邮箱格式有误");
        String codeKey = CacheConstant.LOGIN_CODE_EMAIL + normalizedEmail;
        cacheUtils.remove(codeKey);
        return true;
    }
}
