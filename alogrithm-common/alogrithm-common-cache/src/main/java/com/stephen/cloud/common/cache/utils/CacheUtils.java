package com.stephen.cloud.common.cache.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * CacheUtils: 基于 Redisson 的 Redis 缓存工具类，提供通用的缓存管理功能。
 * <p>
 * 支持存取任意对象类型，包含多种数据结构，如字符串、列表、Map 和 Set 等。
 * </p>
 * 默认缓存时间为 5 分钟，可通过方法参数进行覆盖。
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class CacheUtils {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 默认缓存时间，单位：秒
     */
    private final Long DEFAULT_EXPIRED = 5 * 60L;

    /**
     * Redis key 前缀 (目前为空，预留扩展)
     */
    private static final String REDIS_KEY_PREFIX = "";

    /**
     * 读取缓存内容，自动序列化和反序列化
     *
     * @param key 缓存键
     * @param <T> 返回值的类型
     * @return 缓存值，或 null 如果键不存在
     */
    public <T> T get(String key) {
        return getWithCodec(key, null);
    }

    /**
     * 以字符串形式读取缓存内容
     *
     * @param key 缓存键
     * @return 缓存中的字符串值，或 null 如果键不存在
     */
    public String getString(String key) {
        return getWithCodec(key, StringCodec.INSTANCE);
    }

    /**
     * 设置缓存内容，自动序列化并设置默认过期时间
     *
     * @param key   缓存键
     * @param value 缓存值
     * @param <T>   值的类型
     */
    public <T> void put(String key, T value) {
        putWithExpiration(key, value, DEFAULT_EXPIRED);
    }

    /**
     * 设置缓存内容，允许自定义过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param expired 过期时间（秒），若为 0 或负数则使用默认过期时间
     * @param <T>     值的类型
     */
    public <T> void put(String key, T value, long expired) {
        putWithExpiration(key, value, expired);
    }

    /**
     * 以字符串形式设置缓存内容
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void putString(String key, String value) {
        putWithExpiration(key, value, DEFAULT_EXPIRED, StringCodec.INSTANCE);
    }

    /**
     * 以字符串形式设置缓存内容，允许自定义过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param expired 过期时间（秒），若为 0 或负数则使用默认过期时间
     */
    public void putString(String key, String value, long expired) {
        putWithExpiration(key, value, expired, StringCodec.INSTANCE);
    }

    /**
     * 删除缓存项
     *
     * @param key 缓存键
     */
    public void remove(String key) {
        redissonClient.getBucket(prefixedKey(key)).delete();
    }

    /**
     * 删除缓存项（别名方法）
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        remove(key);
    }

    /**
     * 检查缓存项是否存在
     *
     * @param key 缓存键
     * @return true 如果缓存项存在；否则 false
     */
    public boolean exists(String key) {
        return redissonClient.getBucket(prefixedKey(key)).isExists();
    }

    // ==================== Hash 操作 ====================

    /**
     * 设置 Hash 类型的缓存
     *
     * @param key 缓存键
     * @param map Hash 数据
     * @param <K> Hash 键的类型
     * @param <V> Hash 值的类型
     */
    public <K, V> void setHash(String key, Map<K, V> map) {
        setHash(key, map, DEFAULT_EXPIRED);
    }

    /**
     * 设置 Hash 类型的缓存，允许自定义过期时间
     *
     * @param key     缓存键
     * @param map     Hash 数据
     * @param expired 过期时间（秒）
     * @param <K>     Hash 键的类型
     * @param <V>     Hash 值的类型
     */
    public <K, V> void setHash(String key, Map<K, V> map, long expired) {
        RMap<K, V> rMap = redissonClient.getMap(prefixedKey(key));
        rMap.putAll(map);
        if (expired > 0) {
            rMap.expire(Duration.ofSeconds(expired));
        }
    }

    /**
     * 获取 Hash 类型的缓存
     *
     * @param key 缓存键
     * @param <K> Hash 键的类型
     * @param <V> Hash 值的类型
     * @return Hash 数据
     */
    public <K, V> Map<K, V> getHash(String key) {
        RMap<K, V> rMap = redissonClient.getMap(prefixedKey(key));
        return rMap.readAllMap();
    }

    /**
     * 获取 Hash 中的单个字段值
     *
     * @param key   缓存键
     * @param field Hash 字段
     * @param <K>   Hash 键的类型
     * @param <V>   Hash 值的类型
     * @return 字段值
     */
    public <K, V> V getHashField(String key, K field) {
        RMap<K, V> rMap = redissonClient.getMap(prefixedKey(key));
        return rMap.get(field);
    }

    /**
     * 设置 Hash 中的单个字段值
     *
     * @param key   缓存键
     * @param field Hash 字段
     * @param value 字段值
     * @param <K>   Hash 键的类型
     * @param <V>   Hash 值的类型
     */
    public <K, V> void setHashField(String key, K field, V value) {
        RMap<K, V> rMap = redissonClient.getMap(prefixedKey(key));
        rMap.put(field, value);
    }

    /**
     * 删除 Hash 中的字段
     *
     * @param key    缓存键
     * @param fields Hash 字段
     * @param <K>    Hash 键的类型
     */
    @SafeVarargs
    public final <K> void deleteHashFields(String key, K... fields) {
        RMap<K, ?> rMap = redissonClient.getMap(prefixedKey(key));
        rMap.fastRemove(fields);
    }

    /**
     * 检查 Hash 中是否存在指定字段
     *
     * @param key   缓存键
     * @param field Hash 字段
     * @param <K>   Hash 键的类型
     * @return true 如果字段存在；否则 false
     */
    public <K> boolean hashFieldExists(String key, K field) {
        RMap<K, ?> rMap = redissonClient.getMap(prefixedKey(key));
        return rMap.containsKey(field);
    }

    /**
     * 封装通用的缓存写入方法，允许指定过期时间
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param expired 过期时间（秒），为 0 或负数时使用默认过期时间
     * @param <T>     值的类型
     */
    public <T> void putWithExpiration(String key, T value, long expired) {
        putWithExpiration(key, value, expired, null);
    }

    /**
     * 封装通用的缓存写入方法，允许指定过期时间和自定义编解码器
     *
     * @param key     缓存键
     * @param value   缓存值
     * @param expired 过期时间（秒），为 0 或负数时使用默认过期时间
     * @param codec   编解码器（用于指定数据的序列化方式）
     * @param <T>     值的类型
     */
    private <T> void putWithExpiration(String key, T value, long expired, StringCodec codec) {
        RBucket<T> bucket = codec == null
                ? redissonClient.getBucket(prefixedKey(key))
                : redissonClient.getBucket(prefixedKey(key), codec);
        Duration expireDuration = Duration.ofSeconds(expired <= 0 ? DEFAULT_EXPIRED : expired);
        bucket.set(value, expireDuration);
    }

    /**
     * 封装通用的缓存读取方法，允许自定义编解码器
     *
     * @param key   缓存键
     * @param codec 编解码器
     * @return 缓存值，或 null 如果键不存在
     */
    private <T> T getWithCodec(String key, StringCodec codec) {
        RBucket<T> bucket = codec == null
                ? redissonClient.getBucket(prefixedKey(key))
                : redissonClient.getBucket(prefixedKey(key), codec);
        return bucket.get();
    }

    /**
     * 为键添加 Redis 前缀
     *
     * @param key 原始缓存键
     * @return 添加前缀后的缓存键
     */
    private String prefixedKey(String key) {
        return REDIS_KEY_PREFIX + key;
    }
}
