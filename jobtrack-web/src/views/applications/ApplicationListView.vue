<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="card-header"><span class="card-title">投递管理</span><el-button type="primary" :icon="Plus" @click="openCreate">新建投递</el-button></div>
      </template>
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="关键词"><el-input v-model="query.keyword" clearable placeholder="公司 / 岗位 / 备注" /></el-form-item>
        <el-form-item label="公司"><el-select v-model="query.companyId" clearable filterable placeholder="全部公司" style="width: 170px"><el-option v-for="item in companies" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px"><el-option v-for="(text, key) in APPLICATION_STATUS_TEXT" :key="key" :label="text" :value="key" /></el-select></el-form-item>
        <el-form-item label="归档"><el-switch v-model="query.archived" active-text="仅看归档" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="loading" @click="search">查询</el-button><el-button @click="resetQuery">重置</el-button></el-form-item>
      </el-form>

      <el-alert v-if="loadFailed" title="投递列表加载失败，请稍后重试" type="error" show-icon :closable="false" class="mb" />
      <el-table :data="records" v-loading="loading" :empty-text="loadFailed ? '加载失败' : '暂无投递记录'" row-key="id">
        <el-table-column label="公司 / 岗位" min-width="220" show-overflow-tooltip>
          <template #default="{ row }"><div>{{ row.companyName || '-' }}</div><small>{{ row.positionTitle || '-' }}</small></template>
        </el-table-column>
        <el-table-column label="状态" width="115"><template #default="{ row }"><el-tag :type="APPLICATION_STATUS_TAG[row.status]" size="small">{{ APPLICATION_STATUS_TEXT[row.status] }}</el-tag></template></el-table-column>
        <el-table-column prop="source" label="来源" width="100"><template #default="{ row }">{{ row.source || '-' }}</template></el-table-column>
        <el-table-column label="简历" min-width="120"><template #default="{ row }">{{ row.resumeVersionName || '未指定' }}</template></el-table-column>
        <el-table-column label="投递时间" width="120"><template #default="{ row }">{{ formatDate(row.appliedAt) }}</template></el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row.id)">编辑</el-button>
            <el-button type="warning" link :loading="actionId === row.id" @click="openTransition(row)">流转</el-button>
            <el-button link @click="openTimeline(row.id)">时间线</el-button>
            <el-button v-if="!row.archived" type="info" link :loading="actionId === row.id" @click="archive(row.id)">归档</el-button>
            <el-button v-else type="success" link :loading="actionId === row.id" @click="restore(row.id)">恢复</el-button>
            <el-button v-if="row.status === 'SAVED'" type="danger" link @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-bar"><el-pagination v-model:current-page="query.page" v-model:page-size="query.pageSize" :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next" @change="loadData" /></div>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑投递' : '新建投递'" width="650px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="公司" prop="companyId"><el-select v-model="form.companyId" :disabled="Boolean(form.id)" filterable style="width: 100%" @change="loadPositions"><el-option v-for="item in companies" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="岗位" prop="positionId"><el-select v-model="form.positionId" :disabled="Boolean(form.id)" filterable style="width: 100%"><el-option v-for="item in positions" :key="item.id" :label="item.title" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="简历"><el-select v-model="form.resumeId" clearable placeholder="可不指定" style="width: 100%"><el-option v-for="item in resumes" :key="item.id" :label="item.versionName" :value="item.id" /></el-select></el-form-item>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="来源"><el-input v-model="form.source" maxlength="50" /></el-form-item></el-col><el-col :span="12"><el-form-item label="优先级"><el-select v-model="form.priority" style="width: 100%"><el-option label="低" value="LOW" /><el-option label="中" value="MEDIUM" /><el-option label="高" value="HIGH" /></el-select></el-form-item></el-col></el-row>
        <el-row :gutter="12"><el-col :span="12"><el-form-item label="投递时间"><el-date-picker v-model="form.appliedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" style="width: 100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="下一步时间"><el-date-picker v-model="form.nextActionAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" style="width: 100%" /></el-form-item></el-col></el-row>
        <el-form-item label="下一步计划"><el-input v-model="form.nextAction" maxlength="255" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.referralName" maxlength="100" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.note" type="textarea" :rows="4" maxlength="5000" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="formVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="transitionVisible" title="状态流转" width="460px">
      <el-form label-width="95px"><el-form-item label="目标状态"><el-select v-model="transitionForm.targetStatus" style="width: 100%"><el-option v-for="item in transitionTargets" :key="item" :label="APPLICATION_STATUS_TEXT[item]" :value="item" /></el-select></el-form-item><el-form-item label="流转原因"><el-input v-model="transitionForm.reason" type="textarea" maxlength="500" placeholder="拒绝、撤回、关闭、接受 Offer 时必填" /></el-form-item></el-form>
      <template #footer><el-button @click="transitionVisible = false">取消</el-button><el-button type="primary" :loading="transitionSubmitting" @click="submitTransition">确认流转</el-button></template>
    </el-dialog>

    <el-drawer v-model="timelineVisible" title="投递时间线" size="420px"><el-empty v-if="timeline.length === 0" description="暂无历史" /><el-timeline v-else><el-timeline-item v-for="item in timeline" :key="item.id" :timestamp="formatDateTime(item.occurredAt)" placement="top"><div class="timeline-status">{{ item.fromStatus ? APPLICATION_STATUS_TEXT[item.fromStatus] : '创建' }} → {{ APPLICATION_STATUS_TEXT[item.toStatus] }}</div><div v-if="item.reason" class="timeline-reason">{{ item.reason }}</div><small>操作人 #{{ item.operatorUserId }}</small></el-timeline-item></el-timeline></el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { fetchCompanyOptions } from '@/api/company'
