import request from './request'
import type { ApiResponse, InterviewInfo, InterviewStatus, InterviewType, PageData } from '@/types'

export interface InterviewQuery {
  status?: InterviewStatus
  companyId?: number
  applicationId?: number
  from?: string
  to?: string
  page: number
  pageSize: number
}

export interface InterviewCreatePayload {
  applicationId: number
  roundNumber: number
  roundName: string
  interviewType: InterviewType
  scheduledStartAt: string
  scheduledEndAt: string
  timezone: string
  location?: string | null
  meetingUrl?: string | null
  interviewer?: string | null
  contactInfo?: string | null
  notes?: string | null
}

export interface InterviewUpdatePayload {
  version: number
  interviewType?: InterviewType
  scheduledStartAt?: string
  scheduledEndAt?: string
  timezone?: string
  location?: string | null
  meetingUrl?: string | null
  interviewer?: string | null
  contactInfo?: string | null
  notes?: string | null
}

export function pageInterviews(params: InterviewQuery): Promise<ApiResponse<PageData<InterviewInfo>>> {
  return request.get('/interviews', { params }) as unknown as Promise<ApiResponse<PageData<InterviewInfo>>>
}
export function fetchInterview(id: number): Promise<ApiResponse<InterviewInfo>> {
  return request.get(`/interviews/${id}`) as unknown as Promise<ApiResponse<InterviewInfo>>
}
export function createInterview(data: InterviewCreatePayload): Promise<ApiResponse<InterviewInfo>> {
  return request.post('/interviews', data) as unknown as Promise<ApiResponse<InterviewInfo>>
}
export function updateInterview(id: number, data: InterviewUpdatePayload): Promise<ApiResponse<InterviewInfo>> {
  return request.patch(`/interviews/${id}`, data) as unknown as Promise<ApiResponse<InterviewInfo>>
}
export function cancelInterview(id: number, version: number, reason: string): Promise<ApiResponse<InterviewInfo>> {
  return request.post(`/interviews/${id}/cancel`, { version, reason }) as unknown as Promise<ApiResponse<InterviewInfo>>
}
export function completeInterview(id: number, version: number, result: string, feedback: string): Promise<ApiResponse<InterviewInfo>> {
  return request.post(`/interviews/${id}/complete`, { version, result, feedback }) as unknown as Promise<ApiResponse<InterviewInfo>>
}
export function deleteInterview(id: number, version: number): Promise<ApiResponse<void>> {
  return request.delete(`/interviews/${id}`, { params: { version } }) as unknown as Promise<ApiResponse<void>>
}
