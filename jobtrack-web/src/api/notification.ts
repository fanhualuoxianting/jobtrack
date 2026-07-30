import request from './request'
import type { ApiResponse, PageData, ReminderInfo } from '@/types'

export function pageNotifications(params: { readState?: 'READ' | 'UNREAD'; page: number; pageSize: number }): Promise<ApiResponse<PageData<ReminderInfo>>> {
  return request.get('/notifications', { params }) as unknown as Promise<ApiResponse<PageData<ReminderInfo>>>
}
export function fetchUnreadCount(): Promise<ApiResponse<number>> {
  return request.get('/notifications/unread-count') as unknown as Promise<ApiResponse<number>>
}
export function markNotificationRead(id: number): Promise<ApiResponse<ReminderInfo>> {
  return request.patch(`/notifications/${id}/read`) as unknown as Promise<ApiResponse<ReminderInfo>>
}
export function markAllNotificationsRead(): Promise<ApiResponse<void>> {
  return request.post('/notifications/read-all') as unknown as Promise<ApiResponse<void>>
}
