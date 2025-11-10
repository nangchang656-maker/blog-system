import request from '@/utils/request'

/**
 * 评论相关接口
 */

// ========== 类型定义 ==========

/** 评论项 */
export interface CommentItem {
  id: number
  articleId: number
  userId: number
  userNickname: string
  userAvatar: string
  content: string
  parentId: number
  rootId: number
  toUserId?: number
  toUserNickname?: string
  likeCount: number
  isLiked: boolean
  createTime: string
  replies?: CommentItem[] // 子评论列表
}

/** 创建评论参数 */
export interface CreateCommentParams {
  articleId: number
  content: string
  parentId?: number // 父评论ID，不传或0表示一级评论
  toUserId?: number // 回复的用户ID
}

// ========== API接口 ==========

/**
 * 获取文章评论列表
 * @param articleId 文章ID
 * @returns 评论列表（树形结构）
 */
export function getCommentList(articleId: number) {
  return request<CommentItem[]>({
    url: `/api/comment/article/${articleId}`,
    method: 'get'
  })
}

/**
 * 创建评论
 * @param data 评论数据
 * @returns 评论ID
 */
export function createComment(data: CreateCommentParams) {
  return request<number>({
    url: '/api/comment/create',
    method: 'post',
    data
  })
}

/**
 * 删除评论
 * @param id 评论ID
 */
export function deleteComment(id: number) {
  return request({
    url: `/api/comment/${id}`,
    method: 'delete'
  })
}

/**
 * 点赞评论
 * @param id 评论ID
 */
export function likeComment(id: number) {
  return request({
    url: `/api/comment/${id}/like`,
    method: 'post'
  })
}

/**
 * 取消点赞评论
 * @param id 评论ID
 */
export function unlikeComment(id: number) {
  return request({
    url: `/api/comment/${id}/unlike`,
    method: 'post'
  })
}

