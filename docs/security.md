# 安全基线

## 已落地措施

- 生产环境通过环境变量注入数据库、Redis、JWT 和 CORS 配置；示例文件只保留 `CHANGE_ME` 占位符。
- 生产默认关闭 Swagger/OpenAPI，Actuator 只暴露 `health`、`info`、`metrics`，健康详情不对外返回。
- Nginx 添加 MIME、Frame、Referrer、Permissions 等安全响应头；上传大小限制为 12 MB。
- 简历上传校验扩展名、MIME、文件结构和存储路径；生产上传目录使用独立 volume，不直接映射静态资源。
- SQL 查询统一按当前用户隔离；投递、简历、面试和通知服务在业务层再次校验资源归属。
- Access Token 使用 JWT，Refresh Token 使用 HttpOnly Cookie；生产可通过 `JOBTRACK_COOKIE_SECURE=true` 强制 Secure Cookie。

## 本次交付扫描

- `npm audit --audit-level=high`：0 vulnerabilities。
- Git 跟踪文件密钥模式扫描：未发现真实 Token、私钥或生产密码；命中的内容仅为开发默认值和 `CHANGE_ME` 模板。
- 后端 `mvn test`：89 个测试全部通过，包含真实 MySQL/Redis Testcontainers 集成测试。

生产部署前仍应替换所有占位符、启用 HTTPS、设置 `JOBTRACK_COOKIE_SECURE=true`，并在 CI 或发布平台补充组织级镜像漏洞扫描工具。
