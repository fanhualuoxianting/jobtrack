/** 统一 API 响应结构 */
export interface ApiResponse<T = unknown> {
  code: string
  message: string
  data: T
  traceId: string
  timestamp: string
}

/** 分页响应 */
export interface PageData<T = unknown> {
  records: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

/** 投递状态枚举 */
export type ApplicationStatus =
  | 'SAVED'
  | 'APPLIED'
  | 'ASSESSMENT'
  | 'INTERVIEWING'
  | 'OFFERED'
  | 'ACCEPTED'
  | 'REJECTED'
  | 'WITHDRAWN'
  | 'CLOSED'

/** 面试状态 */
export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'

/** 面试形式 */
export type InterviewType = 'PHONE' | 'ONLINE' | 'ONSITE' | 'WRITTEN' | 'OTHER'

/** 提醒状态 */
export type ReminderStatus = 'PENDING' | 'SENT' | 'FAILED' | 'CANCELLED'

/** 用户信息 */
export interface UserInfo {
  id: number
  username: string
  email: string
  nickname: string
  avatarUrl: string | null
  role: 'USER' | 'ADMIN'
  lastLoginAt: string | null
}

/** 登录/刷新令牌响应数据 */
export interface AuthTokenData {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

/** 注册响应数据 */
export interface RegisterResult {
  userId: number
  username: string
  email: string
  nickname: string
}

/** 登录会话信息 */
export interface SessionInfo {
  sessionId: string
  deviceName: string | null
  userAgent: string | null
  ipAddress: string | null
  createdAt: string
  lastActiveAt: string
  expiresAt: string
  current: boolean
}
