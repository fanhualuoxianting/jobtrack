# JobTrack 项目交接文档

> 交接时间：2026-07-29
> 项目位置：`D:\AXE\code\jobtrack\`
> 需求规格：`D:\浏览器\PROJECT_SPEC.md`（最高需求来源，务必完整阅读）

---

## 一、当前进度

阶段 1（工程骨架）代码已全部生成，但**最终验收未完成**。具体状态：

| 项目 | 状态 |
|------|------|
| 后端工程 + 编译 | 已完成，`mvn test` BUILD SUCCESS |
| 前端工程 + 构建 | 已完成，`npm run build` 通过 |
| Docker Compose 语法 | 已验证通过 |
| MySQL 容器启动 | healthy，10 张表 + 演示数据已导入 |
| Redis 容器启动 | healthy |
| Spring Boot 连接真实 MySQL | **未通过**（见下方已知问题） |
| /actuator/health 返回 UP | **未通过** |
| /v3/api-docs | 已返回有效 OpenAPI JSON |
| Swagger UI | 页面可加载 |
| docker compose restart 幂等验证 | 未执行 |

---

## 二、已知问题与修复方向

### 问题 1：Spring Boot 连接 MySQL 失败（health DOWN）

**现象**：`/actuator/health` 返回 `{"status":"DOWN"}`，日志报 `DataSource health check failed - Failed to obtain JDBC Connection`。

**已排查**：
- Docker MySQL 容器 healthy，`docker exec` 内可正常查询
- 宿主机 `Test-NetConnection localhost 3307` 成功
- 已将 JDBC URL 中 `characterEncoding=utf8mb4` 改为 `characterEncoding=UTF-8`（原值不是合法 Java 字符集名）
- 改后重启仍然 DOWN

**下一步排查方向**：
1. 检查 MySQL 用户 `jobtrack` 是否允许从宿主机 TCP 连接（Docker MySQL 默认 `MYSQL_USER` 可能只授权了 `%` 或 `localhost`，需确认）
2. 在宿主机用 mysql 客户端测试：`mysql -h 127.0.0.1 -P 3307 -u jobtrack -pchange_me jobtrack`
3. 检查 `boot.log` 中 HikariPool 的完整异常栈（可能是 `Access denied` 或 `Unknown database`）
4. 确认 `application-dev.yml` 中的 `${MYSQL_PORT:3307}` 在 mvn 运行时是否正确解析（可临时硬编码 3307 测试）

### 问题 2：宿主机端口冲突

本机已有服务占用：
- 3306 → 本地 MySQL（PID 32624）
- 6379 → 本地 Redis
- 8080 → 本地 Tomcat（PID 33236）
- 8081 → 之前未杀干净的 Spring Boot 进程

**当前 .env 配置**：MySQL 映射 3307，Redis 映射 6380。Spring Boot 需手动指定端口（如 8082）。

### 问题 3：Redis 健康检查

即使 MySQL 修好，Redis 健康检查也可能因 Spring Boot 默认连 6379 而失败。`application-dev.yml` 已改为默认 6380，但需确认运行时生效。

---

## 三、项目结构

```
jobtrack/
├── docker-compose.yml          # MySQL 8.4 + Redis 8，含 volume/healthcheck/restart
├── .env                        # 本地端口配置（MYSQL_PORT=3307, REDIS_PORT=6380）
├── .env.example                # 模板
├── .gitignore
├── README.md                   # 骨架
├── sql/
│   ├── 01_schema.sql           # 10 张表，jt_company 无唯一索引（Service 层校验）
│   └── 02_init_data.sql        # 演示数据（BCrypt 密码）
├── jobtrack-server/
│   ├── pom.xml                 # Spring Boot 3.5.3, MyBatis-Plus 3.5.12, Java 21
│   └── src/
│       ├── main/java/com/fanhua/jobtrack/
│       │   ├── JobTrackApplication.java
│       │   ├── common/api/         Result, PageResult
│       │   ├── common/enums/       ErrorCode
│       │   ├── common/exception/   BusinessException, NotFound, Forbidden, Conflict
│       │   ├── common/handler/     GlobalExceptionHandler
│       │   ├── common/util/        TraceIdFilter
│       │   └── config/            SecurityConfig, RedisConfig, MybatisPlusConfig,
│       │                           OpenApiConfig, WebMvcConfig
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   └── logback-spring.xml
│       └── test/                   H2 内存库测试，3 个测试类全部通过
└── jobtrack-web/
    ├── package.json + lock
    ├── vite.config.ts
    ├── tsconfig.json
    └── src/
        ├── main.ts, App.vue
        ├── router/index.ts
        ├── types/index.ts
        ├── api/request.ts          Axios 封装
        ├── stores/auth.ts, app.ts
        └── views/                  占位页面（auth, dashboard, errors）
