<template>
  <div class="article-editor-container">
    <el-card>
      <template #header>
        <div class="editor-header">
          <h2>{{ isEdit ? '编辑文章' : '发布新文章' }}</h2>
          <div class="header-actions">
            <el-text v-if="lastAutoSaveTime" type="info" size="small" class="auto-save-hint">
              自动保存于 {{ formatTime(lastAutoSaveTime) }}
            </el-text>
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

      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <!-- 基础信息区域 -->
        <el-card shadow="never" class="form-section">
          <template #header>
            <div class="section-header">
              <span class="section-title">基础信息</span>
            </div>
          </template>
          
          <el-form-item label="文章标题" prop="title">
            <el-input 
              v-model="form.title" 
              placeholder="请输入文章标题，建议30-60字" 
              maxlength="100" 
              show-word-limit
              clearable
            />
          </el-form-item>

          <el-form-item label="文章封面">
            <div class="cover-upload-wrapper">
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
                    <div class="cover-uploader-hint">建议尺寸 16:9</div>
                  </div>
                </div>
              </el-upload>
              <div class="upload-tip">
                <el-text type="info" size="small">
                  <el-icon><InfoFilled /></el-icon>
                  支持 JPG、PNG 格式，大小不超过 5MB，建议尺寸 800x450 像素
                </el-text>
              </div>
            </div>
          </el-form-item>

          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="文章分类" prop="categoryId">
                <el-select
                  v-model="categoryValue"
                  placeholder="选择或输入分类"
                  filterable
                  allow-create
                  default-first-option
                  @change="handleCategoryChange"
                  style="width: 100%"
                  clearable
                >
                  <el-option
                    v-for="category in categories"
                    :key="category.id"
                    :label="category.name"
                    :value="category.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="文章标签" prop="tagIds">
                <el-select
                  v-model="selectedTags"
                  multiple
                  placeholder="选择或输入标签"
                  filterable
                  allow-create
                  @change="handleTagChange"
                  style="width: 100%"
                  collapse-tags
                  collapse-tags-tooltip
                >
                  <el-option
                    v-for="tag in tags"
                    :key="tag.id"
                    :label="tag.name"
                    :value="tag.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <!-- 内容编辑区域和大纲区域（左右布局） -->
        <el-row :gutter="20" class="content-outline-row">
          <!-- 文章内容（左侧主体） -->
          <el-col :xs="24" :sm="24" :md="showOutline ? 18 : 24" :lg="showOutline ? 18 : 24" :xl="showOutline ? 18 : 24">
            <el-card shadow="never" class="form-section content-card">
              <template #header>
                <div class="section-header">
                  <span class="section-title">文章内容</span>
                  <div class="header-right">
                    <div class="content-stats">
                      <el-text type="info" size="small">
                        字数：{{ contentWordCount }} 字
                      </el-text>
                    </div>
                    <el-button 
                      size="small" 
                      :type="showOutline ? 'info' : 'primary'"
                      plain
                      @click="showOutline = !showOutline"
                      style="margin-left: 12px;"
                    >
                      <el-icon style="margin-right: 4px">
                        <ArrowRight v-if="!showOutline" />
                        <ArrowLeft v-else />
                      </el-icon>
                      {{ showOutline ? '隐藏大纲' : '显示大纲' }}
                    </el-button>
                  </div>
                </div>
              </template>

              <el-form-item prop="content" required class="content-form-item">
                <!-- AI辅助工具栏 -->
                <div class="ai-toolbar">
                  <el-space wrap>
                    <el-button 
                      size="default" 
                      type="primary" 
                      @click="handlePolishContent" 
                      :loading="polishing"
                      :disabled="!form.content || form.content.trim().length === 0"
                    >
                      <el-icon style="margin-right: 4px"><MagicStick /></el-icon>
                      AI润色
                    </el-button>
                    <el-button 
                      size="default" 
                      type="success" 
                      @click="handleGenerateOutline" 
                      :loading="generatingOutline"
                      :disabled="!form.content || form.content.trim().length === 0"
                    >
                      <el-icon style="margin-right: 4px"><Document /></el-icon>
                      AI生成大纲
                    </el-button>
                  </el-space>
                  <el-text type="info" size="small" class="ai-tip">
                    <el-icon><InfoFilled /></el-icon>
                    AI功能可帮助优化文章表达和生成结构大纲
                  </el-text>
                </div>
                
                <div class="editor-wrapper">
                  <mavon-editor
                    v-model="form.content"
                    :toolbars="toolbars"
                    @imgAdd="handleImageAdd"
                    class="markdown-editor"
                    :box-shadow="false"
                    :subfield="true"
                    :default-open="'edit'"
                  />
                </div>
              </el-form-item>
            </el-card>
          </el-col>

          <!-- 大纲区域（右侧） -->
          <el-col v-show="showOutline" :xs="24" :sm="24" :md="6" :lg="6" :xl="6">
            <el-card shadow="never" class="form-section outline-card">
              <template #header>
                <div class="section-header">
                  <span class="section-title">文章大纲</span>
                  <el-button 
                    size="small" 
                    type="success" 
                    plain
                    @click="handleGenerateOutline" 
                    :loading="generatingOutline"
                    :disabled="!form.content || form.content.trim().length === 0"
                  >
                    <el-icon style="margin-right: 4px"><Document /></el-icon>
                    AI生成
                  </el-button>
                </div>
              </template>

              <el-form-item>
                <el-input
                  v-model="form.outline"
                  type="textarea"
                  :rows="12"
                  placeholder="文章大纲（Markdown格式），可手动输入或点击上方按钮使用AI生成。大纲将帮助读者快速了解文章结构。"
                  resize="vertical"
                />
                <div class="outline-actions" v-if="form.outline">
                  <el-button 
                    size="small" 
                    type="info" 
                    plain
                    @click="handlePreviewOutline"
                    style="width: 100%; margin-bottom: 8px;"
                  >
                    <el-icon style="margin-right: 4px"><View /></el-icon>
                    预览大纲
                  </el-button>
                  <el-button 
                    size="small" 
                    type="primary" 
                    plain
                    @click="handleInsertOutlineToContent"
                    style="width: 100%;"
                  >
                    <el-icon style="margin-right: 4px"><DocumentCopy /></el-icon>
                    插入到文章开头
                  </el-button>
                </div>
              </el-form-item>
            </el-card>
          </el-col>
        </el-row>

        <!-- 摘要区域 -->
        <el-card shadow="never" class="form-section">
          <template #header>
            <div class="section-header">
              <span class="section-title">文章摘要</span>
              <el-button 
                size="small" 
                type="primary" 
                plain
                @click="handleGenerateSummary" 
                :loading="generatingSummary"
                :disabled="!form.content || form.content.trim().length === 0"
              >
                <el-icon style="margin-right: 4px"><MagicStick /></el-icon>
                AI生成摘要
              </el-button>
            </div>
          </template>

          <el-form-item>
            <el-input
              v-model="form.summary"
              type="textarea"
              :rows="4"
              placeholder="文章摘要，可手动输入或点击上方按钮使用AI生成（留空将在发布时自动生成）"
              maxlength="200"
              show-word-limit
              resize="vertical"
            />
          </el-form-item>
        </el-card>
      </el-form>
    </el-card>

    <!-- 封面裁剪对话框 -->
    <ArticleCoverCropper
      v-model:visible="showCoverCropper"
      :image-file="currentCoverFile"
      @confirm="handleCropperConfirm"
    />

    <!-- 大纲预览对话框 -->
    <el-dialog
      v-model="showOutlineDialog"
      title="大纲预览"
      width="600px"
      :before-close="handleCloseOutlineDialog"
    >
      <div class="outline-preview">
        <div class="outline-content markdown-body" v-html="renderedOutline"></div>
      </div>
      <template #footer>
        <el-button @click="showOutlineDialog = false">关闭</el-button>
        <el-button 
          type="primary" 
          @click="handleInsertOutline"
          :disabled="!generatedOutline"
        >
          插入到文章开头
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, MagicStick, Document, InfoFilled, View, DocumentCopy, ArrowRight, ArrowLeft } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import {
  publishArticle,
  updateArticle,
  getArticleDetail,
  getCategoryList,
  getTagList,
  generateSummary,
  polishContent,
  generateOutlineFromContent,
  uploadCoverImage,
  type ArticleFormData,
  type Category,
  type Tag
} from '@/api/article'
import { compressImage, blobToFile } from '@/utils/imageCompress'
import { debounce } from '@/utils/debounce'
import ArticleCoverCropper from '@/components/ArticleCoverCropper.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()

