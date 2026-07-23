package com.secondbrain.service.impl;

import com.secondbrain.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/** 缓存服务实现类. <p>缓存服务实现，封装Redis操作</p> */
@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 设置缓存.
     *
     * @param key 缓存键
     * @param value 缓存值
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return void
     */
    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.debug("缓存设置成功，key：{}，过期时间：{} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("缓存设置失败，key：{}", key, e);
        }
    }

    /**
     * 获取缓存.
     *
     * @param key 缓存键
     * @param clazz 值类型
     * @return 缓存值
     * @param <T> 值类型
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null && clazz.isInstance(value)) {
                log.debug("缓存命中，key：{}", key);
                return clazz.cast(value);
            }
            log.debug("缓存未命中，key：{}", key);
            return null;
        } catch (Exception e) {
            log.error("缓存获取失败，key：{}", key, e);
            return null;
        }
    }

    /**
     * 删除缓存.
     *
     * @param key 缓存键
     * @return void
     */
    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("缓存删除成功，key：{}", key);
        } catch (Exception e) {
            log.error("缓存删除失败，key：{}", key, e);
        }
    }

    /**
     * 检查缓存是否存在.
     *
     * @param key 缓存键
     * @return 是否存在
     */
    @Override
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("检查缓存存在性失败，key：{}", key, e);
            return false;
        }
    }

    /**
     * 设置缓存过期时间.
     *
     * @param key 缓存键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return void
     */
    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
            log.debug("设置缓存过期时间，key：{}，过期时间：{} {}", key, timeout, unit);
        } catch (Exception e) {
            log.error("设置缓存过期时间失败，key：{}", key, e);
        }
    }

    /**
     * 获取缓存过期时间.
     *
     * @param key 缓存键
     * @param unit 时间单位
     * @return 过期时间
     */
    @Override
    public long getExpire(String key, TimeUnit unit) {
        try {
            Long expire = redisTemplate.getExpire(key, unit);
            return expire != null ? expire : -1;
        } catch (Exception e) {
            log.error("获取缓存过期时间失败，key：{}", key, e);
            return -1;
        }
    }

    /**
     * 获取缓存，未命中则加载.
     *
     * @param key 缓存键
     * @param clazz 值类型
     * @param timeout 过期时间
     * @param unit 时间单位
     * @param loader 缓存加载器
     * @return 缓存值
     * @param <T> 值类型
     */
    @Override
    public <T> T getOrLoad(String key, Class<T> clazz, long timeout, TimeUnit unit, CacheLoader<T> loader) {
        try {
            T cached = get(key, clazz);
            if (cached != null) {
                return cached;
            }

            T value = loader.load();
            if (value != null) {
                set(key, value, timeout, unit);
            }
            return value;
        } catch (Exception e) {
            log.error("缓存加载失败，key：{}", key, e);
            return null;
        }
    }

    /**
     * 按模式批量删除缓存.
     *
     * @param pattern 键模式
     * @return void
     */
    @Override
    public void deletePattern(String pattern) {
        try {
            redisTemplate.delete(redisTemplate.keys(pattern));
            log.debug("批量删除缓存成功，pattern：{}", pattern);
        } catch (Exception e) {
            log.error("批量删除缓存失败，pattern：{}", pattern, e);
        }
    }
}
