import { refreshTokenApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// ==================== Token 刷新相关 ====================

// 是否正在刷新token
let isRefreshing = false
// 等待刷新token的请求队列
let refreshSubscribers: Array<(token: string) => void> = []

/**
 * 添加订阅者到队列
 */
function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb)
}

/**
 * 通知所有订阅者token已刷新
 */
function onRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token))
  refreshSubscribers = []
}

// ==================== 业务错误码处理 ====================

/**
 * 处理业务错误码，显示错误消息
 * @param code 业务错误码
 * @param message 错误消息
 */
function handleBusinessError(code: number, message: string) {
  const errorMessages: Record<number, string> = {
    400: '请求参数错误',
    403: '拒绝访问',
    404: '请求资源不存在',
    500: '服务器错误'
  }

  const defaultMessage = errorMessages[code] || '请求失败'
  ElMessage.error(message || defaultMessage)
}

/**
 * 处理业务错误（统一处理逻辑）
 * @param code 业务错误码
 * @param msg 错误消息
 */
function processBusinessError(code: number, msg: string) {
  const businessMessage = msg || '请求失败'
  handleBusinessError(code, businessMessage)
  return Promise.reject(new Error(businessMessage))
}

// ==================== Token 刷新逻辑 ====================

/**
 * 处理401未授权，尝试刷新token
 * @param originalRequest 原始请求配置
 */
async function handleUnauthorized(originalRequest: InternalAxiosRequestConfig & { _retry?: boolean }) {
  // 如果是刷新token的请求失败，直接跳转登录
  if (originalRequest.url?.includes('/refresh-token')) {
    ElMessage.error('登录已过期，请重新登录')
    const userStore = useUserStore()
    userStore.clearState()
    window.location.href = '/login'
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }

  // 如果正在刷新token，将请求加入队列
  if (isRefreshing) {
    return new Promise((resolve) => {
      subscribeTokenRefresh((token: string) => {
        originalRequest.headers.Authorization = `Bearer ${token}`
        resolve(request(originalRequest))
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
    userStore.clearState()
    window.location.href = '/login'
    return Promise.reject(new Error('登录已过期，请重新登录'))
  }

  try {
    // 调用刷新token方法
    const res = await refreshTokenApi({
      userId,
      refreshToken: userRefreshToken
    })

    // 更新accessToken（RefreshToken保持不变，无需更新）
    userStore.updateToken(res.accessToken)

    // 更新原始请求的token
    originalRequest.headers.Authorization = `Bearer ${res.accessToken}`

    // 通知所有等待的请求
    onRefreshed(res.accessToken)

    // 重试原始请求（使用request实例，确保经过拦截器处理）
    return request(originalRequest)
  } catch (refreshError) {
    // 刷新失败，说明RefreshToken也过期了，清理持久化数据
    ElMessage.error('登录已过期，请重新登录')
    userStore.clearState()
    // 延迟跳转，避免页面抖动，给用户展示提示
    setTimeout(() => {
      window.location.href = '/login'
    }, 1200)
    return Promise.reject(refreshError)
  } finally {
    isRefreshing = false
  }
}

// ==================== 创建 Axios 实例 ====================

const request: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
})

// ==================== 请求拦截器 ====================

request.interceptors.request.use(
  (config) => {
    // 从userStore获取token并添加到请求头
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

// ==================== 响应拦截器 ====================

request.interceptors.response.use(
  // 成功回调：处理 HTTP 200 响应
  async (response: AxiosResponse) => {
    const { code, data, msg } = response.data

    // 检查响应头中的自动续期Token（后端主动续期）
    const newToken = response.headers['x-new-token']
    if (newToken) {
      console.log('newToken', newToken)
      const userStore = useUserStore()
      userStore.updateToken(newToken)
      console.log('Token已自动续期')
    }

    // 业务成功：返回数据
    if (code === 200 || code === 0) {
      return data
    }

    // 业务错误：统一处理业务错误码（403、404、500等）
    return processBusinessError(code, msg)
  },
  // 错误回调：处理 HTTP 非 2xx 响应和网络错误
  async (error) => {
    // 网络层错误处理
    if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
      return Promise.reject(error)
    }

    if (error.message === 'Network Error') {
      ElMessage.error('网络连接失败，请检查网络')
      return Promise.reject(error)
    }

    // HTTP 错误处理
    if (error.response) {
      const { status, data } = error.response
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean }

      // 检查响应体是否包含业务错误码（后端统一使用R对象返回）
      if (data && typeof data === 'object' && 'code' in data && 'msg' in data) {
        const { code, msg } = data as { code: number; msg: string }

        // 401 未授权：特殊处理（尝试刷新token）
        if (code === 401 && !originalRequest._retry) {
          return handleUnauthorized(originalRequest)
        }

        // 其他业务错误码：统一处理（兜底逻辑，理论上不应该出现）
        return processBusinessError(code, msg)
      }

      // 如果响应体不包含业务错误码，使用HTTP状态码处理（真正的HTTP错误）
      handleBusinessError(status, '')
      return Promise.reject(error)
    }

    // 其他未知错误
    ElMessage.error('请求失败，请稍后重试')
    return Promise.reject(error)
  }
)

export default request
