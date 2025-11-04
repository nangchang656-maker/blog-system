<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Lock } from '@element-plus/icons-vue'
import type { UploadProps, UploadRawFile } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateUserInfoApi, updatePasswordApi, sendEmailCodeApi, uploadAvatarApi } from '@/api/user'
import { encrypt } from '@/utils/crypto'
import { compressImage, blobToFile } from '@/utils/imageCompress'
import ImageCropper from '@/components/ImageCropper.vue'

const router = useRouter()
const userStore = useUserStore()

// 编辑表单数据
const editForm = reactive({
  avatar: '',
  nickname: '',
  email: '',
  phone: '',
  bio: ''
})

// 密码修改表单
const passwordForm = reactive({
  code: '',
  newPassword: '',
  confirmPassword: ''
})

// 验证码倒计时
const countdown = ref(0)
const codeBtnText = ref('获取验证码')
const avatarUploading = ref(false)

// 图片裁剪相关
const showCropper = ref(false)
const currentImageFile = ref<File | null>(null)

// 初始化表单
onMounted(async () => {
  // 如果 userInfo 为空，先获取用户信息
  if (!userStore.userInfo && userStore.token) {
    await userStore.getUserInfo()
  }

  // 填充表单数据
  if (userStore.userInfo) {
    Object.assign(editForm, {
      avatar: userStore.userInfo.avatar || '',
      nickname: userStore.userInfo.nickname || '',
      email: userStore.userInfo.email || '',
      phone: userStore.userInfo.phone || '',
      bio: userStore.userInfo.bio || ''
    })
  }
})

// 自定义验证：密码强度
const validatePassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入新密码'))
  } else if (value.length < 8 || value.length > 12) {
    callback(new Error('密码长度为8-12位'))
  } else if (!/[a-zA-Z]/.test(value)) {
    callback(new Error('密码必须包含英文字母'))
  } else if (!/\d/.test(value)) {
    callback(new Error('密码必须包含数字'))
  } else if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(value)) {
    callback(new Error('密码必须包含特殊符号'))
  } else {
    callback()
  }
}

// 自定义验证：确认密码
const validateConfirmPassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const editRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '昵称长度在2-20个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  bio: [
    { max: 200, message: '个人简介最多200个字符', trigger: 'blur' }
  ]
}

const passwordRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const editFormRef = ref()
const passwordFormRef = ref()
const loading = ref(false)
const activeTab = ref('info')

// 发送验证码
const handleSendCode = async () => {
  if (!userStore.userInfo?.email) {
    ElMessage.warning('无法获取用户邮箱')
    return
  }

  if (countdown.value > 0) return

  try {
    await sendEmailCodeApi(userStore.userInfo.email)
    ElMessage.success('验证码已发送至您的邮箱，请查收')
    countdown.value = 60

    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        codeBtnText.value = '获取验证码'
      } else {
        codeBtnText.value = `${countdown.value}s后重试`
      }
    }, 1000)
  } catch (error) {
    // 错误已在axios拦截器处理
  }
}

// 处理文件选择
const handleFileChange = async (file: any) => {
  const rawFile = file.raw as File

  // 1. 检查文件类型
  const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg']
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.error('头像必须是 JPG 或 PNG 格式')
    return
  }

  // 2. 检查文件大小
  const maxSize = 5 * 1024 * 1024 // 放宽到 5MB，因为压缩后会变小
  if (rawFile.size > maxSize) {
    ElMessage.error('头像大小不能超过 5MB')
    return
  }

  // 3. 检查图片尺寸
  try {
    await validateImageSize(rawFile)

    // 通过验证，保存文件并打开裁剪器
    currentImageFile.value = rawFile
    showCropper.value = true
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
        if (img.width < 50 || img.height < 50) {
          ElMessage.error('图片尺寸过小，建议至少 50x50 像素')
          reject(new Error('图片尺寸过小'))
          return
        }
        // 检查最大尺寸
        if (img.width > 4096 || img.height > 4096) {
          ElMessage.error('图片尺寸过大，建议不超过 4096x4096 像素')
          reject(new Error('图片尺寸过大'))
          return
        }
        resolve()
      }
      img.onerror = () => {
        ElMessage.error('图片加载失败，请重试')
        reject(new Error('图片加载失败'))
      }
      img.src = e.target?.result as string
    }
    reader.onerror = () => {
      ElMessage.error('文件读取失败，请重试')
      reject(new Error('文件读取失败'))
    }
    reader.readAsDataURL(file)
  })
}

