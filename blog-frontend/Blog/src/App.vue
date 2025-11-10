<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 初始化时加载用户信息
onMounted(async () => {
  // 如果有 token 但没有用户信息，尝试自动加载
  // Pinia持久化插件会自动从localStorage恢复token和userId
  if (userStore.isLoggedIn && !userStore.userInfo) {
    try {
      await userStore.getUserInfo()
      console.log('用户信息加载成功')
    } catch (error) {
      console.error('用户信息加载失败，可能token已过期')
      // 如果获取用户信息失败，说明token可能已失效
      // 401错误会被axios拦截器处理，自动尝试刷新token
    }
  }
})

// 退出登录
const handleLogout = async () => {
  await userStore.logout()
  router.push('/')
}
</script>

<template>
  <el-container class="layout-container">
    <el-header class="header" height="60px">
      <div class="header-content">
        <!-- Logo -->
        <div class="logo">
          <RouterLink to="/">MyBlog</RouterLink>
        </div>

        <!-- 导航菜单 -->
        <el-menu mode="horizontal" class="nav-menu" :ellipsis="false">
          <el-menu-item index="1">
            <RouterLink to="/">首页</RouterLink>
          </el-menu-item>
          <el-menu-item index="2">
            <RouterLink to="/articles">文章</RouterLink>
          </el-menu-item>
          <el-menu-item index="3" v-if="userStore.isLoggedIn">
            <RouterLink to="/my-articles">我的文章</RouterLink>
          </el-menu-item>
          <el-menu-item index="4" v-if="userStore.isLoggedIn">
            <RouterLink to="/profile">个人中心</RouterLink>
          </el-menu-item>
          <el-menu-item index="5" v-if="userStore.isLoggedIn && userStore.isAdmin">
            <RouterLink to="/admin">管理后台</RouterLink>
          </el-menu-item>
        </el-menu>

        <!-- 用户操作区 -->
        <div class="user-actions">
          <template v-if="!userStore.isLoggedIn">
            <RouterLink to="/login">
              <el-button type="primary" size="default">登录</el-button>
            </RouterLink>
            <RouterLink to="/register">
              <el-button size="default">注册</el-button>
            </RouterLink>
          </template>
          <template v-else>
            <el-dropdown>
              <el-avatar :size="40" :src="userStore.userInfo?.avatar || 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'" />
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <RouterLink to="/profile">个人信息</RouterLink>
                  </el-dropdown-item>
                  <el-dropdown-item>
                    <RouterLink to="/edit-profile">修改信息</RouterLink>
                  </el-dropdown-item>
                  <el-dropdown-item v-if="userStore.isAdmin" divided>
                    <RouterLink to="/admin">管理后台</RouterLink>
                  </el-dropdown-item>
                  <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <RouterView />
    </el-main>

    <el-footer class="footer" height="60px">
      <p>© 2025 MyBlog. All rights reserved.</p>
    </el-footer>
  </el-container>
</template>

<style scoped>
.layout-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
  height: 60px !important;
  line-height: 60px;
  width: 100%;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  width: 100%;
}

.logo {
  flex-shrink: 0;
}

.logo a {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  text-decoration: none;
  white-space: nowrap;
}

.logo a:hover {
  opacity: 0.8;
}

.nav-menu {
  flex: 1;
  margin: 0 20px;
  border-bottom: none;
  min-width: 0;
}

.nav-menu a {
  color: inherit;
  text-decoration: none;
  display: block;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.user-actions a {
  text-decoration: none;
}

.user-actions .el-dropdown a {
  color: inherit;
}

.main-content {
  flex: 1;
  background-color: #f5f5f5;
  padding: 20px;
  width: 100%;
  overflow-x: hidden;
}

.footer {
  background-color: #fff;
  text-align: center;
  color: #666;
  border-top: 1px solid #e8e8e8;
  height: 60px !important;
  line-height: 60px;
  width: 100%;
}

.footer p {
  margin: 0;
}

/* 响应式布局 - 平板 */
@media screen and (max-width: 1024px) {
  .nav-menu {
    margin: 0 15px;
  }
}

/* 响应式布局 - 手机 */
@media screen and (max-width: 768px) {
  .header {
    padding: 0 10px;
  }

  .logo span {
    font-size: 20px;
  }

  .nav-menu {
    margin: 0 10px;
  }

  .user-actions {
    gap: 5px;
  }

  .main-content {
    padding: 10px;
  }
}
</style>

