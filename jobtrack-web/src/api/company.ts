import request from './request'
import type { ApiResponse, CompanyDetail, CompanyListItem, CompanyOption, PageData } from '@/types'

/** 公司查询参数 */
export interface CompanyQuery {
  keyword?: string
  city?: string
  industry?: string
  page: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface CompanyPayload {
  name: string
  shortName?: string | null
  industry?: string | null
  scale?: string | null
  city?: string | null
  website?: string | null
  description?: string | null
}

export function pageCompanies(params: CompanyQuery): Promise<ApiResponse<PageData<CompanyListItem>>> {
  return request.get('/companies', { params }) as unknown as Promise<ApiResponse<PageData<CompanyListItem>>>
}

export function fetchCompanyOptions(): Promise<ApiResponse<CompanyOption[]>> {
  return request.get('/companies/options') as unknown as Promise<ApiResponse<CompanyOption[]>>
}

export function fetchCompany(id: number): Promise<ApiResponse<CompanyDetail>> {
  return request.get(`/companies/${id}`) as unknown as Promise<ApiResponse<CompanyDetail>>
}

export function createCompany(data: CompanyPayload): Promise<ApiResponse<CompanyDetail>> {
  return request.post('/companies', data) as unknown as Promise<ApiResponse<CompanyDetail>>
}

export function updateCompany(id: number, data: CompanyPayload): Promise<ApiResponse<CompanyDetail>> {
  return request.put(`/companies/${id}`, data) as unknown as Promise<ApiResponse<CompanyDetail>>
}

export function deleteCompany(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/companies/${id}`) as unknown as Promise<ApiResponse<void>>
}
