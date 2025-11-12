import type { LoginParams, UserInfo } from '@/api/user'
import { getUserInfoApi, loginApi, logoutApi } from '@/api/user'
import { ADMIN_USER_IDS } from '@/constants/user'
import { ElMessage } from 'element-plus'
import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

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

    // 计算属性：是否为管理员（ID为1或2）
    const isAdmin = computed(() => ADMIN_USER_IDS.includes(userId.value ?? -1))

    // 登录
    const login = async (params: LoginParams) => {
      try {
        const res = await loginApi(params)

        // 保存 token 和用户信息（Pinia持久化插件会自动保存到localStorage）
        token.value = res.accessToken
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

    // 更新 token（用于刷新token后更新，RefreshToken保持不变）
    const updateToken = (newToken: string) => {
      token.value = newToken
    }

    // 清空前端状态（不调用后端接口，用于token过期等情况）
    const clearState = () => {
      token.value = ''
      refreshToken.value = ''
      userId.value = null
      userInfo.value = null
    }

    return {
      token,
      refreshToken,
      userId,
      userInfo,
      isLoggedIn,
      isAdmin,
      login,
      logout,
      getUserInfo,
      updateToken,
      clearState
    }
  },
  {
    // 配置持久化
    persist: {
      key: 'user-store',
      storage: localStorage,
      // 只持久化必要的字段
      pick: ['token', 'refreshToken', 'userId', 'userInfo']
    }
  }
)
