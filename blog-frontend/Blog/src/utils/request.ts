import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

// 是否正在刷新token
let isRefreshing = false
// 等待刷新token的请求队列
let refreshSubscribers: Array<(token: string) => void> = []

// 添加订阅者到队列
function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

// 通知所有订阅者token已刷新
function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

// 创建一个独立的axios实例用于刷新token（不使用拦截器）
const refreshTokenRequest = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000
})

// 刷新token的专用方法（避免循环）
async function refreshToken(userId: number, refreshToken: string) {
  const response = await refreshTokenRequest.post('/api/user/refresh-token', {
    userId,
    refreshToken
  })
  return response.data.data // 返回 { accessToken, refreshToken, ... }
}

// 创建axios实例
const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    // 从userStore获取token
    const userStore = useUserStore()
    const token = userStore.token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, data, message } = response.data

    // **重要：检查响应头中的自动续期Token（后端主动续期）**
    const newToken = response.headers['x-new-token']
    if (newToken) {
      const userStore = useUserStore()
      userStore.updateToken(newToken)
      console.log('Token已自动续期')
    }

    // 成功
    if (code === 200 || code === 0) {
      return data
    }

    // 业务错误
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message || '请求失败'))
  },
  async (error) => {
    const originalRequest = error.config

    // 网络错误处理
    if (error.response) {
      const { status, data } = error.response

      // 401 未授权，尝试刷新token
      if (status === 401 && !originalRequest._retry) {
        // 如果是刷新token的请求失败，直接跳转登录
        if (originalRequest.url?.includes('/refresh-token')) {
          ElMessage.error('登录已过期，请重新登录')
          const userStore = useUserStore()
          userStore.logout()
          window.location.href = '/login'
          return Promise.reject(error)
        }

        // 如果正在刷新token，将请求加入队列
        if (isRefreshing) {
          return new Promise((resolve) => {
            subscribeTokenRefresh((token: string) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              resolve(axios(originalRequest))
            })
          })
        }

        originalRequest._retry = true
        isRefreshing = true

        const userStore = useUserStore()
        const { userId, refreshToken: userRefreshToken } = userStore

        // 检查是否有refreshToken
        if (!userId || !userRefreshToken) {
          isRefreshing = false
          ElMessage.error('登录已过期，请重新登录')
          userStore.logout()
          window.location.href = '/login'
          return Promise.reject(error)
        }

        try {
          // 调用刷新token方法（使用独立实例）
          const res = await refreshToken(userId, userRefreshToken)

          // 更新token
          userStore.updateToken(res.accessToken, res.refreshToken)

          // 更新原始请求的token
          originalRequest.headers.Authorization = `Bearer ${res.accessToken}`

          // 通知所有等待的请求
          onRefreshed(res.accessToken)

          // 重试原始请求
          return axios(originalRequest)
        } catch (refreshError) {
          // 刷新失败，清空登录状态
          ElMessage.error('登录已过期，请重新登录')
          userStore.logout()
          window.location.href = '/login'
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }

      // 其他状态码处理
      switch (status) {
        case 400:
          ElMessage.error(data?.msg || data?.message || '请求参数错误')
          break
        case 403:
          ElMessage.error('拒绝访问')
          break
        case 404:
          ElMessage.error('请求资源不存在')
          break
        case 500:
          ElMessage.error(data?.message || data?.msg || '服务器错误')
          break
        default:
          ElMessage.error(data?.message || data?.msg || '请求失败')
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.message === 'Network Error') {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('请求失败，请稍后重试')
    }

    return Promise.reject(error)
  }
)

export default request
