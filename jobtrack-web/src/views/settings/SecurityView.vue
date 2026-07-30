<template>
  <div class="security-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">登录设备管理</span>
          <el-button
            type="danger"
            plain
            size="small"
            :loading="revokingOthers"
            :disabled="otherSessionCount === 0"
            @click="confirmLogoutOthers"
          >
            注销其他全部设备
          </el-button>
        </div>
      </template>

      <el-alert type="info" :closable="false" class="tip">
        <template #title>
          同一账号可在多台设备登录。设备失窃或不再使用时请及时注销对应会话；
          如果怀疑令牌泄露，还可点击下方操作注销全部设备（包括本机）。
        </template>
      </el-alert>

      <el-table :data="sessions" v-loading="loading" :empty-text="emptyText" class="session-table">
        <el-table-column label="设备" min-width="160">
          <template #default="{ row }">
            <div class="device-cell">
              <span>{{ row.deviceName || '未知设备' }}</span>
              <el-tag v-if="row.current" size="small" type="success" class="current-tag">本设备</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="IP 地址" width="130">
          <template #default="{ row }">{{ row.ipAddress || '-' }}</template>
        </el-table-column>
        <el-table-column label="登录时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="最近活跃" width="170">
          <template #default="{ row }">{{ formatTime(row.lastActiveAt) }}</template>
        </el-table-column>
        <el-table-column label="会话过期" width="170">
          <template #default="{ row }">{{ formatTime(row.expiresAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="!row.current"
              type="danger"
              link
              :loading="revokingId === row.sessionId"
              @click="confirmRevoke(row)"
            >
              注销
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="danger-zone">
        <el-button type="danger" text bg :loading="loggingOutAll" @click="confirmLogoutAll">
          注销全部设备（包括本机）
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { fetchSessions, logoutAll, revokeSession } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import type { SessionInfo } from '@/types'

const router = useRouter()
const authStore = useAuthStore()

const sessions = ref<SessionInfo[]>([])
const loading = ref(false)
const loadFailed = ref(false)
const revokingId = ref<string | null>(null)
const revokingOthers = ref(false)
const loggingOutAll = ref(false)

const otherSessionCount = computed(() => sessions.value.filter((s) => !s.current).length)
const emptyText = computed(() => (loadFailed.value ? '加载失败，请稍后重试' : '暂无有效会话'))

async function loadSessions() {
  loading.value = true
  loadFailed.value = false
  try {
    const res = await fetchSessions()
    sessions.value = res.data
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function confirmRevoke(row: SessionInfo) {
  try {
    await ElMessageBox.confirm(`确定注销设备「${row.deviceName || '未知设备'}」吗？`, '注销设备', {
      confirmButtonText: '注销',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  revokingId.value = row.sessionId
  try {
    await revokeSession(row.sessionId)
    ElMessage.success('该设备已注销')
    await loadSessions()
  } finally {
    revokingId.value = null
  }
}

async function confirmLogoutOthers() {
  try {
    await ElMessageBox.confirm('将注销除本设备外的全部登录会话，确定继续吗？', '注销其他设备', {
      confirmButtonText: '注销',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  revokingOthers.value = true
  try {
    await logoutAll(true)
    ElMessage.success('其他设备已全部注销')
    await loadSessions()
  } finally {
    revokingOthers.value = false
  }
}

async function confirmLogoutAll() {
  try {
    await ElMessageBox.confirm('将注销包括本机在内的全部设备，之后需要重新登录。确定继续吗？', '注销全部设备', {
      confirmButtonText: '全部注销',
      cancelButtonText: '取消',
      type: 'error',
    })
  } catch {
    return
  }
  loggingOutAll.value = true
  try {
    await authStore.logoutAll(false)
    router.push('/login')
  } finally {
    loggingOutAll.value = false
  }
}

function formatTime(iso: string): string {
  return iso ? new Date(iso).toLocaleString('zh-CN', { hour12: false }) : '-'
}

onMounted(loadSessions)
</script>

<style scoped>
.security-page {
  max-width: 1080px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-weight: 600;
}
.tip {
  margin-bottom: 16px;
}
.session-table {
  width: 100%;
}
.device-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.danger-zone {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px dashed #e4e7ed;
}
</style>
