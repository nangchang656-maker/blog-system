<template>
  <div class="tag-manage-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>标签管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增标签</el-button>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="search-section">
        <el-input
          v-model="keyword"
          placeholder="搜索标签名称..."
          clearable
          style="width: 300px; margin-right: 16px"
          @keyup.enter="handleSearch"
        >
          <template #prepend>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>

      <!-- 标签列表 -->
      <el-table v-loading="loading" :data="filteredTags" style="width: 100%; margin-top: 20px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" min-width="200">
          <template #default="{ row }">
            <span v-if="!row.editing">{{ row.name }}</span>
            <el-input
              v-else
              v-model="row.editName"
              size="small"
              style="width: 200px"
              @keyup.enter="handleSave(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="articleCount" label="文章数" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="!row.editing">
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
            </template>
            <template v-else>
              <el-button link type="primary" size="small" @click="handleSave(row)">保存</el-button>
              <el-button link size="small" @click="handleCancel(row)">取消</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="formData" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getTagList,
  createTag,
  updateTag,
  deleteTag,
  type TagManageParams,
  type Tag
} from '@/api/admin'

const loading = ref(false)
const tags = ref<(Tag & { editing?: boolean; editName?: string })[]>([])
const keyword = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('新增标签')
const formRef = ref<FormInstance>()
const formData = ref<TagManageParams>({
  name: ''
})
const editingId = ref<number | null>(null)

const rules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

// 过滤标签
const filteredTags = computed(() => {
  if (!keyword.value) {
    return tags.value
  }
  return tags.value.filter(tag => tag.name.toLowerCase().includes(keyword.value.toLowerCase()))
})

// 加载标签列表
const loadTags = async () => {
  loading.value = true
  try {
    const res = await getTagList()
    tags.value = res.map(tag => ({
      ...tag,
      editing: false
    }))
  } catch (error) {
    ElMessage.error('加载标签列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  // 搜索逻辑已在computed中实现
}

// 重置
const handleReset = () => {
  keyword.value = ''
}

// 新增标签
const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增标签'
  formData.value = { name: '' }
  dialogVisible.value = true
}

// 编辑标签
const handleEdit = (row: Tag & { editing?: boolean }) => {
  tags.value.forEach(tag => {
    if (tag.id === row.id) {
      tag.editing = true
      tag.editName = tag.name
    } else {
      tag.editing = false
    }
  })
}

// 保存编辑
const handleSave = async (row: Tag & { editing?: boolean; editName?: string }) => {
  if (!row.editName || row.editName.trim() === '') {
    ElMessage.warning('标签名称不能为空')
    return
  }

  try {
    await updateTag({
      id: row.id,
      name: row.editName!
    })
    ElMessage.success('更新成功')
    row.name = row.editName!
    row.editing = false
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

// 取消编辑
const handleCancel = (row: Tag & { editing?: boolean }) => {
  row.editing = false
  delete row.editName
}

// 删除标签
const handleDelete = async (row: Tag) => {
  try {
    await ElMessageBox.confirm(
      `确认删除标签"${row.name}"吗？删除后该标签与文章的关联关系将被清除。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteTag(row.id)
    ElMessage.success('删除成功')
    loadTags()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (editingId.value) {
          await updateTag({
            id: editingId.value,
            ...formData.value
          })
          ElMessage.success('更新成功')
        } else {
          await createTag(formData.value)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadTags()
      } catch (error) {
        // 错误信息已在API拦截器中显示
      }
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
  editingId.value = null
}

onMounted(() => {
  loadTags()
})
</script>

<style scoped lang="scss">
.tag-manage-view {
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
}
</style>

