<template>
  <div class="home-container">
    <!-- Hero Section -->
    <div class="hero-section">
      <h1>欢迎来到 MyBlog</h1>
      <p>分享技术,记录成长</p>
      <el-button type="primary" size="large" @click="goToArticles">
        开始阅读
      </el-button>
      <el-button v-if="userStore.isLoggedIn" size="large" @click="goToEditor">
        写文章
      </el-button>
    </div>

    <!-- 最新文章 -->
    <div class="articles-section">
      <div class="section-header">
        <h2>最新文章</h2>
        <el-link type="primary" @click="goToArticles">查看更多 →</el-link>
      </div>

      <div class="article-grid" v-loading="loading">
        <el-card
          v-for="article in articles"
          :key="article.id"
          class="article-card"
          shadow="hover"
          @click="goToDetail(article.id)"
        >
          <div class="card-cover" v-if="article.coverImage">
            <el-image :src="article.coverImage" fit="cover" lazy />
          </div>
          <h3>{{ article.title }}</h3>
          <p>{{ article.summary }}</p>
          <div class="meta">
            <span>{{ article.authorName }}</span>
            <span>{{ formatTime(article.createTime) }}</span>
          </div>
          <div class="stats">
            <span><el-icon><View /></el-icon> {{ article.viewCount }}</span>
            <span><el-icon><Star /></el-icon> {{ article.likeCount }}</span>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { View, Star } from '@element-plus/icons-vue'
import { getArticleList, type ArticleListItem } from '@/api/article'

const router = useRouter()
const userStore = useUserStore()

const articles = ref<ArticleListItem[]>([])
const loading = ref(false)

// 加载最新文章
const loadArticles = async () => {
  loading.value = true
  try {
    const res = await getArticleList({
      page: 1,
      size: 6,
      orderBy: 'create_time',
      orderType: 'desc'
    })
    articles.value = res.records
  } catch (error) {
    console.error('加载文章失败', error)
  } finally {
    loading.value = false
  }
}

const goToArticles = () => {
  router.push('/articles')
}

const goToEditor = () => {
  router.push('/article/editor')
}

const goToDetail = (id: number) => {
  router.push({ name: 'article-detail', params: { id } })
}

const formatTime = (time: string) => {
  return new Date(time).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadArticles()
})
</script>

<style scoped lang="scss">
.home-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.hero-section {
  text-align: center;
  padding: 60px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
  margin-bottom: 40px;

  h1 {
    font-size: 48px;
    font-weight: 700;
    margin: 0 0 16px 0;
  }

  p {
    font-size: 20px;
    margin: 0 0 32px 0;
    opacity: 0.9;
  }

  .el-button {
    margin: 0 8px;
  }
}

.articles-section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;

    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
    }
  }
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.article-card {
  cursor: pointer;
  transition: transform 0.3s;

  &:hover {
    transform: translateY(-5px);
  }

  .card-cover {
    width: 100%;
    height: 180px;
    margin-bottom: 12px;
    border-radius: 4px;
    overflow: hidden;

    .el-image {
      width: 100%;
      height: 100%;
    }
  }

  h3 {
    margin: 0 0 10px;
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  p {
    margin: 0 0 15px;
    color: #606266;
    line-height: 1.6;
    font-size: 14px;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }

  .meta {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
  }

  .stats {
    display: flex;
    gap: 16px;
    font-size: 13px;
    color: #909399;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }
}

@media screen and (max-width: 1024px) {
  .article-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media screen and (max-width: 768px) {
  .hero-section h1 {
    font-size: 32px;
  }

  .article-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}
</style>
