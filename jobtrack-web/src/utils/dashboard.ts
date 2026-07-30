/** Dashboard 展示层的小型纯函数，避免后端异常值把页面渲染成 NaN。 */
export function formatDashboardRate(value: number | null | undefined): string {
  const safeValue = typeof value === 'number' && Number.isFinite(value) ? value : 0
  return `${safeValue.toFixed(2)}%`
}
