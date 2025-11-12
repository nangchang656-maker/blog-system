<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { VueCropper } from 'vue-cropper'
import 'vue-cropper/dist/index.css'

interface Props {
  visible: boolean
  imageFile: File | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'confirm': [file: File]
}>()

const cropper = ref()
const imageUrl = ref('')
const loading = ref(false)

// 监听图片文件变化，加载图片
watch(
  () => props.imageFile,
  (newFile) => {
    if (newFile) {
      const reader = new FileReader()
      reader.onload = (e) => {
        // 这里的问号 ?. 是 TypeScript 的可选链操作符，意思是只有当 e.target 存在时才会去访问 result 属性，避免 e.target 可能为 null 或 undefined 时出现运行时错误。
        imageUrl.value = e.target?.result as string
      }
      reader.readAsDataURL(newFile)
    }
  },
  { immediate: true }
)

// 监听对话框打开，刷新裁剪器
watch(
  () => props.visible,
  async (visible) => {
    if (visible && imageUrl.value) {
      await nextTick()
      // 给一点时间让 cropper 渲染
      setTimeout(() => {
        if (cropper.value) {
          cropper.value.refresh()
        }
      }, 100)
    }
  }
)

// 确认裁剪
const handleConfirm = () => {
  if (!cropper.value) {
    ElMessage.error('裁剪器未初始化')
    return
  }

  loading.value = true

  // 获取裁剪后的 Blob
  cropper.value.getCropBlob((blob: Blob) => {
    loading.value = false

    if (!blob) {
      ElMessage.error('裁剪失败，请重试')
      return
    }

    // 转换为 File 对象
    const fileName = props.imageFile?.name || 'avatar.png'
    const file = new File([blob], fileName, { type: blob.type })

    emit('confirm', file)
    emit('update:visible', false)
  })
}

// 取消裁剪
const handleCancel = () => {
  emit('update:visible', false)
}

// 旋转图片
const rotateLeft = () => {
  cropper.value?.rotateLeft()
}

const rotateRight = () => {
  cropper.value?.rotateRight()
}

// 缩放图片
const zoomIn = () => {
  cropper.value?.changeScale(1)
}

const zoomOut = () => {
  cropper.value?.changeScale(-1)
}

// 重置
const reset = () => {
  cropper.value?.refresh()
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="(val) => emit('update:visible', val)"
    title="裁剪头像"
    width="700px"
    :close-on-click-modal="false"
  >
    <div class="cropper-container">
      <VueCropper
        ref="cropper"
        :img="imageUrl"
        :output-size="1"
        :output-type="'png'"
        :can-scale="true"
        :auto-crop="true"
        :auto-crop-width="200"
        :auto-crop-height="200"
        :fixed-box="false"
        :fixed="true"
        :fixed-number="[1, 1]"
        :center-box="true"
        :can-move="true"
        :can-move-box="true"
        :original="false"
        :info-true="true"
        :high="true"
        :enlarge="1"
        mode="contain"
      />
    </div>

    <div class="cropper-toolbar">
      <el-button-group>
        <el-button @click="zoomIn">放大</el-button>
        <el-button @click="zoomOut">缩小</el-button>
        <el-button @click="rotateLeft">左旋转</el-button>
        <el-button @click="rotateRight">右旋转</el-button>
        <el-button @click="reset">重置</el-button>
      </el-button-group>
    </div>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">确认裁剪</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.cropper-container {
  width: 100%;
  height: 400px;
  background-color: #f5f5f5;
  border-radius: 4px;
  overflow: hidden;
}

.cropper-toolbar {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
