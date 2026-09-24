import axios from 'axios'
import { ElMessage } from 'element-plus'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code && res.code !== 200) {
      // 409：并发冲突/重复提交。统一提示，业务页面捕获后会自动刷新到最新状态
      ElMessage({
        type: res.code === 409 ? 'warning' : 'error',
        message: res.message || '请求失败',
        duration: 3500
      })
      const err = new Error(res.message || 'Error')
      err.code = res.code
      return Promise.reject(err)
    }
    return res
  },
  error => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
