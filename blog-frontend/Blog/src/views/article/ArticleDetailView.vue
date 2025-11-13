<template>
  <div class="article-detail-container">
    <el-card v-loading="loading">
      <template v-if="article">
        <!-- 文章头部 -->
        <div class="article-header">
          <div class="header-actions">
            <el-button 
              type="primary" 
              :icon="ArrowLeft" 
              @click="handleBack"
              size="small"
            >
              返回
            </el-button>
          </div>
          <h1 class="article-title" v-html="highlightedTitle"></h1>

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
        <div class="article-content markdown-body" v-html="highlightedContent"></div>

        <!-- 操作按钮 -->
        <el-divider />
        <div class="article-actions">
          <el-button
            :type="article.isLiked === true ? 'primary' : 'default'"
            :icon="Star"
            @click="handleLike"
          >
            {{ article.isLiked === true ? '已点赞' : '点赞' }} ({{ article.likeCount }})
          </el-button>
          <el-button
            :type="article.isCollected === true ? 'warning' : 'default'"
            :icon="Collection"
            @click="handleCollect"
          >
            {{ article.isCollected === true ? '已收藏' : '收藏' }}
          </el-button>
          <el-button :icon="Share" @click="handleShare">分享</el-button>
          <el-button :icon="Document" @click="handleViewSummary" v-if="article.summary">
            查看摘要
          </el-button>
          <el-button :icon="List" @click="handleViewOutline" :loading="generatingOutline">
            查看大纲
          </el-button>
        </div>
      </template>
    </el-card>

    <!-- 评论区 -->
    <el-card class="comment-section" style="margin-top: 20px" v-if="article">
      <template #header>
        <div class="card-header">
          <span>评论 ({{ article.commentCount || 0 }})</span>
        </div>
      </template>
      <CommentSection :article-id="article.id" ref="commentSectionRef" @comment-added="handleCommentAdded" />
    </el-card>

    <!-- 摘要对话框 -->
    <el-dialog
      v-model="showSummaryDialog"
      title="文章摘要"
      width="500px"
    >
      <div class="summary-content">
        <p>{{ article?.summary }}</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="showSummaryDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 大纲对话框 -->
    <el-dialog
      v-model="showOutlineDialog"
      title="文章大纲"
      width="600px"
    >
      <div class="outline-preview" v-loading="generatingOutline">
        <div class="outline-content markdown-body" v-html="renderedOutline" v-if="generatedOutline"></div>
        <el-empty v-else description="暂无大纲" />
      </div>
      <template #footer>
        <el-button type="primary" @click="showOutlineDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { View, Star, ChatDotRound, Collection, Share, Document, List, ArrowLeft } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import {
  getArticleDetail,
  likeArticle,
  unlikeArticle,
  collectArticle,
  uncollectArticle,
  generateOutlineFromContent,
  type ArticleDetail
} from '@/api/article'
import { useUserStore } from '@/stores/user'
import CommentSection from '@/components/CommentSection.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const article = ref<ArticleDetail | null>(null)
const loading = ref(false)
const commentSectionRef = ref<InstanceType<typeof CommentSection> | null>(null)

// 搜索关键词（从路由query参数获取）
const searchKeyword = computed(() => {
  return (route.query.keyword as string) || ''
})

// 摘要和大纲相关
const showSummaryDialog = ref(false)
const showOutlineDialog = ref(false)
const generatedOutline = ref('')
const generatingOutline = ref(false)

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

const renderedOutline = computed(() => {
  return generatedOutline.value ? md.render(generatedOutline.value) : ''
})

