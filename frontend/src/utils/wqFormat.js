/** 时间格式化（兼容后端数组/字符串/ISO） */
export function formatTime(t) {
  if (!t) return '—'
  let d
  if (Array.isArray(t)) {
    d = new Date(t[0], (t[1] || 1) - 1, t[2] || 1, t[3] || 0, t[4] || 0, t[5] || 0)
  } else {
    d = new Date(t)
  }
  if (isNaN(d.getTime())) return String(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
