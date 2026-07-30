<template>
  <el-container class="main-layout">
    <el-header class="header" height="56px">
      <div class="header-left">
        <span class="logo">JobTrack</span>
        <el-menu
          :default-active="activeMenu"
          mode="horizontal"
          :ellipsis="false"
          router
          class="nav-menu"
        >
          <el-menu-item index="/dashboard">首页</el-menu-item>
          <el-menu-item index="/companies">公司</el-menu-item>
          <el-menu-item index="/positions">岗位</el-menu-item>
          <el-menu-item index="/resumes">简历</el-menu-item>
        <el-menu-item index="/applications">投递</el-menu-item>
        <el-menu-item index="/interviews">面试</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
          <el-button circle :icon="Bell" aria-label="通知" @click="openNotifications" />
        </el-badge>
        <el-dropdown @command="handleCommand">
          <span class="user-entry">
            <el-icon class="avatar"><UserFilled /></el-icon>
            {{ authStore.user?.nickname || authStore.user?.username }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>{{ authStore.user?.email }}</el-dropdown-item>
              <el-dropdown-item command="security" divided>安全设置</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="content">
      <router-view />
    </el-main>
    <el-drawer v-model="notificationVisible" title="通知中心" size="390px" @open="loadNotifications">
      <div class="notification-tools"><span :class="['sse-state', { connected: sseConnected }]">{{ sseConnected ? '实时连接中' : '连接重试中' }}</span><el-button link type="primary" :disabled="unreadCount === 0" @click="readAll">全部已读</el-button></div>
      <el-alert v-if="notificationFailed" title="通知加载失败，可稍后重试" type="error" :closable="false" show-icon class="drawer-alert" />
      <el-empty v-if="notifications.length === 0 && !notificationLoading" description="暂无通知" />
      <div v-loading="notificationLoading" class="notification-list">
        <div v-for="item in notifications" :key="item.id" :class="['notification-item', { unread: !item.readAt }]" @click="readOne(item.id)">
          <div class="notification-title">{{ item.title }}</div><div class="notification-content">{{ item.content || '-' }}</div><small>{{ formatDateTime(item.sentAt || item.createdAt) }}</small>
        </div>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Bell, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchUnreadCount, markAllNotificationsRead, markNotificationRead, pageNotifications } from '@/api/notification'
import type { ReminderInfo } from '@/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => (route.path.startsWith('/settings') ? '' : route.path))
const notificationVisible = ref(false); const notificationLoading = ref(false); const notificationFailed = ref(false); const notifications = ref<ReminderInfo[]>([]); const unreadCount = ref(0); const sseConnected = ref(false)
let sseAbort: AbortController | null = null; let reconnectTimer: number | null = null

async function loadUnread() { try { unreadCount.value = (await fetchUnreadCount()).data } catch { /* 认证拦截器负责提示 */ } }
async function loadNotifications() { notificationLoading.value = true; notificationFailed.value = false; try { notifications.value = (await pageNotifications({ page: 1, pageSize: 50 })).data.records; await loadUnread() } catch { notificationFailed.value = true } finally { notificationLoading.value = false } }
function openNotifications() { notificationVisible.value = true; void loadNotifications() }
async function readOne(id: number) { try { await markNotificationRead(id); const item = notifications.value.find((entry) => entry.id === id); if (item && !item.readAt) { item.readAt = new Date().toISOString(); unreadCount.value = Math.max(0, unreadCount.value - 1) } } catch { /* request interceptor already explains the error */ } }
async function readAll() { try { await markAllNotificationsRead(); notifications.value.forEach((item) => { item.readAt ||= new Date().toISOString() }); unreadCount.value = 0 } catch { /* request interceptor already explains the error */ } }
function scheduleSseReconnect() { if (reconnectTimer == null) reconnectTimer = window.setTimeout(() => { reconnectTimer = null; void startSse() }, 5000) }
async function startSse() {
  if (!authStore.accessToken || sseAbort) return
  sseAbort = new AbortController(); sseConnected.value = false
  try {
    const base = String(import.meta.env.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '')
    const response = await fetch(`${base}/notifications/stream`, { headers: { Authorization: `Bearer ${authStore.accessToken}` }, signal: sseAbort.signal })
    if (!response.ok || !response.body) throw new Error(`SSE ${response.status}`)
    sseConnected.value = true
    const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = ''
    while (true) { const chunk = await reader.read(); if (chunk.done) break; buffer += decoder.decode(chunk.value, { stream: true }); const blocks = buffer.split('\n\n'); buffer = blocks.pop() || ''; blocks.forEach(handleSseBlock) }
  } catch (error) { if (!(error instanceof DOMException && error.name === 'AbortError')) scheduleSseReconnect() } finally { sseConnected.value = false; sseAbort = null; if (authStore.accessToken) scheduleSseReconnect() }
}
function handleSseBlock(block: string) { const data = block.split('\n').filter((line) => line.startsWith('data:')).map((line) => line.slice(5).trim()).join('\n'); if (!data) return; try { const event = JSON.parse(data) as { title?: string }; if (event.title) { ElMessage.info(`新通知：${event.title}`); void loadUnread(); if (notificationVisible.value) void loadNotifications() } } catch { /* heartbeat or malformed event is ignored */ } }
function formatDateTime(value: string | null): string { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-' }
function stopSse() { if (reconnectTimer != null) { window.clearTimeout(reconnectTimer); reconnectTimer = null } sseAbort?.abort(); sseAbort = null; sseConnected.value = false }

async function handleCommand(command: string) {
  if (command === 'security') {
    router.push('/settings/security')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出当前设备登录吗？', '退出登录', {
        confirmButtonText: '退出',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      return
    }
    await authStore.logout()
    router.push('/login')
  }
}

watch(() => authStore.isAuthenticated, (value) => { if (value) { void loadUnread(); void startSse() } else stopSse() })
onMounted(() => { if (authStore.isAuthenticated) { void loadUnread(); void startSse() } })
onBeforeUnmount(stopSse)
</script>

<style scoped>
.main-layout {
  min-height: 100vh;
  background: #f5f7fa;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}
.logo {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
}
.nav-menu {
  border-bottom: none;
}
.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
}
.avatar {
  font-size: 18px;
}
.content {
  padding: 20px 24px;
}
.notification-badge { margin-right: 18px; }.notification-tools { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }.sse-state { color: #e6a23c; font-size: 12px; }.sse-state.connected { color: #67c23a; }.drawer-alert { margin-bottom: 12px; }.notification-item { padding: 12px 8px; border-bottom: 1px solid #ebeef5; cursor: pointer; }.notification-item.unread { background: #f0f9ff; }.notification-title { font-weight: 600; }.notification-content { margin: 6px 0; color: #606266; white-space: pre-wrap; }.notification-item small { color: #909399; }
</style>
