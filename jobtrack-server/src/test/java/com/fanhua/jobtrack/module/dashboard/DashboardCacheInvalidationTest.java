package com.fanhua.jobtrack.module.dashboard;

import com.fanhua.jobtrack.module.dashboard.service.DashboardCacheInvalidation;
import com.fanhua.jobtrack.module.dashboard.service.DashboardCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class DashboardCacheInvalidationTest {

    private final DashboardCacheService cache = mock(DashboardCacheService.class);
    private final DashboardCacheInvalidation invalidation = new DashboardCacheInvalidation(cache);

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void rollbackPathDoesNotAdvanceVersionBeforeCommit() {
        TransactionSynchronizationManager.initSynchronization();

        invalidation.afterCommit(7L);

        verifyNoInteractions(cache);
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCompletion(0);
        verifyNoInteractions(cache);
    }

    @Test
    void commitPathAdvancesVersionAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        invalidation.afterCommit(7L);
        TransactionSynchronizationManager.getSynchronizations().get(0).afterCommit();

        verify(cache).invalidate(7L);
    }
}