import { fetchPositionOptions } from '@/api/position'
import { fetchResumes } from '@/api/resume'
import { archiveApplication, allowedApplicationTransitions, createApplication, deleteApplication, fetchApplication, fetchApplicationTimeline, pageApplications, restoreApplication, transitionApplication, updateApplication, type ApplicationCreatePayload } from '@/api/application'
import type { ApplicationInfo, ApplicationStatus, ApplicationTimelineItem, CompanyOption, PositionOption } from '@/types'
import { APPLICATION_STATUS_TAG, APPLICATION_STATUS_TEXT } from '@/utils/dict'

const loading = ref(false); const loadFailed = ref(false); const records = ref<ApplicationInfo[]>([]); const total = ref(0); const companies = ref<CompanyOption[]>([]); const positions = ref<PositionOption[]>([]); const resumes = ref<{ id: number; versionName: string }[]>([])
const query = reactive({ keyword: '', companyId: undefined as number | undefined, status: undefined as ApplicationStatus | undefined, archived: false, page: 1, pageSize: 20 })
const actionId = ref<number | null>(null)
const formVisible = ref(false); const submitting = ref(false); const formRef = ref<FormInstance>()
const form = reactive<ApplicationCreatePayload & { id?: number; version?: number }>({ companyId: 0, positionId: 0, resumeId: null, priority: 'MEDIUM', source: '', appliedAt: null, referralName: '', nextAction: '', nextActionAt: null, note: '' })
const formRules: FormRules = { companyId: [{ required: true, message: '请选择公司', trigger: 'change' }], positionId: [{ required: true, message: '请选择岗位', trigger: 'change' }] }
const transitionVisible = ref(false); const transitionSubmitting = ref(false); const transitionTargets = ref<ApplicationStatus[]>([]); const transitionForm = reactive<{ id: number; version: number; targetStatus?: ApplicationStatus; reason: string; idempotencyKey: string }>({ id: 0, version: 0, reason: '', idempotencyKey: '' })
const timelineVisible = ref(false); const timeline = ref<ApplicationTimelineItem[]>([])

