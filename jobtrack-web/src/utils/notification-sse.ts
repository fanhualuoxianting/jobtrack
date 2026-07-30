export interface NotificationSsePayload {
  title?: string
  [key: string]: unknown
}

/** 从一个 SSE data block 提取通知 JSON；心跳或异常内容返回 null。 */
export function parseNotificationSseBlock(block: string): NotificationSsePayload | null {
  const data = block.split('\n').filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trim()).join('\n')
  if (!data) return null
  try {
    return JSON.parse(data) as NotificationSsePayload
  } catch {
    return null
  }
}
