# 🚀 MyBlog - 个人博客系统

<div align="center">

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.12-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-8.2-red)
![License](https://img.shields.io/badge/License-MIT-yellow)

一个功能完善的现代化个人博客系统，支持 Markdown 编辑、AI 辅助写作、评论互动等功能。

[在线演示](#) | [快速开始](#快速开始) | [技术文档](./claude.md)

</div>

---

## 📖 项目简介

MyBlog 是一个基于 **Spring Boot 3** + **Vue 3** 开发的全栈个人博客系统，集成了多项主流技术栈和中间件。项目采用前后端分离架构，提供完整的用户管理、文章发布、评论互动、后台管理等功能，并引入 AI 辅助写作、全文搜索、消息队列等技术亮点。

### ✨ 核心特色

- 🎨 **现代化界面**：基于 Vue3 + Element-Plus，提供流畅的用户体验
- 🤖 **AI 辅助写作**：集成智谱 AI，支持文章润色和大纲生成
- 🔍 **全文搜索**：基于 ElasticSearch 实现高性能文章搜索
- 💬 **评论互动**：支持楼中楼评论、点赞、收藏等社交功能
- 🔐 **安全可靠**：采用 JWT + RefreshToken 双 Token 机制
- 📊 **数据统计**：实时展示文章数、用户数、访问量等数据
- 🚀 **高性能**：Redis 缓存 + 消息队列框架（RocketMQ）

---

## 🎯 功能特性

### 访客端

#### 👤 用户模块
- [x] 用户注册（手机号/邮箱 + 验证码）
- [x] 用户登录（JWT 鉴权 + RefreshToken）
- [x] 个人中心（头像上传、昵称修改、个人简介、密码修改）

#### 📝 文章模块
- [x] Markdown 编辑器（支持实时预览）
- [x] 文章发布/编辑/删除
- [x] 草稿箱功能
- [x] 分类和标签管理
- [x] 文章列表（支持分页、多维度排序）
- [x] 文章详情（浏览量统计、相关推荐）
- [x] 全文搜索（ElasticSearch + 关键词高亮）

#### 💬 互动模块
- [x] 评论功能（支持楼中楼回复）
- [x] 点赞功能（文章点赞、评论点赞）
- [x] 收藏功能

### 管理端

- [x] 文章管理（批量操作、分类重划分、标签修改）
- [x] 用户管理（用户列表、状态管理）
- [x] 评论管理（审核、删除）
- [x] 数据统计（可视化仪表盘）

---

## 🛠️ 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.12 | 核心框架 |
| Spring Security | - | 安全框架 + 自定义 JWT 鉴权 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 关系型数据库 |
| Redis | 8.2 | 缓存 + 分布式锁 |
| ElasticSearch | 7.17.18 | 全文搜索引擎 |
| RocketMQ | 5.1.4 | 消息队列（框架已实现，待业务集成） |
| Hutool | - | Java 工具类库 |
| Swagger | - | API 文档 |
| JWT | - | Token 生成与验证 |

### 前端技术栈

| 技术 | 说明 |
|------|------|
| Vue 3 | 渐进式 JavaScript 框架 |
| Element-Plus | UI 组件库 |
| Vue Router | 路由管理 |
| Pinia | 状态管理 |
| Axios | HTTP 请求库 |
| markdown-it | Markdown 渲染 |
| highlight.js | 代码高亮 |
| mavon-editor | Markdown 编辑器 |

### 开发环境

- **JDK**: 17
- **Maven**: 3.9.9
- **Node.js**: 推荐 16+
- **Docker**: 可选（用于部署）

---

## 📁 项目结构

```
MyBlog/
├── blog-common/              # 公共基础模块
│   ├── annotation/           # 自定义注解
│   ├── config/               # 配置类（跨域、Redis、Security等）
│   ├── constants/            # 常量定义
│   ├── entity/               # 实体类（User、Article、Comment等）
│   ├── enums/                # 枚举类
│   ├── exception/            # 全局异常处理
│   ├── filter/               # 过滤器（JWT认证）
│   ├── handler/              # 处理器
│   ├── service/              # 公共服务（TokenService）
│   └── utils/                # 工具类（JWT、Redis、AES、Email等）
│
├── blog-application/         # 应用主模块
│   ├── controller/           # 控制器层
│   │   ├── admin/            # 后台管理接口
│   │   └── api/              # 前台API接口
│   ├── service/              # 业务逻辑层
│   ├── mapper/               # 数据访问层
│   ├── dto/                  # 数据传输对象
│   ├── vo/                   # 视图对象
│   ├── config/               # 配置（AI、ES、MinIO、MQ）
│   └── integration/          # 第三方集成（AI、ES、MQ、Storage）
│
├── blog-frontend/            # 前端项目（Vue3 + TypeScript）
│   └── Blog/
│       ├── src/              # 源码目录
│       ├── public/           # 静态资源
│       ├── package.json      # 项目配置
│       └── vite.config.ts    # Vite配置
│
├── sql/                      # 数据库脚本
│   └── blog_db.sql           # 初始化SQL（9张表）
│
├── claude.md                 # 项目开发文档
└── pom.xml                   # 父POM（多模块管理）
```

---

## 🚀 快速开始

### 环境准备

确保您的开发环境已安装：

- JDK 17+
- Maven 3.9+
- MySQL 8.0+
- Redis 8.0+
- Node.js 16+
- RocketMQ 5.1+ (可选，用于消息队列功能)

### 后端启动

1. **克隆项目**

```bash
git clone https://github.com/your-username/MyBlog.git
cd MyBlog
```

2. **导入数据库**

```bash
mysql -u root -p < sql/blog_db.sql
```

3. **修改配置**

编辑 `blog-application/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog_db
    username: your-username
    password: your-password
  redis:
    host: localhost
    port: 6379
```

4. **编译运行**

```bash
# 编译项目
mvn clean install

# 启动后端
cd blog-application
mvn spring-boot:run
```

后端服务将运行在 `http://localhost:8080`

### 前端启动

1. **安装依赖**

```bash
cd blog-frontend/Blog
npm install
```

2. **启动开发服务器**

```bash
npm run dev
```

前端服务将运行在 `http://localhost:5173`

---

## 💡 技术亮点

### 1️⃣ ElasticSearch 全文搜索 ⭐⭐⭐

- 文章内容实时同步到 ES
- 支持标题、内容、标签的全文搜索
- 搜索关键词高亮显示
- 智能搜索建议

### 2️⃣ Redis 多场景应用 ⭐⭐⭐

- **热门排行榜**：基于 ZSet 实现文章热度排序
- **数据缓存**：文章详情、用户信息等高频数据缓存
- **Token 管理**：RefreshToken 存储与过期控制
- **限流保护**：防止评论、点赞等接口被刷
- **分布式锁**：防止重复提交

### 3️⃣ RocketMQ 异步处理 ⭐⭐ (待完善)

- ✅ 完整的生产者/消费者框架（支持同步、异步、延迟消息）
- ✅ 消息主题和标签常量定义（博客、用户、评论、邮件）
- 🔄 待集成：异步发送评论通知
- 🔄 待集成：异步统计文章浏览量
- 🔄 待集成：解耦业务逻辑，提升系统性能

### 4️⃣ AI 辅助写作 ⭐⭐⭐

- 集成智谱 AI API
- 自动生成文章摘要和大纲
- 智能润色文章内容
- 提升写作效率

### 5️⃣ JWT 双 Token 机制 ⭐⭐

- **AccessToken**：短期有效（15分钟），用于接口鉴权
- **RefreshToken**：长期有效（7天），用于刷新 AccessToken
- 支持 Token 黑名单机制（修改密码、禁用账号立即失效）

---

## 📊 数据库设计

项目包含 9 张核心表：

- `user` - 用户表
- `article` - 文章表
- `category` - 分类表
- `tag` - 标签表
- `article_tag` - 文章标签关联表
- `comment` - 评论表
- `like_record` - 点赞记录表
- `collect` - 收藏表
- `admin` - 管理员表

详细的表结构设计请查看 `sql/blog_db.sql`

---

## 📝 API 文档

启动项目后，访问 Swagger 文档：

```
http://localhost:8080/swagger-ui.html
```

---

## 🔧 配置说明

### 邮件配置

编辑 `application.yml` 中的邮件配置：

```yaml
spring:
  mail:
    host: smtp.qq.com
    username: your-email@qq.com
    password: your-authorization-code
```

### AI 配置（可选）

如需启用 AI 辅助写作功能，请配置智谱 AI 的 API Key。

### ElasticSearch 配置（可选）

如需启用全文搜索功能，请配置 ElasticSearch 连接信息。

### RocketMQ 配置（可选）

如需启用消息队列功能，请取消 `application.yml` 中 RocketMQ 配置的注释：

```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    group: blog-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
```

---

## 📅 开发计划

- [x] 基础功能开发（用户、文章、评论、点赞、收藏）
- [x] 后台管理功能
- [x] JWT 鉴权机制
- [x] RocketMQ 框架搭建（生产者、消费者、常量定义）
- [x] ElasticSearch 全文搜索
- [x] AI 辅助写作
- [ ] RocketMQ 业务集成（评论通知、浏览量统计等）
- [x] 图片上传（对象存储）
- [x] 定时任务（数据统计、缓存清理）
- [ ] Docker 容器化部署

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

---

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。

---

## 📧 联系方式

如有问题或建议，欢迎通过以下方式联系：

- **项目地址**: [github.com/nangchang656-maker/blog-system](https://github.com/nangchang656-maker/blog-system)
- **Issue**: [提交问题](https://github.com/nangchang656-maker/blog-system/issues)

---

## 🌟 致谢

感谢以下开源项目的支持：

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [MyBatis-Plus](https://baomidou.com/)
- [Element-Plus](https://element-plus.org/)

---

<div align="center">
**如果这个项目对您有帮助，请给个 ⭐️ Star 支持一下！

</div>

