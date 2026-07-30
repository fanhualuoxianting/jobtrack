import request from './request'
import type { ApiResponse } from '@/types'

/** 简历版本 */
export interface ResumeInfo {
  id: number
  versionName: string
  originalFileName: string
  mimeType: string
  fileSize: number
  sha256: string
  isDefault: boolean
  note: string | null
  uploadedAt: string
  version: number
}

export interface ResumeUpdatePayload {
  versionName: string
  note?: string | null
}

export function fetchResumes(): Promise<ApiResponse<ResumeInfo[]>> {
  return request.get('/resumes') as unknown as Promise<ApiResponse<ResumeInfo[]>>
}

export function fetchResume(id: number): Promise<ApiResponse<ResumeInfo>> {
  return request.get(`/resumes/${id}`) as unknown as Promise<ApiResponse<ResumeInfo>>
}

/** 上传简历（multipart） */
export function uploadResume(file: File, versionName?: string, note?: string): Promise<ApiResponse<ResumeInfo>> {
  const formData = new FormData()
  formData.append('file', file)
  if (versionName?.trim()) formData.append('versionName', versionName.trim())
  if (note?.trim()) formData.append('note', note.trim())
  return request.post('/resumes', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }) as unknown as Promise<ApiResponse<ResumeInfo>>
}

export function updateResume(id: number, data: ResumeUpdatePayload): Promise<ApiResponse<ResumeInfo>> {
  return request.patch(`/resumes/${id}`, data) as unknown as Promise<ApiResponse<ResumeInfo>>
}

export function setDefaultResume(id: number): Promise<ApiResponse<ResumeInfo>> {
  return request.put(`/resumes/${id}/default`) as unknown as Promise<ApiResponse<ResumeInfo>>
}

export function deleteResume(id: number): Promise<ApiResponse<void>> {
  return request.delete(`/resumes/${id}`) as unknown as Promise<ApiResponse<void>>
}

/** 下载简历：走浏览器原生导航，由后端写 Content-Disposition（Authorization 无法经导航携带，改用 blob） */
export async function downloadResume(id: number, token: string): Promise<void> {
  const resp = await fetch(`${request.defaults.baseURL}/resumes/${id}/download`, {
    headers: { Authorization: `Bearer ${token}` },
    credentials: 'include',
  })
  if (!resp.ok) {
    const body = await resp.json().catch(() => null)
    throw new Error(body?.message || '下载失败')
  }
  const disposition = resp.headers.get('Content-Disposition') || ''
  const star = /filename\*=UTF-8''([^;]+)/.exec(disposition)
  const fallback = /filename="([^"]+)"/.exec(disposition)
  const filename = star ? decodeURIComponent(star[1]) : (fallback?.[1] ?? `resume-${id}`)
  const blob = await resp.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}
