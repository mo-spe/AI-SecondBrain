package com.secondbrain.common;

/**
 *
 *系统常量类
 * @author songliquan
 * @version 1.0
 * @since 2026/7/29 14:26
 */
public final class SystemConstants {

    public static final String SENSITIVE_WORD_CACHE_KEY="cache:sensitive:words";

    public static final long CACHE_TTL_HOURS = 1;

    public SystemConstants(){
        throw new UnsupportedOperationException("禁止实例化常量");
    }
}