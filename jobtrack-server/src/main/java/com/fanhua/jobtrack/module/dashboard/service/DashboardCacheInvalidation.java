package com.fanhua.jobtrack.module.dashboard.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class DashboardCacheInvalidation {
    private final DashboardCacheService cacheService;

    public DashboardCacheInvalidation(DashboardCacheService cacheService) {
        this.cacheService = cacheService;
    }

    /** 只在业务事务提交后递增用户版本，避免回滚事务误使缓存失效。 */
    public void afterCommit(Long userId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cacheService.invalidate(userId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cacheService.invalidate(userId);
            }
        });
    }
}
