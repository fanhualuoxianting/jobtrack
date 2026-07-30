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

export interface ApplicationInfo {
  id: number
  companyId: number
  companyName: string | null
  positionId: number
  positionTitle: string | null
  resumeId: number | null
  resumeVersionName: string | null
  status: ApplicationStatus
  priority: 'LOW' | 'MEDIUM' | 'HIGH'
  source: string | null
  appliedAt: string | null
  referralName: string | null
  nextAction: string | null
  nextActionAt: string | null
  note: string | null
  archived: boolean
  createdAt: string
  updatedAt: string
  version: number
}

export interface ApplicationTimelineItem {
  id: number
  applicationId: number
  fromStatus: ApplicationStatus | null
  toStatus: ApplicationStatus
  reason: string | null
  operatorUserId: number
  occurredAt: string
  traceId: string | null
}

/** 面试状态 */
export type InterviewStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW'

/** 面试形式 */
export type InterviewType = 'PHONE' | 'VIDEO' | 'ONSITE' | 'WRITTEN' | 'OTHER'

/** 提醒状态 */
export type ReminderStatus = 'PENDING' | 'READY' | 'SENT' | 'FAILED' | 'CANCELLED'

export interface InterviewInfo {
  id: number
  applicationId: number
  roundNumber: number
  roundName: string
  interviewType: InterviewType
  status: InterviewStatus
  scheduledStartAt: string
  scheduledEndAt: string | null
  timezone: string
  location: string | null
  meetingUrl: string | null
  interviewer: string | null
  contactInfo: string | null
  notes: string | null
  result: string | null
  feedback: string | null
  cancelReason: string | null
  createdAt: string | null
  updatedAt: string | null
  version: number
}

export interface ReminderInfo {
  id: number
  interviewId: number | null
  applicationId: number | null
  reminderType: string | null
  status: ReminderStatus
  title: string
  content: string | null
  idempotencyKey: string | null
  scheduledAt: string | null
  sentAt: string | null
  readAt: string | null
  cancelledAt: string | null
  createdAt: string | null
}

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

/** 公司列表项 */
export interface CompanyListItem {
  id: number
  name: string
  shortName: string | null
  industry: string | null
  scale: string | null
  city: string | null
  positionCount: number
  applicationCount: number
  updatedAt: string
}

/** 公司详情 */
export interface CompanyDetail {
  id: number
  name: string
  shortName: string | null
  industry: string | null
  scale: string | null
  city: string | null
  website: string | null
  description: string | null
  createdAt: string
  updatedAt: string
  version: number
}

/** 公司下拉选项 */
export interface CompanyOption {
  id: number
  name: string
}

/** 岗位列表项 */
export interface PositionListItem {
  id: number
  companyId: number
  companyName: string | null
  title: string
  department: string | null
  city: string | null
  workType: WorkType
  workplaceType: WorkplaceType
  salaryMin: number | null
  salaryMax: number | null
  salaryUnit: 'DAY' | 'MONTH' | 'YEAR' | null
  currency: string
  source: string | null
  status: 'OPEN' | 'CLOSED'
  deadlineAt: string | null
  updatedAt: string
}

/** 岗位详情 */
export interface PositionDetail extends PositionListItem {
  sourceUrl: string | null
  description: string | null
  requirements: string | null
  publishedAt: string | null
  createdAt: string
  version: number
}

/** 岗位下拉选项 */
export interface PositionOption {
  id: number
  companyId: number
  title: string
  city: string | null
}

export type WorkType = 'INTERNSHIP' | 'FULL_TIME' | 'PART_TIME'
export type WorkplaceType = 'ONSITE' | 'REMOTE' | 'HYBRID'