```

---

## 四、关键设计决策

1. **jt_company 唯一性**：已移除数据库 `(user_id, name, deleted)` 唯一索引，改为普通索引 `idx_company_user_name`。唯一性由 Service 层校验 `deleted=0` 下同名不可重复。创建和修改都必须调用同一套校验。

2. **SecurityConfig 阶段 1 状态**：仅放行 `/actuator/health`、`/swagger-ui/**`、`/swagger-ui.html`、`/v3/api-docs/**`、`/api/v1/auth/register|login|refresh`。无 JWT 过滤器，无 UserDetailsService 实现。

3. **RedisConfig**：加了 `@ConditionalOnBean(RedisConnectionFactory.class)`，测试环境排除 Redis 时不会报错。

4. **MyBatis-Plus 3.5.12**：分页插件需额外依赖 `mybatis-plus-jsqlparser`（已加入 pom）。

5. **演示账号**：`demo@jobtrack.local` / `JobTrack@123456`，BCrypt 哈希已写入 `02_init_data.sql`。

---

## 五、阶段 1 剩余验收清单

按 PROJECT_SPEC.md 第 33 节格式，接手者需完成：

- [ ] 修复 MySQL 连接问题，`/actuator/health` 返回 `{"status":"UP"}`
- [ ] 确认 `/v3/api-docs` 返回 200 + 有效 JSON
- [ ] 确认 Swagger UI 完整加载（不只是 HTML 骨架）
- [ ] `docker compose restart` 后数据仍在、容器恢复 healthy、初始化 SQL 不重复插入
- [ ] 后端用真实 MySQL+Redis 启动无 ERROR 日志
- [ ] 按第 33 节格式提交最终汇报

---

## 六、后续阶段概览

| 阶段 | 核心内容 | 前置条件 |
|------|----------|----------|
| 2 认证 | 注册/登录/JWT/Refresh Rotation/退出/前端登录页 | 阶段 1 验收通过 |
| 3 公司+岗位 | CRUD/分页/筛选/数据隔离/关联删除保护 | 阶段 2 |
| 4 简历 | 上传/下载/文件校验/默认简历/权限隔离 | 阶段 2 |
| 5 投递核心 | 状态机/时间线/乐观锁/组合筛选/归档 | 阶段 3+4 |
| 6 面试+提醒 | 面试 CRUD/自动提醒/定时扫描/通知中心 | 阶段 5 |
| 7 统计+缓存 | 首页卡片/漏斗/趋势/Redis 缓存与失效 | 阶段 5+6 |
| 8 测试+部署 | 30+ 测试/E2E/Dockerfile/Nginx/README | 全部 |

---

## 七、环境备忘

- Java 21（`java -version` 确认）
- Maven 3.9.9：`D:\atools\apache-maven-3.9.9\bin\mvn`
- Node.js + npm（前端构建）
- Docker Desktop（需手动启动）
- 本机有本地 MySQL(3306)、Redis(6379)、Tomcat(8080)，Docker 映射需避开
- Windows bash 中 `$` 变量会被吞，复杂 PowerShell 操作写 `.ps1` 文件执行

---

## 八、快速恢复命令

```bash
# 1. 启动 Docker Desktop，然后：
cd D:\AXE\code\jobtrack
docker compose up -d

# 2. 确认容器 healthy
docker compose ps

# 3. 验证 MySQL
docker exec jobtrack-mysql mysql -u jobtrack -pchange_me jobtrack -e "SHOW TABLES;"

# 4. 启动后端（避开 8080/8081）
cd jobtrack-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments="--server.port=8082"

# 5. 验证
curl http://localhost:8082/actuator/health
curl http://localhost:8082/v3/api-docs

# 6. 前端
cd jobtrack-web
npm install
npm run dev
```
