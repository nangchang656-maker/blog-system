import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
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
      path: '/my-articles',
      name: 'my-articles',
      component: () => import('../views/article/MyArticlesView.vue'),
      meta: { title: '我的文章', requiresAuth: true }
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
      path: '/article/:id',
      name: 'article-detail',
      component: () => import('../views/article/ArticleDetailView.vue'),
      meta: { title: '文章详情' }
    },
    // 管理员模块路由
    {
      path: '/admin',
      name: 'admin',
      component: () => import('../views/admin/AdminView.vue'),
      meta: { title: '管理后台', requiresAuth: true, requiresAdmin: true },
      redirect: '/admin/statistics',
      children: [
        {
          path: 'statistics',
          name: 'admin-statistics',
          component: () => import('../views/admin/StatisticsView.vue'),
          meta: { title: '数据统计', requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'articles',
          name: 'admin-articles',
          component: () => import('../views/admin/ArticleManageView.vue'),
          meta: { title: '文章管理', requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('../views/admin/UserManageView.vue'),
          meta: { title: '访客管理', requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'comments',
          name: 'admin-comments',
          component: () => import('../views/admin/CommentManageView.vue'),
          meta: { title: '评论管理', requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('../views/admin/CategoryManageView.vue'),
          meta: { title: '分类管理', requiresAuth: true, requiresAdmin: true }
        },
        {
          path: 'tags',
          name: 'admin-tags',
          component: () => import('../views/admin/TagManageView.vue'),
          meta: { title: '标签管理', requiresAuth: true, requiresAdmin: true }
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - MyBlog`
  }

  const userStore = useUserStore()

  // 检查是否需要登录
  if (to.meta.requiresAuth) {
    if (!userStore.isLoggedIn) {
      next('/login')
      return
    }
  }

  // 检查是否需要管理员权限
  if (to.meta.requiresAdmin) {
    if (!userStore.isAdmin) {
      ElMessage.error('无权限访问')
      next('/')
      return
    }
  }

  next()
})

export default router
