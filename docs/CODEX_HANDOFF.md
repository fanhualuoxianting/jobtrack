# JobTrack · Codex 接力清单

> 目标：在不破坏现有业务闭环的前提下，完成“最新验证、展示证据、少量高价值优化”，让仓库与简历经得住面试官逐项核对。

## 一、审查结论

JobTrack 的主体已经完整，不需要为了显得复杂继续堆模块。当前最有价值的工程证据是：

- SQL 与 Service 双层用户隔离；
- JWT Access Token + Refresh Token Rotation + 重放检测；
- PDF / DOCX 真实结构校验、SHA-256 与文件事务补偿；
- 投递状态机、乐观锁和数据库幂等；
- 数据库原子抢占提醒、通知落库和 SSE；
- 基于状态历史的 Dashboard；
- Redis 用户版本缓存与 MySQL 回源；
- Testcontainers、前端测试、Docker、Nginx 和 GitHub Actions。

本轮已经删除过时的阶段 1 `HANDOVER.md` 和“等待补截图”的内部占位说明。后续不要再把本机路径、端口 PID、临时故障记录或阶段交付口吻放回仓库根目录。

## 二、P0：必须完成

### P0-1：重新跑全量验证并固定真实数字

不要沿用旧简历中的“87 项后端测试”“7 个接口”“约 120 秒”等数字，除非当前分支重新运行后仍可复现。

执行：

```powershell
# 后端
cd jobtrack-server
mvn -B clean test
mvn -B -DskipTests package

# 前端
cd ..\jobtrack-web
npm ci
npm run type-check
npm run test
npm run build
npm audit --audit-level=high

# Compose 和镜像
cd ..
docker compose --env-file .env.production.example -f docker-compose.prod.yml config
docker build -f jobtrack-server/Dockerfile jobtrack-server
docker build -f jobtrack-web/Dockerfile .
```

然后启动完整环境：

```powershell
docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
./scripts/smoke-test.ps1 -BaseUrl http://localhost:8080
```

输出一份 `docs/current-verification.md`，只记录：

- 分支与 commit SHA；
- JDK / Node / Docker 版本；
- 后端测试总数与失败数；
- 前端测试总数与构建结果；
- Smoke 通过的关键业务链；
- 失败项、限制和复现命令。

不得复制历史阶段报告中的测试数量冒充当前结果。

### P0-2：补真实运行截图和短演示

在真实启动环境中采集：

1. 登录页；
2. Dashboard；
3. 公司 / 岗位管理；
4. 简历上传与默认版本；
5. 投递状态时间线；
6. 面试与提醒；
7. 通知中心 / SSE；
8. Docker Compose 健康状态或 Smoke 结果。

要求：

- 使用专门的 demo 用户和虚构公司 / 岗位；
- 截图不得包含真实手机号、邮箱、Token、本机绝对路径或浏览器个人信息；
- README 首页只放 2～3 张最能说明项目的图，其余放 `docs/showcase/`；
- 最好补一段 60～90 秒 GIF / MP4，展示“创建投递 → 状态变化 → 安排面试 → 通知 → Dashboard 更新”。

### P0-3：许可证由仓库所有者确认

当前仓库没有 LICENSE。Codex 不能自行假设代码所有权并直接添加 MIT。

先让仓库所有者明确选择：

- 完全保留所有权，不开源；
- MIT；
- Apache-2.0；
- 其他与课程 / 团队归属一致的许可证。

确认后再添加 LICENSE，并同步 README。若暂不决定，保持当前“未声明许可证”的准确说明。

### P0-4：补基础供应链安全

建议在 CI 中增加：

- Maven dependency review / OWASP Dependency-Check 或等价方案；
- 前端 `npm audit --audit-level=high` 已存在时保留；
- Trivy 对生产镜像进行高危漏洞扫描；
- secret scan；
- 生成 CycloneDX SBOM 并作为 CI artifact 上传。

扫描失败要区分“项目引入的可修漏洞”和“基础镜像暂未修复漏洞”，不要用无条件 `continue-on-error` 掩盖。

## 三、P1：高价值工程优化

### P1-1：补浏览器 E2E

使用 Playwright 或 Cypress 覆盖一条完整主流程：

