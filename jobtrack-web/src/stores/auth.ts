import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { AxiosError } from 'axios'
import type { ApiResponse, AuthTokenData, UserInfo } from '@/types'
import { login as loginApi, logout as logoutApi, logoutAll as logoutAllApi } from '@/api/auth'
import request from '@/api/request'

/**
 * 认证状态。
 * Access Token 只保存在内存（Pinia）中；Refresh Token 完全由 HttpOnly Cookie 承载，
 * 前端 JS 不读取、不落盘。页面刷新后通过 restoreSession 尝试恢复会话。
 */
export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const accessToken = ref<string | null>(null)
  /** 是否已尝试过一次会话恢复（路由守卫用于避免重复请求） */
  const sessionRestored = ref(false)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setToken(token: string | null) {
    accessToken.value = token
  }

  function setUser(userInfo: UserInfo | null) {
    user.value = userInfo
  }

  /** 登录：成功写入内存令牌与用户信息 */
  async function login(payload: { account: string; password: string; deviceName?: string }) {
    const res = await loginApi(payload)
    accessToken.value = res.data.accessToken
    user.value = res.data.user
    sessionRestored.value = true
  }

  /** 正常登出：无论接口成败都清空本地状态 */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      forceLogout()
    }
  }

  /** 注销全部设备（可选保留当前）后清空本地状态 */
  async function logoutAll(keepCurrent: boolean) {
    await logoutAllApi(keepCurrent)
    if (!keepCurrent) {
      forceLogout()
    }
  }

  function forceLogout() {
    user.value = null
    accessToken.value = null
    sessionRestored.value = true
  }

  /**
   * 恢复会话：浏览器刷新后调用一次。
   * 后端 Refresh Cookie 有效则换到新令牌；失败保持未登录态。
   */
  async function restoreSession(): Promise<boolean> {
    if (sessionRestored.value) {
      return isAuthenticated.value
    }
    sessionRestored.value = true
    try {
      const res = (await request.post('/auth/refresh')) as unknown as ApiResponse<AuthTokenData>
      accessToken.value = res.data.accessToken
      user.value = res.data.user
      return true
    } catch (e) {
      const status = (e as AxiosError).response?.status
      // 401 表示没有有效会话，属正常路径；其他错误打印供排查
      if (status !== 401) {
        console.warn('会话恢复失败', e)
      }
      return false
    }
  }

  return {
    user,
    accessToken,
    sessionRestored,
    isAuthenticated,
    setToken,
    setUser,
    login,
    logout,
    logoutAll,
    forceLogout,
    restoreSession,
  }
})
