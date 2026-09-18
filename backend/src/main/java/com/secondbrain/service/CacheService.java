package com.secondbrain.service;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口.
 * <p>提供Redis缓存的基本操作功能</p>
 */
public interface CacheService {

    /**
     * 设置缓存.
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    void set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 获取缓存.
     *
     * @param key 缓存键
     * @param clazz 值类型
     * @return 缓存值
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 按完整泛型类型获取缓存。
     *
     * @param key 缓存键
     * @param typeReference 包含元素类型的目标类型
     * @return 缓存值，缓存不存在或转换失败时返回 null
     * @param <T> 值类型
     */
    <T> T get(String key, TypeReference<T> typeReference);

    /**
     * 删除缓存.
     *
     * @param key 缓存键
     */
    void delete(String key);

    /**
     * 判断缓存是否存在.
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean exists(String key);

    /**
     * 设置缓存过期时间.
     *
     * @param key 缓存键
     * @param timeout 超时时间
     * @param unit 时间单位
     */
    void expire(String key, long timeout, TimeUnit unit);

    /**
     * 获取缓存剩余过期时间.
     *
     * @param key 缓存键
     * @param unit 时间单位
     * @return 剩余时间
     */
    long getExpire(String key, TimeUnit unit);

    /**
     * 获取或加载缓存.
     *
     * @param key 缓存键
     * @param clazz 值类型
     * @param timeout 超时时间
     * @param unit 时间单位
     * @param loader 加载器
     * @return 缓存值
     */
    <T> T getOrLoad(String key, Class<T> clazz, long timeout, TimeUnit unit, CacheLoader<T> loader);

    /**
     * 按完整泛型类型获取或加载缓存。
     *
     * @param key 缓存键
     * @param typeReference 包含元素类型的目标类型
     * @param timeout 过期时间
     * @param unit 时间单位
     * @param loader 缓存加载器
     * @return 缓存值
     * @param <T> 值类型
     */
    <T> T getOrLoad(String key, TypeReference<T> typeReference, long timeout,
                    TimeUnit unit, CacheLoader<T> loader);

    /**
     * 按模式删除缓存.
     *
     * @param pattern 键模式
     */
    void deletePattern(String pattern);

    /**
     * 缓存加载器接口.
     *
     * @param <T> 值类型
     */
    interface CacheLoader<T> {
        /**
         * 加载数据.
         *
         * @return 加载的数据
         */
        T load();
    }
}