// 高亮关键词的函数
const highlightKeyword = (text: string, keyword: string): string => {
  if (!keyword || !text) return text
  // 转义HTML特殊字符
  const escapedText = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  // 创建正则表达式，不区分大小写，支持中文
  const regex = new RegExp(`(${keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi')
  // 替换匹配的文本为高亮标签
  return escapedText.replace(regex, `<em class='highlight'>$1</em>`)
}

// 高亮后的标题
const highlightedTitle = computed(() => {
  if (!article.value?.title) return ''
  if (!searchKeyword.value) return article.value.title
  return highlightKeyword(article.value.title, searchKeyword.value)
})

// 高亮后的内容（在Markdown渲染后高亮）
const highlightedContent = computed(() => {
  if (!renderedContent.value) return ''
  if (!searchKeyword.value) return renderedContent.value
  // 在已渲染的HTML中高亮关键词（需要处理HTML标签）
  return highlightKeywordInHTML(renderedContent.value, searchKeyword.value)
})

// 在HTML中高亮关键词（避免高亮HTML标签内的内容）
const highlightKeywordInHTML = (html: string, keyword: string): string => {
  if (!keyword || !html) return html
  
  // 找到所有不在标签内的文本位置
  const textMatches: Array<{ start: number; end: number }> = []
  let textStart = 0
  
  for (let i = 0; i < html.length; i++) {
    if (html[i] === '<') {
      if (i > textStart) {
        textMatches.push({ start: textStart, end: i })
      }
      // 跳过标签内容，找到标签结束位置
      while (i < html.length && html[i] !== '>') {
        i++
      }
      textStart = i + 1
    }
  }
  // 添加最后的文本段
  if (textStart < html.length) {
    textMatches.push({ start: textStart, end: html.length })
  }
  
  // 在每个文本段中高亮关键词
  const resultParts: string[] = []
  let currentIndex = 0
  
  for (const match of textMatches) {
    // 添加标签部分
    if (currentIndex < match.start) {
      resultParts.push(html.substring(currentIndex, match.start))
    }
    // 高亮文本部分
    const textPart = html.substring(match.start, match.end)
    resultParts.push(highlightKeyword(textPart, keyword))
    currentIndex = match.end
  }
  if (currentIndex < html.length) {
    resultParts.push(html.substring(currentIndex))
  }
  
  return resultParts.join('')
}

// 加载文章详情
const loadArticle = async () => {
  const id = Number(route.params.id)
  if (!id) return

  loading.value = true
  try {
    const res = await getArticleDetail(id)
    article.value = res
    // 调试：打印文章详情，检查点赞和收藏状态
    console.log('文章详情:', res)
    console.log('是否已点赞:', res.isLiked)
    console.log('是否已收藏:', res.isCollected)
    // 刷新评论列表
    if (commentSectionRef.value) {
      commentSectionRef.value.loadComments()
    }
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
    if (article.value.isLiked === true) {
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
    if (article.value.isCollected === true) {
      await uncollectArticle(article.value.id)
      article.value.isCollected = false
      article.value.collectCount = (article.value.collectCount || 1) - 1
      // 同步更新用户信息中的收藏数
      if (userStore.userInfo) {
        userStore.userInfo.collectCount = (userStore.userInfo.collectCount || 1) - 1
      }
      ElMessage.success('取消收藏')
    } else {
      await collectArticle(article.value.id)
      article.value.isCollected = true
      article.value.collectCount = (article.value.collectCount || 0) + 1
      // 同步更新用户信息中的收藏数
      if (userStore.userInfo) {
        userStore.userInfo.collectCount = (userStore.userInfo.collectCount || 0) + 1
      }
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

// 处理评论添加
const handleCommentAdded = () => {
  if (article.value) {
    article.value.commentCount = (article.value.commentCount || 0) + 1
  }
}

// 查看摘要
const handleViewSummary = () => {
  if (article.value?.summary) {
    showSummaryDialog.value = true
  } else {
    ElMessage.warning('该文章暂无摘要')
  }
}

// 查看大纲
const handleViewOutline = async () => {
  // 优先使用文章保存的大纲
  if (article.value?.outline && article.value.outline.trim()) {
    generatedOutline.value = article.value.outline
    showOutlineDialog.value = true
    return
  }

  // 如果已经生成过大纲，直接显示
  if (generatedOutline.value) {
    showOutlineDialog.value = true
    return
  }

  // 如果没有保存的大纲，且文章内容为空，提示用户
  if (!article.value?.content) {
    ElMessage.warning('文章内容为空，无法生成大纲')
    return
  }

  // 生成大纲
  generatingOutline.value = true
  showOutlineDialog.value = true
  
  try {
    const outline = await generateOutlineFromContent(article.value.content)
    if (outline && typeof outline === 'string') {
      // 清理AI返回内容
      const cleaned = outline.trim()
        .replace(/^```(?:markdown)?\s*\n?/i, '')
        .replace(/\n?```\s*$/i, '')
        .trim()
      generatedOutline.value = cleaned
    } else {
      ElMessage.error('生成大纲失败：返回数据格式错误')
      showOutlineDialog.value = false
    }
  } catch (error) {
    console.error('生成大纲失败', error)
    ElMessage.error('生成大纲失败，请稍后重试')
    showOutlineDialog.value = false
  } finally {
    generatingOutline.value = false
  }
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

// 返回列表页，保留搜索参数
const handleBack = () => {
  const query: Record<string, any> = {}
  if (route.query.keyword) {
    query.keyword = route.query.keyword
  }
  if (route.query.categoryId) {
    query.categoryId = route.query.categoryId
  }
  if (route.query.tagId) {
    query.tagId = route.query.tagId
  }
  if (route.query.page) {
    query.page = route.query.page
  }
  if (route.query.orderBy) {
    query.orderBy = route.query.orderBy
  }
  
  router.push({ 
    name: 'article-list',
    query: Object.keys(query).length > 0 ? query : undefined
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
  .header-actions {
    margin-bottom: 16px;
  }

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

  // 搜索关键词高亮样式
  :deep(.highlight) {
    background-color: #fff3cd;
    color: #856404;
    font-weight: 600;
    padding: 2px 4px;
    border-radius: 3px;
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

.summary-content {
  padding: 16px;
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  
  p {
    margin: 0;
    text-align: justify;
  }
}

.outline-preview {
  max-height: 500px;
  overflow-y: auto;
  min-height: 200px;
  
  .outline-content {
    padding: 16px;
    font-size: 14px;
    line-height: 1.6;
    
    :deep(h1),
    :deep(h2),
    :deep(h3),
    :deep(h4) {
      margin-top: 16px;
      margin-bottom: 8px;
      font-weight: 600;
      color: #303133;
    }
    
    :deep(h1) {
      font-size: 20px;
    }
    
    :deep(h2) {
      font-size: 18px;
    }
    
    :deep(h3) {
      font-size: 16px;
    }
    
    :deep(ul),
    :deep(ol) {
      margin: 8px 0;
      padding-left: 24px;
    }
    
    :deep(li) {
      margin: 4px 0;
      color: #606266;
    }
  }
}
</style>
