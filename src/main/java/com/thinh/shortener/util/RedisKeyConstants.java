package com.thinh.shortener.util;

public final class RedisKeyConstants {
    private RedisKeyConstants() {
    }

    public static final String BLACKLIST_PREFIX = "shortener:blacklist:";
    public static final String REFRESH_TOKEN_PREFIX = "shortener:refresh_tokens:";
    public static final String URL_PREFIX = "shortener:urls:";
}