const isEdit = ref(false)
const articleId = ref<number>()
const saving = ref(false)
const publishing = ref(false)
const generatingSummary = ref(false)
const polishing = ref(false)
const generatingOutline = ref(false)
const coverUploading = ref(false)
const selectedTags = ref<(number | string)[]>([])
const categoryValue = ref<number | string>() // 分类选择值（可以是ID或新分类名称）

// 自动保存相关
const autoSaving = ref(false)
const lastAutoSaveTime = ref<Date | null>(null)
const autoSaveInterval = ref<ReturnType<typeof setInterval> | null>(null)

// 封面裁剪相关
const showCoverCropper = ref(false)
const currentCoverFile = ref<File | null>(null)

// 大纲相关
const showOutlineDialog = ref(false)
const generatedOutline = ref('')
const showOutline = ref(true) // 控制大纲区域的显示/隐藏

// Markdown渲染器（用于预览大纲）
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
})

const renderedOutline = computed(() => {
  return generatedOutline.value ? md.render(generatedOutline.value) : ''
})

// 内容字数统计（使用防抖优化性能）
const contentWordCount = computed(() => {
  if (!form.content) return 0
  // 移除Markdown语法标记，统计纯文本字数
  const text = form.content
    .replace(/```[\s\S]*?```/g, '') // 代码块
    .replace(/`[^`]+`/g, '') // 行内代码
    .replace(/\[([^\]]+)\]\([^\)]+\)/g, '$1') // 链接
    .replace(/[#*_~`\[\]()!]/g, '') // Markdown标记
    .replace(/\s+/g, '') // 空白字符
  return text.length
})

