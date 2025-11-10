import request from '@/utils/request'

/**
 * 文章相关接口
 */

// ========== 类型定义 ==========

/** 文章列表查询参数 */
export interface ArticleQueryParams {
  page: number
  size: number
  categoryId?: number
  tagId?: number
  keyword?: string
  status?: 0 | 1 // 状态筛选（仅"我的文章"使用）
  orderBy?: 'create_time' | 'view_count' | 'like_count'
  orderType?: 'asc' | 'desc'
}

/** 文章列表项 */
export interface ArticleListItem {
  id: number
  title: string
  summary: string
  coverImage: string
  categoryId: number
  categoryName: string
  tags: Array<{ id: number; name: string }>
  authorId: number
  authorName: string
  authorAvatar: string
  viewCount: number
  likeCount: number
  commentCount: number
  status?: number // 0-草稿 1-已发布 4-已屏蔽
  createTime: string
  updateTime: string
}

/** 文章详情 */
export interface ArticleDetail {
  id: number
  title: string
  content: string
  summary: string
  coverImage: string
  categoryId: number
  categoryName: string
  tags: Array<{ id: number; name: string }>
  authorId: number
  authorName: string
  authorAvatar: string
  viewCount: number
  likeCount: number
  commentCount: number
  collectCount: number
  isLiked: boolean
  isCollected: boolean
  createTime: string
  updateTime: string
}

/** 文章发布/编辑参数 */
export interface ArticleFormData {
  id?: number
  title: string
  content: string
  summary?: string
  coverImage?: string
  categoryId?: number
  categoryName?: string
  tagIds?: number[]
  tagNames?: string[]
  status: 0 | 1 // 0-草稿 1-发布
}

/** 分类 */
export interface Category {
  id: number
  name: string
  description?: string
  sort?: number
  articleCount?: number
}

/** 标签 */
export interface Tag {
  id: number
  name: string
  articleCount?: number
}

// ========== API接口 ==========

/**
 * 获取文章列表（分页）
 * @param params 查询参数
 * @returns 分页文章列表
 */
export function getArticleList(params: ArticleQueryParams) {
  return request({
    url: '/api/article/list',
    method: 'get',
    params
  })
}

/**
 * 获取文章详情
 * @param id 文章ID
 * @returns 文章详情（包含完整内容）
 */
export function getArticleDetail(id: number) {
  return request({
    url: `/api/article/${id}`,
    method: 'get'
  })
}

/**
 * 发布文章
 * @param data 文章数据（支持草稿/发布状态）
 * @returns 新建文章ID
 */
export function publishArticle(data: ArticleFormData) {
  return request({
    url: '/api/article/publish',
    method: 'post',
    data
  })
}

/**
 * 更新文章
 * @param id 文章ID
 * @param data 更新后的文章数据
 */
export function updateArticle(id: number, data: ArticleFormData) {
  return request({
    url: `/api/article/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除文章（逻辑删除）
 * @param id 文章ID
 */
export function deleteArticle(id: number) {
  return request({
    url: `/api/article/${id}`,
    method: 'delete'
  })
}

/**
 * 获取我的文章列表（包含草稿）
 * @param params 查询参数（可按status筛选草稿/已发布）
 * @returns 分页文章列表
 */
export function getMyArticles(params: ArticleQueryParams) {
  return request({
    url: '/api/article/my',
    method: 'get',
    params
  })
}

/**
 * 获取所有分类
 * @returns 分类列表（含文章数统计）
 */
export function getCategoryList() {
  return request({
    url: '/api/category/list',
    method: 'get'
  })
}

/**
 * 获取所有标签
 * @returns 标签列表
 */
export function getTagList() {
  return request({
    url: '/api/tag/list',
    method: 'get'
  })
}

/**
 * 获取热门标签（按文章数量排序）
 * @param limit 返回数量限制（默认10）
 * @returns 热门标签列表
 */
export function getHotTags(limit = 10) {
  return request({
    url: '/api/tag/hot',
    method: 'get',
    params: { limit }
  })
}

/**
 * 点赞文章
 * @param id 文章ID
 */
export function likeArticle(id: number) {
  return request({
    url: `/api/article/${id}/like`,
    method: 'post'
  })
}

/**
 * 取消点赞文章
 * @param id 文章ID
 */
export function unlikeArticle(id: number) {
  return request({
    url: `/api/article/${id}/unlike`,
    method: 'post'
  })
}

/**
 * 收藏文章
 * @param id 文章ID
 */
export function collectArticle(id: number) {
  return request({
    url: `/api/article/${id}/collect`,
    method: 'post'
  })
}

/**
 * 取消收藏文章
 * @param id 文章ID
 */
export function uncollectArticle(id: number) {
  return request({
    url: `/api/article/${id}/uncollect`,
    method: 'post'
  })
}

/**
 * AI生成文章摘要
 * @param content 文章内容（Markdown格式）
 * @returns 自动生成的摘要（100-200字）
 */
export function generateSummary(content: string) {
  return request({
    url: '/api/ai/summary',
    method: 'post',
    data: { content }
  })
}

/**
 * AI润色文章内容
 * @param content 原始内容
 * @returns 润色后的内容
 */
export function polishContent(content: string) {
  return request({
    url: '/api/ai/polish',
    method: 'post',
    data: { content }
  })
}

/**
 * AI生成文章大纲
 * @param topic 文章主题
 * @returns Markdown格式的文章大纲
 */
export function generateOutline(topic: string) {
  return request({
    url: '/api/ai/outline',
    method: 'post',
    data: { topic }
  })
}

/**
 * 获取我的收藏列表
 * @param params 分页参数
 * @returns 收藏的文章列表
 */
export function getMyCollections(params: { page: number; size: number }) {
  return request({
    url: '/api/article/my-collections',
    method: 'get',
    params
  })
}

/**
 * 上传文章封面
 * @param file 封面图片文件
 * @param articleId 文章ID(可选,编辑文章时传入)
 * @returns 封面URL
 */
export function uploadCoverImage(file: File, articleId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (articleId) {
    formData.append('articleId', articleId.toString())
  }
  return request.post<any, { url: string }>('/api/article/cover/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
