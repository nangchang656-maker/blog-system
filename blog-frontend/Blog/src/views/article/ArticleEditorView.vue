<template>
  <div class="article-editor-container">
    <el-card>
      <template #header>
        <div class="editor-header">
          <h2>{{ isEdit ? '编辑文章' : '发布新文章' }}</h2>
          <div class="header-actions">
            <el-button @click="handleCancel">取消</el-button>
            <el-button type="info" @click="handleSaveDraft" :loading="saving">
              保存草稿
            </el-button>
            <el-button type="primary" @click="handlePublish" :loading="publishing">
              发布文章
            </el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="文章标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入文章标题" maxlength="100" show-word-limit />
        </el-form-item>

        <el-form-item label="文章封面">
          <el-upload
            class="cover-uploader"
            :show-file-list="false"
            :auto-upload="false"
            :on-change="handleCoverChange"
            :disabled="coverUploading"
            accept="image/jpeg,image/png,image/jpg"
          >
            <div v-loading="coverUploading" element-loading-text="上传中...">
              <img v-if="form.coverImage" :src="form.coverImage" class="cover-image" />
              <div v-else class="cover-uploader-placeholder">
                <el-icon class="cover-uploader-icon"><Plus /></el-icon>
                <div class="cover-uploader-text">点击上传封面</div>
              </div>
            </div>
          </el-upload>
          <div class="upload-tip">
            建议尺寸 16:9,支持 JPG、PNG 格式,大小不超过 5MB
          </div>
        </el-form-item>

        <el-form-item label="文章分类" prop="categoryId">
          <el-select
            v-model="categoryValue"
            placeholder="选择或输入分类"
            filterable
            allow-create
            default-first-option
            @change="handleCategoryChange"
            style="width: 300px"
          >
            <el-option
              v-for="category in categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
          <el-text type="info" size="small" style="margin-left: 10px">
            可以选择已有分类或输入新分类名称
          </el-text>
        </el-form-item>

        <el-form-item label="文章标签" prop="tagIds">
          <el-select
            v-model="selectedTags"
            multiple
            placeholder="选择或输入标签"
            filterable
            allow-create
            @change="handleTagChange"
            style="width: 400px"
          >
            <el-option
              v-for="tag in tags"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
          <el-text type="info" size="small" style="margin-left: 10px">
            可以选择已有标签或输入新标签名称
          </el-text>
        </el-form-item>

        <el-form-item label="文章内容" prop="content" required>
          <mavon-editor
            v-model="form.content"
            :toolbars="toolbars"
            @imgAdd="handleImageAdd"
            style="height: 600px; z-index: 1"
          />
        </el-form-item>

        <el-form-item label="文章摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="请输入文章摘要（留空将自动生成）"
            maxlength="200"
            show-word-limit
          />
          <el-button size="small" style="margin-top: 8px" @click="handleGenerateSummary" :loading="generatingSummary">
            AI生成摘要
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 封面裁剪对话框 -->
    <ArticleCoverCropper
      v-model:visible="showCoverCropper"
      :image-file="currentCoverFile"
      @confirm="handleCropperConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { mavonEditor } from 'mavon-editor'
import 'mavon-editor/dist/css/index.css'
import {
  publishArticle,
  updateArticle,
  getArticleDetail,
  getCategoryList,
  getTagList,
  generateSummary,
  uploadCoverImage,
  type ArticleFormData,
  type Category,
  type Tag
} from '@/api/article'
import { compressImage, blobToFile } from '@/utils/imageCompress'
import ArticleCoverCropper from '@/components/ArticleCoverCropper.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()

const isEdit = ref(false)
const articleId = ref<number>()
const saving = ref(false)
const publishing = ref(false)
const generatingSummary = ref(false)
const coverUploading = ref(false)
const selectedTags = ref<(number | string)[]>([])
const categoryValue = ref<number | string>() // 分类选择值（可以是ID或新分类名称）

// 封面裁剪相关
const showCoverCropper = ref(false)
const currentCoverFile = ref<File | null>(null)

// 表单数据
const form = reactive<ArticleFormData>({
  title: '',
  content: '',
  summary: '',
  coverImage: '',
  categoryId: undefined,
  categoryName: undefined,
  tagIds: [],
  tagNames: [],
  status: 1
})

// 原始表单数据快照（用于检测是否有修改）
const originalFormSnapshot = ref<string>('')

