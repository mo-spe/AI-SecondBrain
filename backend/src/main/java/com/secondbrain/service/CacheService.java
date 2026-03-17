package com.secondbrain.service;

import java.util.concurrent.TimeUnit;

public interface CacheService {

    void set(String key, Object value, long timeout, TimeUnit unit);

    <T> T get(String key, Class<T> clazz);

    void delete(String key);

    boolean exists(String key);

    void expire(String key, long timeout, TimeUnit unit);

    long getExpire(String key, TimeUnit unit);

    <T> T getOrLoad(String key, Class<T> clazz, long timeout, TimeUnit unit, CacheLoader<T> loader);

    void deletePattern(String pattern);

    interface CacheLoader<T> {
        T load();
    }
}
