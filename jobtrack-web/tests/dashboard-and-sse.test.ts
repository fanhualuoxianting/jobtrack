import { describe, expect, it } from 'vitest'
import { formatDashboardRate } from '@/utils/dashboard'
import { parseNotificationSseBlock } from '@/utils/notification-sse'

describe('Dashboard and SSE presentation helpers', () => {
  it('zero or invalid rates never render NaN', () => {
    expect(formatDashboardRate(0)).toBe('0.00%')
    expect(formatDashboardRate(null)).toBe('0.00%')
    expect(formatDashboardRate(Number.NaN)).toBe('0.00%')
  })

  it('formats a finite dashboard rate', () => {
    expect(formatDashboardRate(37.5)).toBe('37.50%')
  })

  it('parses notification SSE data and ignores heartbeats', () => {
    expect(parseNotificationSseBlock('event:notification\ndata:{"title":"面试提醒"}\n\n')).toEqual({ title: '面试提醒' })
    expect(parseNotificationSseBlock(':heartbeat\n\n')).toBeNull()
  })

  it('ignores malformed SSE payloads', () => {
    expect(parseNotificationSseBlock('data:{broken-json}\n\n')).toBeNull()
  })
})
