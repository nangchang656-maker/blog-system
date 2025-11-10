<template>
  <div class="article-table">
    <el-table :data="articles" v-loading="loading" stripe>
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <div class="title-cell" @click="viewArticle(row.id)">
            {{ row.title }}
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="categoryName" label="分类" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.categoryName }}</el-tag>
        </template>
      </el-table-column>

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

      <el-table-column label="标签" width="200">
        <template #default="{ row }">
          <el-tag v-for="tag in row.tags.slice(0, 2)" :key="tag.id" size="small" style="margin-right: 4px">
            {{ tag.name }}
          </el-tag>
          <span v-if="row.tags.length > 2" style="color: #909399; font-size: 12px">
            +{{ row.tags.length - 2 }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="统计" width="150">
        <template #default="{ row }">
          <div class="stats-cell">
            <span><el-icon><View /></el-icon> {{ row.viewCount }}</span>
            <span><el-icon><Star /></el-icon> {{ row.likeCount }}</span>
            <span><el-icon><ChatDotRound /></el-icon> {{ row.commentCount }}</span>
          </div>
        </template>
      </el-table-column>

      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatTime(row.createTime) }}
        </template>
      </el-table-column>

      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="emit('edit', row.id)">
            编辑
          </el-button>
          <el-button link type="danger" size="small" @click="emit('delete', row.id)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container" v-if="total > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { View, Star, ChatDotRound } from '@element-plus/icons-vue'
import type { ArticleListItem } from '@/api/article'
import { ARTICLE_STATUS_DRAFT, ARTICLE_STATUS_PUBLISHED, ARTICLE_STATUS_BLOCKED } from '@/constants/article'

interface Props {
  articles: ArticleListItem[]
  loading: boolean
  total: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  pageChange: [page: number, size: number]
  edit: [id: number]
  delete: [id: number]
}>()

const router = useRouter()
const currentPage = ref(1)
const pageSize = ref(10)

// 查看文章详情
const viewArticle = (id: number) => {
  router.push({ name: 'article-detail', params: { id } })
}

// 分页变化
const handlePageChange = () => {
  emit('pageChange', currentPage.value, pageSize.value)
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
  return new Date(time).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 监听total变化,重置分页
watch(
  () => props.total,
  () => {
    if (currentPage.value > 1 && props.articles.length === 0) {
      currentPage.value = 1
      handlePageChange()
    }
  }
)
</script>

<style scoped lang="scss">
.article-table {
  .title-cell {
    cursor: pointer;
    color: #409eff;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;

    &:hover {
      text-decoration: underline;
    }
  }

  .stats-cell {
    display: flex;
    gap: 12px;
    font-size: 13px;
    color: #606266;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>
