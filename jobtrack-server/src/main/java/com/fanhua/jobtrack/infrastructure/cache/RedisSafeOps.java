package com.fanhua.jobtrack.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 安全操作封装：
 * Redis 是加速器而非主数据源，任何异常都只降级、不抛出影响核心业务。
 * 所有降级路径都会写 WARN 日志。
 */
@Slf4j
@Component
public class RedisSafeOps {

    private final StringRedisTemplate redisTemplate;

    public RedisSafeOps(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /** 读取字符串值；Redis 异常时返回 null 并告警，调用方需回源数据库 */
    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis 读取失败，降级处理: key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    /** 写入并设置 TTL；失败仅告警 */
    public void set(String key, String value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (Exception e) {
            log.warn("Redis 写入失败，降级处理: key={}, error={}", key, e.getMessage());
        }
    }

    /** 自增并保证过期时间；Redis 异常返回 -1，调用方走降级逻辑 */
    public long incrWithExpire(String key, Duration ttl) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                redisTemplate.expire(key, ttl);
            }
            return value == null ? -1 : value;
        } catch (Exception e) {
            log.warn("Redis 自增失败，降级为内存计数: key={}, error={}", key, e.getMessage());
            return -1;
        }
    }

    /** 删除失败记录告警，不抛出 */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis 删除失败: key={}, error={}", key, e.getMessage());
        }
    }

    /** 判断 Redis 当前是否可用（供降級分支选择） */
    public boolean isAvailable() {
        try {
            String pong = redisTemplate.getConnectionFactory() != null
                    ? redisTemplate.getConnectionFactory().getConnection().ping()
                    : null;
            return pong != null;
        } catch (Exception e) {
            return false;
        }
    }
}