// 表单数据
const form = reactive<ArticleFormData>({
  title: '',
  content: '',
  summary: '',
  outline: '',
  coverImage: '',
  categoryId: undefined,
  categoryName: undefined,
  tagIds: [],
  tagNames: [],
  status: 1
})

// 原始表单数据快照（用于检测是否有修改）
const originalFormSnapshot = ref<string>('')

// 计算是否有修改（使用防抖优化性能）
const hasChanges = computed(() => {
  if (!originalFormSnapshot.value) return false
  const currentSnapshot = JSON.stringify({
    title: form.title,
    content: form.content,
    summary: form.summary,
    outline: form.outline,
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
    outline: form.outline,
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
      outline: article.outline || '',
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

    // 3. 上传到服务器(如果是编辑模式,传入articleId)
    const result = await uploadCoverImage(finalFile, articleId.value)

    // 4. 更新表单封面 URL
    form.coverImage = result.url

    ElMessage.success('封面上传成功')
  } catch (error: any) {
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

// 清理AI返回的内容（移除markdown代码块标记和额外说明）
const cleanAIContent = (content: string): string => {
  if (!content) return content
  
  let cleaned = content.trim()
  
  // 如果内容被包裹在markdown代码块中，提取代码块内的内容
  const codeBlockMatch = cleaned.match(/^```(?:markdown)?\s*\n([\s\S]*?)\n?```\s*(.*)$/i)
  if (codeBlockMatch) {
    // 提取代码块内的内容
    cleaned = codeBlockMatch[1].trim()
    // 如果代码块后面还有说明文字，忽略它
    // codeBlockMatch[2] 就是说明文字，我们不需要
  } else {
    // 如果没有完整的代码块，尝试移除开头的标记
    cleaned = cleaned.replace(/^```(?:markdown)?\s*\n?/i, '')
    // 移除结尾的标记
    cleaned = cleaned.replace(/\n?```\s*$/i, '')
  }
  
  // 移除常见的AI说明文字（这些说明通常在内容末尾）
  const tailPatterns = [
    // 带换行的说明
    /\n以上对原文进行了[^。]*。?\s*$/,
    /\n以上内容已[^。]*。?\s*$/,
    /\n已对[^。]*进行[^。]*。?\s*$/,
    /\n优化后的内容如下[：:]\s*$/,
    /\n以上内容在保留[^。]*基础上[^。]*。?\s*$/,
    /\n以上内容在保留原文[^。]*基础上[^。]*。?\s*$/,
    // 不带换行的说明
    /以上对原文进行了[^。]*。?\s*$/,
    /以上内容已[^。]*。?\s*$/,
    /已对[^。]*进行[^。]*。?\s*$/,
    /以上内容在保留[^。]*基础上[^。]*。?\s*$/,
    /以上内容在保留原文[^。]*基础上[^。]*。?\s*$/,
    // 更具体的匹配：包含"保留原文技术准确性"、"语言流畅性"、"段落结构"等关键词的说明
    /\n以上内容在保留原文技术准确性的基础上[^。]*。?\s*$/,
    /以上内容在保留原文技术准确性的基础上[^。]*。?\s*$/,
    // 匹配包含"同时，保留了原文的Markdown格式"的说明
    /\n[^。]*同时[^。]*保留了原文的Markdown格式[^。]*。?\s*$/,
    /[^。]*同时[^。]*保留了原文的Markdown格式[^。]*。?\s*$/,
  ]
  
  tailPatterns.forEach(pattern => {
    cleaned = cleaned.replace(pattern, '').trim()
  })
  
  // 特殊处理：匹配完整的长说明段落（如"以上内容在保留原文技术准确性的基础上..."）
  // 匹配模式：以"以上内容"开头，包含"保留"、"优化"、"修正"等关键词，以句号结尾的长段落
  const fullExplanationPattern = /\n以上内容[^。]*保留[^。]*基础上[^。]*优化[^。]*修正[^。]*同时[^。]*保留[^。]*格式[^。]*。\s*$/i
  if (fullExplanationPattern.test(cleaned)) {
    cleaned = cleaned.replace(fullExplanationPattern, '').trim()
  }
  
  // 特殊处理：如果末尾有很长的说明段落（超过50字符且包含说明性关键词），尝试移除
  const longTailMatch = cleaned.match(/\n([^。]{50,}(?:保留|优化|修正|流畅性|段落结构|Markdown格式)[^。]{0,100}。?\s*)$/i)
  if (longTailMatch) {
    const tailText = longTailMatch[1]
    // 如果尾部文字包含多个说明性关键词，则很可能是说明文字，移除它
    const keywordCount = [
      '保留', '优化', '修正', '流畅性', '段落结构', 'Markdown格式', 
      '技术准确性', '语言流畅度', '专业性', '语法', '标点'
    ].filter(keyword => tailText.includes(keyword)).length
    
    // 如果包含3个或以上关键词，且文字较长，很可能是说明文字
    if (keywordCount >= 3 && tailText.length > 50) {
      cleaned = cleaned.substring(0, longTailMatch.index).trim()
    }
  }
  
  return cleaned.trim()
}

// AI生成摘要
const handleGenerateSummary = async () => {
  if (!form.content || form.content.trim().length === 0) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  generatingSummary.value = true
  try {
    const summary = await generateSummary(form.content)
    if (summary && typeof summary === 'string') {
      form.summary = cleanAIContent(summary)
      ElMessage.success('摘要生成成功')
    } else {
      ElMessage.error('生成摘要失败：返回数据格式错误')
    }
  } catch (error: any) {
    const errorMsg = error?.message || '生成摘要失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    generatingSummary.value = false
  }
}

// AI润色内容
const handlePolishContent = async () => {
  if (!form.content || form.content.trim().length === 0) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  polishing.value = true
  try {
    const polished = await polishContent(form.content)
    if (polished && typeof polished === 'string') {
      form.content = cleanAIContent(polished)
      ElMessage.success('内容润色成功')
    } else {
      ElMessage.error('润色失败：返回数据格式错误')
    }
  } catch (error: any) {
    const errorMsg = error?.message || '润色失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    polishing.value = false
  }
}

// AI生成大纲（基于内容）
const handleGenerateOutline = async () => {
  if (!form.content || form.content.trim().length === 0) {
    ElMessage.warning('请先输入文章内容')
    return
  }

  generatingOutline.value = true
  try {
    const outline = await generateOutlineFromContent(form.content)
    if (outline && typeof outline === 'string') {
      const cleaned = cleanAIContent(outline)
      // 直接保存到表单的大纲字段
      form.outline = cleaned
      ElMessage.success('大纲生成成功')
    } else {
      ElMessage.error('生成大纲失败：返回数据格式错误')
    }
  } catch (error: any) {
    const errorMsg = error?.message || '生成大纲失败，请稍后重试'
    ElMessage.error(errorMsg)
  } finally {
    generatingOutline.value = false
  }
}

// 预览大纲（从表单中的大纲）
const handlePreviewOutline = () => {
  if (!form.outline || form.outline.trim().length === 0) {
    ElMessage.warning('请先输入或生成大纲')
    return
  }
  generatedOutline.value = form.outline
  showOutlineDialog.value = true
}

// 将大纲插入到文章开头（从表单）
const handleInsertOutlineToContent = () => {
  if (!form.outline || form.outline.trim().length === 0) {
    ElMessage.warning('请先输入或生成大纲')
    return
  }
  // 如果文章内容不为空，在大纲后添加分隔符
  const separator = form.content.trim() ? '\n\n---\n\n' : ''
  form.content = form.outline + separator + form.content
  ElMessage.success('大纲已插入到文章开头')
}

// 插入大纲到文章开头（从对话框）
const handleInsertOutline = () => {
  if (generatedOutline.value) {
    // 如果文章内容不为空，在大纲后添加两个换行
    const separator = form.content.trim() ? '\n\n---\n\n' : ''
    form.content = generatedOutline.value + separator + form.content
    // 同时保存大纲到form.outline字段
    form.outline = generatedOutline.value
    showOutlineDialog.value = false
    ElMessage.success('大纲已插入到文章开头并保存')
  }
}

// 关闭大纲对话框
const handleCloseOutlineDialog = () => {
  showOutlineDialog.value = false
  generatedOutline.value = ''
}

// 保存草稿
const handleSaveDraft = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    form.status = 0
    await saveArticle()
    saveFormSnapshot() // 更新快照
    ElMessage.success('草稿保存成功')
    router.push('/my-articles')
  } catch (error: any) {
    // 验证失败时，不显示错误（表单验证会自动显示）
    if (error?.message && !error.message.includes('验证')) {
      ElMessage.error(error.message || '保存草稿失败')
    }
  }
}

// 发布文章
const handlePublish = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    form.status = 1
    await saveArticle()
    saveFormSnapshot() // 更新快照
    ElMessage.success('文章发布成功')
    router.push('/my-articles')
  } catch (error: any) {
    // 验证失败时，不显示错误（表单验证会自动显示）
    if (error?.message && !error.message.includes('验证')) {
      ElMessage.error(error.message || '发布文章失败')
    }
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
      const result = await updateArticle(articleId.value, form)
      // 如果是新建文章，保存返回的文章ID
      if (result && typeof result === 'number') {
        articleId.value = result
        isEdit.value = true
      }
    } else {
      const result = await publishArticle(form)
      // 保存返回的文章ID
      if (result && typeof result === 'number') {
        articleId.value = result
        isEdit.value = true
      }
    }
  } catch (error: any) {
    const errorMsg = error?.message || '保存失败，请稍后重试'
    ElMessage.error(errorMsg)
    throw error
  } finally {
    loading.value = false
  }
}

