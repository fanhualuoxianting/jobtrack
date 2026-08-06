# JobTrack 工程证据与验收口径

本文档只记录可以从代码、测试、迁移脚本或运行结果中核验的能力，用于 README、简历和技术面试时统一口径。阶段开发过程和本机临时信息不属于公开项目证据。

## 1. 项目边界

JobTrack 是一个面向个人求职管理的模块化单体应用，覆盖：

- 公司与岗位管理；
- 简历上传、下载、重命名与默认版本；
- 投递状态流转、历史时间线、归档与恢复；
- 多轮面试、提醒和通知中心；
- Dashboard 漏斗、趋势、来源、行业和周期统计；
- 本地开发与 Docker 完整部署。

它不是招聘平台、企业 ATS、爬虫系统或自动投递工具，也没有接入真实招聘网站账号。

## 2. 可核验的核心设计

### 2.1 用户数据隔离

- Controller 从认证上下文取得当前用户；
- Service 层执行资源归属校验；
- Mapper SQL 继续强制携带 `user_id` 和软删除条件；
- 越权访问按资源不存在处理，避免泄露资源是否存在；
- Dashboard 的 `companyId` 筛选先校验公司归属。

面试时不要只回答“前端不显示别人的数据”。真正的隔离发生在服务与 SQL 层。

### 2.2 JWT 会话与 Refresh Token Rotation

- Access Token 与 Refresh Token 分离；
- Refresh Token 保存在 HttpOnly Cookie 中，数据库只保存哈希；
- Rotation 通过旧哈希条件更新完成原子替换；
- 旧 Refresh Token 再次出现时判定为重放并撤销会话；
- 支持多设备会话查看、单会话撤销、退出和全部退出；
- 登录失败限流在 Redis 不可用时有本地降级。

### 2.3 简历文件安全与一致性

上传 PDF / DOCX 时同时检查：

- 扩展名；
- 声明 MIME；
- PDF 文件头或 DOCX ZIP 内部结构；
- 流式 SHA-256；
- 存储路径是否仍在授权根目录内；
- 软链接与路径穿越风险。

文件先写临时目录，再在数据库事务中建立记录并原子移动；事务回滚会清理临时文件。删除后的物理文件清理由事务提交后动作和补偿队列处理，避免数据库与文件系统出现“半成功”。

### 2.4 投递领域状态机

状态变更通过统一领域入口完成，而不是直接修改 `status` 字段。该入口集中处理：

- 合法前后状态；
- 终态原因；
- 状态历史；
- 审计事件；
- 乐观锁；
- 数据库幂等键；
- 事务提交后的关联事件。

这样可以区分并发修改、网络重试和真正的业务冲突。

### 2.5 面试提醒与通知

- 创建面试时校验投递归属、非归档 / 非终态、时区、时间顺序和轮次唯一性；
- 首场面试可将投递推进到 `INTERVIEWING`；
- 修改面试使用乐观锁，并取消旧待发送提醒后重建；
- 扫描器先查候选，再通过带 due 条件的数据库原子 UPDATE 抢占提醒；
- Redis 不参与提醒去重事实；
- 通知先落数据库，再尝试 SSE 推送；用户离线后可从通知列表补拉；
- 单用户 SSE 连接数受限，连接关闭后释放资源。

提醒状态：

| 状态 | 含义 |
| --- | --- |
| `PENDING` | 已生成，等待扫描。 |
| `READY` | 已被某个扫描线程原子抢占。 |
| `SENT` | 通知事实已成功持久化。 |
| `FAILED` | 达到最大重试次数。 |
| `CANCELLED` | 业务取消后不再处理。 |

### 2.6 Dashboard 与 Redis 缓存

Dashboard 提供摘要、漏斗、趋势、来源、行业、周期和未来面试查询。阶段统计读取状态历史，而不是只看当前状态，因此可以表达“曾经到达过的阶段”。

缓存键包含：用户、统计类型、过滤条件哈希和用户版本号。应用、公司、面试等写事务提交后递增用户版本；新请求自动使用新版本键，旧键自然过期。Redis 读取、写入、序列化或连接失败时回源 MySQL，不影响核心统计接口可用性。

代表性索引位于：

- `V5__dashboard_analytics_indexes.sql`；
- `idx_app_dashboard_analytics`；
- `idx_status_log_dashboard_analytics`；
- `idx_interview_dashboard_analytics`。

执行计划必须在当前数据规模和 MySQL 版本上重新验证，不要把历史 `rows` 估计值当成永久结论。

## 3. 自动化验证

### 后端

```powershell
cd jobtrack-server
mvn -B clean test
```

集成测试使用 Testcontainers 启动真实 MySQL / Redis，覆盖认证、用户隔离、公司与岗位、简历文件安全、投递状态机、面试提醒、通知、SSE、Dashboard 与缓存降级。

### 前端

```powershell
cd jobtrack-web
npm ci
npm run type-check
npm run test
npm run build
```

### 生产拓扑

```powershell
Copy-Item .env.production.example .env.production
# 替换全部 CHANGE_ME 值
docker compose --env-file .env.production -f docker-compose.prod.yml config
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
```

### 真实 HTTP Smoke

```powershell
./scripts/smoke-test.ps1 -BaseUrl http://localhost:8080
```

脚本应以唯一测试用户运行，失败返回非零退出码，且不得输出 Token 或密码。

GitHub Actions 当前覆盖后端测试与打包、前端类型检查 / 测试 / 构建 / 依赖审计，以及生产 Compose 和镜像构建。

## 4. 简历表述与代码证据

| 可写表述 | 主要证据 |
| --- | --- |
| 使用 Spring Security 实现 JWT 与 Refresh Token Rotation，并检测旧令牌重放 | 认证模块、会话表迁移、认证集成测试 |
| 通过状态机、乐观锁和数据库幂等键处理并发流转与重试 | 投递领域服务、状态日志迁移、投递集成测试 |
| 对 PDF / DOCX 做扩展名、MIME、真实结构和 SHA-256 联合校验 | 简历模块、文件存储服务、简历集成测试 |
| 通过数据库原子抢占和 SSE 实现面试提醒与实时通知 | 提醒扫描服务、通知表、并发测试、SSE 测试 |
| 使用用户版本键失效 Dashboard 缓存，Redis 故障时回源 MySQL | Dashboard cache service、统计 Mapper、缓存降级测试 |
| 使用 Testcontainers、Docker Compose、Nginx 与 GitHub Actions建立验证和交付链 | 测试基类、Compose、Dockerfile、Nginx、CI workflow |

不要写无法从仓库证明的数字。测试数量、接口数量、构建体积和性能数据都应以当前分支重新运行后的结果为准。

## 5. 当前限制

- 尚未提供公开在线 Demo；
- README 缺少当前版本的真实运行截图与短视频；
- 统计周期目前提供平均、最小和最大值，尚无中位数 / 分位数；
- ECharts 仍是前端较大的异步资源，需要继续观察包体积；
- 当前仓库尚未声明开源许可证，公开复用边界需要由仓库所有者决定；
- 文件默认存储在本地 volume，尚未抽象为对象存储适配器；
- 单体架构适合当前规模，不应为了“技术栈更多”强行拆微服务。

剩余工作与 Codex 验收要求见 [CODEX_HANDOFF.md](CODEX_HANDOFF.md)。
