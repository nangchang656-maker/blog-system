1. 接口规范（RESTful）
  - POST /api/user/register - 注册
  - POST /api/user/login - 登录
  - POST /api/user/logout - 退出
  - GET /api/user/info - 获取用户信息
  - PUT /api/user/info - 更新用户信息
  - PUT /api/user/password - 修改密码

  2. 核心文件
  - ✅ utils/request.ts - axios封装，统一错误处理
  - ✅ api/user.ts - 用户API接口
  - ✅ stores/user.ts - Pinia用户状态管理

  3. 页面集成
  - ✅ LoginView - 登录功能
  - ✅ RegisterView - 注册功能
  - ✅ ProfileView - 个人信息展示（加载态+空态）
  - ✅ EditProfileView - 信息修改+密码修改
  - ✅ App.vue - 登录状态联动+退出登录

  4. 错误处理
  - 401未授权→清token跳登录
  - 403/404/500→ElMessage提示
  - 网络错误/超时→友好提示
  - 业务错误→显示后端返回message

  5. 成功提示
  - 登录/注册/保存/修改→ElMessage.success
  - Loading状态统一处理

1. API接口（RESTful）
  - ✅ POST /api/user/code/email - 发送邮箱验证码

  2. 注册页面更新
  - ✅ 新增验证码输入框
  - ✅ 新增"获取验证码"按钮
  - ✅ 60秒倒计时防重复发送
  - ✅ 邮箱格式前置校验

  3. 注册逻辑优化
  - ✅ 表单新增code字段（必填，6位）
  - ✅ 发送验证码前验证邮箱格式
  - ✅ 验证码发送成功提示
  - ✅ 注册时携带验证码提交

  4. 完整流程
  1. 输入邮箱
  2. 点击"获取验证码" → 后端发送邮件
  3. 输入收到的6位验证码
  4. 填写其他信息
  5. 提交注册 → 后端验证码校验

  1. 密码加密方案
  - ✅ 采用 AES 对称加密
  - ✅ 密钥：MyBlogSecretKey2025（前后端共享）
  - ✅ 传输层保护，与后端BCrypt存储独立

  2. 创建文件
  - ✅ utils/crypto.ts - 加密工具类（encrypt/decrypt）

  3. 更新页面
  - ✅ RegisterView - 注册时密码AES加密
  - ✅ LoginView - 登录时密码AES加密

  4. 完整流程
  前端明文密码 → AES加密 → 网络传输 → 后端AES解密 → BCrypt加密 → 数据库存储

  5. 安全性
  - 网络传输：AES加密保护
  - 数据存储：BCrypt单向加密
  - 双重保护，完全独立

  注意： 后端需同步实现AES解密逻辑，使用相同密钥 MyBlogSecretKey2025

  密码规范已更新：
  - ✅ 长度：8-12位
  - ✅ 必须包含英文字母（大小写均可）
  - ✅ 必须包含数字
  - ✅ 必须包含特殊符号（!@#$%^&*()_+-=[]{};':"\\|,.<>/?）

  实现方式：
  - 自定义验证器 validatePassword
  - 正则逐项校验，精准提示