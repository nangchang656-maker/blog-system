<template>
  <div class="category-manage-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增分类</el-button>
        </div>
      </template>

      <!-- 分类列表 -->
      <el-table v-loading="loading" :data="categories" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="200">
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
        <el-table-column prop="sort" label="排序" width="150">
          <template #default="{ row }">
            <span v-if="!row.editing">{{ row.sort }}</span>
            <el-input-number
              v-else
              v-model="row.editSort"
              size="small"
              :min="0"
              style="width: 120px"
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
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" style="width: 100%" />
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getCategoryList,
  createCategory,
  updateCategory,
  deleteCategory,
  type CategoryManageParams,
  type Category
} from '@/api/admin'

const loading = ref(false)
const categories = ref<(Category & { editing?: boolean; editName?: string; editSort?: number })[]>([])
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formRef = ref<FormInstance>()
const formData = ref<CategoryManageParams>({
  name: '',
  sort: 0
})
const editingId = ref<number | null>(null)

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'blur' }]
}

// 加载分类列表
const loadCategories = async () => {
  loading.value = true
  try {
    const res = await getCategoryList()
    categories.value = res.map(cat => ({
      ...cat,
      editing: false
    }))
  } catch (error) {
    ElMessage.error('加载分类列表失败')
  } finally {
    loading.value = false
  }
}

// 新增分类
const handleAdd = () => {
  editingId.value = null
  dialogTitle.value = '新增分类'
  formData.value = { name: '', sort: 0 }
  dialogVisible.value = true
}

// 编辑分类
const handleEdit = (row: Category & { editing?: boolean }) => {
  categories.value.forEach(cat => {
    if (cat.id === row.id) {
      cat.editing = true
      cat.editName = cat.name
      cat.editSort = cat.sort
    } else {
      cat.editing = false
    }
  })
}

// 保存编辑
const handleSave = async (row: Category & { editing?: boolean; editName?: string; editSort?: number }) => {
  if (!row.editName || row.editName.trim() === '') {
    ElMessage.warning('分类名称不能为空')
    return
  }

  try {
    await updateCategory({
      id: row.id,
      name: row.editName!,
      sort: row.editSort!
    })
    ElMessage.success('更新成功')
    row.name = row.editName!
    row.sort = row.editSort!
    row.editing = false
  } catch (error) {
    ElMessage.error('更新失败')
  }
}

// 取消编辑
const handleCancel = (row: Category & { editing?: boolean }) => {
  row.editing = false
  delete row.editName
  delete row.editSort
}

// 删除分类
const handleDelete = async (row: Category) => {
  try {
    await ElMessageBox.confirm(
      `确认删除分类"${row.name}"吗？${row.articleCount && row.articleCount > 0 ? `该分类下有${row.articleCount}篇文章，删除后这些文章将无法显示分类。` : ''}`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadCategories()
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
          await updateCategory({
            id: editingId.value,
            ...formData.value
          })
          ElMessage.success('更新成功')
        } else {
          await createCategory(formData.value)
          ElMessage.success('创建成功')
        }
        dialogVisible.value = false
        loadCategories()
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
  loadCategories()
})
</script>

<style scoped lang="scss">
.category-manage-view {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>

