<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h1 class="title">注册账号</h1>
      <p class="subtitle">注册成功后请重新登录</p>
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="false"
        class="error-alert"
      />
      <el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4-50 位字母、数字或下划线" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="用于登录，例如 you@example.com" />
        </el-form-item>
        <el-form-item label="昵称（可选）" prop="nickname">
          <el-input v-model="form.nickname" placeholder="留空则与用户名一致" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="8-64 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="submit" :loading="submitting" @click="submit">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="footer-links">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import type { AxiosError } from 'axios'
import { register } from '@/api/auth'

const router = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  email: '',
  nickname: '',
  password: '',
  confirmPassword: '',
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,50}$/, message: '4-50 位字母、数字或下划线', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不合法', trigger: 'blur' },
  ],
  nickname: [{ max: 60, message: '昵称不超过 60 字符', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 8, max: 64, message: '密码长度 8-64 位', trigger: 'blur' },
    { pattern: /^(?=.*[A-Za-z])(?=.*\d).+$/, message: '需同时包含字母和数字', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_r, value: string, cb) =>
        value === form.password ? cb() : cb(new Error('两次输入的密码不一致')),
      trigger: 'blur',
    },
  ],
}

async function submit() {
  errorMessage.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await register({
      username: form.username.trim(),
      email: form.email.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      nickname: form.nickname.trim() || undefined,
    })
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (e) {
    const err = e as AxiosError<{ code?: string; message?: string }>
    // 409 冲突直接展示后端消息（用户名/邮箱占用），不暴露额外信息
    errorMessage.value = err.response?.data?.message || '注册失败，请稍后重试'
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
  padding: 24px 0;
}
.auth-card {
  width: 440px;
  padding: 4px 20px 20px;
}
.title {
  margin: 12px 0 4px;
  font-size: 24px;
  text-align: center;
}
.subtitle {
  margin: 0 0 16px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
.error-alert {
  margin-bottom: 12px;
}
.submit {
  width: 100%;
}
.footer-links {
  text-align: center;
  font-size: 13px;
  color: #606266;
}
</style>