// 格式化时间
const formatTime = (date: Date) => {
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const seconds = Math.floor(diff / 1000)
  const minutes = Math.floor(seconds / 60)
  const hours = Math.floor(minutes / 60)

  if (seconds < 60) {
    return '刚刚'
  } else if (minutes < 60) {
    return `${minutes}分钟前`
  } else if (hours < 24) {
    return `${hours}小时前`
  } else {
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
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

// 自动保存草稿（静默保存，不提示用户）
const autoSaveDraft = debounce(async () => {
  // 如果文章是已发布状态，不进行自动保存（避免将已发布文章改为草稿）
  if (form.status === 1) {
    return
  }

  // 如果没有标题或内容，不自动保存
  if (!form.title.trim() && !form.content.trim()) {
    return
  }

  // 如果正在保存或发布，不自动保存
  if (saving.value || publishing.value || autoSaving.value) {
    return
  }

  // 如果没有修改，不自动保存
  if (!hasChanges.value) {
    return
  }

  autoSaving.value = true
  try {
    // 只验证标题和内容，不验证分类（自动保存时分类可以为空）
    if (!formRef.value) return
    
    // 临时保存原始状态
    const originalStatus = form.status
    
    // 设置为草稿状态
    form.status = 0
    
    // 尝试保存（如果分类为空，会失败，但不影响）
    try {
      if (isEdit.value && articleId.value) {
        await updateArticle(articleId.value, form)
      } else if (form.title.trim() && form.content.trim()) {
        // 新建文章时，至少需要标题和内容
        const result = await publishArticle(form)
        if (result && typeof result === 'number') {
          articleId.value = result
          isEdit.value = true
        }
      }
      lastAutoSaveTime.value = new Date()
      // 更新快照
      saveFormSnapshot()
    } catch (error) {
      // 自动保存失败不提示用户
    } finally {
      form.status = originalStatus
    }
  } finally {
    autoSaving.value = false
  }
}, 30000) // 30秒防抖

// 键盘快捷键处理
const handleKeydown = (event: KeyboardEvent) => {
  // Ctrl+S 或 Cmd+S 保存草稿
  if ((event.ctrlKey || event.metaKey) && event.key === 's') {
    event.preventDefault()
    if (!saving.value && !publishing.value) {
      handleSaveDraft()
    }
  }
}

// 监听表单变化，自动保存
watch(
  () => [form.title, form.content, form.summary, form.outline],
  () => {
    if (form.title.trim() || form.content.trim()) {
      autoSaveDraft()
    }
  },
  { deep: true }
)

// 页面离开前提示
onBeforeRouteLeave((to, from, next) => {
  if (!hasChanges.value) {
    next()
    return
  }

  ElMessageBox.confirm('您有未保存的修改，确定要离开吗？', '提示', {
    confirmButtonText: '确定离开',
    cancelButtonText: '取消',
    type: 'warning',
    distinguishCancelAndClose: true
  })
    .then(() => {
      next()
    })
    .catch(() => {
      next(false)
    })
})

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

  // 注册键盘快捷键
  window.addEventListener('keydown', handleKeydown)

  // 启动自动保存定时器（每5分钟自动保存一次）
  autoSaveInterval.value = setInterval(() => {
    if (hasChanges.value && (form.title.trim() || form.content.trim())) {
      autoSaveDraft()
    }
  }, 5 * 60 * 1000) // 5分钟
})

