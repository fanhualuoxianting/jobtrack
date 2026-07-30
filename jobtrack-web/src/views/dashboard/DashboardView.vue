<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="24">
        <el-card>
          <h2 class="welcome">
            {{ greeting }}，{{ authStore.user?.nickname || authStore.user?.username }}
          </h2>
          <el-descriptions :column="3" border size="small" class="profile">
            <el-descriptions-item label="用户名">{{ authStore.user?.username }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ authStore.user?.email }}</el-descriptions-item>
            <el-descriptions-item label="角色">
              <el-tag size="small" :type="authStore.user?.role === 'ADMIN' ? 'danger' : 'primary'">
                {{ authStore.user?.role }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">
              {{ authStore.user?.lastLoginAt ? formatTime(authStore.user.lastLoginAt) : '首次登录' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="modules">
      <el-col v-for="m in upcomingModules" :key="m.name" :xs="12" :sm="8" :md="6">
        <el-card shadow="hover" class="module-card">
          <div class="module-name">{{ m.name }}</div>
          <div class="module-stage">{{ m.stage }}</div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 阶段 2 仅交付认证；业务模块按阶段计划陆续开放，不展示任何假数据
const upcomingModules = [
  { name: '公司管理', stage: '阶段 3' },
  { name: '岗位管理', stage: '阶段 3' },
  { name: '简历管理', stage: '阶段 4' },
  { name: '投递记录', stage: '阶段 5' },
  { name: '面试日程', stage: '阶段 6' },
  { name: '提醒通知', stage: '阶段 6' },
  { name: '统计看板', stage: '阶段 7' },
]

function formatTime(iso: string): string {
  return new Date(iso).toLocaleString('zh-CN', { hour12: false })
}
</script>

<style scoped>
.welcome {
  margin: 0 0 16px;
  font-size: 20px;
}
.profile {
  max-width: 720px;
}
.modules {
  margin-top: 16px;
}
.module-card {
  margin-bottom: 16px;
  text-align: center;
  color: #606266;
}
.module-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
.module-stage {
  font-size: 12px;
  color: #909399;
}
</style>
