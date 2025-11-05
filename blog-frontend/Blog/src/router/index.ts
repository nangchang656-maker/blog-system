import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import { useUserStore } from '@/stores/user'

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
    },
    // 文章模块路由
    {
      path: '/articles',
      name: 'article-list',
      component: () => import('../views/article/ArticleListView.vue'),
      meta: { title: '文章列表' }
    },
    {
      path: '/article/:id',
      name: 'article-detail',
      component: () => import('../views/article/ArticleDetailView.vue'),
      meta: { title: '文章详情' }
    },
    {
      path: '/article/editor',
      name: 'article-editor',
      component: () => import('../views/article/ArticleEditorView.vue'),
      meta: { title: '写文章', requiresAuth: true }
    },
    {
      path: '/article/editor/:id',
      name: 'article-editor-edit',
      component: () => import('../views/article/ArticleEditorView.vue'),
      meta: { title: '编辑文章', requiresAuth: true }
    },
    {
      path: '/my-articles',
      name: 'my-articles',
      component: () => import('../views/article/MyArticlesView.vue'),
      meta: { title: '我的文章', requiresAuth: true }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - MyBlog`
  }

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    const userStore = useUserStore()
    if (!userStore.isLoggedIn) {
      next('/login')
      return
    }
  }

  next()
})

export default router
