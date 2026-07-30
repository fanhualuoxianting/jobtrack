import request from './request'
import type { ApiResponse, AuthTokenData, RegisterResult, SessionInfo, UserInfo } from '@/types'

/** 认证相关 API */
export interface RegisterPayload {
  username: string
  email: string
  password: string
  confirmPassword: string
  nickname?: string
}

export interface LoginPayload {
  account: string
  password: string
  deviceName?: string
}

export function register(data: RegisterPayload): Promise<ApiResponse<RegisterResult>> {
  return request.post('/auth/register', data) as unknown as Promise<ApiResponse<RegisterResult>>
}

export function login(data: LoginPayload): Promise<ApiResponse<AuthTokenData>> {
  return request.post('/auth/login', data) as unknown as Promise<ApiResponse<AuthTokenData>>
}

export function logout(): Promise<ApiResponse<void>> {
  return request.post('/auth/logout') as unknown as Promise<ApiResponse<void>>
}

export function logoutAll(keepCurrent: boolean): Promise<ApiResponse<void>> {
  return request.post('/auth/logout-all', { keepCurrent }) as unknown as Promise<ApiResponse<void>>
}

export function fetchMe(): Promise<ApiResponse<UserInfo>> {
  return request.get('/auth/me') as unknown as Promise<ApiResponse<UserInfo>>
}

export function fetchSessions(): Promise<ApiResponse<SessionInfo[]>> {
  return request.get('/auth/sessions') as unknown as Promise<ApiResponse<SessionInfo[]>>
}

export function revokeSession(sessionId: string): Promise<ApiResponse<void>> {
  return request.delete(`/auth/sessions/${sessionId}`) as unknown as Promise<ApiResponse<void>>
}
