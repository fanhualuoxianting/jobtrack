# 面试、提醒与通知

创建面试时校验投递归属、非归档 / 非终态、时区、时间顺序和同一投递轮次唯一性；首场面试自动将投递推进到 `INTERVIEWING`。面试修改使用乐观锁，并取消旧的未发送提醒后重建。

提醒状态含义：`PENDING` 待扫描、`READY` 原子抢占中、`SENT` 通知事实已落库、`FAILED` 达到最大重试次数、`CANCELLED` 业务取消后不再处理。抢占 SQL 额外要求 `scheduled_at <= UTC_TIMESTAMP()`，Redis 不参与去重事实。

通知先写数据库，再尝试 SSE；SSE 断开不影响 `SENT` 事实，客户端可通过通知列表补拉。每用户最多 3 条 SSE 连接，Nginx 对 `/api/v1/notifications/stream` 关闭缓冲并设置读取超时。

完整的并发、降级与验收口径见 [工程证据与验收口径](engineering-evidence.md)。