1. 注册 / 登录；
2. 新建公司和岗位；
3. 上传测试简历；
4. 创建投递；
5. 修改投递状态并查看时间线；
6. 安排面试；
7. 检查通知与 Dashboard。

测试必须使用隔离数据库或唯一用户，结束后可安全清理，不能依赖固定 ID。

### P1-2：统计周期增加中位数与分位数

当前平均值容易被极端样本影响。可增加：

- median / P50；
- P75；
- P90；
- 样本数与“样本不足”提示。

要求先明确 MySQL 8.4 下的计算方案和索引影响，再改 API 与前端；不要在 Java 中一次性加载全量历史记录后排序。

### P1-3：优化 ECharts 异步包体积

- 先用构建分析报告确认真正的大模块；
- 继续按图表类型按需引入；
- 页面级异步加载；
- 记录优化前后 raw / gzip 体积；
- 不要为了减少几十 KB 牺牲可维护性。

### P1-4：抽象文件存储接口

在不改变业务 Service 的前提下抽象：

```text
ResumeStorage
├── LocalVolumeResumeStorage
└── S3CompatibleResumeStorage（可选）
```

要求：

- 继续保留真实结构校验与 SHA-256；
- 对象 key 不使用用户原始文件名；
- 上传、数据库事务和补偿删除语义不退化；
- 集成测试默认使用本地或容器化 MinIO，不调用真实云服务。

## 四、P2：可选优化

- 为关键审计事件增加统一 traceId；
- 增加慢查询与缓存命中率指标；
- 将 OpenAPI JSON 作为版本化 Release artifact；
- 补数据库 ER 图和关键状态机图的自动生成；
- 加入测试数据工厂，减少集成测试中的重复准备代码。

## 五、不建议做的事

- 不要为了“架构高级”拆成微服务；
- 不要加入招聘网站爬虫、自动投递或账号接管；
- 不要把 demo 数据包装成真实用户增长或性能数据；
- 不要把本机 Docker 健康等同于公网生产可用；
- 不要在 README 堆完整阶段开发日志；
- 不要把 AI 生成的测试数量或性能数字直接写进简历；
- 不要删除失败分支、异常日志或安全拒绝逻辑来让演示表面成功。

## 六、简历一致性检查

当前建议表述：

> 面向个人求职管理场景开发前后端分离平台，使用 Spring Security 实现 JWT 与 Refresh Token Rotation，设计投递状态机并结合乐观锁和数据库幂等处理并发更新；对 PDF / DOCX 进行真实结构与 SHA-256 校验，通过数据库原子抢占和 SSE 完成面试提醒，并使用 Redis 用户版本键缓存 Dashboard 统计、在故障时回源 MySQL。

提交简历前逐项核对：

- 测试数与当前 CI 一致；
- Dashboard 接口数与当前 Controller 一致；
- 缓存 TTL 与配置一致；
- 截图来自当前版本；
- 没有写“线上已部署”却只存在本地 Compose；
- 没有写“生产级”却缺少可核验的运维、安全和容量证据。

## 七、给 Codex 的首轮执行指令

```text
完整阅读 README.md、docs/engineering-evidence.md、docs/architecture.md、
docs/authentication.md、docs/application-state-machine.md、docs/file-storage.md、
docs/interview-reminder.md、docs/security.md 和本文件。

第一轮只做审计，不改业务：
1. 在新分支运行后端、前端、Docker 和 Smoke 全量验证；
2. 从测试报告和 Controller 自动统计当前测试数与接口数；
3. 检查 README、简历描述与代码是否存在数字或能力错位；
4. 生成 docs/current-verification.md；
5. 列出最多 5 个真正影响投递展示的问题，按 P0/P1 排序；
6. 不通过测试证明的结论不要写入 README。
```

## 八、完成定义

- 主流程 E2E 可重复运行；
- GitHub Actions 全绿；
- README 有当前真实截图和演示；
- 所有简历数字来自当前验证结果；
- 许可证状态明确；
- 高危依赖、镜像漏洞与密钥扫描有可查看结果；
- 不存在本机绝对路径、临时账号、Token、真实求职数据或过时交接文档。
