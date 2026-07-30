# 认证与安全会话

- 密码使用 BCrypt 10；登录支持用户名或邮箱，错误消息不区分账号不存在和密码错误。
- Access Token 使用 HS256 JWT，默认 30 分钟，仅保存在前端内存。
- Refresh Token 使用 `HttpOnly + SameSite=Strict` Cookie，后端只保存 SHA-256 哈希；每次刷新执行原子 Rotation，旧令牌重放会撤销整个会话并记录审计。
- 登录失败默认 10 分钟内 5 次触发 15 分钟锁定；Redis 可用时使用 Redis，故障时降级到内存计数。
- 会话撤销数据库为事实源，Redis 只做快速撤销标记；Redis 故障可以回源数据库。
- 生产必须注入至少 32 字节 `JWT_SECRET`，生产 Cookie 默认 `Secure=true`，Swagger 默认关闭。

前端 Axios 使用单例刷新队列：并发 401 共享一次 Refresh，失败清理 Pinia 会话并跳转登录。测试见 `jobtrack-web/tests/refresh-queue.test.ts`。
