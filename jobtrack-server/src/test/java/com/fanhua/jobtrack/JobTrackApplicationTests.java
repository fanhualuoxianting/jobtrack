package com.fanhua.jobtrack;

import org.junit.jupiter.api.Test;

/**
 * Spring 上下文启动测试（Testcontainers 真实 MySQL + Redis + Flyway 迁移）
 */
class JobTrackApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // 验证 Spring 上下文在真实基础设施下可以正常启动
    }
}
