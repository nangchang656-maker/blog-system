<template>
  <div class="article-manage-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>文章管理</span>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="search-section">
        <el-input
          v-model="searchParams.keyword"
          placeholder="搜索文章标题..."
          clearable
          style="width: 300px; margin-right: 16px"
          @keyup.enter="handleSearch"
        >
          <template #prepend>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="searchParams.status"
          placeholder="状态筛选"
          clearable
          style="width: 150px; margin-right: 16px"
          @change="handleSearch"
        >
          <el-option label="全部" :value="undefined" />
          <el-option label="草稿" :value="ARTICLE_STATUS_DRAFT" />
          <el-option label="已发布" :value="ARTICLE_STATUS_PUBLISHED" />
          <el-option label="已屏蔽" :value="ARTICLE_STATUS_BLOCKED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <!-- 文章列表 -->
      <el-table v-loading="loading" :data="articles" style="width: 100%; margin-top: 20px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120">
          <template #default="{ row }">
            <el-select
              v-model="row.categoryId"
              placeholder="选择分类"
              size="small"
              style="width: 100%"
              @change="handleCategoryChange(row)"
            >
              <el-option
                v-for="category in categories"
                :key="category.id"
                :label="category.name"
                :value="category.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="tags" label="标签" min-width="200">
          <template #default="{ row }">
            <el-select
              v-model="row.tagIds"
              multiple
              placeholder="选择标签"
              size="small"
              style="width: 100%"
              @change="handleTagChange(row)"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              v-if="row.status !== undefined"
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览量" width="100" />
        <el-table-column prop="likeCount" label="点赞数" width="100" />
        <el-table-column prop="commentCount" label="评论数" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === ARTICLE_STATUS_BLOCKED ? 'success' : 'warning'"
              size="small"
              @click="handleStatusChange(row)"
            >
              {{ row.status === ARTICLE_STATUS_BLOCKED ? '取消屏蔽' : '屏蔽' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="searchParams.current"
          v-model:page-size="searchParams.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadArticles"
          @size-change="loadArticles"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { getCategoryList, getTagList, type Category, type Tag } from '@/api/article'
import {
  getAdminArticleList,
  updateArticleCategory,
  updateArticleTags,
  updateArticleStatus,
  type ArticleListItem
} from '@/api/admin'
import { ARTICLE_STATUS_DRAFT, ARTICLE_STATUS_PUBLISHED, ARTICLE_STATUS_BLOCKED } from '@/constants/article'

const loading = ref(false)
const articles = ref<ArticleListItem[]>([])
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])
const total = ref(0)

const searchParams = ref({
  current: 1,
  size: 10,
  keyword: '',
  status: undefined as number | undefined
})

// 加载文章列表
const loadArticles = async () => {
  loading.value = true
  try {
    const res = await getAdminArticleList({
      current: searchParams.value.current,
      size: searchParams.value.size,
      keyword: searchParams.value.keyword || undefined,
      status: searchParams.value.status
    })
    articles.value = res.records.map(article => ({
      ...article,
      tagIds: article.tags.map(tag => tag.id)
    }))
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

// 加载标签列表
const loadTags = async () => {
  try {
    const res = await getTagList()
    tags.value = res
  } catch (error) {
    console.error('加载标签失败', error)
  }
}

// 搜索
const handleSearch = () => {
  searchParams.value.current = 1
  loadArticles()
}

// 重置
const handleReset = () => {
  searchParams.value.keyword = ''
  searchParams.value.status = undefined
  handleSearch()
}

// 分类变更
const handleCategoryChange = async (row: ArticleListItem & { tagIds: number[] }) => {
  try {
    await updateArticleCategory({
      articleId: row.id,
      categoryId: row.categoryId
    })
    ElMessage.success('分类更新成功')
  } catch (error) {
    ElMessage.error('分类更新失败')
    // 恢复原值
    loadArticles()
  }
}

// 标签变更
const handleTagChange = async (row: ArticleListItem & { tagIds: number[] }) => {
  try {
    await updateArticleTags({
      articleId: row.id,
      tagIds: row.tagIds || []
    })
    ElMessage.success('标签更新成功')
  } catch (error) {
    ElMessage.error('标签更新失败')
    // 恢复原值
    loadArticles()
  }
}

// 状态变更
const handleStatusChange = async (row: ArticleListItem & { tagIds: number[] }) => {
  try {
    const action = row.status === ARTICLE_STATUS_BLOCKED ? '取消屏蔽' : '屏蔽'
    await ElMessageBox.confirm(`确认${action}该文章吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    // 如果已屏蔽则恢复为已发布，否则屏蔽
    const newStatus = row.status === ARTICLE_STATUS_BLOCKED ? ARTICLE_STATUS_PUBLISHED : ARTICLE_STATUS_BLOCKED
    await updateArticleStatus({
      articleId: row.id,
      status: newStatus
    })
    ElMessage.success(`${action}成功`)
    loadArticles()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 获取状态文本
const getStatusText = (status: number | undefined) => {
  if (status === ARTICLE_STATUS_DRAFT) return '草稿'
  if (status === ARTICLE_STATUS_PUBLISHED) return '已发布'
  if (status === ARTICLE_STATUS_BLOCKED) return '已屏蔽'
  return '未知'
}

// 获取状态类型
const getStatusType = (status: number | undefined) => {
  if (status === ARTICLE_STATUS_DRAFT) return 'info'
  if (status === ARTICLE_STATUS_PUBLISHED) return 'success'
  if (status === ARTICLE_STATUS_BLOCKED) return 'danger'
  return ''
}

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadArticles()
  loadCategories()
  loadTags()
})
</script>

<style scoped lang="scss">
.article-manage-view {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-section {
    display: flex;
    align-items: center;
    margin-bottom: 20px;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>

