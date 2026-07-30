package com.fanhua.jobtrack.infrastructure.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 物理文件删除失败补偿：
 * 前端回调中删除失败时进入内存队列，由定时任务重试；
 * 超过重试上限后以 ERROR 日志告警，供人工清理。
 * 说明：内存队列在进程重启后清空，孤儿文件由存储巡检（可选运维脚本）兜底，
 * 补偿失败绝不会静默忽略。
 */
@Slf4j
@Service
public class FileCleanupService {

    private static final int MAX_RETRY = 5;

    private final FileStorageService storageService;
    private final ConcurrentLinkedQueue<PendingDelete> pendingDeletes = new ConcurrentLinkedQueue<>();
    private final Map<String, AtomicInteger> retryCount = new ConcurrentHashMap<>();

    public FileCleanupService(FileStorageService storageService) {
        this.storageService = storageService;
    }

    /** 登记一个待删除文件（删除失败时调用） */
    public void registerPendingDelete(String storageKey) {
        pendingDeletes.offer(new PendingDelete(storageKey));
        retryCount.putIfAbsent(storageKey, new AtomicInteger(0));
        log.warn("物理文件删除失败，已登记补偿队列: {}", storageKey);
    }

    /** 定时重试删除失败文件 */
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void retryPendingDeletes() {
        PendingDelete pending;
        while ((pending = pendingDeletes.poll()) != null) {
            try {
                storageService.delete(pending.storageKey());
                retryCount.remove(pending.storageKey());
                log.info("补偿删除成功: {}", pending.storageKey());
            } catch (IOException e) {
                int attempts = retryCount.get(pending.storageKey()).incrementAndGet();
                if (attempts >= MAX_RETRY) {
                    retryCount.remove(pending.storageKey());
                    log.error("物理文件删除多次失败，达到重试上限，需要人工介入: {}, error={}",
                            pending.storageKey(), e.getMessage());
                } else {
                    pendingDeletes.offer(pending);
                }
            } catch (SecurityException e) {
                // 非法键不再重试
                log.error("补偿删除遇到非法存储键，放弃: {}", pending.storageKey());
                retryCount.remove(pending.storageKey());
            }
        }
    }

    /** 当前待补偿数量（测试与监控用） */
    public int pendingCount() {
        return pendingDeletes.size();
    }

    private record PendingDelete(String storageKey) {
    }
}
