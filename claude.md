# 个人博客系统开发

## 一、项目

个人博客系统+ai辅助[润色/总结大纲]

---

## 二、项目功能规划

### 核心功能（必须完成）

#### 1. 访客模块
- [x] 用户注册（手机号/邮箱 + **验证码**）
- [x] 用户登录（**JWT鉴权**）
- [x] 个人中心（**头像**、昵称、个人简介、修改密码）

> 普通注销场景：可以只删除 RefreshToken，接受15分钟的安全窗口期
> 敏感操作场景：保留黑名单机制（修改密码、禁用账号等），确保立即失效

#### 2. 文章模块
- [x] 文章发布（Markdown编辑器）
- [x] 文章编辑/删除
- [x] 文章草稿箱
- [x] 文章分类管理
- [x] 文章标签功能
- [x] 文章列表（分页、排序）
- [x] 文章详情（浏览量统计）
- [ ] 文章搜索（ElasticSearch全文搜索）

#### 3. 互动模块
- [x] 评论功能（支持楼中楼[不能太深]）
- [x] 点赞功能（文章点赞、评论点赞）
- [x] 收藏功能

#### 4. 后台管理
- [x] 文章管理（分类重划分,标签综合修改）
- [x] 访客管理
- [x] 评论管理
- [x] 数据统计（文章数、用户数、访问量等）- 展示

---

## 三、技术架构设计

### 技术栈清单

#### 后端技术栈
```
核心框架：
- Spring Boot 3.2.12
- Spring Security+自定义JWT鉴权+RefreshToken
- MyBatis-Plus 3.5.5

数据存储：
- MySQL 8.0
- Redis 8.2
- ElasticSearch 7.17.18 (可选) --版本,自定义配置

中间件：
- RocketMQ 5.1.4（可选）

工具库：
- Hutool（工具类）
- Lombok（简化代码）
- Swagger（接口文档）
- JWT（Token生成）

补充:
- maven 3.9.9
- jdk 17
```

#### 前端技术栈
```
框架：
- Vue 3
- Element-Plus
- Vue Router
- Pinia --新项目,选择

工具：
- Axios（HTTP请求）
- markdown-it（Markdown渲染）
- highlight.js（代码高亮）
- mavon-editor（Markdown编辑器）
```

#### 部署相关
```
- Docker（容器化）
- Nginx（反向代理） -- 可选,暂不决定
- 云服务器（阿里云/腾讯云）|| 本地部署  --暂不决定
```

### 数据库设计

#### 核心表结构(sql/init.sql)


### 项目结构(多模块Maven架构) - ✅ 已重构优化