// 裁剪确认后上传
const handleCropperConfirm = async (croppedFile: File) => {
  avatarUploading.value = true

  try {
    // 1. 压缩图片
    const compressedBlob = await compressImage(croppedFile, {
      quality: 0.8,
      maxWidth: 800,
      maxHeight: 800,
      mimeType: 'image/jpeg'
    })

    // 2. 转换为 File
    const fileName = `avatar_${Date.now()}.jpg`
    const finalFile = blobToFile(compressedBlob, fileName)

    console.log('原始文件大小:', (croppedFile.size / 1024).toFixed(2), 'KB')
    console.log('压缩后大小:', (finalFile.size / 1024).toFixed(2), 'KB')

    // 3. 上传到服务器
    await doUpload(finalFile)
  } catch (error: any) {
    console.error('头像处理失败:', error)
    ElMessage.error(error.message || '头像处理失败，请重试')
    avatarUploading.value = false
  }
}

// 执行上传
const doUpload = async (file: File) => {
  try {
    console.log('🚀 开始上传头像...')

    // 1. 调用上传接口
    const result = await uploadAvatarApi(file)
    console.log('✅ 上传成功，返回URL:', result.url)

    // 2. 添加时间戳防止浏览器缓存
    const avatarUrlWithTimestamp = `${result.url}?t=${Date.now()}`
    console.log('📝 添加时间戳:', avatarUrlWithTimestamp)

    // 3. 更新本地表单头像 URL
    editForm.avatar = avatarUrlWithTimestamp

    console.log('💾 准备保存到数据库...')
    // 4. 立即保存到后端数据库（保存不带时间戳的URL）
    await updateUserInfoApi({
      nickname: editForm.nickname,
      phone: editForm.phone || undefined,
      avatar: result.url,  // 数据库保存原始URL
      bio: editForm.bio
    })
    console.log('✅ 数据库保存成功')

    console.log('🔄 准备刷新用户信息...')
    // 5. 刷新全局用户信息状态
    await userStore.getUserInfo()
    console.log('✅ 用户信息刷新成功')

    ElMessage.success('头像上传成功')
    console.log('🎉 全部完成！')
  } catch (error: any) {
    // 详细的错误处理
    console.error('❌ 头像上传失败:', error)

    if (error.response) {
      // 后端返回的错误
      const message = error.response.data?.msg || error.response.data?.message || '头像上传失败'
      ElMessage.error(message)
    } else if (error.message) {
      // 网络错误或其他错误
      if (error.message.includes('Network')) {
        ElMessage.error('网络连接失败，请检查网络后重试')
      } else {
        ElMessage.error(`上传失败: ${error.message}`)
      }
    } else {
      ElMessage.error('头像上传失败，请重试')
    }
  } finally {
    avatarUploading.value = false
  }
}

// 保存基本信息
const handleSaveInfo = async () => {
  if (!editFormRef.value) return

  await editFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        await updateUserInfoApi({
          nickname: editForm.nickname,
          phone: editForm.phone || undefined,
          avatar: editForm.avatar,
          bio: editForm.bio
        })
        await userStore.getUserInfo()
        ElMessage.success('保存成功')
      } catch (error) {
        // 错误已在axios拦截器处理
      } finally {
        loading.value = false
      }
    }
  })
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordFormRef.value) return

  await passwordFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        await updatePasswordApi({
          code: passwordForm.code,
          newPassword: encrypt(passwordForm.newPassword) // AES加密
        })
        ElMessage.success('密码修改成功，请重新登录')
        passwordFormRef.value.resetFields()
        await userStore.logout()
        router.push('/login')
      } catch (error) {
        // 错误已在axios拦截器处理
      } finally {
        loading.value = false
      }
    }
  })
}

