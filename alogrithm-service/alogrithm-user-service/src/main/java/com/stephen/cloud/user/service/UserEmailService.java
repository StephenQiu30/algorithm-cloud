package com.stephen.cloud.user.service;

/**
 * 用户邮箱服务
 *
 * @author stephen
 */
public interface UserEmailService {

    /**
     * 发送邮箱验证码 (包含速率限制和过期机制)
     *
     * @param email    接收者邮箱
     * @param clientIp 客户端请求 IP (用于限流)
     * @return 验证码有效期（秒），如果发送过于频繁可能抛出异常
     */
    Integer sendEmailCode(String email, String clientIp);

    /**
     * 验证邮箱验证码的正确性
     *
     * @param email 邮箱
     * @param code  待验证的验证码
     * @return 验证是否通过
     */
    boolean verifyEmailCode(String email, String code);

    /**
     * 删除已存储的邮箱验证码 (验证通过后清理)
     *
     * @param email 邮箱
     * @return 是否成功删除
     */
    boolean deleteEmailCode(String email);
}
