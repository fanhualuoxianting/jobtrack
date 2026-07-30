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
        </el-menu>
      </div>
      <div class="header-right">
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
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, UserFilled } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeMenu = computed(() => (route.path.startsWith('/settings') ? '' : route.path))

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
</style>
