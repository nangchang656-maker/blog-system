<template>
  <div class="comment-section">
    <!-- 评论输入框 -->
    <div class="comment-input-box" v-if="userStore.isLoggedIn">
      <el-input
        v-model="commentContent"
        type="textarea"
        :rows="3"
        placeholder="写下你的评论..."
        maxlength="1000"
        show-word-limit
      />
      <div class="input-actions">
        <el-button type="primary" @click="handleSubmitComment" :loading="submitting">
          发表评论
        </el-button>
      </div>
    </div>
    <div v-else class="login-tip">
      <el-alert type="info" :closable="false">
        <template #default>
          请先<el-link type="primary" href="/login">登录</el-link>后再发表评论
        </template>
      </el-alert>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list" v-loading="loading">
      <div v-if="comments.length === 0" class="empty-comments">
        <el-empty description="暂无评论，快来发表第一条评论吧~" />
      </div>

      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <CommentItemComponent
          :comment="comment"
          :article-id="articleId"
          @reply="handleReply"
          @like="handleLikeComment"
          @delete="handleDeleteComment"
          @refresh="loadComments"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCommentList, createComment, type CommentItem, type CreateCommentParams } from '@/api/comment'
import CommentItemComponent from './CommentItem.vue'

interface Props {
  articleId: number
}

const props = defineProps<Props>()
const userStore = useUserStore()

const comments = ref<CommentItem[]>([])
const commentContent = ref('')
const loading = ref(false)
const submitting = ref(false)
const replyingTo = ref<{ id: number; nickname: string } | null>(null)

// 加载评论列表
const loadComments = async () => {
  loading.value = true
  try {
    const res = await getCommentList(props.articleId)
    comments.value = res
  } catch (error) {
    ElMessage.error('加载评论失败')
  } finally {
    loading.value = false
  }
}

// 提交评论
const handleSubmitComment = async () => {
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  submitting.value = true
  try {
    // 移除@用户名前缀（如果存在）
    let content = commentContent.value.trim()
    if (replyingTo.value && content.startsWith(`@${replyingTo.value.nickname} `)) {
      content = content.replace(`@${replyingTo.value.nickname} `, '')
    }

    const params: CreateCommentParams = {
      articleId: props.articleId,
      content: content
    }

    // 如果是回复评论
    if (replyingTo.value) {
      // 需要找到被回复的评论来确定parentId和toUserId
      const targetComment = findCommentById(comments.value, replyingTo.value.id)
      if (targetComment) {
        // 如果目标评论是根评论，parentId就是它的id，否则使用它的rootId
        params.parentId = targetComment.rootId > 0 ? targetComment.rootId : targetComment.id
        params.toUserId = targetComment.userId
      }
    }

    await createComment(params)
    ElMessage.success('评论发表成功')
    commentContent.value = ''
    replyingTo.value = null
    emit('comment-added')
    await loadComments()
  } catch (error: any) {
    ElMessage.error(error?.message || '发表评论失败')
  } finally {
    submitting.value = false
  }
}

// 查找评论（递归查找）
const findCommentById = (commentList: CommentItem[], id: number): CommentItem | null => {
  for (const comment of commentList) {
    if (comment.id === id) {
      return comment
    }
    if (comment.replies) {
      const found = findCommentById(comment.replies, id)
      if (found) return found
    }
  }
  return null
}

// 处理回复
const handleReply = (comment: CommentItem) => {
  replyingTo.value = { id: comment.id, nickname: comment.userNickname }
  commentContent.value = `@${comment.userNickname} `
  // 滚动到输入框
  setTimeout(() => {
    const inputBox = document.querySelector('.comment-input-box')
    inputBox?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, 100)
}

// 处理点赞评论
const handleLikeComment = async (commentId: number, isLiked: boolean) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    if (isLiked) {
      const { unlikeComment } = await import('@/api/comment')
      await unlikeComment(commentId)
    } else {
      const { likeComment } = await import('@/api/comment')
      await likeComment(commentId)
    }
    await loadComments()
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  }
}

// 处理删除评论
const handleDeleteComment = async (commentId: number) => {
  try {
    const { deleteComment } = await import('@/api/comment')
    await deleteComment(commentId)
    ElMessage.success('删除成功')
    await loadComments()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  }
}

onMounted(() => {
  loadComments()
})

// 定义emit
const emit = defineEmits<{
  'comment-added': []
}>()

// 暴露刷新方法给父组件
defineExpose({
  loadComments
})
</script>

<style scoped lang="scss">
.comment-section {
  .comment-input-box {
    margin-bottom: 24px;

    .input-actions {
      margin-top: 12px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .login-tip {
    margin-bottom: 24px;
  }

  .comment-list {
    .empty-comments {
      padding: 40px 0;
    }

    .comment-item {
      margin-bottom: 16px;
    }
  }
}
</style>

