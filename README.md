# JobTrack

JobTrack 是面向高校学生求职场景的前后端分离投递管理平台，覆盖公司与岗位、简历版本、投递状态流转、多轮面试、站内提醒、SSE 通知和求职数据统计。

## 技术栈

- 后端：Java 21、Spring Boot 3.5、Spring Security、JWT、MyBatis-Plus、Flyway、MySQL 8.4、Redis 8
- 前端：Vue 3、TypeScript strict、Vite、Element Plus、Pinia、ECharts、Vitest
- 交付：Docker 多阶段构建、Docker Compose、Nginx、GitHub Actions

## 快速开始：开发模式

要求：Java 21、Maven、Node 22 LTS、Docker Desktop。

```powershell
Copy-Item .env.example .env
docker compose up -d mysql redis

# 终端 1
cd jobtrack-server
mvn spring-boot:run

# 终端 2
cd jobtrack-web
npm ci
npm run dev
```

默认访问：前端 <http://localhost:5173>，后端健康检查 <http://localhost:8082/actuator/health>，Swagger <http://localhost:8082/swagger-ui/index.html>。本机端口冲突时，在 `.env` 中调整 `MYSQL_PORT`、`REDIS_PORT`、`SERVER_PORT` 和 `VITE_PROXY_TARGET`。

开发 profile 会加载 `db/migration` 和 `db/demo`；生产 profile 只加载正式迁移。开发数据重置是破坏性操作，只针对当前开发 Compose：

```powershell
./scripts/reset-dev-data.ps1
```

脚本要求输入 `RESET-DEV-DATA` 或显式传 `-Force`，不会删除项目文件。

## 完整 Docker 部署

```powershell
Copy-Item .env.production.example .env.production
# 编辑 .env.production，替换全部 CHANGE_ME 值；JWT_SECRET 至少 32 字节
# 本地演示默认使用 http://localhost:8080 和 JOBTRACK_COOKIE_SECURE=false
# 正式部署请改为真实 HTTPS Origin，并将 JOBTRACK_COOKIE_SECURE=true
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

访问 <http://localhost:8080>。生产 Compose 包含 MySQL、Redis、Spring Boot Server、Nginx Web；只有 Web 暴露宿主机端口，数据库、Redis 和 Server 通过内部网络连接。上传文件使用独立 volume。重启验证：

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml restart
```

生产默认不插入演示数据，Swagger 默认关闭；设置 `SPRINGDOC_ENABLED=true` 后可临时开启。`JOBTRACK_FRONTEND_ORIGIN` 必须填写包含协议和端口的完整 Origin，例如本地演示使用 `http://localhost:8080`。不要把 `.env.production` 或真实密钥提交到 Git。

## 演示账号

仅限开发环境：

```text
账号：demo@jobtrack.local
密码：JobTrack@123456
```

密码由开发演示迁移以当前 BCrypt 配置写入。生产环境不会创建该账号。

## 测试与 Smoke

```powershell
# 后端：Testcontainers 启动真实 MySQL/Redis
cd jobtrack-server; mvn clean test

# 前端
cd ..\jobtrack-web
npm ci
npm run type-check
npm run test
npm run build

# 真实环境 Smoke；默认使用唯一用户，不删除已有数据
cd ..
./scripts/smoke-test.ps1 -BaseUrl http://localhost:8080
```

Smoke 支持 `SMOKE_BASE_URL`、`SMOKE_ACCOUNT`、`SMOKE_PASSWORD`，失败返回非零退出码且不输出 Token/密码。

## 核心设计

- SQL 层 `user_id` 隔离 + 服务层归属校验
- JWT Access Token + Refresh Token Rotation
- 投递状态机、乐观锁、数据库幂等
- 简历真实结构校验、路径穿越防护、文件事务补偿
- 面试提醒数据库原子抢占、SSE 实时通知和用户连接上限
- 基于状态历史的 Dashboard 统计；Redis 用户版本缓存，故障回源 MySQL
- TraceId、审计日志、Actuator 健康/指标端点和失败降级日志

## 文档

- [系统架构](docs/architecture.md)
- [数据库与迁移](docs/database.md)
- [认证与会话](docs/authentication.md)
- [投递状态机](docs/application-state-machine.md)
- [文件存储](docs/file-storage.md)
- [面试、提醒与通知](docs/interview-reminder.md)
- [Dashboard 指标与缓存](docs/stage7-dashboard.md)
- [部署说明](docs/deployment.md)
- [安全基线](docs/security.md)
- [演示脚本](docs/demo-script.md)
- [面试追问](docs/interview-notes.md)
- [简历项目描述](docs/resume-project-description.md)
- [阶段 6 收尾记录](docs/stage6-closure.md)
- [截图清单](docs/images/README.md)
- OpenAPI：开发环境访问 `/swagger-ui/index.html`，生产默认关闭，可通过 `SPRINGDOC_ENABLED=true` 临时开启。

## 目录结构

```text
jobtrack/
├── jobtrack-server/       # Spring Boot 后端、正式/开发 Flyway 迁移
├── jobtrack-web/           # Vue 3 前端和 Vitest
├── deploy/nginx.conf       # SPA、API、SSE 代理和安全头
├── scripts/                # 开发数据重置、真实 Smoke
├── docs/                   # 架构、部署、指标、演示和面试文档
├── docker-compose.yml      # 开发 MySQL/Redis
├── docker-compose.prod.yml # 完整生产拓扑
└── .github/workflows/ci.yml
```

## 许可证

当前仓库未声明开源许可证；如需公开发布，请先补充与项目归属一致的 LICENSE。
