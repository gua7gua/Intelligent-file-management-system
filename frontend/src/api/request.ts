import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// 递归清理空字符串：后端 LocalDate/枚举等字段不接受空串，统一转为 null
function cleanEmptyStrings(obj: any): any {
  if (Array.isArray(obj)) return obj.map(cleanEmptyStrings)
  if (obj && typeof obj === 'object') {
    const out: any = {}
    for (const [k, v] of Object.entries(obj)) {
      out[k] = v === '' ? null : cleanEmptyStrings(v)
    }
    return out
  }
  return obj
}

// 请求拦截器：注入 token
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    // POST/PUT body 递归清理空字符串，避免空串导致后端 LocalDate 等反序列化 400
    // 跳过 FormData/Blob：Object.entries 无法枚举 FormData，会把 multipart 上传体清成空对象 {}，
    // 导致后端报 "Current request is not a multipart request"（人#13 销毁 photos）。
    if (config.data && typeof config.data === 'object' && !(config.data instanceof FormData) && !(config.data instanceof Blob)) {
      config.data = cleanEmptyStrings(config.data)
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：解包响应、统一错误处理
request.interceptors.response.use(
  (response) => {
    // 文件流（blob）直接透传：xlsx/pdf 等导出场景，response.data 是 Blob 而非 R<T>
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response.data
    }
    const res = response.data
    if (res.code === 'OK') {
      return res.data
    }
    const message = res.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const { response } = error
    if (response?.status === 401) {
      // 登录接口的 401 是「账号或密码错误」（凭证错误），不是 token 过期：
      // 不登出、不跳转、不弹「登录已过期」，交由登录页自身展示账号密码错误提示。
      const url = (error.config?.url || '') as string
      if (url.includes('/auth/login')) {
        return Promise.reject(error)
      }
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (response?.data?.message) {
      ElMessage.error(response.data.message)
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request
