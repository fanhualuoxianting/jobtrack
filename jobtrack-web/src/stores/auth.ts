import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserInfo | null>(null)
  const accessToken = ref<string | null>(null)

  const isAuthenticated = computed(() => !!accessToken.value)

  function setToken(token: string) {
    accessToken.value = token
  }

  function setUser(userInfo: UserInfo) {
    user.value = userInfo
  }

  function logout() {
    user.value = null
    accessToken.value = null
  }

  return { user, accessToken, isAuthenticated, setToken, setUser, logout }
})
