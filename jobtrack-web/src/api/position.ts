import request from './request'
import type { ApiResponse, PageData, PositionDetail, PositionListItem, PositionOption } from '@/types'

/** 岗位查询参数 */
export interface PositionQuery {
  keyword?: string
  companyId?: number
  city?: string
  workType?: string
  workplaceType?: string
  status?: string
  source?: string
  salaryMin?: number
  salaryMax?: number
  deadlineFrom?: string
  deadlineTo?: string
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface PositionPayload {
  companyId: number | null
  title: string
  department?: string | null
  city?: string | null
  workType: string
  workplaceType: string
  salaryMin?: number | null
  salaryMax?: number | null
  salaryUnit?: string | null
  currency?: string
  source?: string | null
  sourceUrl?: string | null
  description?: string | null
  requirements?: string | null
  publishedAt?: string | null
  deadlineAt?: string | null
}

export function pagePositions(params: PositionQuery): Promise<ApiResponse<PageData<PositionListItem>>> {
  return request.get('/positions', { params }) as unknown as Promise<ApiResponse<PageData<PositionListItem>>>
}

export function fetchPositionOptions(companyId?: number): Promise<ApiResponse<PositionOption[]>> {
  return request.get('/positions/options', { params: { companyId } }) as unknown as Promise<ApiResponse<PositionOption[]>>
}

export function fetchPosition(id: number): Promise<ApiResponse<PositionDetail>> {
  return request.get(`/positions/${id}`) as unknown as Promise<ApiResponse<PositionDetail>>
}

export function createPosition(data: PositionPayload): Promise<ApiResponse<PositionDetail>> {
  return request.post('/positions', data) as unknown as Promise<ApiResponse<PositionDetail>>
}

export function updatePosition(id: number, data: PositionPayload): Promise<ApiResponse<PositionDetail>> {
  return request.put(`/positions/${id}`, data) as unknown as Promise<ApiResponse<PositionDetail>>
}

export function changePositionStatus(id: number, status: 'OPEN' | 'CLOSED'): Promise<ApiResponse<PositionDetail>> {
  return request.patch(`/positions/${id}/status`, { status }) as unknown as Promise<ApiResponse<PositionDetail>>
}

export function deletePosition(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/positions/${id}`) as unknown as Promise<ApiResponse<void>>
}
