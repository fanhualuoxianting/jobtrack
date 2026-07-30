<template>
  <div class="dashboard-page">
    <el-card class="filter-card">
      <el-form :model="query" inline @submit.prevent>
        <el-form-item label="时间范围"><el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" /><span class="separator">至</span><el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="公司"><el-select v-model="query.companyId" clearable filterable placeholder="全部公司" style="width: 180px"><el-option v-for="item in companies" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="来源"><el-input v-model="query.source" clearable placeholder="例如 BOSS" style="width: 140px" /></el-form-item>
        <el-form-item label="粒度"><el-select v-model="query.granularity" style="width: 100px"><el-option label="日" value="DAY" /><el-option label="周" value="WEEK" /><el-option label="月" value="MONTH" /></el-select></el-form-item>
        <el-form-item><el-checkbox v-model="query.includeArchived">包含归档</el-checkbox><el-button type="primary" :loading="loading.summary" @click="reload">刷新</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-alert v-if="errors.summary" title="汇总加载失败，请稍后重试" type="error" show-icon :closable="false" class="block-error" />
    <el-row :gutter="12" class="summary-row">
      <el-col v-for="card in summaryCards" :key="card.label" :xs="12" :sm="8" :md="4"><el-card shadow="hover" class="summary-card"><div class="summary-label">{{ card.label }}</div><div class="summary-value">{{ card.value }}</div></el-card></el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12"><el-card class="chart-card"><template #header>投递漏斗</template><el-alert v-if="errors.funnel" title="漏斗加载失败" type="error" :closable="false" /><div ref="funnelElement" class="chart" /></el-card></el-col>
      <el-col :xs="24" :lg="12"><el-card class="chart-card"><template #header>投递趋势</template><el-alert v-if="errors.trends" title="趋势加载失败" type="error" :closable="false" /><div ref="trendElement" class="chart" /></el-card></el-col>
      <el-col :xs="24" :lg="12"><el-card class="chart-card"><template #header>来源分布</template><el-alert v-if="errors.sources" title="来源分布加载失败" type="error" :closable="false" /><div ref="sourceElement" class="chart" /></el-card></el-col>
      <el-col :xs="24" :lg="12"><el-card class="chart-card"><template #header>行业分布</template><el-alert v-if="errors.industries" title="行业分布加载失败" type="error" :closable="false" /><div ref="industryElement" class="chart" /></el-card></el-col>
    </el-row>

    <el-row :gutter="16" class="lower-row">
      <el-col :xs="24" :lg="14"><el-card><template #header>平均流程周期（小时）</template><el-table :data="cycleRows" v-loading="loading.cycle" :empty-text="errors.cycle ? '加载失败' : '暂无完整样本'"><el-table-column prop="label" label="阶段" /><el-table-column prop="sampleCount" label="样本数" width="90" /><el-table-column prop="averageHours" label="平均" width="100" /><el-table-column prop="minHours" label="最小" width="100" /><el-table-column prop="maxHours" label="最大" width="100" /></el-table></el-card></el-col>
      <el-col :xs="24" :lg="10"><el-card><template #header>未来 7 天面试</template><el-alert v-if="errors.upcoming" title="面试加载失败" type="error" :closable="false" /><el-empty v-if="upcoming.length === 0 && !loading.upcoming" description="暂无即将到来的面试" /><div v-for="item in upcoming" :key="item.interviewId" class="upcoming-item"><div class="upcoming-title">{{ item.companyName }} · {{ item.roundName }}</div><div>{{ item.positionTitle }}</div><small>{{ formatDateTime(item.scheduledStartAt) }}（{{ item.timezone }}）</small></div></el-card></el-col>
      <el-col :xs="24"><el-card><template #header>最近未读通知</template><el-empty v-if="notifications.length === 0 && !loading.notifications" description="暂无未读通知" /><div v-for="item in notifications" :key="item.id" class="notification-item"><span>{{ item.title }}</span><small>{{ formatDateTime(item.sentAt || item.createdAt) }}</small></div></el-card></el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { fetchCompanyOptions } from '@/api/company'
