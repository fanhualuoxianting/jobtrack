/**
 * 全站统一的枚举文本与标签颜色映射。
 * 新增状态/类型时在此处集中维护，避免散落各组件。
 */

export const WORK_TYPE_TEXT: Record<string, string> = {
  INTERNSHIP: '实习',
  FULL_TIME: '全职',
  PART_TIME: '兼职',
}

export const WORKPLACE_TEXT: Record<string, string> = {
  ONSITE: '现场办公',
  REMOTE: '远程',
  HYBRID: '混合办公',
}

export const POSITION_STATUS_TEXT: Record<string, string> = {
  OPEN: '招聘中',
  CLOSED: '已关闭',
}

export const POSITION_STATUS_TAG: Record<string, 'success' | 'info'> = {
  OPEN: 'success',
  CLOSED: 'info',
}

export const SALARY_UNIT_TEXT: Record<string, string> = {
  DAY: '元/天',
  MONTH: '元/月',
  YEAR: '元/年',
}

/** 金额格式化（空值显示 -） */
export function formatSalary(min: number | null, max: number | null, unit: string | null): string {
  if (min == null && max == null) return '-'
  const unitText = unit ? (SALARY_UNIT_TEXT[unit] ?? unit) : ''
  if (min != null && max != null) return `${min}-${max}${unitText}`
  return `${min ?? max}${unitText}`
}

/** 表格空值兜底 */
export function dash(value: string | null | undefined): string {
  return value && value.trim() ? value : '-'
}