// 计算是否有修改
const hasChanges = computed(() => {
  const currentSnapshot = JSON.stringify({
    title: form.title,
    content: form.content,
    summary: form.summary,
    coverImage: form.coverImage,
    categoryValue: categoryValue.value,
    selectedTags: selectedTags.value
  })
  return currentSnapshot !== originalFormSnapshot.value
})

// 保存表单快照
const saveFormSnapshot = () => {
  originalFormSnapshot.value = JSON.stringify({
    title: form.title,
    content: form.content,
    summary: form.summary,
    coverImage: form.coverImage,
    categoryValue: categoryValue.value,
    selectedTags: selectedTags.value
  })
}

// 分类和标签
const categories = ref<Category[]>([])
const tags = ref<Tag[]>([])

// 表单验证
const rules: FormRules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }]
}

// 处理分类变更
const handleCategoryChange = (value: number | string) => {
  if (typeof value === 'number') {
    // 选择已有分类
    form.categoryId = value
    form.categoryName = undefined
  } else if (typeof value === 'string' && value.trim()) {
    // 输入新分类名称
    form.categoryId = undefined
    form.categoryName = value.trim()
  }
}

// 处理标签变更
const handleTagChange = (values: (number | string)[]) => {
  const tagIds: number[] = []
  const tagNames: string[] = []

  values.forEach((value) => {
    if (typeof value === 'number') {
      tagIds.push(value)
    } else {
      tagNames.push(value)
    }
  })

  form.tagIds = tagIds.length > 0 ? tagIds : undefined
  form.tagNames = tagNames.length > 0 ? tagNames : undefined
}

// Markdown编辑器工具栏
const toolbars = {
  bold: true,
  italic: true,
  header: true,
  underline: true,
  strikethrough: true,
  mark: true,
  superscript: true,
  subscript: true,
  quote: true,
  ol: true,
  ul: true,
  link: true,
  imagelink: true,
  code: true,
  table: true,
  fullscreen: true,
  readmodel: true,
  htmlcode: true,
  help: true,
  undo: true,
  redo: true,
  trash: true,
  save: false,
  navigation: true,
  alignleft: true,
  aligncenter: true,
  alignright: true,
  subfield: true,
  preview: true
}

// 加载分类和标签
const loadCategoriesAndTags = async () => {
  try {
    const [catRes, tagRes] = await Promise.all([getCategoryList(), getTagList()])
    categories.value = catRes
    tags.value = tagRes
  } catch (error) {
    ElMessage.error('加载分类标签失败')
  }
}

// 加载文章详情(编辑模式)
const loadArticle = async (id: number) => {
  try {
    const res = await getArticleDetail(id)
    const article = res
    Object.assign(form, {
      title: article.title,
      content: article.content,
      summary: article.summary,
      coverImage: article.coverImage,
      categoryId: article.categoryId,
      categoryName: undefined,
      tagIds: article.tags.map((t: any) => t.id),
      tagNames: [],
      status: 1
    })
    // 设置分类选择值
    categoryValue.value = article.categoryId
    // 设置选中的标签
    selectedTags.value = article.tags.map((t: any) => t.id)
    // 保存原始数据快照
    saveFormSnapshot()
  } catch (error) {
    ElMessage.error('加载文章失败')
  }
}

// 处理封面选择
const handleCoverChange = async (file: any) => {
  const rawFile = file.raw as File

  // 1. 检查文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg']
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.error('封面必须是 JPG 或 PNG 格式')
    return
  }

  // 2. 检查文件大小
  const maxSize = 5 * 1024 * 1024 // 5MB
  if (rawFile.size > maxSize) {
    ElMessage.error('封面大小不能超过 5MB')
    return
  }

  // 3. 检查图片尺寸
  try {
    await validateImageSize(rawFile)

    // 通过验证,保存文件并打开裁剪器
    currentCoverFile.value = rawFile
    showCoverCropper.value = true
  } catch (error: any) {
    // 错误已在 validateImageSize 中提示
  }
}

// 验证图片尺寸
const validateImageSize = (file: File): Promise<void> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      const img = new Image()
      img.onload = () => {
        // 检查最小尺寸
        if (img.width < 400 || img.height < 225) {
          ElMessage.error('图片尺寸过小,建议至少 800x450 像素')
          reject(new Error('图片尺寸过小'))
          return
        }
        // 检查最大尺寸
        if (img.width > 4096 || img.height > 4096) {
          ElMessage.error('图片尺寸过大,建议不超过 4096x4096 像素')
          reject(new Error('图片尺寸过大'))
          return
        }
        resolve()
      }
      img.onerror = () => {
        ElMessage.error('图片加载失败,请重试')
        reject(new Error('图片加载失败'))
      }
      img.src = e.target?.result as string
    }
    reader.onerror = () => {
      ElMessage.error('文件读取失败,请重试')
      reject(new Error('文件读取失败'))
    }
    reader.readAsDataURL(file)
  })
}

