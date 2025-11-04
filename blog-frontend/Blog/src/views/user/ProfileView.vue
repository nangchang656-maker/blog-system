<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('info')
const loading = ref(false)

// 加载用户信息
onMounted(async () => {
  if (!userStore.userInfo) {
    loading.value = true
    await userStore.getUserInfo()
    loading.value = false
  }
})

// 跳转到编辑页
const goToEdit = () => {
  router.push('/edit-profile')
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
          <p class="bio">{{ userStore.userInfo?.bio || '暂无个人简介' }}</p>
          <div class="stats">
            <div class="stat-item">
              <span class="count">{{ userStore.userInfo?.articleCount || 0 }}</span>
              <span class="label">文章</span>
            </div>
            <div class="stat-item">
              <span class="count">{{ userStore.userInfo?.followCount || 0 }}</span>
              <span class="label">关注</span>
            </div>
            <div class="stat-item">
              <span class="count">{{ userStore.userInfo?.fansCount || 0 }}</span>
              <span class="label">粉丝</span>
            </div>
          </div>
          <el-button type="primary" @click="goToEdit">编辑资料</el-button>
        </div>
      </div>
    </el-card>

    <!-- 详细信息标签页 -->
    <el-card v-if="userStore.userInfo" class="detail-card">
      <el-tabs v-model="activeTab">
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
              {{ userStore.userInfo?.bio || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="我的文章" name="articles">
          <el-empty description="暂无文章" />
        </el-tab-pane>

        <el-tab-pane label="收藏" name="collections">
          <el-empty description="暂无收藏" />
        </el-tab-pane>

        <el-tab-pane label="关注" name="following">
          <el-empty description="暂无关注" />
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
</style>
