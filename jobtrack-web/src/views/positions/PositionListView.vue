<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">岗位管理</span>
          <el-button type="primary" :icon="Plus" @click="openForm()">新建岗位</el-button>
        </div>
      </template>

      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="岗位 / 部门 / 要求" clearable style="width: 170px" />
        </el-form-item>
        <el-form-item label="公司">
          <el-select v-model="query.companyId" clearable filterable style="width: 180px" placeholder="全部公司">
            <el-option v-for="c in companyOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable style="width: 110px" placeholder="全部">
            <el-option label="招聘中" value="OPEN" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作类型">
          <el-select v-model="query.workType" clearable style="width: 110px" placeholder="全部">
            <el-option v-for="(text, key) in WORK_TYPE_TEXT" :key="key" :label="text" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="records" v-loading="loading" :empty-text="emptyText">
        <el-table-column prop="title" label="岗位名称" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="openForm(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="companyName" label="公司" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ dash(row.companyName) }}</template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="90">
          <template #default="{ row }">{{ dash(row.city) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ WORK_TYPE_TEXT[row.workType] }}</template>
        </el-table-column>
        <el-table-column label="办公" width="95">
          <template #default="{ row }">{{ WORKPLACE_TEXT[row.workplaceType] }}</template>
        </el-table-column>
        <el-table-column label="薪资" width="130" align="right">
          <template #default="{ row }">{{ formatSalary(row.salaryMin, row.salaryMax, row.salaryUnit) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="90">
          <template #default="{ row }">{{ dash(row.source) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="POSITION_STATUS_TAG[row.status]" size="small">
              {{ POSITION_STATUS_TEXT[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" width="105">
          <template #default="{ row }">{{ row.deadlineAt ? formatDate(row.deadlineAt) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="185" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.status === 'OPEN'" type="warning" link @click="toggleStatus(row)">关闭</el-button>
            <el-button v-else type="success" link @click="toggleStatus(row)">开启</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑岗位' : '新建岗位'" width="640px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="96px">
        <el-form-item label="所属公司" prop="companyId">
          <el-select v-model="form.companyId" filterable style="width: 100%" placeholder="选择公司">
            <el-option v-for="c in companyOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="form.title" maxlength="150" placeholder="必然，如 Java 后端开发实习生" />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="form.department" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市">
              <el-input v-model="form.city" maxlength="80" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="工作类型" prop="workType">
              <el-select v-model="form.workType" style="width: 100%">
                <el-option v-for="(text, key) in WORK_TYPE_TEXT" :key="key" :label="text" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="办公方式" prop="workplaceType">
              <el-select v-model="form.workplaceType" style="width: 100%">
                <el-option v-for="(text, key) in WORKPLACE_TEXT" :key="key" :label="text" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="薪资范围">
          <div class="salary-row">
            <el-input-number v-model="form.salaryMin" :min="0" :precision="2" controls-position="right" style="width: 160px" />
            <span class="salary-sep">至</span>
            <el-input-number v-model="form.salaryMax" :min="0" :precision="2" controls-position="right" style="width: 160px" />
            <el-select v-model="form.salaryUnit" clearable placeholder="单位" style="width: 110px; margin-left: 8px">
              <el-option v-for="(text, key) in SALARY_UNIT_TEXT" :key="key" :label="text" :value="key" />
            </el-select>
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="招聘来源">
              <el-input v-model="form.source" maxlength="50" placeholder="如 BOSS / 官网 / 内推" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发布时间">
              <el-date-picker v-model="form.publishedAt" type="datetime" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ssZ" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="岗位链接" prop="sourceUrl">
              <el-input v-model="form.sourceUrl" maxlength="1000" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止时间">
              <el-date-picker v-model="form.deadlineAt" type="datetime" style="width: 100%" value-format="YYYY-MM-DDTHH:mm:ssZ" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="岗位描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="10000" />
        </el-form-item>
        <el-form-item label="岗位要求">
          <el-input v-model="form.requirements" type="textarea" :rows="3" maxlength="10000" />
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
import { fetchCompanyOptions } from '@/api/company'
import {
  changePositionStatus,
  createPosition,
  deletePosition,
  fetchPosition,
  pagePositions,
  updatePosition,
  type PositionPayload,
} from '@/api/position'
import type { CompanyOption, PositionListItem } from '@/types'
import {
  dash,
  formatSalary,
  POSITION_STATUS_TAG,
  POSITION_STATUS_TEXT,
  SALARY_UNIT_TEXT,
  WORK_TYPE_TEXT,
  WORKPLACE_TEXT,
} from '@/utils/dict'

const loading = ref(false)
const loadFailed = ref(false)
const records = ref<PositionListItem[]>([])
const total = ref(0)
const companyOptions = ref<CompanyOption[]>([])

const query = reactive({
  keyword: '',
  companyId: undefined as number | undefined,
  status: '',
  workType: '',
  page: 1,
  pageSize: 20,
})

const emptyText = computed(() => (loadFailed.value ? '加载失败，请稍后重试' : '暂无岗位，点击右上角新建'))

async function loadData() {
  loading.value = true
  loadFailed.value = false
  try {
    const res = await pagePositions({
      ...query,
      keyword: query.keyword || undefined,
      companyId: query.companyId,
      status: query.status || undefined,
      workType: query.workType || undefined,
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
  query.companyId = undefined
  query.status = ''
  query.workType = ''
  search()
}

async function loadCompanyOptions() {
  const res = await fetchCompanyOptions()
  companyOptions.value = res.data
}

// ---------- 新增 / 编辑 ----------
const formVisible = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const emptyForm = (): PositionPayload & { id?: number } => ({
  id: undefined,
  companyId: null,
  title: '',
  department: '',
  city: '',
  workType: 'INTERNSHIP',
  workplaceType: 'ONSITE',
  salaryMin: null,
  salaryMax: null,
  salaryUnit: 'DAY',
  source: '',
  sourceUrl: '',
  description: '',
  requirements: '',
  publishedAt: null,
  deadlineAt: null,
})
const form = reactive<PositionPayload & { id?: number }>(emptyForm())

const formRules: FormRules = {
  companyId: [{ required: true, message: '请选择公司', trigger: 'change' }],
  title: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { max: 150, message: '不超过 150 字符', trigger: 'blur' },
  ],
  workType: [{ required: true, message: '请选择工作类型', trigger: 'change' }],
  workplaceType: [{ required: true, message: '请选择办公方式', trigger: 'change' }],
  sourceUrl: [
    { pattern: /^$|^https?:\/\/[\w\-.]+(?::\d+)?(\/\S*)?$/, message: '请输入合法 URL', trigger: 'blur' },
  ],
}

function openForm(row?: PositionListItem) {
  if (row) {
    fetchPosition(row.id).then((res) => {
      const d = res.data
      Object.assign(form, {
        id: d.id,
        companyId: d.companyId,
        title: d.title,
        department: d.department ?? '',
        city: d.city ?? '',
        workType: d.workType,
        workplaceType: d.workplaceType,
        salaryMin: d.salaryMin,
        salaryMax: d.salaryMax,
        salaryUnit: d.salaryUnit,
        source: d.source ?? '',
        sourceUrl: d.sourceUrl ?? '',
        description: d.description ?? '',
        requirements: d.requirements ?? '',
        publishedAt: d.publishedAt,
        deadlineAt: d.deadlineAt,
      })
    })
    form.id = row.id
    form.title = row.title
  } else {
    Object.assign(form, emptyForm())
  }
  loadCompanyOptions()
  formVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.salaryMin != null && form.salaryMax != null && form.salaryMin > form.salaryMax) {
    ElMessage.warning('薪资下限不能大于薪资上限')
    return
  }
  submitting.value = true
  try {
    const payload: PositionPayload = {
      companyId: form.companyId,
      title: form.title.trim(),
      department: form.department?.trim() || null,
      city: form.city?.trim() || null,
      workType: form.workType,
      workplaceType: form.workplaceType,
      salaryMin: form.salaryMin,
      salaryMax: form.salaryMax,
      salaryUnit: form.salaryUnit || null,
      source: form.source?.trim() || null,
      sourceUrl: form.sourceUrl?.trim() || null,
      description: form.description?.trim() || null,
      requirements: form.requirements?.trim() || null,
      publishedAt: form.publishedAt,
      deadlineAt: form.deadlineAt,
    }
    if (form.id) {
      await updatePosition(form.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createPosition(payload)
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

// ---------- 状态启停 ----------
async function toggleStatus(row: PositionListItem) {
  const target = row.status === 'OPEN' ? 'CLOSED' : 'OPEN'
  try {
    await ElMessageBox.confirm(
      target === 'CLOSED'
        ? `确定关闭岗位「${row.title}」吗？历史投递记录会保留。`
        : `确定重新开启岗位「${row.title}」吗？`,
      '状态变更',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  await changePositionStatus(row.id, target)
  ElMessage.success('状态已更新')
  await loadData()
}

// ---------- 删除 ----------
async function confirmDelete(row: PositionListItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除岗位「${row.title}」吗？存在投递记录时将被拒绝，可改为关闭。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await deletePosition(row.id)
    ElMessage.success('删除成功')
    if (records.value.length === 1 && query.page > 1) query.page -= 1
    await loadData()
  } catch (e) {
    const err = e as AxiosError<{ message?: string }>
    ElMessage.error(err.response?.data?.message || '删除失败')
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadData()
  loadCompanyOptions()
})
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
.salary-row {
  display: flex;
  align-items: center;
}
.salary-sep {
  margin: 0 8px;
  color: #909399;
}
</style>
