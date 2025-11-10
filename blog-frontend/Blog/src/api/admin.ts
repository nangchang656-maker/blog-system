import request from '@/utils/request'
import type { Category, Tag, ArticleListItem } from '@/api/article'

/**
 * 管理员相关接口
 */

// ========== 类型定义 ==========

/** 文章分类更新参数 */
export interface ArticleCategoryUpdateParams {
  articleId: number
  categoryId: number
}

/** 文章标签更新参数 */
export interface ArticleTagUpdateParams {
  articleId: number
  tagIds?: number[]
}

/** 用户管理列表项 */
export interface UserManageItem {
  id: number
  username: string
  nickname: string
  email: string
  phone: string
  avatar: string
  intro: string
  status: number // 1-正常 0-禁用
  articleCount: number
  commentCount: number
  createTime: string
  updateTime: string
}

/** 评论管理列表项 */
export interface CommentManageItem {
  id: number
  articleId: number
  articleTitle: string
  userId: number
  userNickname: string
  userAvatar: string
  content: string
  parentId: number
  rootId: number
  toUserId?: number
  toUserNickname?: string
  likeCount: number
  status: number // 1-正常 2-隐藏
  createTime: string
  updateTime: string
}

/** 评论状态更新参数 */
export interface CommentStatusUpdateParams {
  commentId: number
  status: number // 1-正常 2-隐藏
}

/** 数据统计 */
export interface Statistics {
  articleCount: number
  publishedArticleCount: number
  draftArticleCount: number
  userCount: number
  normalUserCount: number
  disabledUserCount: number
  commentCount: number
  normalCommentCount: number
  hiddenCommentCount: number
  totalViewCount: number
  totalLikeCount: number
  totalCollectCount: number
  categoryCount: number
  tagCount: number
}

/** 分页响应 */
export interface PageResponse<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ========== API接口 ==========

/**
 * 更新文章分类（分类重划分）
 */
export function updateArticleCategory(data: ArticleCategoryUpdateParams) {
  return request({
    url: '/admin/article/category',
    method: 'put',
    data
  })
}

/**
 * 更新文章标签（标签综合修改）
 */
export function updateArticleTags(data: ArticleTagUpdateParams) {
  return request({
    url: '/admin/article/tags',
    method: 'put',
    data
  })
}

/**
 * 获取文章列表（管理员，包含所有状态）
 */
export function getAdminArticleList(params: {
  current: number
  size: number
  keyword?: string
  status?: number
}) {
  return request<PageResponse<ArticleListItem>>({
    url: '/admin/articles',
    method: 'get',
    params
  })
}

/**
 * 更新文章状态（屏蔽/取消屏蔽）
 */
export function updateArticleStatus(data: { articleId: number; status: number }) {
  return request({
    url: '/admin/article/status',
    method: 'put',
    data
  })
}

/**
 * 获取用户列表（访客管理）
 */
export function getUserList(params: {
  current: number
  size: number
  keyword?: string
}) {
  return request<PageResponse<UserManageItem>>({
    url: '/admin/users',
    method: 'get',
    params
  })
}

/**
 * 更新用户状态（启用/禁用）
 */
export function updateUserStatus(userId: number, status: number) {
  return request({
    url: `/admin/user/${userId}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 获取评论列表（评论管理）
 */
export function getCommentList(params: {
  current: number
  size: number
  status?: number
  keyword?: string
}) {
  return request<PageResponse<CommentManageItem>>({
    url: '/admin/comments',
    method: 'get',
    params
  })
}

/**
 * 更新评论状态
 */
export function updateCommentStatus(data: CommentStatusUpdateParams) {
  return request({
    url: '/admin/comment/status',
    method: 'put',
    data
  })
}

/**
 * 删除评论（管理员）
 */
export function deleteComment(commentId: number) {
  return request({
    url: `/admin/comment/${commentId}`,
    method: 'delete'
  })
}

/**
 * 获取数据统计
 */
export function getStatistics() {
  return request<Statistics>({
    url: '/admin/statistics',
    method: 'get'
  })
}

// ==================== 分类管理 ====================

/** 分类管理参数 */
export interface CategoryManageParams {
  id?: number
  name: string
  sort: number
}

/**
 * 获取所有分类列表
 */
export function getCategoryList() {
  return request<Category[]>({
    url: '/admin/categories',
    method: 'get'
  })
}

/**
 * 创建分类
 */
export function createCategory(data: CategoryManageParams) {
  return request<number>({
    url: '/admin/category',
    method: 'post',
    data
  })
}

/**
 * 更新分类
 */
export function updateCategory(data: CategoryManageParams) {
  return request({
    url: '/admin/category',
    method: 'put',
    data
  })
}

/**
 * 删除分类
 */
export function deleteCategory(categoryId: number) {
  return request({
    url: `/admin/category/${categoryId}`,
    method: 'delete'
  })
}

// ==================== 标签管理 ====================

/** 标签管理参数 */
export interface TagManageParams {
  id?: number
  name: string
}

/**
 * 获取所有标签列表
 */
export function getTagList() {
  return request<Tag[]>({
    url: '/admin/tags',
    method: 'get'
  })
}

/**
 * 创建标签
 */
export function createTag(data: TagManageParams) {
  return request<number>({
    url: '/admin/tag',
    method: 'post',
    data
  })
}

/**
 * 更新标签
 */
export function updateTag(data: TagManageParams) {
  return request({
    url: '/admin/tag',
    method: 'put',
    data
  })
}

/**
 * 删除标签
 */
export function deleteTag(tagId: number) {
  return request({
    url: `/admin/tag/${tagId}`,
    method: 'delete'
  })
}

