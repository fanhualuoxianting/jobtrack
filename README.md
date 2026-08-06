# JobTrack

[![CI](https://github.com/fanhualuoxianting/jobtrack/actions/workflows/ci.yml/badge.svg)](https://github.com/fanhualuoxianting/jobtrack/actions/workflows/ci.yml)

JobTrack 是一个面向高校学生与个人求职者的前后端分离投递管理平台，覆盖公司与岗位、简历版本、投递状态流转、多轮面试、提醒通知和数据统计。项目采用模块化单体架构，重点展示后端状态一致性、安全认证、文件安全、缓存降级、自动化测试和容器化交付。

> **项目边界**
> JobTrack 是个人求职管理工具，不是招聘网站、企业 ATS、爬虫或自动投递系统；仓库不会读取真实招聘平台账号，也不会伪造线上招聘数据。

## 核心能力

- 公司与岗位 CRUD、组合筛选、用户数据隔离和关联删除保护；
- PDF / DOCX 简历上传、下载、重命名、默认版本和安全校验；
- 投递状态机、历史时间线、乐观锁、数据库幂等和归档恢复；
- 多轮面试、时区处理、提醒任务、通知中心和 SSE 实时推送；
- Dashboard 摘要、漏斗、趋势、来源、行业、周期和未来面试；
- Redis 用户版本缓存、写事务提交后失效和故障回源 MySQL；
- Testcontainers、前端行为测试、Docker Compose、Nginx 与 GitHub Actions。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.5、Spring Security、JWT、MyBatis-Plus、Flyway |
| 数据 | MySQL 8.4、Redis 8、本地文件 volume |
| 前端 | Vue 3、TypeScript strict、Vite、Element Plus、Pinia、ECharts、Vitest |
| 交付 | Docker 多阶段构建、Docker Compose、Nginx、GitHub Actions |

## 关键工程设计

### 1. 状态机与一致性

投递状态变化统一经过领域状态机，集中处理合法流转、状态历史、终态原因、审计与事务后事件；结合乐观锁和数据库幂等键，区分并发修改、网络重试与真正的业务冲突。

### 2. 认证与会话安全

使用 JWT Access Token 与 Refresh Token Rotation。Refresh Token 存放在 HttpOnly Cookie 中，数据库只保存哈希；旧令牌再次出现时判定为重放并撤销会话。登录失败限流支持 Redis 与本地降级。

### 3. 文件安全

对 PDF / DOCX 同时检查扩展名、声明 MIME、真实文件结构和 SHA-256；存储层限制路径穿越与软链接逃逸，并通过临时文件、事务同步和补偿清理处理数据库记录与物理文件不一致。

### 4. 提醒与实时通知

提醒扫描通过数据库条件 UPDATE 原子抢占，Redis 不参与去重事实；通知先持久化，再尝试 SSE 推送。用户离线或连接断开后仍可从通知列表补拉。

### 5. Dashboard 缓存

统计读取状态历史而不是只看当前状态。缓存键包含用户、统计类型、过滤条件哈希与用户版本；写事务提交后递增版本，新请求使用新键，Redis 故障时自动回源 MySQL。

更完整的代码证据、验收命令和简历口径见 [docs/engineering-evidence.md](docs/engineering-evidence.md)。

## 快速开始：开发模式

环境要求：Java 21、Maven、Node 22 LTS、Docker Desktop。

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

默认地址：

- 前端：`http://localhost:5173`
- 后端健康检查：`http://localhost:8082/actuator/health`
- Swagger：`http://localhost:8082/swagger-ui/index.html`

本机端口冲突时，在 `.env` 中调整 `MYSQL_PORT`、`REDIS_PORT`、`SERVER_PORT` 与 `VITE_PROXY_TARGET`。

开发 profile 会加载正式迁移和演示数据；生产 profile 只加载正式迁移。重置开发数据是破坏性操作，只允许针对当前开发 Compose：

```powershell
./scripts/reset-dev-data.ps1
```

脚本要求输入 `RESET-DEV-DATA` 或显式传 `-Force`，不会删除项目代码。

## 演示账号

仅限开发环境：

```text
账号：demo@jobtrack.local
密码：JobTrack@123456
```

生产环境不会创建该账号。

## 完整 Docker 部署

```powershell
Copy-Item .env.production.example .env.production
# 替换全部 CHANGE_ME 值；JWT_SECRET 至少 32 字节
# 正式部署必须使用真实 HTTPS Origin，并设置 JOBTRACK_COOKIE_SECURE=true
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

访问 `http://localhost:8080`。生产 Compose 只暴露 Nginx Web 端口，MySQL、Redis 和 Spring Boot Server 位于内部网络，上传文件使用独立 volume。生产默认不插入演示数据，Swagger 默认关闭。

## 测试与 Smoke

```powershell
# 后端：Testcontainers 启动真实 MySQL / Redis
cd jobtrack-server
mvn -B clean test

# 前端
cd ..\jobtrack-web
npm ci
npm run type-check
npm run test
npm run build

# 完整环境 HTTP Smoke
cd ..
./scripts/smoke-test.ps1 -BaseUrl http://localhost:8080
```

Smoke 使用唯一测试用户，不删除已有数据；失败返回非零退出码，且不输出 Token 或密码。

## 仓库结构

```text
jobtrack/
├── jobtrack-server/       # Spring Boot 后端、Flyway 迁移与 Testcontainers 测试
├── jobtrack-web/          # Vue 3 前端、类型检查与 Vitest
├── deploy/nginx.conf      # SPA、API、SSE 代理与安全头
├── scripts/               # 开发数据重置和真实 HTTP Smoke
├── docs/                  # 架构、安全、业务设计、证据与面试文档
├── docker-compose.yml     # 开发 MySQL / Redis
├── docker-compose.prod.yml# 完整生产拓扑
└── .github/workflows/     # CI 与 Docker Smoke
```

## 文档

- [系统架构](docs/architecture.md)
- [数据库与迁移](docs/database.md)
- [认证与会话](docs/authentication.md)
- [投递状态机](docs/application-state-machine.md)
- [文件存储](docs/file-storage.md)
- [面试、提醒与通知](docs/interview-reminder.md)
- [部署说明](docs/deployment.md)
- [安全基线](docs/security.md)
- [工程证据与验收口径](docs/engineering-evidence.md)
- [演示脚本](docs/demo-script.md)
- [面试追问](docs/interview-notes.md)
- [简历项目描述](docs/resume-project-description.md)
- [Codex 接力清单](docs/CODEX_HANDOFF.md)

## 当前限制

- 尚未提供公开在线 Demo 和当前版本运行截图；
- Dashboard 尚未提供中位数 / 分位数周期统计；
- ECharts 仍是较大的异步资源，需要继续观察包体积；
- 文件存储目前使用本地 volume，尚未抽象为对象存储适配器；
- 仓库尚未声明开源许可证，复制、修改和再分发边界需由仓库所有者决定。

## 许可证

当前仓库未声明开源许可证。公开发布前应先选择与项目归属一致的许可证；不要在未确认所有权的情况下直接补 MIT。
