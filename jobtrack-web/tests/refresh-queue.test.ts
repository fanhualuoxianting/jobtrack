import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import type { useAuthStore as UseAuthStoreType } from '@/stores/auth'

/**
 * Axios 刷新队列测试。
 * 关键验证：
 * 1. 并发 401 只触发一次刷新请求（单例/队列）；
 * 2. 刷新成功后，等待队列中的失败请求携带新令牌统一重放；
 * 3. 登录、注册、刷新接口本身不触发刷新逻辑（防无限循环）；
 * 4. 刷新失败后清空本地认证状态。
 */

interface Captured {
  onRejected: ((error: unknown) => Promise<unknown>) | null
}

const captured: Captured = { onRejected: null }
let refreshCalls = 0
let refreshShouldFail = false
let mockPost: ReturnType<typeof vi.fn>

vi.mock('@/router', () => ({
  default: {
    currentRoute: { value: { path: '/dashboard', fullPath: '/dashboard' } },
    push: vi.fn(),
  },
}))

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn(), warning: vi.fn(), success: vi.fn() },
}))

vi.mock('axios', () => {
  mockPost = vi.fn().mockImplementation(() => {
    refreshCalls++
    if (refreshShouldFail) {
      return Promise.reject({ response: { status: 401, data: { code: 'AUTH_SESSION_REVOKED' } } })
    }
    return Promise.resolve({
      data: {
        code: 'SUCCESS',
        data: {
          accessToken: 'brand-new-token',
          tokenType: 'Bearer',
          expiresIn: 1800,
          user: { id: 1, username: 'demo' },
        },
      },
    })
  })

  const create = vi.fn().mockImplementation(() => {
    const callable = vi.fn().mockResolvedValue({ data: { code: 'SUCCESS', data: {} } })
    return Object.assign(callable, {
      defaults: { baseURL: '/api/v1', headers: {} },
      interceptors: {
        request: { use: vi.fn(), eject: vi.fn() },
        response: {
          use: (_ok: unknown, onRejected: unknown) => {
            captured.onRejected = onRejected as (error: unknown) => Promise<unknown>
            return 0
          },
          eject: vi.fn(),
        },
      },
      post: mockPost,
      get: vi.fn(),
      delete: vi.fn(),
    })
  })

  return { default: { create, post: mockPost } }
})

interface TestContext {
  onRejected: (error: unknown) => Promise<unknown>
  authStore: ReturnType<typeof UseAuthStoreType>
}

function make401(url: string, alreadyRetried = false) {
  return {
    response: { status: 401, data: { code: 'AUTH_TOKEN_EXPIRED', message: 'Access Token 过期' } },
    config: { url, headers: {} as Record<string, string>, ...(alreadyRetried ? { __isRetryAfterRefresh: true } : {}) },
    message: 'expired',
  }
}

describe('Axios 刷新队列', () => {
  let ctx: TestContext

  beforeEach(async () => {
    refreshCalls = 0
    refreshShouldFail = false
    captured.onRejected = null
    vi.resetModules()
    setActivePinia(createPinia())
    await import('@/api/request')
    const storeModule = await import('@/stores/auth')
    if (!captured.onRejected) {
      throw new Error('响应拦截器未捕获到')
    }
    ctx = { onRejected: captured.onRejected, authStore: storeModule.useAuthStore() }
  })

  it('并发 401 只发起一次刷新，并重放全部等待请求', async () => {
    const results = await Promise.all([
      ctx.onRejected(make401('/companies')),
      ctx.onRejected(make401('/applications')),
      ctx.onRejected(make401('/interviews')),
    ])

    expect(refreshCalls).toBe(1)
    expect(ctx.authStore.accessToken).toBe('brand-new-token')
    for (const r of results) {
      expect((r as { data: { code: string } }).data.code).toBe('SUCCESS')
    }
  })

  it('登录、注册、刷新接口的 401 不触发刷新', async () => {
    refreshShouldFail = true
    for (const url of ['/auth/login', '/auth/register', '/auth/refresh']) {
      await expect(ctx.onRejected(make401(url))).rejects.toBeTruthy()
    }
    expect(refreshCalls).toBe(0)
  })

  it('刷新失败后清空本地认证状态', async () => {
    refreshShouldFail = true
    ctx.authStore.setToken('old-token')
    ctx.authStore.setUser({ id: 1 } as never)

    await expect(ctx.onRejected(make401('/companies'))).rejects.toBeTruthy()
    expect(refreshCalls).toBe(1)
    expect(ctx.authStore.accessToken).toBeNull()
    expect(ctx.authStore.user).toBeNull()
  })

  it('重放请求再次 401 时不再触发刷新（防循环）', async () => {
    const first = await ctx.onRejected(make401('/companies'))
    expect((first as { data: { code: string } }).data.code).toBe('SUCCESS')

    await expect(ctx.onRejected(make401('/companies', true))).rejects.toBeTruthy()
    expect(refreshCalls).toBe(1)
    expect(ctx.authStore.accessToken).toBe('brand-new-token')
  })
})