import { fetchDashboardCycleTime, fetchDashboardFunnel, fetchDashboardIndustries, fetchDashboardSources, fetchDashboardSummary, fetchDashboardTrends, fetchDashboardUpcoming, fetchRecentUnreadNotifications, type DashboardCycleResult, type DashboardFunnelItem, type DashboardQuery, type DashboardSummary, type DashboardTrendItem } from '@/api/dashboard'
import type { CompanyOption, ReminderInfo } from '@/types'
import { formatDashboardRate } from '@/utils/dashboard'

type EchartsModule = typeof import('echarts/core')
type EChartInstance = ReturnType<EchartsModule['init']>
type ChartOption = Parameters<EChartInstance['setOption']>[0]
type BlockName = 'summary' | 'funnel' | 'trends' | 'sources' | 'industries' | 'cycle' | 'upcoming' | 'notifications'

const today = new Date(); const localDate = (date: Date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}
const query = reactive<DashboardQuery>({ startDate: localDate(new Date(today.getTime() - 29 * 86400000)), endDate: localDate(today), timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai', companyId: undefined, source: '', includeArchived: false, granularity: 'DAY' })
const companies = ref<CompanyOption[]>([]); const summary = ref<DashboardSummary | null>(null); const funnel = ref<DashboardFunnelItem[]>([]); const trends = ref<DashboardTrendItem[]>([]); const sources = ref<{ name: string; count: number }[]>([]); const industries = ref<{ name: string; count: number }[]>([]); const cycle = ref<DashboardCycleResult | null>(null); const upcoming = ref<{ interviewId: number; applicationId: number; companyName: string; positionTitle: string; roundName: string; scheduledStartAt: string; timezone: string }[]>([]); const notifications = ref<ReminderInfo[]>([])
const loading = reactive<Record<BlockName, boolean>>({ summary: false, funnel: false, trends: false, sources: false, industries: false, cycle: false, upcoming: false, notifications: false }); const errors = reactive<Record<BlockName, string>>({ summary: '', funnel: '', trends: '', sources: '', industries: '', cycle: '', upcoming: '', notifications: '' })
const funnelElement = ref<HTMLElement | null>(null); const trendElement = ref<HTMLElement | null>(null); const sourceElement = ref<HTMLElement | null>(null); const industryElement = ref<HTMLElement | null>(null); const charts = new Set<EChartInstance>(); let echartsCore: EchartsModule | null = null

const summaryCards = computed(() => { const item = summary.value; return [{ label: '投递总数', value: item?.totalApplications ?? 0 }, { label: '进行中', value: item?.inProgressApplications ?? 0 }, { label: '待处理', value: item?.pendingTasks ?? 0 }, { label: '未来面试', value: item?.upcomingInterviews ?? 0 }, { label: 'Offer', value: item?.offerApplications ?? 0 }, { label: '接受 Offer', value: item?.acceptedOffers ?? 0 }, { label: '面试转化', value: formatDashboardRate(item?.interviewConversionRate) }, { label: 'Offer 转化', value: formatDashboardRate(item?.offerConversionRate) }] })
const cycleRows = computed(() => { const item = cycle.value; if (!item) return []; return [{ label: '投递 → 首次面试', ...item.appliedToInterviewing }, { label: '投递 → 首次 Offer', ...item.appliedToOffered }, { label: '投递 → 终态', ...item.appliedToTerminal }].map((row) => ({ ...row, averageHours: Number(row.averageHours || 0).toFixed(2), minHours: Number(row.minHours || 0).toFixed(2), maxHours: Number(row.maxHours || 0).toFixed(2) })) })

async function reload() { await Promise.all([loadBlock('summary', () => fetchDashboardSummary(query), (value) => { summary.value = value }), loadBlock('funnel', () => fetchDashboardFunnel(query), (value) => { funnel.value = value }), loadBlock('trends', () => fetchDashboardTrends(query), (value) => { trends.value = value }), loadBlock('sources', () => fetchDashboardSources(query), (value) => { sources.value = value }), loadBlock('industries', () => fetchDashboardIndustries(query), (value) => { industries.value = value }), loadBlock('cycle', () => fetchDashboardCycleTime(query), (value) => { cycle.value = value }), loadBlock('upcoming', () => fetchDashboardUpcoming(query), (value) => { upcoming.value = value }), loadBlock('notifications', () => fetchRecentUnreadNotifications(), (value) => { notifications.value = value.records })]); await nextTick(); await renderCharts() }
async function loadBlock<T>(name: BlockName, loader: () => Promise<{ data: T }>, assign: (value: T) => void) { loading[name] = true; errors[name] = ''; try { assign((await loader()).data) } catch { errors[name] = 'load failed' } finally { loading[name] = false } }
async function loadOptions() { try { companies.value = (await fetchCompanyOptions()).data } catch { /* 筛选下拉失败不影响其余图表 */ } }
async function loadEcharts(): Promise<EchartsModule> {
  if (echartsCore) return echartsCore
  const [core, chartsModule, componentsModule, renderersModule] = await Promise.all([import('echarts/core'), import('echarts/charts'), import('echarts/components'), import('echarts/renderers')])
  core.use([chartsModule.BarChart, chartsModule.LineChart, chartsModule.PieChart, componentsModule.GridComponent, componentsModule.LegendComponent, componentsModule.TooltipComponent, renderersModule.CanvasRenderer])
  echartsCore = core
  return core
}
async function renderCharts() { const core = await loadEcharts(); renderChart(core, funnelElement.value, { tooltip: {}, xAxis: { type: 'value' }, yAxis: { type: 'category', data: funnel.value.map((item) => item.label) }, series: [{ type: 'bar', data: funnel.value.map((item) => item.count), itemStyle: { color: '#409eff' } }] }); renderChart(core, trendElement.value, { tooltip: { trigger: 'axis' }, xAxis: { type: 'category', data: trends.value.map((item) => item.bucket) }, yAxis: { type: 'value' }, series: [{ type: 'line', smooth: true, data: trends.value.map((item) => item.count), itemStyle: { color: '#67c23a' }, areaStyle: { opacity: 0.12 } }] }); renderChart(core, sourceElement.value, pieOption(sources.value)); renderChart(core, industryElement.value, pieOption(industries.value)) }
function pieOption(data: { name: string; count: number }[]): ChartOption { return { tooltip: { trigger: 'item' }, legend: { bottom: 0 }, series: [{ type: 'pie', radius: ['35%', '65%'], data: data.map((item) => ({ name: item.name, value: item.count })) }] } }
function renderChart(core: EchartsModule, element: HTMLElement | null, option: ChartOption) { if (!element) return; const chart = core.getInstanceByDom(element) || core.init(element); charts.add(chart); chart.setOption(option, true) }
function resizeCharts() { charts.forEach((chart) => chart.resize()) }
function formatDateTime(value: string | null): string { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-' }
onMounted(() => { void loadOptions(); void reload(); window.addEventListener('resize', resizeCharts) })
onBeforeUnmount(() => { window.removeEventListener('resize', resizeCharts); charts.forEach((chart) => chart.dispose()); charts.clear() })
</script>

<style scoped>
.filter-card { margin-bottom: 16px; }.separator { margin: 0 8px; color: #909399; }.summary-row { margin-bottom: 16px; }.summary-card { text-align: center; margin-bottom: 12px; }.summary-label { color: #909399; font-size: 13px; }.summary-value { margin-top: 8px; color: #303133; font-size: 24px; font-weight: 700; }.chart-card { margin-bottom: 16px; }.chart { height: 300px; }.lower-row { margin-top: 0; }.lower-row .el-card { margin-bottom: 16px; }.upcoming-item, .notification-item { padding: 10px 0; border-bottom: 1px solid #ebeef5; }.upcoming-title { font-weight: 600; }.upcoming-item small, .notification-item small { display: block; margin-top: 4px; color: #909399; }.notification-item { display: flex; justify-content: space-between; }.block-error { margin-bottom: 12px; }
</style>
