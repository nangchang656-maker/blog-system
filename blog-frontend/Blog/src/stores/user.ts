import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, logoutApi, getUserInfoApi } from '@/api/user'
import type { LoginParams, UserInfo } from '@/api/user'
import { ElMessage } from 'element-plus'

export const useUserStore = defineStore(
  'user',
  () => {
    // 状态
    const token = ref<string>('')
    const refreshToken = ref<string>('')
    const userId = ref<number | null>(null)
    const userInfo = ref<UserInfo | null>(null)

    // 计算属性：是否已登录
    const isLoggedIn = computed(() => !!token.value)

    // 登录
    const login = async (params: LoginParams) => {
      try {
        const res = await loginApi(params)

        // 保存 token 和用户信息（Pinia持久化插件会自动保存到localStorage）
        token.value = res.token
        refreshToken.value = res.refreshToken
        userId.value = res.userInfo.id
        userInfo.value = res.userInfo

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
        // 清空状态（Pinia持久化插件会自动清除localStorage）
        token.value = ''
        refreshToken.value = ''
        userId.value = null
        userInfo.value = null

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

      if (newRefreshToken) {
        refreshToken.value = newRefreshToken
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
  },
  {
    // 配置持久化
    persist: {
      key: 'user-store',
      storage: localStorage,
      // 只持久化必要的字段
      paths: ['token', 'refreshToken', 'userId', 'userInfo']
    }
  }
)
