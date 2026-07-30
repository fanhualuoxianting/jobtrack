import axios, { AxiosError } from 'axios'
import type { AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

/**
 * Axios 单例封装。
 * - 请求携带内存中的 Access Token 与 X-Request-ID；
 * - 401 时单例刷新：并发请求共享一次刷新调用，其余进入等待队列统一重放；
 * - 刷新失败清空认证状态并跳转登录，且不会无限重试。
 */

interface RetryableConfig extends InternalAxiosRequestConfig {
  __isRetryAfterRefresh?: boolean
}

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 15000,
  // 携带 HttpOnly Refresh Cookie（本地经 vite proxy 同源）
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

/** 登录、注册、刷新接口自身不参与刷新重放，防止无限循环 */
function isAuthPublicUrl(url?: string): boolean {
  if (!url) return false
  return ['/auth/login', '/auth/register', '/auth/refresh'].some((p) => url.includes(p))
}

const getAuthStore = () => useAuthStore()

// ---------- 请求拦截 ----------
request.interceptors.request.use((config) => {
  const token = getAuthStore().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Request-ID'] = crypto.randomUUID().replace(/-/g, '').slice(0, 16)
  return config
})

// ---------- 刷新队列 ----------
let refreshingPromise: Promise<string> | null = null

async function doRefresh(): Promise<string> {
  const authStore = getAuthStore()
  try {
    // 裸 axios 调用，避免经过本实例的响应拦截器再次触发刷新逻辑
    const resp = await axios.post(
      `${request.defaults.baseURL}/auth/refresh`,
      null,
      { timeout: 15000, withCredentials: true }
    )
    const newToken: string = resp.data?.data?.accessToken
    if (!newToken) {
      throw new Error('刷新响应缺少令牌')
    }
    authStore.setToken(newToken)
    authStore.setUser(resp.data.data.user)
    return newToken
  } catch (e) {
    authStore.forceLogout()
    const current = router.currentRoute.value
    if (current.path !== '/login') {
      router.push({ path: '/login', query: { redirect: current.fullPath } })
    }
    throw e
  }
}

function refreshTokenSingleFlight(): Promise<string> {
  if (!refreshingPromise) {
    refreshingPromise = doRefresh().finally(() => {
      refreshingPromise = null
    })
  }
  return refreshingPromise
}

// ---------- 响应拦截 ----------
interface ErrorBody {
  code?: string
  message?: string
}

request.interceptors.response.use(
  (response: AxiosResponse) => response.data as unknown as AxiosResponse,
  async (error: AxiosError<ErrorBody>) => {
    const resp = error.response
    const config = error.config as RetryableConfig | undefined
    const message = resp?.data?.message || '网络错误，请稍后重试'

    // 401 且可重放：排队等待唯一的一次刷新
    const canReplay =
      resp?.status === 401 && config && !config.__isRetryAfterRefresh && !isAuthPublicUrl(config.url)
    if (canReplay && config) {
      config.__isRetryAfterRefresh = true
      try {
        const token = await refreshTokenSingleFlight()
        config.headers.Authorization = `Bearer ${token}`
        return request(config)
      } catch {
        return Promise.reject(error)
      }
    }

    // 其余错误统一提示（登录页的凭证错误由页面自行展示，不弹全局提示）
    if (!isAuthPublicUrl(config?.url)) {
      const suppressed = [401, 403].includes(resp?.status ?? 0)
      if (!suppressed) {
        ElMessage.error(message)
      } else if (resp?.status === 403) {
        ElMessage.warning(message)
      }
    }
    return Promise.reject(error)
  }
)

export default request