// 裁剪确认后上传
const handleCropperConfirm = async (croppedFile: File) => {
  coverUploading.value = true

  try {
    // 1. 压缩图片
    const compressedBlob = await compressImage(croppedFile, {
      quality: 0.85,
      maxWidth: 1600,
      maxHeight: 900,
      mimeType: 'image/jpeg'
    })

    // 2. 转换为 File
    const fileName = `cover_${Date.now()}.jpg`
    const finalFile = blobToFile(compressedBlob, fileName)

    console.log('原始文件大小:', (croppedFile.size / 1024).toFixed(2), 'KB')
    console.log('压缩后大小:', (finalFile.size / 1024).toFixed(2), 'KB')

    // 3. 上传到服务器(如果是编辑模式,传入articleId)
    const result = await uploadCoverImage(finalFile, articleId.value)
    console.log('封面上传成功,返回URL:', result.url)

    // 4. 更新表单封面 URL
    form.coverImage = result.url

    ElMessage.success('封面上传成功')
  } catch (error: any) {
    console.error('封面上传失败:', error)
    ElMessage.error(error.message || '封面上传失败,请重试')
  } finally {
    coverUploading.value = false
  }
}

// 图片上传
const handleImageAdd = (pos: any, file: File) => {
  // TODO: 实现图片上传到服务器
  ElMessage.warning('图片上传功能待实现')
}

// AI生成摘要
const handleGenerateSummary = async () => {
  if (!form.content) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  generatingSummary.value = true
  try {
    const res = await generateSummary(form.content)
    if (res.code === 200) {
      form.summary = res.data
      ElMessage.success('摘要生成成功')
    }
  } catch (error) {
    ElMessage.error('生成摘要失败')
  } finally {
    generatingSummary.value = false
  }
}

// 保存草稿
const handleSaveDraft = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    form.status = 0
    await saveArticle()
    ElMessage.success('草稿保存成功')
    router.push('/my-articles')
  } catch (error) {
    console.error('验证失败', error)
  }
}

// 发布文章
const handlePublish = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    form.status = 1
    await saveArticle()
    ElMessage.success('文章发布成功')
    router.push('/my-articles')
  } catch (error) {
    console.error('验证失败', error)
  }
}

// 保存文章
const saveArticle = async () => {
  // 验证必填字段
  if (!form.categoryId && !form.categoryName) {
    ElMessage.warning('请选择或输入文章分类')
    throw new Error('分类不能为空')
  }

  const loading = form.status === 0 ? saving : publishing
  loading.value = true

  try {
    if (isEdit.value && articleId.value) {
      await updateArticle(articleId.value, form)
    } else {
      await publishArticle(form)
    }
  } finally {
    loading.value = false
  }
}

// 取消
const handleCancel = () => {
  // 如果没有修改，直接返回
  if (!hasChanges.value) {
    router.back()
    return
  }

  // 有修改时才显示确认弹窗
  ElMessageBox.confirm('确认取消吗？未保存的内容将丢失', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    router.back()
  })
}

// 初始化
onMounted(() => {
  loadCategoriesAndTags()

  // 检查是否编辑模式
  const id = route.params.id
  if (id) {
    isEdit.value = true
    articleId.value = Number(id)
    loadArticle(articleId.value)
  } else {
    // 新建文章时也保存初始快照
    saveFormSnapshot()
  }
})
</script>

<style scoped lang="scss">
.article-editor-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }
}

:deep(.markdown-body) {
  padding: 16px;
  min-height: 500px;
}

.cover-uploader {
  display: inline-block;
}

.cover-image {
  width: 400px;
  height: 225px;
  display: block;
  border-radius: 8px;
  object-fit: cover;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    opacity: 0.8;
  }
}

.cover-uploader-placeholder {
  width: 400px;
  height: 225px;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;

    .cover-uploader-icon {
      color: #409eff;
    }
  }
}

.cover-uploader-icon {
  font-size: 48px;
  color: #8c939d;
  margin-bottom: 8px;
}

.cover-uploader-text {
  font-size: 14px;
  color: #606266;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
