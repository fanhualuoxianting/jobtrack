<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h1 class="title">登录 JobTrack</h1>
      <p class="subtitle">实习投递与面试管理平台</p>
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        class="error-alert"
      />
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
        <el-form-item prop="account">
          <el-input v-model="form.account" placeholder="用户名或邮箱" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit" :loading="submitting" @click="submit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer-links">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
      <el-divider class="divider">演示账号</el-divider>
      <p class="demo-tip">demo@jobtrack.local / JobTrack@123456</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { AxiosError } from 'axios'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const submitting = ref(false)
/** 错误提示统一为"账号或密码错误"，不暴露账号是否存在 */
const errorMessage = ref('')

const form = reactive({ account: '', password: '' })

const rules: FormRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function submit() {
  errorMessage.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await authStore.login({
      account: form.account.trim(),
      password: form.password,
      deviceName: navigator.userAgent.includes('Mobile') ? '移动端浏览器' : '桌面浏览器',
    })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.push(redirect)
  } catch (e) {
    const err = e as AxiosError<{ code?: string; message?: string }>
    errorMessage.value = err.response?.data?.message || '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #f0f5ff 0%, #f5f7fa 100%);
}
.auth-card {
  width: 400px;
  padding: 12px 20px 24px;
}
.title {
  margin: 8px 0 4px;
  font-size: 24px;
  text-align: center;
}
.subtitle {
  margin: 0 0 20px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.error-alert {
  margin-bottom: 16px;
}
.submit {
  width: 100%;
}
.footer-links {
  text-align: center;
  font-size: 13px;
  color: #606266;
}
.divider {
  margin: 20px 0 8px;
}
.demo-tip {
  text-align: center;
  font-size: 12px;
  color: #909399;
  margin: 0;
}
</style>
