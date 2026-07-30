import request from './request'
import type { ApiResponse, ApplicationInfo, ApplicationStatus, ApplicationTimelineItem, PageData } from '@/types'

export interface ApplicationQuery {
  keyword?: string
  companyId?: number
  positionId?: number
  status?: ApplicationStatus
  source?: string
  archived?: boolean
  page: number
  pageSize: number
  sortBy?: 'updatedAt' | 'createdAt' | 'appliedAt' | 'nextActionAt' | 'status'
  sortOrder?: 'asc' | 'desc'
}

export interface ApplicationCreatePayload {
  companyId: number
  positionId: number
  resumeId?: number | null
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  source?: string | null
  appliedAt?: string | null
  referralName?: string | null
  expectedSalaryMin?: number | null
  expectedSalaryMax?: number | null
  currency?: string
  nextAction?: string | null
  nextActionAt?: string | null
  note?: string | null
}

export type ApplicationUpdatePayload = Omit<ApplicationCreatePayload, 'companyId' | 'positionId' | 'priority'> & { version: number }

export function pageApplications(params: ApplicationQuery): Promise<ApiResponse<PageData<ApplicationInfo>>> {
  return request.get('/applications', { params }) as unknown as Promise<ApiResponse<PageData<ApplicationInfo>>>
}
export function fetchApplication(id: number): Promise<ApiResponse<ApplicationInfo>> {
  return request.get(`/applications/${id}`) as unknown as Promise<ApiResponse<ApplicationInfo>>
}
export function createApplication(data: ApplicationCreatePayload): Promise<ApiResponse<ApplicationInfo>> {
  return request.post('/applications', data) as unknown as Promise<ApiResponse<ApplicationInfo>>
}
export function updateApplication(id: number, data: ApplicationUpdatePayload): Promise<ApiResponse<ApplicationInfo>> {
  return request.patch(`/applications/${id}`, data) as unknown as Promise<ApiResponse<ApplicationInfo>>
}
export function allowedApplicationTransitions(id: number): Promise<ApiResponse<ApplicationStatus[]>> {
  return request.get(`/applications/${id}/allowed-transitions`) as unknown as Promise<ApiResponse<ApplicationStatus[]>>
}
export function transitionApplication(id: number, targetStatus: ApplicationStatus, reason: string, expectedVersion: number, idempotencyKey: string): Promise<ApiResponse<ApplicationInfo>> {
  return request.post(`/applications/${id}/transitions`, { targetStatus, reason: reason || undefined, expectedVersion, idempotencyKey }) as unknown as Promise<ApiResponse<ApplicationInfo>>
}
export function fetchApplicationTimeline(id: number): Promise<ApiResponse<ApplicationTimelineItem[]>> {
  return request.get(`/applications/${id}/timeline`) as unknown as Promise<ApiResponse<ApplicationTimelineItem[]>>
}
export function archiveApplication(id: number): Promise<ApiResponse<ApplicationInfo>> {
  return request.post(`/applications/${id}/archive`) as unknown as Promise<ApiResponse<ApplicationInfo>>
}
export function restoreApplication(id: number): Promise<ApiResponse<ApplicationInfo>> {
  return request.post(`/applications/${id}/restore`) as unknown as Promise<ApiResponse<ApplicationInfo>>
}
export function deleteApplication(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/applications/${id}`) as unknown as Promise<ApiResponse<void>>
}
