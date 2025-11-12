<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerApi, sendEmailCodeApi } from '@/api/user'
import { encrypt } from '@/utils/crypto'

const router = useRouter()

// 注册表单数据
const registerForm = reactive({
  username: '',
  email: '',
  phone: '',
  code: '',
  password: '',
  confirmPassword: ''
})

// 验证码倒计时
const countdown = ref(0)
const codeBtnText = ref('获取验证码')

// 自定义验证：密码强度
const validatePassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
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
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const formRef = ref()
const loading = ref(false)

// 发送验证码
const handleSendCode = async () => {
  if (!registerForm.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }

  // 验证邮箱格式
  const emailReg = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailReg.test(registerForm.email)) {
    ElMessage.warning('请输入正确的邮箱格式')
    return
  }

  if (countdown.value > 0) return

  try {
    await sendEmailCodeApi(registerForm.email)
    ElMessage.success('验证码已发送，请查收邮箱')
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

// 注册处理
const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid: boolean) => {
    if (valid) {
      loading.value = true
      try {
        await registerApi({
          username: registerForm.username,
          email: registerForm.email,
          phone: registerForm.phone || undefined,
          code: registerForm.code,
          password: encrypt(registerForm.password) // AES加密
        })
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        // 错误已在axios拦截器处理
      } finally {
        loading.value = false
      }
    }
  })
}

// 跳转登录
const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="register-container">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <h2>用户注册</h2>
          <p>Create Your Account</p>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        label-width="0"
        size="large"
      >
        <el-form-item prop="username">
          <el-input
            v-model="registerForm.username"
            placeholder="用户名"
            prefix-icon="User"
            clearable
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱"
            prefix-icon="Message"
            clearable
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item prop="code">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-input
              v-model="registerForm.code"
              placeholder="邮箱验证码"
              prefix-icon="Lock"
              maxlength="6"
              clearable
              style="flex: 1"
              @keyup.enter="handleRegister"
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

        <el-form-item prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="手机号（可选）"
            prefix-icon="Phone"
            clearable
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            type="password"
            placeholder="确认密码"
            prefix-icon="Lock"
            show-password
            @keyup.enter="handleRegister"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="form-footer">
          <span>已有账号？</span>
          <el-link type="primary" @click="goToLogin">立即登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 180px);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 450px;
  border-radius: 8px;
}

.card-header {
  text-align: center;
}

.card-header h2 {
  margin: 0 0 10px;
  color: #303133;
  font-size: 24px;
}

.card-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.form-footer {
  text-align: center;
  margin-top: 10px;
  color: #606266;
}

.form-footer span {
  margin-right: 5px;
}
</style>