// 清理
onBeforeUnmount(() => {
  // 移除键盘事件监听
  window.removeEventListener('keydown', handleKeydown)
  
  // 清除自动保存定时器
  if (autoSaveInterval.value) {
    clearInterval(autoSaveInterval.value)
    autoSaveInterval.value = null
  }
})
</script>

<style scoped lang="scss">
.article-editor-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);

  @media (max-width: 768px) {
    padding: 12px;
  }
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;

  h2 {
    margin: 0;
    font-size: 20px;
    font-weight: 600;
    color: #303133;
  }

  .header-actions {
    display: flex;
    gap: 12px;
    align-items: center;
    flex-wrap: wrap;

    .auto-save-hint {
      margin-right: 8px;
      font-size: 12px;
      color: #909399;
    }
  }

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: stretch;

    .header-actions {
      width: 100%;
      flex-direction: column;
      align-items: stretch;

      .auto-save-hint {
        margin-right: 0;
        margin-bottom: 8px;
        text-align: center;
      }
      
      .el-button {
        flex: 1;
      }
    }
  }
}

// 表单区域卡片
.form-section {
  margin-bottom: 20px;
  border-radius: 8px;
  transition: all 0.3s;

  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.08);
  }

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .content-stats {
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  :deep(.el-card__body) {
    padding: 20px;
  }

  :deep(.el-form-item) {
    margin-bottom: 20px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  // 内容表单项特殊处理
  :deep(.content-form-item) {
    .el-form-item__label {
      display: none !important;
    }
    
    .el-form-item__content {
      width: 100% !important;
      margin-left: 0 !important;
      flex: 1;
    }
  }
}

.form-section.content-card {
  :deep(.el-card__body) {
    padding: 12px 12px 16px 12px;
  }
}

// 编辑器包装器
.editor-wrapper {
  width: 100% !important;
  min-height: clamp(360px, 60vh, 720px);
  position: relative;
  
  .markdown-editor {
    width: 100% !important;
    min-height: clamp(360px, 60vh, 720px) !important;
    display: block !important;
    visibility: visible !important;
    opacity: 1 !important;
  }
}

// 封面上传区域
.cover-upload-wrapper {
  .cover-uploader {
    display: inline-block;
  }

  .cover-image {
    width: 100%;
    max-width: 500px;
    height: auto;
    aspect-ratio: 16 / 9;
    display: block;
    border-radius: 8px;
    object-fit: cover;
    cursor: pointer;
    transition: all 0.3s;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

    &:hover {
      opacity: 0.9;
      transform: scale(1.02);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
  }

  .cover-uploader-placeholder {
    width: 100%;
    max-width: 500px;
    aspect-ratio: 16 / 9;
    border: 2px dashed #d9d9d9;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s;
    background-color: #fafafa;

    &:hover {
      border-color: #409eff;
      background-color: #f0f9ff;

      .cover-uploader-icon {
        color: #409eff;
        transform: scale(1.1);
      }
    }
  }

  .cover-uploader-icon {
    font-size: 48px;
    color: #8c939d;
    margin-bottom: 8px;
    transition: all 0.3s;
  }

  .cover-uploader-text {
    font-size: 14px;
    color: #606266;
    font-weight: 500;
    margin-bottom: 4px;
  }

  .cover-uploader-hint {
    font-size: 12px;
    color: #909399;
  }

  .upload-tip {
    margin-top: 12px;
    display: flex;
    align-items: center;
    gap: 4px;

    .el-icon {
      font-size: 14px;
    }
  }
}

// AI工具栏
.ai-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4f8 100%);
  border-radius: 6px;
  flex-wrap: wrap;
  gap: 12px;

  .ai-tip {
    display: flex;
    align-items: center;
    gap: 4px;

    .el-icon {
      font-size: 14px;
      color: #409eff;
    }
  }

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: stretch;

    .el-space {
      width: 100%;
      
      .el-button {
        flex: 1;
      }
    }
  }
}

