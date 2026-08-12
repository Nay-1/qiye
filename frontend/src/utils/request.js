import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { useUserStore } from '@/stores/user'

const request = axios.create({ baseURL: '/api', timeout: 90000 })

request.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

request.interceptors.response.use(
  resp => {
    const res = resp.data
    if (res && res.code === 200) return res.data
    if (res && res.code === 401) {
      handleUnauthorized()
      return Promise.reject(res)
    }
    ElMessage.error(res?.message || '请求失败')
    return Promise.reject(res)
  },
  err => {
    const status = err.response?.status
    const msg = err.response?.data?.message
    if (status === 401) {
      handleUnauthorized()
    } else if (status === 403) {
      ElMessage.error(msg || '无权限执行该操作')
    } else {
      ElMessage.error(msg || err.message || '网络错误')
    }
    return Promise.reject(err)
  }
)

function handleUnauthorized() {
  const store = useUserStore()
  store.logout()
  ElMessage.warning('登录已过期，请重新登录')
  router.push('/login')
}

export default request
