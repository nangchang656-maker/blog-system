import request from '@/utils/request'

// 用户登录请求参数
export interface LoginParams {
  username: string
  password: string
}

// 用户注册请求参数
export interface RegisterParams {
  username: string
  email: string
  phone?: string
  password: string
  code: string
}

// 用户信息
export interface UserInfo {
  id: number
  username: string
  email: string
  phone?: string
  nickname?: string
  avatar?: string
  bio?: string
  createTime?: string
  articleCount?: number
  followCount?: number
  fansCount?: number
}

// 修改用户信息参数
export interface UpdateUserParams {
  nickname?: string
  email?: string
  phone?: string
  avatar?: string
  bio?: string
}

// 修改密码参数
export interface UpdatePasswordParams {
  code: string
  newPassword: string
}

// 刷新Token参数
export interface RefreshTokenParams {
  userId: number
  refreshToken: string
}

// 刷新Token响应
export interface RefreshTokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: string
}

/**
 * 发送邮箱验证码
 */
export const sendEmailCodeApi = (email: string) => {
  return request.post('/api/user/code/email', { email })
}

/**
 * 用户注册
 */
export const registerApi = (data: RegisterParams) => {
  return request.post('/api/user/register', data)
}

/**
 * 用户登录
 */
export const loginApi = (data: LoginParams) => {
  return request.post<any, { token: string; userInfo: UserInfo }>('/api/user/login', data)
}

/**
 * 用户退出
 */
export const logoutApi = () => {
  return request.post('/api/user/logout')
}

/**
 * 获取用户信息
 */
export const getUserInfoApi = () => {
  return request.get<any, UserInfo>('/api/user/info')
}

/**
 * 修改用户信息
 */
export const updateUserInfoApi = (data: UpdateUserParams) => {
  return request.put('/api/user/info', data)
}

/**
 * 修改密码
 */
export const updatePasswordApi = (data: UpdatePasswordParams) => {
  return request.put('/api/user/password', data)
}

/**
 * 上传头像
 */
export const uploadAvatarApi = (file: File) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<any, { url: string }>('/api/user/avatar/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 刷新Token
 */
export const refreshTokenApi = (data: RefreshTokenParams) => {
  return request.post<any, RefreshTokenResponse>('/api/user/refresh-token', data)
}