```
MyBlog/
├── blog-common/             # 公共基础模块(已合并优化)
│   ├── src/main/java/cn/lzx/
│   │   ├── annotation/      # 自定义注解(1个) ✅
│   │   │   └── NoLogin.java # 免登录注解
│   │   ├── config/          # 配置类(4个) ✅
│   │   │   ├── CorsConfig.java              # 跨域配置
│   │   │   ├── RedisConfig.java            # Redis配置
│   │   │   ├── SecurityConfig.java         # Spring Security配置
│   │   │   └── NoLoginRequestMatcher.java  # 免登录请求匹配器
│   │   ├── constants/       # 常量定义(4个) ✅
│   │   │   ├── AdminConstants.java         # 后台管理常量
│   │   │   ├── ArticleOrderConstants.java  # 文章排序常量
│   │   │   ├── CommonConstants.java        # 通用常量
│   │   │   └── SecurityConstants.java      # 安全相关常量
│   │   ├── entity/          # 实体类(8个) ✅
│   │   │   ├── User.java                   # 用户实体
│   │   │   ├── Article.java                # 文章实体
│   │   │   ├── Category.java               # 分类实体
│   │   │   ├── Tag.java                    # 标签实体
│   │   │   ├── ArticleTag.java             # 文章标签关联
│   │   │   ├── Comment.java                # 评论实体
│   │   │   ├── LikeRecord.java             # 点赞记录实体
│   │   │   └── Collect.java                # 收藏实体
│   │   ├── enums/           # 枚举类(1个) ✅
│   │   │   └── RedisKeyEnum.java           # Redis键枚举
│   │   ├── exception/       # 异常处理(4个) ✅
│   │   │   ├── BaseException.java          # 基础异常类
│   │   │   ├── BusinessException.java      # 业务异常
│   │   │   ├── JwtException.java           # JWT相关异常
│   │   │   └── GlobalExceptionAdvice.java  # 全局异常处理器
│   │   ├── filter/          # 过滤器(1个) ✅
│   │   │   └── JwtAuthenticationFilter.java # JWT认证过滤器
│   │   ├── handler/         # 处理器(2个) ✅
│   │   │   ├── AuthenticationEntryPointImpl.java # 认证入口点
│   │   │   └── MyMetaObjectHandler.java    # MyBatis字段自动填充
│   │   ├── service/         # 公共服务(1个) ✅
│   │   │   └── TokenService.java           # Token管理服务
│   │   └── utils/           # 工具类(6个) ✅
│   │       ├── AesUtil.java                # AES加密工具
│   │       ├── EmailSendUtil.java          # 邮件发送工具
│   │       ├── JwtTokenUtil.java           # JWT工具类
│   │       ├── RedisUtil.java              # Redis工具类
│   │       ├── R.java                      # 统一响应结果
│   │       └── SecurityContextUtil.java    # 安全上下文工具
│   ├── src/main/resources/  # 资源目录(空)
│   └── pom.xml
│
├── blog-application/        # 应用主模块
│   ├── src/main/java/cn/lzx/blog/
│   │   ├── BlogApplication.java         # 启动类 ✅
│   │   ├── controller/      # 控制器层 ✅
│   │   │   ├── admin/       # 后台管理接口
│   │   │   └── api/         # 前台API接口
│   │   ├── service/         # 业务逻辑层 ✅
│   │   ├── mapper/          # 数据访问层 ✅
│   │   ├── config/          # 配置文件 ✅
│   │   │   ├── ai/          # AI配置
│   │   │   ├── es/          # ElasticSearch配置
│   │   │   ├── minio/       # MinIO配置
│   │   │   └── mq/          # RocketMQ配置
│   │   ├── integration/     # 集成工具类 ✅
│   │   │   ├── ai/          # AI工具类
│   │   │   ├── es/          # ElasticSearch工具类
│   │   │   ├── mq/          # RocketMQ工具类
│   │   │   └── storage/     # 存储工具类
│   │   ├── dto/             # 数据传输对象 ✅
│   │   └── vo/              # 视图对象 ✅
│   ├── src/main/resources/
│   │   └── application.yml  # 主配置文件 ✅
│   └── pom.xml
│
├── blog-frontend/           # 前端项目 ✅
│   └── Blog/                # Vue3 + TypeScript 项目(已初始化)
│       ├── src/             # 源码目录
│       ├── public/          # 静态资源
│       ├── node_modules/    # 依赖包(已安装)
│       ├── package.json     # 项目配置
│       ├── vite.config.ts   # Vite配置
│       ├── tsconfig.json    # TypeScript配置
│       └── index.html       # 入口HTML
│
├── sql/                     # 数据库脚本
│   └── blog_db.sql          # 初始化SQL(9张表) ✅
│
├── .vscode/                 # VS Code配置
├── .claude/                 # Claude配置
├── .gitignore               # Git忽略配置
├── claude.md                # 项目开发指导文档
└── pom.xml                  # 父POM(管理2个子模块)
```

---

## 四、开发计划

### 技术亮点（选择3-4个实现）

#### 亮点1：ElasticSearch全文搜索 ⭐⭐⭐

**实现内容：**

- 文章内容同步到ES
- 支持标题、内容、标签的全文搜索
- 搜索关键词高亮
- 搜索建议（输入提示）

**技术价值：**

- 展示中间件使用能力
- 展示搜索优化思路
- 面试高频考点

#### 亮点2：Redis多场景应用 ⭐⭐⭐

**实现内容：**

- 热门文章排行榜（ZSet）
- 文章详情缓存（String）
- 用户Token存储（String + 过期时间）
- 限流功能（防刷评论/点赞）
- 分布式锁（防止重复提交）

**技术价值：**

- 展示Redis多种数据结构的使用
- 展示性能优化思路
- 展示并发控制能力

#### 亮点3：RocketMQ异步处理 ⭐⭐

**实现内容：**

- 异步发送评论通知
- 异步统计文章浏览量

**技术价值：**

- 展示消息队列使用
- 展示系统解耦思路
- 面试常问知识点

#### 亮点4：AI辅助写作 ⭐⭐⭐(可选-但推荐)

**实现内容：**

- 使用智普AI生成文章摘要/大纲 - 用于展示
- 使用智普AI润色文章内容

**技术价值：**

- 展示AI集成能力（当前热点）
- 展示创新思维
- 增加项目差异化

#### 亮点5：图片上传（对象存储）⭐

**实现内容：**

- 集成七牛云/阿里云OSS
- 文章配图上传
- 用户头像上传

**技术价值：**

- 展示第三方服务集成
- 解决实际业务问题

#### 亮点6：定时任务 ⭐

**实现内容：**

- 定时统计文章数据
- 定时清理过期缓存
- 定时生成热门榜单

**技术价值：**

- 展示任务调度能力
- 展示系统自动化思路

