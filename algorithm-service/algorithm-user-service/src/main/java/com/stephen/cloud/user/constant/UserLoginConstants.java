package com.stephen.cloud.user.constant;

import java.util.concurrent.TimeUnit;

/**
 * 用户登录相关常量
 *
 * @author stephen
 */
public interface UserLoginConstants {


    /**
     * 注册/登录分布式锁超时时间（秒）
     */
    long REGISTER_LOCK_SECONDS = 5L;

    /**
     * 注册/登录分布式锁超时时间单位，与 {@link #REGISTER_LOCK_SECONDS} 配合使用
     */
    TimeUnit REGISTER_LOCK_TIME_UNIT = TimeUnit.SECONDS;
}
