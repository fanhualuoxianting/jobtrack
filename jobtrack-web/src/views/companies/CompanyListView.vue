<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">公司管理</span>
          <el-button type="primary" :icon="Plus" @click="openForm()">新建公司</el-button>
        </div>
      </template>

      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="名称 / 简称" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="query.city" placeholder="如 南京" clearable style="width: 120px" />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="query.industry" placeholder="如 互联网" clearable style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="records" v-loading="loading" :empty-text="emptyText">
        <el-table-column prop="name" label="公司名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="shortName" label="简称" width="110" show-overflow-tooltip>
          <template #default="{ row }">{{ dash(row.shortName) }}</template>
        </el-table-column>
        <el-table-column prop="industry" label="行业" width="110">
          <template #default="{ row }">{{ dash(row.industry) }}</template>
        </el-table-column>
        <el-table-column prop="scale" label="规模" width="110">
          <template #default="{ row }">{{ dash(row.scale) }}</template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="100">
          <template #default="{ row }">{{ dash(row.city) }}</template>
        </el-table-column>
        <el-table-column prop="positionCount" label="岗位数" width="80" align="center" />
        <el-table-column prop="applicationCount" label="投递数" width="80" align="center" />
        <el-table-column label="更新时间" width="165">
          <template #default="{ row }">{{ formatTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openForm(row)">编辑</el-button>
            <el-button type="danger" link @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @change="loadData"
        />
      </div>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑公司' : '新建公司'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="150" placeholder="必填，同账号下不可重复" />
        </el-form-item>
        <el-form-item label="简称">
          <el-input v-model="form.shortName" maxlength="80" />
        </el-form-item>
        <el-form-item label="行业">
          <el-input v-model="form.industry" maxlength="80" placeholder="如 互联网 / 大数据" />
        </el-form-item>
        <el-form-item label="规模">
          <el-input v-model="form.scale" maxlength="30" placeholder="如 500-999人" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.city" maxlength="80" />
        </el-form-item>
        <el-form-item label="官网">
          <el-input v-model="form.website" maxlength="500" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AxiosError } from 'axios'
import {
  createCompany,
  deleteCompany,
  fetchCompany,
  pageCompanies,
  updateCompany,
  type CompanyPayload,
} from '@/api/company'
import type { CompanyListItem } from '@/types'
import { dash } from '@/utils/dict'

const loading = ref(false)
const loadFailed = ref(false)
const records = ref<CompanyListItem[]>([])
const total = ref(0)

const query = reactive({
  keyword: '',
  city: '',
  industry: '',
  page: 1,
  pageSize: 20,
})

const emptyText = computed(() => (loadFailed.value ? '加载失败，请稍后重试' : '暂无公司，点击右上角新建'))

async function loadData() {
  loading.value = true
  loadFailed.value = false
  try {
    const res = await pageCompanies({
      ...query,
      keyword: query.keyword || undefined,
      city: query.city || undefined,
      industry: query.industry || undefined,
    })
    records.value = res.data.records
    total.value = res.data.total
  } catch {
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function search() {
  query.page = 1
  loadData()
}

function resetQuery() {
  query.keyword = ''
  query.city = ''
  query.industry = ''
  search()
}

// ---------- 新增 / 编辑 ----------
const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<CompanyPayload & { id?: number }>({
  name: '',
  shortName: '',
  industry: '',
  scale: '',
  city: '',
  website: '',
  description: '',
})

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入公司名称', trigger: 'blur' },
    { max: 150, message: '不超过 150 字符', trigger: 'blur' },
  ],
  website: [
    { pattern: /^$|^https?:\/\/[\w\-.]+(?::\d+)?(\/\S*)?$/, message: '请输入合法 URL', trigger: 'blur' },
  ],
}

function openForm(row?: CompanyListItem) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      shortName: row.shortName ?? '',
      industry: row.industry ?? '',
      scale: row.scale ?? '',
      city: row.city ?? '',
      website: '',
      description: '',
    })
    // 编辑时加载完整详情（website/description 不在列表返回）
    fetchCompany(row.id).then((res) => {
      form.website = res.data.website ?? ''
      form.description = res.data.description ?? ''
    })
  } else {
    Object.assign(form, {
      id: undefined,
      name: '',
      shortName: '',
      industry: '',
      scale: '',
      city: '',
      website: '',
      description: '',
    })
  }
  formVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const payload: CompanyPayload = {
      name: form.name.trim(),
      shortName: form.shortName?.trim() || null,
      industry: form.industry?.trim() || null,
      scale: form.scale?.trim() || null,
      city: form.city?.trim() || null,
      website: form.website?.trim() || null,
      description: form.description?.trim() || null,
    }
    if (form.id) {
      await updateCompany(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createCompany(payload)
      ElMessage.success('创建成功')
    }
    formVisible.value = false
    await loadData()
  } catch (e) {
    const err = e as AxiosError<{ message?: string }>
    ElMessage.error(err.response?.data?.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

// ---------- 删除 ----------
async function confirmDelete(row: CompanyListItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除公司「${row.name}」吗？存在关联岗位或投递时将被拒绝。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deleteCompany(row.id)
    ElMessage.success('删除成功')
    if (records.value.length === 1 && query.page > 1) query.page -= 1
    await loadData()
  } catch (e) {
    const err = e as AxiosError<{ code?: string; message?: string }>
    // 409 关联保护：明确提示，不静默
    ElMessage.error(err.response?.data?.message || '删除失败')
  }
}

function formatTime(iso: string): string {
  return iso ? new Date(iso).toLocaleString('zh-CN', { hour12: false }) : '-'
}

onMounted(loadData)
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-title {
  font-weight: 600;
}
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
