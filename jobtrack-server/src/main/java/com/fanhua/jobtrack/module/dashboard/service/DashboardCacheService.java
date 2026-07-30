package com.fanhua.jobtrack.module.dashboard.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fanhua.jobtrack.infrastructure.cache.RedisSafeOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Service
public class DashboardCacheService {
    private static final Duration TTL = Duration.ofMinutes(2);
    private static final Duration VERSION_TTL = Duration.ofDays(30);

    private final RedisSafeOps redis;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public DashboardCacheService(RedisSafeOps redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public String key(Long userId, String type, String filter) {
        return "dashboard:v1:" + userId + ":" + type + ":" + hash(filter) + ":v" + version(userId);
    }

    public <T> T getOrLoad(String key, JavaType type, Supplier<T> loader) {
        T cached = read(key, type);
        if (cached != null) return cached;

        ReentrantLock lock = locks.computeIfAbsent(key, ignored -> new ReentrantLock());
        boolean acquired = false;
        try {
            acquired = lock.tryLock(200, TimeUnit.MILLISECONDS);
            if (!acquired) return loader.get();
            cached = read(key, type);
            if (cached != null) return cached;
            T value = loader.get();
            if (value != null) write(key, value);
            return value;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } finally {
            if (acquired) {
                lock.unlock();
                locks.remove(key, lock);
            }
        }
    }

    public void invalidate(Long userId) {
        redis.incrWithExpire("dashboard:version:" + userId, VERSION_TTL);
    }

    private String version(Long userId) {
        String version = redis.get("dashboard:version:" + userId);
        return version == null || version.isBlank() ? "0" : version;
    }

    private <T> T read(String key, JavaType type) {
        String value = redis.get(key);
        if (value == null) return null;
        try {
            return objectMapper.readValue(value, type);
        } catch (Exception e) {
            log.warn("看板缓存反序列化失败，回源数据库: key={}, error={}", key, e.getMessage());
            redis.delete(key);
            return null;
        }
    }

    private void write(String key, Object value) {
        try {
            redis.set(key, objectMapper.writeValueAsString(value), TTL);
        } catch (Exception e) {
            log.warn("看板缓存序列化失败，继续返回数据库结果: key={}, error={}", key, e.getMessage());
        }
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
