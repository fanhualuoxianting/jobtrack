<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header"><span class="card-title">面试管理</span><el-button type="primary" :icon="Plus" @click="openCreate">安排面试</el-button></div>
      </template>
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px"><el-option v-for="(text, key) in STATUS_TEXT" :key="key" :label="text" :value="key" /></el-select></el-form-item>
        <el-form-item label="公司"><el-select v-model="query.companyId" clearable filterable placeholder="全部公司" style="width: 180px"><el-option v-for="item in companies" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="query.from" type="date" value-format="YYYY-MM-DDT00:00:00Z" placeholder="开始日期" /><span class="date-separator">至</span><el-date-picker v-model="query.to" type="date" value-format="YYYY-MM-DDT23:59:59Z" placeholder="结束日期" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
      <el-alert v-if="loadFailed" title="面试列表加载失败，请稍后重试" type="error" show-icon :closable="false" class="mb" />
      <el-table :data="records" v-loading="loading" :empty-text="loadFailed ? '加载失败' : '暂无面试安排'" row-key="id">
        <el-table-column label="面试" min-width="220"><template #default="{ row }"><div class="strong">{{ row.roundName }}</div><small>投递 #{{ row.applicationId }} · {{ typeText(row.interviewType) }}</small></template></el-table-column>
        <el-table-column label="时间" min-width="190"><template #default="{ row }"><div>{{ formatDateTime(row.scheduledStartAt) }}</div><small>{{ row.timezone }}</small></template></el-table-column>
        <el-table-column label="状态" width="100"><template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="地点 / 联系人" min-width="180"><template #default="{ row }"><div>{{ row.location || (row.meetingUrl ? '线上会议' : '-') }}</div><small>{{ row.interviewer || '-' }}</small></template></el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'SCHEDULED'" type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 'SCHEDULED'" type="warning" link @click="cancel(row)">取消</el-button>
            <el-button v-if="row.status === 'SCHEDULED'" type="success" link @click="complete(row)">完成</el-button>
            <el-button type="danger" link @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar"><el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="loadData" /></div>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑面试' : '安排面试'" width="650px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="投递" prop="applicationId"><el-select v-model="form.applicationId" :disabled="Boolean(form.id)" filterable placeholder="选择投递" style="width: 100%"><el-option v-for="item in applications" :key="item.id" :label="`#${item.id} ${item.companyName || ''} / ${item.positionTitle || ''}`" :value="item.id" /></el-select></el-form-item>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="轮次" prop="roundNumber"><el-input-number v-model="form.roundNumber" :min="1" :max="20" style="width: 100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="轮次名称" prop="roundName"><el-input v-model="form.roundName" maxlength="150" /></el-form-item></el-col></el-row>
        <el-form-item label="面试形式" prop="interviewType"><el-select v-model="form.interviewType" style="width: 100%"><el-option v-for="(text, key) in TYPE_TEXT" :key="key" :label="text" :value="key" /></el-select></el-form-item>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="开始时间" prop="scheduledStartAt"><el-date-picker v-model="form.scheduledStartAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" style="width: 100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="结束时间" prop="scheduledEndAt"><el-date-picker v-model="form.scheduledEndAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" style="width: 100%" /></el-form-item></el-col></el-row>
        <el-form-item label="时区" prop="timezone"><el-input v-model="form.timezone" placeholder="例如 Asia/Shanghai" /></el-form-item>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="地点"><el-input v-model="form.location" maxlength="500" /></el-form-item></el-col><el-col :span="12"><el-form-item label="会议链接"><el-input v-model="form.meetingUrl" placeholder="https://..." /></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="面试官"><el-input v-model="form.interviewer" maxlength="200" /></el-form-item></el-col><el-col :span="12"><el-form-item label="联系方式"><el-input v-model="form.contactInfo" maxlength="500" /></el-form-item></el-col></el-row>
        <el-form-item label="备注"><el-input v-model="form.notes" type="textarea" :rows="3" maxlength="5000" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { pageApplications } from '@/api/application'
import { fetchCompanyOptions } from '@/api/company'
import { cancelInterview, completeInterview, createInterview, deleteInterview, pageInterviews, updateInterview, type InterviewCreatePayload, type InterviewQuery } from '@/api/interview'
import type { ApplicationInfo, CompanyOption, InterviewInfo, InterviewStatus, InterviewType } from '@/types'