async function loadData() { loading.value = true; loadFailed.value = false; try { const res = await pageApplications({ ...query, keyword: query.keyword || undefined, status: query.status, companyId: query.companyId }); records.value = res.data.records; total.value = res.data.total } catch { loadFailed.value = true } finally { loading.value = false } }
async function loadOptions() { const [companyRes, resumeRes] = await Promise.all([fetchCompanyOptions(), fetchResumes()]); companies.value = companyRes.data; resumes.value = resumeRes.data.map((item) => ({ id: item.id, versionName: item.versionName })) }
async function loadPositions() { positions.value = form.companyId ? (await fetchPositionOptions(form.companyId)).data : [] }
function search() { query.page = 1; loadData() }
function resetQuery() { query.keyword = ''; query.companyId = undefined; query.status = undefined; query.archived = false; search() }
function emptyForm() { Object.assign(form, { id: undefined, version: undefined, companyId: 0, positionId: 0, resumeId: null, priority: 'MEDIUM', source: '', appliedAt: null, referralName: '', nextAction: '', nextActionAt: null, note: '' }) }
function openCreate() { emptyForm(); positions.value = []; formVisible.value = true; loadOptions() }
async function openEdit(id: number) { try { const res = await fetchApplication(id); Object.assign(form, res.data); await loadOptions(); await loadPositions(); formVisible.value = true } catch { /* request interceptor already explains the error */ } }
async function submitForm() { if (!(await formRef.value?.validate().catch(() => false))) return; submitting.value = true; try { if (form.id && form.version != null) { await updateApplication(form.id, { version: form.version, resumeId: form.resumeId, source: form.source || null, appliedAt: form.appliedAt, referralName: form.referralName || null, nextAction: form.nextAction || null, nextActionAt: form.nextActionAt, note: form.note || null }) } else { await createApplication({ ...form, companyId: form.companyId, positionId: form.positionId, source: form.source || null, referralName: form.referralName || null, nextAction: form.nextAction || null, note: form.note || null }) } ElMessage.success('保存成功'); formVisible.value = false; await loadData() } catch (error) { ElMessage.error(errorMessage(error, '保存失败')) } finally { submitting.value = false } }
async function openTransition(row: ApplicationInfo) { actionId.value = row.id; try { transitionTargets.value = (await allowedApplicationTransitions(row.id)).data; transitionForm.id = row.id; transitionForm.version = row.version; transitionForm.targetStatus = transitionTargets.value[0]; transitionForm.reason = ''; transitionForm.idempotencyKey = crypto.randomUUID(); transitionVisible.value = true } catch (error) { ElMessage.error(errorMessage(error, '无法读取合法状态')) } finally { actionId.value = null } }
async function submitTransition() { if (!transitionForm.targetStatus) return; transitionSubmitting.value = true; try { await transitionApplication(transitionForm.id, transitionForm.targetStatus, transitionForm.reason, transitionForm.version, transitionForm.idempotencyKey); ElMessage.success('状态流转成功'); transitionVisible.value = false; await loadData() } catch (error) { const code = errorCode(error); ElMessage.error(code === 'APPLICATION_VERSION_CONFLICT' ? '数据已被更新，请刷新后重试' : errorMessage(error, '状态流转失败')) } finally { transitionSubmitting.value = false } }
async function archive(id: number) { await runAction(id, archiveApplication, '归档成功') }
async function restore(id: number) { await runAction(id, restoreApplication, '恢复成功') }
async function runAction(id: number, action: (id: number) => Promise<unknown>, success: string) { actionId.value = id; try { await action(id); ElMessage.success(success); await loadData() } catch (error) { ElMessage.error(errorMessage(error, '操作失败')) } finally { actionId.value = null } }
async function remove(id: number) { try { await ElMessageBox.confirm('仅未投递草稿允许删除，确定删除吗？', '删除确认', { type: 'warning' }); await deleteApplication(id); ElMessage.success('删除成功'); await loadData() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error(errorMessage(error, '删除失败')) } }
async function openTimeline(id: number) { try { timeline.value = (await fetchApplicationTimeline(id)).data; timelineVisible.value = true } catch (error) { ElMessage.error(errorMessage(error, '时间线加载失败')) } }
function errorCode(error: unknown): string { return ((error as { response?: { data?: { code?: string } } }).response?.data?.code) || '' }
function errorMessage(error: unknown, fallback: string): string { return ((error as { response?: { data?: { message?: string } } }).response?.data?.message) || fallback }
function formatDate(value: string | null): string { return value ? new Date(value).toLocaleDateString('zh-CN') : '-' }
function formatDateTime(value: string): string { return new Date(value).toLocaleString('zh-CN') }
onMounted(() => { loadOptions(); loadData() })
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }.card-title { font-weight: 600; }.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }.mb { margin-bottom: 12px; }.timeline-status { font-weight: 600; }.timeline-reason { margin: 6px 0; color: #606266; white-space: pre-wrap; }
</style>
