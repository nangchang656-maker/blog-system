<template>
  <div class="comment-manage-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>评论管理</span>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="search-section">
        <el-input
          v-model="searchParams.keyword"
          placeholder="搜索评论内容/文章标题..."
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
          <el-option label="正常" :value="1" />
          <el-option label="待审核" :value="2" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <!-- 评论列表 -->
      <el-table v-loading="loading" :data="comments" style="width: 100%; margin-top: 20px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="articleTitle" label="文章标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link :href="`/article/${row.articleId}`" target="_blank" type="primary">
              {{ row.articleTitle }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="评论用户" width="150">
          <template #default="{ row }">
            <div class="user-info">
              <el-avatar :size="32" :src="row.userAvatar">
                {{ row.userNickname?.charAt(0) }}
              </el-avatar>
              <span style="margin-left: 8px">{{ row.userNickname }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="回复对象" width="150">
          <template #default="{ row }">
            <span v-if="row.toUserNickname">@{{ row.toUserNickname }}</span>
            <span v-else style="color: #909399">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞数" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">
              {{ row.status === 1 ? '正常' : '待审核' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              size="small"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '隐藏' : '显示' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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
          @current-change="loadComments"
          @size-change="loadComments"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import {
  getCommentList,
  updateCommentStatus,
  deleteComment,
  type CommentManageItem
} from '@/api/admin'

const loading = ref(false)
const comments = ref<CommentManageItem[]>([])
const total = ref(0)

const searchParams = ref({
  current: 1,
  size: 10,
  status: undefined as number | undefined,
  keyword: ''
})

// 加载评论列表
const loadComments = async () => {
  loading.value = true
  try {
    const res = await getCommentList({
      current: searchParams.value.current,
      size: searchParams.value.size,
      status: searchParams.value.status,
      keyword: searchParams.value.keyword || undefined
    })
    comments.value = res.records
    total.value = res.total
  } catch (error) {
    ElMessage.error('加载评论列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  searchParams.value.current = 1
  loadComments()
}

// 重置
const handleReset = () => {
  searchParams.value.keyword = ''
  searchParams.value.status = undefined
  handleSearch()
}

// 状态变更
const handleStatusChange = async (row: CommentManageItem) => {
  try {
    const action = row.status === 1 ? '隐藏' : '显示'
    await ElMessageBox.confirm(`确认${action}该评论吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const newStatus = row.status === 1 ? 2 : 1
    await updateCommentStatus({
      commentId: row.id,
      status: newStatus
    })
    ElMessage.success(`${action}成功`)
    loadComments()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

// 删除评论
const handleDelete = async (row: CommentManageItem) => {
  try {
    await ElMessageBox.confirm('确认删除该评论吗？删除后不可恢复！', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteComment(row.id)
    ElMessage.success('删除成功')
    loadComments()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
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
  loadComments()
})
</script>

<style scoped lang="scss">
.comment-manage-view {
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

  .user-info {
    display: flex;
    align-items: center;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>

