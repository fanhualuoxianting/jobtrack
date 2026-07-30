import request from './request'
import type { ApiResponse, ReminderInfo } from '@/types'

export interface DashboardQuery {
  startDate?: string
  endDate?: string
  timezone?: string
  companyId?: number
  source?: string
  includeArchived?: boolean
  granularity?: 'DAY' | 'WEEK' | 'MONTH'
}

export interface DashboardSummary {
  totalApplications: number
  inProgressApplications: number
  pendingTasks: number
  upcomingInterviews: number
  offerApplications: number
  acceptedOffers: number
  advancedApplications: number
  interviewApplications: number
  offeredApplications: number
  interviewConversionRate: number
  offerConversionRate: number
  overdueReminders: number
}

export interface DashboardFunnelItem { stage: string; label: string; count: number }
export interface DashboardTrendItem { bucket: string; count: number }
export interface DashboardBreakdownItem { name: string; count: number }
export interface DashboardCycleItem { sampleCount: number; averageHours: number; minHours: number; maxHours: number }
export interface DashboardCycleResult { appliedToInterviewing: DashboardCycleItem; appliedToOffered: DashboardCycleItem; appliedToTerminal: DashboardCycleItem }
export interface DashboardUpcoming { interviewId: number; applicationId: number; companyName: string; positionTitle: string; roundName: string; scheduledStartAt: string; timezone: string }

function queryParams(params: DashboardQuery): { params: DashboardQuery } { return { params } }
export function fetchDashboardSummary(params: DashboardQuery): Promise<ApiResponse<DashboardSummary>> { return request.get('/dashboard/summary', queryParams(params)) as unknown as Promise<ApiResponse<DashboardSummary>> }
export function fetchDashboardFunnel(params: DashboardQuery): Promise<ApiResponse<DashboardFunnelItem[]>> { return request.get('/dashboard/funnel', queryParams(params)) as unknown as Promise<ApiResponse<DashboardFunnelItem[]>> }
export function fetchDashboardTrends(params: DashboardQuery): Promise<ApiResponse<DashboardTrendItem[]>> { return request.get('/dashboard/trends', queryParams(params)) as unknown as Promise<ApiResponse<DashboardTrendItem[]>> }
export function fetchDashboardSources(params: DashboardQuery): Promise<ApiResponse<DashboardBreakdownItem[]>> { return request.get('/dashboard/sources', queryParams(params)) as unknown as Promise<ApiResponse<DashboardBreakdownItem[]>> }
export function fetchDashboardIndustries(params: DashboardQuery): Promise<ApiResponse<DashboardBreakdownItem[]>> { return request.get('/dashboard/industries', queryParams(params)) as unknown as Promise<ApiResponse<DashboardBreakdownItem[]>> }
export function fetchDashboardCycleTime(params: DashboardQuery): Promise<ApiResponse<DashboardCycleResult>> { return request.get('/dashboard/cycle-time', queryParams(params)) as unknown as Promise<ApiResponse<DashboardCycleResult>> }
export function fetchDashboardUpcoming(params: DashboardQuery): Promise<ApiResponse<DashboardUpcoming[]>> { return request.get('/dashboard/upcoming', queryParams(params)) as unknown as Promise<ApiResponse<DashboardUpcoming[]>> }
export function fetchRecentUnreadNotifications(): Promise<ApiResponse<{ records: ReminderInfo[] }>> { return request.get('/notifications', { params: { readState: 'UNREAD', page: 1, pageSize: 5 } }) as unknown as Promise<ApiResponse<{ records: ReminderInfo[] }>> }
