import { ref } from 'vue'

/**
 * 当前操作人（系统暂无登录体系，用本地身份切换模拟）。
 * 抽检提交人/复核人/复检人均取自此值：复核人不能复核自己提交的批次。
 */
const CURRENT_USER_KEY = 'wq_current_user'

export const currentUser = ref(localStorage.getItem(CURRENT_USER_KEY) || 'admin')

export function setCurrentUser(name) {
  currentUser.value = name
  localStorage.setItem(CURRENT_USER_KEY, name)
}

/** 生成幂等键：每次点击一个新 UUID；网络重试复用同一键，服务端只认第一次 */
export function newRequestId() {
  if (window.crypto && typeof window.crypto.randomUUID === 'function') {
    return window.crypto.randomUUID()
  }
  return 'req-' + Date.now() + '-' + Math.random().toString(16).slice(2)
}
