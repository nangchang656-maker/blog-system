<template>
  <div class="article-detail-container">
    <el-card v-loading="loading">
      <template v-if="article">
        <!-- 文章头部 -->
        <div class="article-header">
          <h1 class="article-title">{{ article.title }}</h1>

          <div class="article-author">
            <el-avatar :size="40" :src="article.authorAvatar">
              {{ article.authorName?.charAt(0) }}
            </el-avatar>
            <div class="author-info">
              <div class="author-name">{{ article.authorName }}</div>
              <div class="article-time">{{ formatTime(article.createTime) }}</div>
            </div>
          </div>

          <div class="article-stats">
            <span class="stat-item">
              <el-icon><View /></el-icon>
              {{ article.viewCount }} 阅读
            </span>
            <span class="stat-item">
              <el-icon><Star /></el-icon>
              {{ article.likeCount }} 点赞
            </span>
            <span class="stat-item">
              <el-icon><Collection /></el-icon>
              {{ article.collectCount }} 收藏
            </span>
            <span class="stat-item">
              <el-icon><ChatDotRound /></el-icon>
              {{ article.commentCount }} 评论
            </span>
          </div>

          <div class="article-tags">
            <el-tag type="primary">{{ article.categoryName }}</el-tag>
            <el-tag v-for="tag in article.tags" :key="tag.id" style="margin-left: 8px">
              {{ tag.name }}
            </el-tag>
          </div>
        </div>

        <!-- 文章内容 -->
        <el-divider />
        <div class="article-content markdown-body" v-html="renderedContent"></div>

        <!-- 操作按钮 -->
        <el-divider />
        <div class="article-actions">
          <el-button
            :type="article.isLiked ? 'primary' : 'default'"
            :icon="Star"
            @click="handleLike"
          >
            {{ article.isLiked ? '已点赞' : '点赞' }} ({{ article.likeCount }})
          </el-button>
          <el-button
            :type="article.isCollected ? 'warning' : 'default'"
            :icon="Collection"
            @click="handleCollect"
          >
            {{ article.isCollected ? '已收藏' : '收藏' }}
          </el-button>
          <el-button :icon="Share" @click="handleShare">分享</el-button>
        </div>
      </template>
    </el-card>

    <!-- 评论区(待实现) -->
    <el-card class="comment-section" style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span>评论 ({{ article?.commentCount || 0 }})</span>
        </div>
      </template>
      <el-empty description="评论功能待开发" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { View, Star, ChatDotRound, Collection, Share } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import {
  getArticleDetail,
  likeArticle,
  unlikeArticle,
  collectArticle,
  uncollectArticle,
  type ArticleDetail
} from '@/api/article'

const route = useRoute()
const article = ref<ArticleDetail | null>(null)
const loading = ref(false)

// Markdown渲染
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return ''
  }
})

const renderedContent = computed(() => {
  return article.value?.content ? md.render(article.value.content) : ''
})

// 加载文章详情
const loadArticle = async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res = await getArticleDetail(id)
    article.value = res
  } catch (error) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

// 点赞
const handleLike = async () => {
  if (!article.value) return

  try {
    if (article.value.isLiked) {
      await unlikeArticle(article.value.id)
      article.value.isLiked = false
      article.value.likeCount--
      ElMessage.success('取消点赞')
    } else {
      await likeArticle(article.value.id)
      article.value.isLiked = true
      article.value.likeCount++
      ElMessage.success('点赞成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 收藏
const handleCollect = async () => {
  if (!article.value) return

  try {
    if (article.value.isCollected) {
      await uncollectArticle(article.value.id)
      article.value.isCollected = false
      article.value.collectCount = (article.value.collectCount || 1) - 1
      ElMessage.success('取消收藏')
    } else {
      await collectArticle(article.value.id)
      article.value.isCollected = true
      article.value.collectCount = (article.value.collectCount || 0) + 1
      ElMessage.success('收藏成功')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 分享
const handleShare = () => {
  // 复制链接到剪贴板
  const url = window.location.href
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  })
}

// 格式化时间
const formatTime = (time: string) => {
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped lang="scss">
.article-detail-container {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
}

.article-header {
  .article-title {
    font-size: 32px;
    font-weight: 600;
    margin: 0 0 20px 0;
    color: #303133;
    line-height: 1.4;
  }

  .article-author {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 16px;

    .author-info {
      .author-name {
        font-weight: 500;
        color: #303133;
      }

      .article-time {
        font-size: 14px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .article-stats {
    display: flex;
    gap: 24px;
    margin-bottom: 16px;
    font-size: 14px;
    color: #606266;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .article-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

.article-content {
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  min-height: 300px;

  :deep(img) {
    max-width: 100%;
    height: auto;
    border-radius: 4px;
  }

  :deep(pre) {
    padding: 16px;
    border-radius: 4px;
    overflow-x: auto;
  }

  :deep(code) {
    font-family: 'Courier New', monospace;
  }

  :deep(blockquote) {
    padding: 12px 16px;
    margin: 16px 0;
    border-left: 4px solid #409eff;
    background-color: #f4f4f5;
  }

  :deep(table) {
    width: 100%;
    border-collapse: collapse;
    margin: 16px 0;

    th,
    td {
      border: 1px solid #dcdfe6;
      padding: 8px 12px;
    }

    th {
      background-color: #f4f4f5;
    }
  }
}

.article-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.comment-section {
  .card-header {
    font-size: 18px;
    font-weight: 600;
  }
}
</style>
