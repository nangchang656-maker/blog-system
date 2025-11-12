<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMyCollections, type ArticleListItem } from '@/api/article'
import { ElMessage } from 'element-plus'
import { View, Star } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeTab = ref('info')
const loading = ref(false)

// 收藏列表相关
const collections = ref<ArticleListItem[]>([])
const collectionsLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载用户信息
const loadUserInfo = async () => {
  if (userStore.isLoggedIn) {
    loading.value = true
    try {
      await userStore.getUserInfo()
    } catch (error) {
      console.error('加载用户信息失败', error)
    } finally {
      loading.value = false
    }
  }
}

onMounted(() => {
  loadUserInfo()
})

// 监听路由变化，当进入个人中心页面时刷新用户信息
watch(
  () => route.path,
  (newPath) => {
    if (newPath === '/profile') {
      loadUserInfo()
    }
  }
)

// 加载收藏列表
const loadCollections = async () => {
  collectionsLoading.value = true
  try {
    const res: any = await getMyCollections({
      page: currentPage.value,
      size: pageSize.value
    })
    collections.value = res.records || []
    total.value = res.total || 0
  } catch (error) {
    ElMessage.error('加载收藏列表失败')
  } finally {
    collectionsLoading.value = false
  }
}

// 监听标签页切换
const handleTabChange = (tabName: string) => {
  if (tabName === 'collections' && collections.value.length === 0) {
    loadCollections()
  }
}

// 分页变化
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadCollections()
}

// 跳转到编辑页
const goToEdit = () => {
  router.push('/edit-profile')
}

// 跳转到文章详情
const goToArticle = (id: number) => {
  router.push(`/article/${id}`)
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}
</script>

<template>
  <div class="profile-container" v-loading="loading">
    <el-empty v-if="!userStore.userInfo && !loading" description="请先登录" />
    <el-card v-else class="profile-card">
      <!-- 用户头部信息 -->
      <div class="user-header">
        <el-avatar :size="100" :src="userStore.userInfo?.avatar" />
        <div class="user-info">
          <h2>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</h2>
          <p class="username">@{{ userStore.userInfo?.username }}</p>
          <p class="bio">{{ userStore.userInfo?.intro || '暂无个人简介' }}</p>
          <div class="stats">
            <div class="stat-item">
              <span class="count">{{ userStore.userInfo?.articleCount || 0 }}</span>
              <span class="label">文章</span>
            </div>
            <div class="stat-item">
              <span class="count">{{ userStore.userInfo?.collectCount || 0 }}</span>
              <span class="label">收藏</span>
            </div>
          </div>
          <el-button type="primary" @click="goToEdit">编辑资料</el-button>
        </div>
      </div>
    </el-card>

    <!-- 详细信息标签页 -->
    <el-card v-if="userStore.userInfo" class="detail-card">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="个人信息" name="info">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="用户ID">
              {{ userStore.userInfo?.id }}
            </el-descriptions-item>
            <el-descriptions-item label="用户名">
              {{ userStore.userInfo?.username }}
            </el-descriptions-item>
            <el-descriptions-item label="昵称">
              {{ userStore.userInfo?.nickname || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ userStore.userInfo?.email }}
            </el-descriptions-item>
            <el-descriptions-item label="手机号">
              {{ userStore.userInfo?.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              {{ userStore.userInfo?.createTime || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="个人简介">
              {{ userStore.userInfo?.intro || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="收藏" name="collections">
          <div v-loading="collectionsLoading">
            <el-empty v-if="collections.length === 0 && !collectionsLoading" description="暂无收藏" />
            <div v-else class="collection-list">
              <div
                v-for="article in collections"
                :key="article.id"
                class="collection-item"
                @click="goToArticle(article.id)"
              >
                <el-image
                  v-if="article.coverImage"
                  :src="article.coverImage"
                  class="cover"
                  fit="cover"
                />
                <div class="content">
                  <h3 class="title">{{ article.title }}</h3>
                  <p class="summary">{{ article.summary }}</p>
                  <div class="meta">
                    <span>{{ article.authorName }}</span>
                    <span>{{ formatTime(article.createTime) }}</span>
                    <span>
                      <el-icon><View /></el-icon>
                      {{ article.viewCount }}
                    </span>
                    <span>
                      <el-icon><Star /></el-icon>
                      {{ article.likeCount }}
                    </span>
                  </div>
                </div>
              </div>
              <el-pagination
                v-if="total > pageSize"
                :current-page="currentPage"
                :page-size="pageSize"
                :total="total"
                layout="prev, pager, next"
                @current-change="handlePageChange"
                style="margin-top: 20px; text-align: center"
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.profile-container {
  max-width: 1000px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.user-header {
  display: flex;
  gap: 30px;
  padding: 20px;
}

.user-info {
  flex: 1;
}

.user-info h2 {
  margin: 0 0 5px;
  font-size: 28px;
  color: #303133;
}

.username {
  margin: 0 0 10px;
  color: #909399;
  font-size: 14px;
}

.bio {
  margin: 0 0 20px;
  color: #606266;
  line-height: 1.6;
}

.stats {
  display: flex;
  gap: 40px;
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat-item .count {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 5px;
}

.stat-item .label {
  font-size: 14px;
  color: #909399;
}

.detail-card {
  border-radius: 8px;
}

.collection-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.collection-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.collection-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.15);
}

.collection-item .cover {
  width: 120px;
  height: 80px;
  border-radius: 4px;
  flex-shrink: 0;
}

.collection-item .content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.collection-item .title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.collection-item .summary {
  margin: 0;
  font-size: 14px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.collection-item .meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}

.collection-item .meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
