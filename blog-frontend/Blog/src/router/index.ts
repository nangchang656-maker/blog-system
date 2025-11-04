import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
      meta: { title: '首页' }
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/user/LoginView.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/user/RegisterView.vue'),
      meta: { title: '注册' }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/user/ProfileView.vue'),
      meta: { title: '个人中心', requiresAuth: true }
    },
    {
      path: '/edit-profile',
      name: 'edit-profile',
      component: () => import('../views/user/EditProfileView.vue'),
      meta: { title: '编辑资料', requiresAuth: true }
    }
  ],
})

// 路由守卫（可选，后续需要时启用）
// router.beforeEach((to, from, next) => {
//   // 设置页面标题
//   if (to.meta.title) {
//     document.title = `${to.meta.title} - MyBlog`
//   }
//
//   // 检查是否需要登录
//   if (to.meta.requiresAuth) {
//     const isLoggedIn = false // 从 pinia store 获取登录状态
//     if (!isLoggedIn) {
//       next('/login')
//       return
//     }
//   }
//
//   next()
// })

export default router
