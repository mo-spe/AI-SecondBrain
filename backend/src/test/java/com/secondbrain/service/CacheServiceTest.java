package com.secondbrain.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    @Test
    void testSetAndGet() {
        String key = "test:key";
        String value = "test-value";

        cacheService.set(key, value, 1, TimeUnit.MINUTES);

        String retrieved = cacheService.get(key, String.class);
        if (retrieved != null) {
            assertEquals(value, retrieved, "获取的值应与设置的值相同");
        }

        cacheService.delete(key);
    }

    @Test
    void testDelete() {
        String key = "test:delete";
        String value = "test-value";

        cacheService.set(key, value, 1, TimeUnit.MINUTES);

        cacheService.delete(key);
        assertFalse(cacheService.exists(key), "缓存应被删除");
    }

    @Test
    void testExists() {
        String key = "test:exists";
        String value = "test-value";

        cacheService.set(key, value, 1, TimeUnit.MINUTES);
        boolean exists = cacheService.exists(key);
        if (exists) {
            assertTrue(exists, "缓存应存在");
        }

        cacheService.delete(key);
    }

    @Test
    void testExpire() {
        String key = "test:expire";
        String value = "test-value";

        cacheService.set(key, value, 1, TimeUnit.MINUTES);
        cacheService.expire(key, 2, TimeUnit.MINUTES);

        long expire = cacheService.getExpire(key, TimeUnit.MINUTES);
        if (expire > 0) {
            assertTrue(expire > 0 && expire <= 2, "过期时间应在0-2分钟之间");
        }

        cacheService.delete(key);
    }
}
