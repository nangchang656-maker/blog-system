<template>
  <div class="article-list-container">
    <!-- 搜索筛选区 -->
    <el-card class="search-card" shadow="never">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-input
            v-model="searchParams.keyword"
            placeholder="搜索文章标题、内容..."
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prepend>
              <el-icon><Search /></el-icon>
            </template>
            <template #append>
              <el-button @click="handleSearch">搜索</el-button>
            </template>
          </el-input>
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchParams.categoryId" placeholder="选择分类" clearable @change="handleSearch">
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="searchParams.orderBy" @change="handleSearch">
            <el-option label="最新发布" value="create_time" />
            <el-option label="最多浏览" value="view_count" />
            <el-option label="最多点赞" value="like_count" />
          </el-select>
        </el-col>
      </el-row>

      <!-- 热门标签 -->
      <div class="tags-section" v-if="hotTags.length > 0">
        <span class="tags-label">热门标签：</span>
        <el-tag
          v-for="tag in hotTags"
          :key="tag.id"
          :type="searchParams.tagId === tag.id ? 'primary' : 'info'"
          style="margin-right: 8px; cursor: pointer"
          @click="handleTagClick(tag.id)"
        >
          {{ tag.name }}
        </el-tag>
      </div>
    </el-card>

    <!-- 文章列表 -->
    <div class="article-list" v-loading="loading">
      <el-empty v-if="articles.length === 0 && !loading" description="暂无文章" />

      <div class="article-grid">
        <el-card
          v-for="article in articles"
          :key="article.id"
          class="article-card"
          shadow="hover"
          @click="goToDetail(article.id)"
        >
          <!-- 封面图 -->
          <div class="article-cover" v-if="article.coverImage">
            <el-image :src="article.coverImage" fit="cover" lazy />
          </div>
          <div class="article-cover-placeholder" v-else>
            <el-icon :size="48"><Document /></el-icon>
          </div>

          <!-- 文章内容 -->
          <div class="article-body">
            <h3 class="article-title">{{ article.title }}</h3>
            <p class="article-summary">{{ article.summary }}</p>

            <!-- 分类和标签 -->
            <div class="article-tags">
              <el-tag size="small" type="primary">{{ article.categoryName }}</el-tag>
              <el-tag
                v-for="tag in article.tags.slice(0, 2)"
                :key="tag.id"
                size="small"
                style="margin-left: 6px"
              >
                {{ tag.name }}
              </el-tag>
            </div>

            <!-- 底部信息 -->
            <div class="article-footer">
              <div class="author-info">
                <el-avatar :size="24" :src="article.authorAvatar">
                  {{ article.authorName?.charAt(0) }}
                </el-avatar>
                <span class="author-name">{{ article.authorName }}</span>
              </div>

              <div class="article-stats">
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }}
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ article.likeCount }}
                </span>
                <span class="stat-item">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ article.commentCount }}
                </span>
              </div>
            </div>

            <div class="article-time">{{ formatTime(article.createTime) }}</div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        v-model:current-page="searchParams.page"
        v-model:page-size="searchParams.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadArticles"
        @size-change="loadArticles"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, View, Star, ChatDotRound, Document } from '@element-plus/icons-vue'
import {
  getArticleList,
  getCategoryList,
  getHotTags,
  type ArticleQueryParams,
  type ArticleListItem,
  type Category,
  type Tag
} from '@/api/article'

const router = useRouter()

// 搜索参数
const searchParams = ref<ArticleQueryParams>({
  page: 1,
  size: 10,
  orderBy: 'create_time',
  orderType: 'desc'
})

// 数据
const articles = ref<ArticleListItem[]>([])
const categories = ref<Category[]>([])
const hotTags = ref<Tag[]>([])
const total = ref(0)
const loading = ref(false)

// 加载文章列表
const loadArticles = async () => {
  loading.value = true
  try {
    const res = await getArticleList(searchParams.value)
    articles.value = res.records
    total.value = res.total
  } catch (error) {
    ElMessage.error('加载文章列表失败')
  } finally {
    loading.value = false
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getCategoryList()
    categories.value = res
  } catch (error) {
    console.error('加载分类失败', error)
  }
}

// 加载热门标签
const loadHotTags = async () => {
  try {
    const res = await getHotTags(10)
    hotTags.value = res
  } catch (error) {
    console.error('加载热门标签失败', error)
  }
}

// 搜索
const handleSearch = () => {
  searchParams.value.page = 1
  loadArticles()
}

// 标签点击
const handleTagClick = (tagId: number) => {
  if (searchParams.value.tagId === tagId) {
    searchParams.value.tagId = undefined
  } else {
    searchParams.value.tagId = tagId
  }
  handleSearch()
}

// 跳转到详情
const goToDetail = (id: number) => {
  router.push({ name: 'article-detail', params: { id } })
}

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)}分钟前`
  if (diff < day) return `${Math.floor(diff / hour)}小时前`
  if (diff < 7 * day) return `${Math.floor(diff / day)}天前`

  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// 初始化
onMounted(() => {
  loadArticles()
  loadCategories()
  loadHotTags()
})
</script>

<style scoped lang="scss">
.article-list-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;

  .tags-section {
    margin-top: 16px;
    display: flex;
    align-items: center;

    .tags-label {
      font-weight: 500;
      margin-right: 12px;
      color: #606266;
    }
  }
}

.article-list {
  min-height: 400px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.article-card {
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 24px rgba(0, 0, 0, 0.15);
  }

  .article-cover {
    width: 100%;
    height: 200px;
    border-radius: 4px;
    overflow: hidden;
    margin-bottom: 12px;

    .el-image {
      width: 100%;
      height: 100%;
    }
  }

  .article-cover-placeholder {
    width: 100%;
    height: 200px;
    border-radius: 4px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    margin-bottom: 12px;
  }

  .article-body {
    display: flex;
    flex-direction: column;
    flex: 1;
  }

  .article-title {
    font-size: 18px;
    font-weight: 600;
    margin: 0 0 10px 0;
    color: #303133;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    line-height: 1.4;
  }

  .article-summary {
    font-size: 14px;
    color: #606266;
    margin: 0 0 12px 0;
    line-height: 1.6;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    flex: 1;
  }

  .article-tags {
    margin-bottom: 12px;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .article-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
    padding-top: 8px;
    border-top: 1px solid #f0f0f0;

    .author-info {
      display: flex;
      align-items: center;
      gap: 8px;

      .author-name {
        font-size: 13px;
        color: #606266;
      }
    }

    .article-stats {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 13px;
      color: #909399;

      .stat-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }
  }

  .article-time {
    font-size: 12px;
    color: #c0c4cc;
    text-align: right;
  }
}

.pagination-container {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

// 响应式
@media (max-width: 1200px) {
  .article-grid {
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  }
}

@media (max-width: 768px) {
  .article-grid {
    grid-template-columns: 1fr;
  }

  .article-card .article-cover,
  .article-card .article-cover-placeholder {
    height: 180px;
  }
}
</style>