// 返回个人中心
const goBack = () => {
  router.push('/profile')
}
</script>

<template>
  <div class="edit-profile-container">
    <el-card class="edit-card">
      <template #header>
        <div class="card-header">
          <h2>编辑资料</h2>
          <el-button text @click="goBack">返回个人中心</el-button>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <!-- 基本信息编辑 -->
        <el-tab-pane label="基本信息" name="info">
          <el-form
            ref="editFormRef"
            :model="editForm"
            :rules="editRules"
            label-width="100px"
          >
            <el-form-item label="头像">
              <el-upload
                class="avatar-uploader"
                :show-file-list="false"
                :auto-upload="false"
                :on-change="handleFileChange"
                :disabled="avatarUploading"
                accept="image/jpeg,image/png,image/jpg"
              >
                <div v-loading="avatarUploading" element-loading-text="上传中...">
                  <img v-if="editForm.avatar" :src="editForm.avatar" class="avatar" />
                  <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                </div>
              </el-upload>
              <div class="upload-tip">
                支持 JPG、PNG 格式，大小不超过 5MB<br />
                自动裁剪为正方形并压缩至合适大小
              </div>
            </el-form-item>

            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
            </el-form-item>

            <el-form-item label="邮箱">
              <el-input v-model="editForm.email" disabled>
                <template #append>
                  <el-tooltip content="邮箱不可修改" placement="top">
                    <el-icon><Lock /></el-icon>
                  </el-tooltip>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item label="手机号" prop="phone">
              <el-input v-model="editForm.phone" placeholder="请输入手机号" />
            </el-form-item>

            <el-form-item label="个人简介" prop="bio">
              <el-input
                v-model="editForm.bio"
                type="textarea"
                :rows="4"
                placeholder="介绍一下自己吧"
                maxlength="200"
                show-word-limit
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSaveInfo">
                保存修改
              </el-button>
              <el-button @click="goBack">取消</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 密码修改 -->
        <el-tab-pane label="修改密码" name="password">
          <el-alert
            title="密码修改需要邮箱验证"
            type="info"
            :closable="false"
            style="margin-bottom: 20px"
          >
            为了账户安全，修改密码需要通过邮箱验证。验证码将发送至您的注册邮箱：{{ userStore.userInfo?.email }}
          </el-alert>

          <el-form
            ref="passwordFormRef"
            :model="passwordForm"
            :rules="passwordRules"
            label-width="100px"
            style="max-width: 500px"
          >
            <el-form-item label="验证码" prop="code">
              <div style="display: flex; gap: 10px; width: 100%">
                <el-input
                  v-model="passwordForm.code"
                  placeholder="请输入邮箱验证码"
                  maxlength="6"
                  clearable
                  style="flex: 1"
                />
                <el-button
                  :disabled="countdown > 0"
                  @click="handleSendCode"
                  style="width: 120px"
                >
                  {{ codeBtnText }}
                </el-button>
              </div>
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="passwordForm.newPassword"
                type="password"
                placeholder="请输入新密码（8-12位，需包含字母、数字、特殊符号）"
                show-password
              />
            </el-form-item>

            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="passwordForm.confirmPassword"
                type="password"
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleChangePassword">
                确认修改
              </el-button>
              <el-button @click="passwordFormRef.resetFields()">重置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 图片裁剪对话框 -->
    <ImageCropper
      v-model:visible="showCropper"
      :image-file="currentImageFile"
      @confirm="handleCropperConfirm"
    />
  </div>
</template>

<style scoped>
.edit-profile-container {
  max-width: 800px;
  margin: 0 auto;
}

.edit-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.avatar-uploader {
  display: inline-block;
}

.avatar-uploader .avatar {
  width: 120px;
  height: 120px;
  display: block;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s;
}

.avatar-uploader-icon:hover {
  border-color: #409eff;
  color: #409eff;
}

.upload-tip {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}
</style>
