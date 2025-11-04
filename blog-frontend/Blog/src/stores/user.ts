import { defineStore } from 'pinia'
import { ref } from 'vue'
import { loginApi, logoutApi, getUserInfoApi } from '@/api/user'
import type { LoginParams, UserInfo } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshToken = ref<string>(localStorage.getItem('refreshToken') || '')
  const userId = ref<number | null>(
    localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null
  )
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = ref<boolean>(!!token.value)

  // 登录
  const login = async (params: LoginParams) => {
    try {
      const res = await loginApi(params)

      // 保存 token 和 refreshToken
      token.value = res.token
      refreshToken.value = res.refreshToken
      userId.value = res.userInfo.id
      userInfo.value = res.userInfo
      isLoggedIn.value = true

      // 持久化到 localStorage
      localStorage.setItem('token', res.token)
      localStorage.setItem('refreshToken', res.refreshToken)
      localStorage.setItem('userId', String(res.userInfo.id))

      ElMessage.success('登录成功')
      return true
    } catch (error) {
      return false
    }
  }

  // 退出登录
  const logout = async () => {
    try {
      await logoutApi()
    } catch (error) {
      // 忽略退出接口错误
    } finally {
      token.value = ''
      refreshToken.value = ''
      userId.value = null
      userInfo.value = null
      isLoggedIn.value = false

      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userId')

      ElMessage.success('已退出登录')
    }
  }

  // 获取用户信息
  const getUserInfo = async () => {
    try {
      const res = await getUserInfoApi()
      userInfo.value = res
      return res
    } catch (error) {
      return null
    }
  }

  // 更新 token（用于刷新token后更新）
  const updateToken = (newToken: string, newRefreshToken?: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)

    if (newRefreshToken) {
      refreshToken.value = newRefreshToken
      localStorage.setItem('refreshToken', newRefreshToken)
    }
  }

  return {
    token,
    refreshToken,
    userId,
    userInfo,
    isLoggedIn,
    login,
    logout,
    getUserInfo,
    updateToken
  }
})
