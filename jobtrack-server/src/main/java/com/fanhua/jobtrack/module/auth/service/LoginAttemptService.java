package com.fanhua.jobtrack.module.auth.service;

import com.fanhua.jobtrack.config.properties.AuthProperties;
import com.fanhua.jobtrack.infrastructure.cache.RedisSafeOps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 登录失败限流。
 *
 * 主路径：Redis 计数（窗口期）+ 锁定键；Redis 不可用时降级为进程内存计数，
 * 保证限流功能本身不成为单点故障。所有降级写 WARN 日志。
 * 键格式：jobtrack:auth:login-fail:{account}:{ip} / jobtrack:auth:login-lock:{account}:{ip}
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final String FAIL_PREFIX = "jobtrack:auth:login-fail:";
    private static final String LOCK_PREFIX = "jobtrack:auth:login-lock:";

    private final RedisSafeOps redisOps;
    private final AuthProperties properties;

    /** Redis 故障时的内存兜底（单机够用，进程重启自动清零） */
    private final Map<String, MemoryRecord> memoryFallback = new ConcurrentHashMap<>();

    public LoginAttemptService(RedisSafeOps redisOps, AuthProperties properties) {
        this.redisOps = redisOps;
        this.properties = properties;
    }

    private String failKey(String account, String ip) {
        return FAIL_PREFIX + account + ":" + ip;
    }

    private String lockKey(String account, String ip) {
        return LOCK_PREFIX + account + ":" + ip;
    }

    /** 是否处于锁定期 */
    public boolean isLocked(String account, String ip) {
        String value = redisOps.get(lockKey(account, ip));
        if (value != null) {
            return true;
        }
        // Redis 不可用（get 返回 null）时检查内存兜底
        MemoryRecord record = memoryFallback.get(memoryKey(account, ip));
        return record != null && record.lockedUntilEpoch > Instant.now().getEpochSecond();
    }

    /** 锁定剩余秒数，未锁定返回 0 */
    public long lockedSecondsRemaining(String account, String ip) {
        MemoryRecord record = memoryFallback.get(memoryKey(account, ip));
        if (record != null) {
            return Math.max(0, record.lockedUntilEpoch - Instant.now().getEpochSecond());
        }
        return properties.getLoginLockMinutes() * 60L;
    }

    /**
     * 记录一次失败；返回本次失败是否触发了新锁定。
     */
    public boolean recordFailure(String account, String ip) {
        Duration window = Duration.ofMinutes(properties.getLoginFailWindowMinutes());
        long count = redisOps.incrWithExpire(failKey(account, ip), window);

        if (count >= 0) {
            // Redis 正常路径
            if (count >= properties.getLoginFailMax()) {
                redisOps.set(lockKey(account, ip), "1", Duration.ofMinutes(properties.getLoginLockMinutes()));
                redisOps.delete(failKey(account, ip));
                log.warn("登录连续失败触发锁定: account={}, ip={}, count={}", mask(account), ip, count);
                return true;
            }
            return false;
        }

        // Redis 故障，内存兜底
        MemoryRecord record = memoryFallback.compute(memoryKey(account, ip), (k, old) -> {
            long now = Instant.now().getEpochSecond();
            if (old == null || now - old.windowStartEpoch > window.getSeconds()) {
                return new MemoryRecord(new AtomicInteger(1), now, 0);
            }
            old.count.incrementAndGet();
            return old;
        });
        if (record.count.get() >= properties.getLoginFailMax()) {
            record.lockedUntilEpoch = Instant.now().getEpochSecond() + properties.getLoginLockMinutes() * 60L;
            record.count.set(0);
            log.warn("登录连续失败触发锁定（内存降级）: account={}, ip={}", mask(account), ip);
            return true;
        }
        return false;
    }

    /** 登录成功后清除失败计数与锁定 */
    public void recordSuccess(String account, String ip) {
        redisOps.delete(failKey(account, ip));
        redisOps.delete(lockKey(account, ip));
        memoryFallback.remove(memoryKey(account, ip));
    }

    /** 定期清理过期内存记录，避免长期运行泄漏 */
    public void evictExpiredMemoryRecords() {
        long now = Instant.now().getEpochSecond();
        memoryFallback.entrySet().removeIf(e ->
                now - e.getValue().windowStartEpoch > Duration.ofMinutes(properties.getLoginLockMinutes() * 2L).getSeconds()
                        && e.getValue().lockedUntilEpoch < now);
    }

    /** 日志中账号脱敏 */
    static String mask(String account) {
        return com.fanhua.jobtrack.common.util.MaskingUtils.maskAccount(account);
    }

    private String memoryKey(String account, String ip) {
        return account + ":" + ip;
    }

    private static final class MemoryRecord {
        private final AtomicInteger count;
        private final long windowStartEpoch;
        private volatile long lockedUntilEpoch;

        private MemoryRecord(AtomicInteger count, long windowStartEpoch, long lockedUntilEpoch) {
            this.count = count;
            this.windowStartEpoch = windowStartEpoch;
            this.lockedUntilEpoch = lockedUntilEpoch;
        }
    }
}
