<template>
  <div class="my-articles-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <h2>我的文章</h2>
          <el-button type="primary" @click="goToEditor">写文章</el-button>
        </div>
      </template>

      <!-- Tab切换 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="已发布" name="published">
          <ArticleTable
            :articles="publishedArticles"
            :loading="loading"
            :total="publishedTotal"
            @page-change="loadPublishedArticles"
            @edit="handleEdit"
            @delete="handleDelete"
          />
        </el-tab-pane>

        <el-tab-pane label="草稿箱" name="draft">
          <ArticleTable
            :articles="draftArticles"
            :loading="loading"
            :total="draftTotal"
            @page-change="loadDraftArticles"
            @edit="handleEdit"
            @delete="handleDelete"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyArticles, deleteArticle, type ArticleListItem } from '@/api/article'
import ArticleTable from './components/ArticleTable.vue'

const router = useRouter()

const activeTab = ref('published')
const loading = ref(false)

// 已发布文章
const publishedArticles = ref<ArticleListItem[]>([])
const publishedTotal = ref(0)
const publishedPage = ref({ page: 1, size: 10 })

// 草稿文章
const draftArticles = ref<ArticleListItem[]>([])
const draftTotal = ref(0)
const draftPage = ref({ page: 1, size: 10 })

// 加载已发布文章
const loadPublishedArticles = async (page = 1, size = 10) => {
  loading.value = true
  try {
    const res = await getMyArticles({ page, size, status: 1 })
    publishedArticles.value = res.records
    publishedTotal.value = res.total
    publishedPage.value = { page, size }
  } catch (error) {
    ElMessage.error('加载文章失败')
  } finally {
    loading.value = false
  }
}

// 加载草稿文章
const loadDraftArticles = async (page = 1, size = 10) => {
  loading.value = true
  try {
    const res = await getMyArticles({ page, size, status: 0 })
    draftArticles.value = res.records
    draftTotal.value = res.total
    draftPage.value = { page, size }
  } catch (error) {
    ElMessage.error('加载草稿失败')
  } finally {
    loading.value = false
  }
}

// Tab切换
const handleTabChange = (name: string) => {
  if (name === 'published') {
    loadPublishedArticles()
  } else {
    loadDraftArticles()
  }
}

// 新建文章
const goToEditor = () => {
  router.push('/article/editor')
}

// 编辑文章
const handleEdit = (id: number) => {
  router.push(`/article/editor/${id}`)
}

// 删除文章
const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除这篇文章吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteArticle(id)
    ElMessage.success('删除成功')

    // 重新加载列表
    if (activeTab.value === 'published') {
      loadPublishedArticles(publishedPage.value.page, publishedPage.value.size)
    } else {
      loadDraftArticles(draftPage.value.page, draftPage.value.size)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(() => {
  loadPublishedArticles()
})
</script>

<style scoped lang="scss">
.my-articles-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
  }
}
</style>
