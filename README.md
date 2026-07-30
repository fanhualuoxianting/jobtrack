# JobTrack 实习投递与面试管理平台

面向高校学生求职场景的前后端分离管理平台，支持公司与岗位管理、简历版本追踪、投递状态流转、多轮面试日程、站内提醒和求职数据统计。

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring Security（JWT + Refresh Token Rotation）、MyBatis-Plus、Flyway、MySQL 8.4、Redis 8
- 前端：Vue 3、TypeScript（strict）、Vite、Element Plus（按需引入）、Pinia、Vitest
- 部署：Docker Compose、Nginx

## 快速启动

### 1. 基础设施

```bash
cp .env.example .env
# 本机 3306/6379 被占用时，.env 中使用 MYSQL_PORT=3307、REDIS_PORT=6380
docker compose up -d mysql redis
```

### 开发数据重置

以下命令只针对当前 Docker Compose 开发环境，会删除 `jobtrack` 的 MySQL 与 Redis
volume，并在下次启动时重新执行 Flyway 迁移和演示数据脚本；不要在生产环境执行：

```powershell
docker compose down -v
docker compose up -d
```

也可以执行仓库内的 `scripts/reset-dev-data.ps1`，脚本会再次确认删除范围。

### 2. 后端

```bash
cd jobtrack-server
mvn clean test        # 自动化测试（Testcontainers 需要本机 Docker 运行中）
mvn spring-boot:run   # 首次启动自动执行 Flyway 建表与演示数据迁移
```

数据库结构由 `jobtrack-server/src/main/resources/db/migration` 中的 Flyway 脚本管理，
dev 环境同时加载 `db/demo` 下的演示数据；`sql/` 目录仅保留为历史参考。

### 3. 前端

```bash
cd jobtrack-web
npm install
npm run dev           # 默认代理 /api 到 http://localhost:8082
```

后端运行在其他端口时，通过 `VITE_PROXY_TARGET` 覆盖代理目标。

### 4. 访问

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8082/actuator/health
- Swagger：http://localhost:8082/swagger-ui/index.html

## 演示账号

```
邮箱：demo@jobtrack.local
密码：JobTrack@123456
```

演示密码以 BCrypt 哈希存库，由 Flyway 的 `V2__insert_demo_data.sql` 迁移写入。

## 当前进度（阶段 5 收尾完成，阶段 6 开始）

- [x] 阶段 1：工程骨架（前后端工程、Docker Compose、统一响应、全局异常、TraceId）
- [x] 阶段 2：认证与会话
  - 注册（BCrypt、用户名/邮箱 409 冲突、自动创建用户设置，注册后重新登录）
  - 登录（用户名或邮箱、统一失败提示不暴露账号存在、连续失败临时锁定 423）
  - Access Token（JWT HS256，30 分钟，claims: sub/userId/sessionId/roles/jti）
  - Refresh Token Rotation（HttpOnly Cookie + SameSite=Strict；只存 SHA-256 哈希；
    原子轮换；旧令牌重放判定攻击并撤销会话、写安全审计日志）
  - 多设备会话（会话列表、注销指定设备、注销其他设备、注销全部设备）
  - 审计日志（jt_audit_log 异步落库，失败不影响主业务）
  - Redis 快速撤销标记 + Redis 故障降级查库；登录限流 Redis 故障降级内存计数
  - 前端：登录/注册页、路由守卫（会话恢复）、Axios 单例刷新队列、安全设置页
- [x] 阶段 3：公司与岗位、简历管理
- [x] 阶段 4：简历文件安全与事务补偿
- [x] 阶段 5：投递生命周期、状态机、乐观锁、幂等、归档与前端页面
- [ ] 阶段 6：多轮面试、数据库提醒、定时扫描、通知中心与 SSE

## 测试

```bash
# 后端（Testcontainers 启动真实 MySQL/Redis，需 Docker 可用）
cd jobtrack-server && mvn test

# 前端（Vitest：Axios 刷新队列）
cd jobtrack-web && npm run test
```

## 项目结构

```
jobtrack/
├── jobtrack-server/     # Spring Boot 后端
│   └── src/main/resources/db/{migration,demo}/   # Flyway 正式迁移来源
├── jobtrack-web/        # Vue 3 前端
├── deploy/              # Docker、Nginx 部署配置
├── docs/                # 接口、数据库、截图
├── sql/                 # 历史参考（已被 Flyway 取代）
├── docker-compose.yml
├── .env.example
└── README.md
```

## 关键设计说明

### 认证与会话

- Access Token 存前端内存，永不落盘；Refresh Token 只在 HttpOnly Cookie 中，
  JS 无法读取；Cookie 限定 `Path=/api/v1/auth`、`SameSite=Strict`，生产环境开启 `Secure`。
- Refresh Token 形如 `sessionId.secret`，数据库 `jt_auth_session` 只保存 SHA-256 哈希。
- 每次刷新执行原子轮换（`UPDATE ... WHERE refresh_token_hash = old`），
  影响行数为 0 判定为并发或重放；确认为旧令牌重放时撤销整个会话并写安全日志。
- 会话撤销以数据库为准，Redis 作快速标记；Redis 宕机时降级查库，
  Access Token 30 分钟短有效期作为最终兜底。

### 安全

- 登录失败不区分"账号不存在"与"密码错误"，错误消息完全一致。
- 连续失败（默认 10 分钟 5 次）触发 15 分钟临时锁定（HTTP 423）。
- JWT 密钥仅经 `JWT_SECRET` 环境变量注入；`application-dev.yml` 中的值仅供本地开发。
- 密码、完整令牌、Cookie 内容不写入任何日志；审计中的账号均脱敏。