const STATUS_TEXT: Record<InterviewStatus, string> = { SCHEDULED: '已安排', COMPLETED: '已完成', CANCELLED: '已取消', NO_SHOW: '未到场' }
const STATUS_TAG: Record<InterviewStatus, 'primary' | 'success' | 'info' | 'danger' | 'warning'> = { SCHEDULED: 'primary', COMPLETED: 'success', CANCELLED: 'info', NO_SHOW: 'danger' }
const TYPE_TEXT: Record<InterviewType, string> = { PHONE: '电话', VIDEO: '视频', ONSITE: '现场', WRITTEN: '笔试', OTHER: '其他' }
const records = ref<InterviewInfo[]>([]); const total = ref(0); const loading = ref(false); const loadFailed = ref(false)
const companies = ref<CompanyOption[]>([]); const applications = ref<ApplicationInfo[]>([])
const query = reactive<InterviewQuery>({ page: 1, pageSize: 20, status: undefined, companyId: undefined, from: undefined, to: undefined })
const formVisible = ref(false); const submitting = ref(false); const formRef = ref<FormInstance>(); const form = reactive<InterviewCreatePayload & { id?: number; version?: number }>({ applicationId: 0, roundNumber: 1, roundName: '一面', interviewType: 'VIDEO', scheduledStartAt: '', scheduledEndAt: '', timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai', location: '', meetingUrl: '', interviewer: '', contactInfo: '', notes: '' })
const formRules: FormRules = { applicationId: [{ required: true, message: '请选择投递', trigger: 'change' }], roundNumber: [{ required: true, message: '请输入轮次', trigger: 'change' }], roundName: [{ required: true, message: '请输入轮次名称', trigger: 'blur' }], interviewType: [{ required: true, message: '请选择面试形式', trigger: 'change' }], scheduledStartAt: [{ required: true, message: '请选择开始时间', trigger: 'change' }], scheduledEndAt: [{ required: true, message: '请选择结束时间', trigger: 'change' }], timezone: [{ required: true, message: '请输入时区', trigger: 'blur' }] }

async function loadData() { loading.value = true; loadFailed.value = false; try { const res = await pageInterviews(query); records.value = res.data.records; total.value = res.data.total } catch { loadFailed.value = true } finally { loading.value = false } }
async function loadOptions() { const [companyRes, applicationRes] = await Promise.all([fetchCompanyOptions(), pageApplications({ page: 1, pageSize: 100, archived: false })]); companies.value = companyRes.data; applications.value = applicationRes.data.records }
function search() { query.page = 1; loadData() }
function resetQuery() { query.status = undefined; query.companyId = undefined; query.from = undefined; query.to = undefined; search() }
function emptyForm() { Object.assign(form, { id: undefined, version: undefined, applicationId: 0, roundNumber: 1, roundName: '一面', interviewType: 'VIDEO', scheduledStartAt: '', scheduledEndAt: '', timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai', location: '', meetingUrl: '', interviewer: '', contactInfo: '', notes: '' }) }
function openCreate() { emptyForm(); formVisible.value = true; loadOptions() }
function openEdit(row: InterviewInfo) { Object.assign(form, { ...row, id: row.id, version: row.version, notes: row.notes || '', location: row.location || '', meetingUrl: row.meetingUrl || '', interviewer: row.interviewer || '', contactInfo: row.contactInfo || '' }); formVisible.value = true }
async function submitForm() { if (!(await formRef.value?.validate().catch(() => false))) return; submitting.value = true; try { if (form.id && form.version != null) await updateInterview(form.id, { version: form.version, interviewType: form.interviewType, scheduledStartAt: form.scheduledStartAt, scheduledEndAt: form.scheduledEndAt, timezone: form.timezone, location: form.location || null, meetingUrl: form.meetingUrl || null, interviewer: form.interviewer || null, contactInfo: form.contactInfo || null, notes: form.notes || null }); else await createInterview({ ...form, location: form.location || null, meetingUrl: form.meetingUrl || null, interviewer: form.interviewer || null, contactInfo: form.contactInfo || null, notes: form.notes || null }); ElMessage.success('保存成功'); formVisible.value = false; await loadData() } catch (error) { ElMessage.error(errorMessage(error, '保存失败')) } finally { submitting.value = false } }
async function cancel(row: InterviewInfo) { try { const reason = await ElMessageBox.prompt('请输入取消原因', '取消面试', { inputPattern: /\S+/, inputErrorMessage: '取消原因不能为空' }); await cancelInterview(row.id, row.version, reason.value); ElMessage.success('面试已取消'); await loadData() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, '取消失败')) } }
async function complete(row: InterviewInfo) { try { const result = await ElMessageBox.prompt('请输入结果：PASS / FAIL / PENDING / UNKNOWN / NO_SHOW', '完成面试', { inputPattern: /^(PASS|FAIL|PENDING|UNKNOWN|NO_SHOW)$/, inputErrorMessage: '请输入合法结果' }); await completeInterview(row.id, row.version, result.value, ''); ElMessage.success('面试已完成'); await loadData() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, '完成失败')) } }
async function remove(row: InterviewInfo) { try { await ElMessageBox.confirm('删除后不再显示该面试记录，确定吗？', '删除确认', { type: 'warning' }); await deleteInterview(row.id, row.version); ElMessage.success('删除成功'); await loadData() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, '删除失败')) } }
function formatDateTime(value: string): string { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-' }
function errorMessage(error: unknown, fallback: string): string { return ((error as { response?: { data?: { message?: string } } }).response?.data?.message) || fallback }
function typeText(value: string): string { return TYPE_TEXT[value as InterviewType] || value }
function statusText(value: string): string { return STATUS_TEXT[value as InterviewStatus] || value }
function statusTag(value: string): 'primary' | 'success' | 'info' | 'danger' | 'warning' { return STATUS_TAG[value as InterviewStatus] || 'info' }
onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }.card-title { font-weight: 600; }.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }.mb { margin-bottom: 12px; }.date-separator { margin: 0 8px; color: #909399; }.strong { font-weight: 600; }small { color: #909399; }
</style>
