package com.fanhua.jobtrack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基类：Testcontainers 启动真实 MySQL 8.4 与 Redis 8。
 *
 * 采用 JVM 级单例容器（static 初始化启动），而非 @Testcontainers 扩展管理：
 * Spring 会缓存 ApplicationContext 供配置相同的测试类复用，若容器在类间
 * 停止-重启，复用上下文的测试类会得到指向已死容器的数据源（连接超时）。
 * 容器最终由 Ryuk 在测试 JVM 退出后统一清理。
 *
 * 表结构与开发库保持一致（Flyway 迁移）；前提：本机 Docker 可用。
 */
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    static final MySQLContainer MYSQL = new MySQLContainer(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("jobtrack_test")
            .withUsername("test")
            .withPassword("test");

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:8-alpine"))
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
        log.info("测试基础设施已启动: MySQL={}, Redis={}:{}", MYSQL.getJdbcUrl(), REDIS.getHost(), REDIS.getMappedPort(6379));
    }

    @DynamicPropertySource
    static void containerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                MYSQL.getJdbcUrl() + "?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false");
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