// Markdown编辑器
.markdown-editor {
  width: 100% !important;
  min-height: clamp(360px, 60vh, 720px) !important;
  border-radius: 6px;
  overflow: visible !important;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
  position: relative !important;

  :deep(.v-note-wrapper) {
    min-height: clamp(360px, 60vh, 720px) !important;
    width: 100% !important;
    display: flex !important;
    visibility: visible !important;
    position: relative !important;
  }

  :deep(.v-note-panel) {
    width: 100% !important;
    min-height: clamp(360px, 60vh, 720px) !important;
    display: flex !important;
    flex-direction: row !important;
  }

  :deep(.v-note-edit) {
    min-height: clamp(300px, 48vh, 600px) !important;
    padding: 0 12px 12px 12px !important;
  }

  :deep(.v-note-edit textarea) {
    padding: 12px !important;
  }

  :deep(.v-note-preview) {
    min-height: clamp(300px, 48vh, 600px) !important;
    padding: 0 12px 12px 12px !important;
  }

  :deep(.markdown-body) {
    padding: 12px;
    min-height: 500px;
  }

  @media (max-width: 768px) {
    min-height: 400px !important;
    
    :deep(.v-note-wrapper) {
      min-height: 400px !important;
    }
  }
}

// 大纲预览对话框
.outline-preview {
  max-height: 500px;
  overflow-y: auto;
  
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
    }
    
    :deep(ul),
    :deep(ol) {
      margin: 8px 0;
      padding-left: 24px;
    }
    
    :deep(li) {
      margin: 4px 0;
    }
  }
}

