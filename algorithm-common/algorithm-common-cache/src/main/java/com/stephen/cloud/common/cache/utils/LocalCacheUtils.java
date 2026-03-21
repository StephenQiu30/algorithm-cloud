package com.stephen.cloud.common.cache.utils;

import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 本地缓存工具类，提供对 Caffeine 缓存的基本操作
 *
 * @author StephenQiu30
 */
@Slf4j
@Component
public class LocalCacheUtils {

    @Resource(name = "localCache")
    private Cache<String, Object> caffeineClient;

    /**
     * 设置缓存
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    public void put(String key, Object value) {
        caffeineClient.put(key, value);
    }

    /**
     * 批量设置缓存
     *
     * @param keyAndValues 缓存键值对
     */
    public void putAll(Map<String, Object> keyAndValues) {
        caffeineClient.putAll(keyAndValues);
    }

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 返回对应的缓存值，如果不存在则返回 null
     */
    public Object get(String key) {
        return caffeineClient.getIfPresent(key);
    }

    /**
     * 获取缓存值（泛型版本）
     *
     * @param key 缓存键
     * @param <T> 返回值类型
     * @return 返回对应的缓存值，如果不存在则返回 null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        Object value = caffeineClient.getIfPresent(key);
        if (value != null && clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 批量获取缓存值
     *
     * @param keys 缓存键集合
     * @return 返回包含所有存在键的缓存值的映射
     */
    public Map<String, Object> getAll(Iterable<String> keys) {
        return caffeineClient.getAllPresent(keys);
    }

    /**
     * 删除指定的缓存
     *
     * @param key 缓存键
     */
    public void delete(String key) {
        caffeineClient.invalidate(key);
    }

    /**
     * 删除指定的缓存（别名方法）
     *
     * @param key 缓存键
     */
    public void remove(String key) {
        delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     */
    public void delete(Iterable<String> keys) {
        caffeineClient.invalidateAll(keys);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        caffeineClient.invalidateAll();
    }

    /**
     * 检查缓存项是否存在
     *
     * @param key 缓存键
     * @return true 如果缓存项存在；否则 false
     */
    public boolean exists(String key) {
        return caffeineClient.getIfPresent(key) != null;
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的条目数
     */
    public long size() {
        return caffeineClient.estimatedSize();
    }
}