// 响应式优化
@media (max-width: 768px) {
  .article-editor-container {
    padding: 12px;
  }

  .form-section {
    :deep(.el-card__body) {
      padding: 16px;
    }
  }

  .cover-upload-wrapper {
    .cover-image,
    .cover-uploader-placeholder {
      max-width: 100%;
    }
  }
}

// 内容和大纲行布局
.content-outline-row {
  transition: all 0.3s ease;
  
  .el-col {
    transition: all 0.3s ease;
  }
}

// 大纲卡片特殊样式
.outline-card {
  position: sticky;
  top: 20px;
  max-height: calc(100vh - 120px);
  
  :deep(.el-card__body) {
    height: 100%;
    max-height: calc(100vh - 200px);
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 16px;
  }

  :deep(.el-form-item) {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
    margin-bottom: 0 !important;
  }

  :deep(.el-form-item__content) {
    flex: 1 1 auto;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  :deep(.el-textarea) {
    flex: 1 1 auto;
  }

  :deep(.el-textarea__inner) {
    flex: 1 1 auto;
    resize: none;
    min-height: 200px !important;
    line-height: 1.6;
  }
  
  .outline-actions {
    margin-top: auto !important;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
}

// 大纲和摘要输入区域
.outline-input-wrapper,
.summary-input-wrapper {
  .outline-actions {
    margin-top: 8px;
    display: flex;
    gap: 8px;
  }
}
</style>
