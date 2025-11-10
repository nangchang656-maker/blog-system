<template>
  <div class="comment-item">
    <div class="comment-main">
      <el-avatar :size="40" :src="comment.userAvatar">
        {{ comment.userNickname?.charAt(0) }}
      </el-avatar>
      <div class="comment-content">
        <div class="comment-header">
          <span class="comment-author">{{ comment.userNickname }}</span>
          <span v-if="comment.toUserNickname" class="reply-to">
            回复 <span class="to-user">@{{ comment.toUserNickname }}</span>
          </span>
          <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
        </div>
        <div class="comment-text">{{ comment.content }}</div>
        <div class="comment-actions">
          <el-button
            link
            :type="comment.isLiked ? 'primary' : 'default'"
            :icon="comment.isLiked ? StarFilled : Star"
            @click="handleLike"
          >
            {{ comment.likeCount || 0 }}
          </el-button>
          <el-button link type="primary" @click="handleReply">回复</el-button>
          <el-button
            v-if="canDelete"
            link
            type="danger"
            @click="handleDelete"
          >
            删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 子评论（回复） -->
    <div v-if="comment.replies && comment.replies.length > 0" class="comment-replies">
      <CommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :article-id="articleId"
        @reply="$emit('reply', $event)"
        @like="$emit('like', $event, reply.isLiked)"
        @delete="$emit('delete', $event)"
        @refresh="$emit('refresh')"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import type { CommentItem } from '@/api/comment'

interface Props {
  comment: CommentItem
  articleId: number
}

const props = defineProps<Props>()
const userStore = useUserStore()

// 判断是否可以删除（只能删除自己的评论）
const canDelete = computed(() => {
  return userStore.isLoggedIn && userStore.userId === props.comment.userId
})

// 格式化时间
const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

// 处理点赞
const handleLike = () => {
  if (!userStore.isLoggedIn) {
    return
  }
  emit('like', props.comment.id, props.comment.isLiked)
}

// 处理回复
const handleReply = () => {
  emit('reply', props.comment)
}

// 处理删除
const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定要删除这条评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    emit('delete', props.comment.id)
  } catch {
    // 用户取消
  }
}

const emit = defineEmits<{
  reply: [comment: CommentItem]
  like: [commentId: number, isLiked: boolean]
  delete: [commentId: number]
  refresh: []
}>()
</script>

<style scoped lang="scss">
.comment-item {
  margin-bottom: 16px;

  .comment-main {
    display: flex;
    gap: 12px;

    .comment-content {
      flex: 1;

      .comment-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;
        font-size: 14px;

        .comment-author {
          font-weight: 500;
          color: #303133;
        }

        .reply-to {
          color: #909399;

          .to-user {
            color: #409eff;
          }
        }

        .comment-time {
          color: #909399;
          font-size: 12px;
        }
      }

      .comment-text {
        color: #606266;
        line-height: 1.6;
        margin-bottom: 8px;
        word-break: break-word;
      }

      .comment-actions {
        display: flex;
        gap: 16px;
        font-size: 14px;
      }
    }
  }

  .comment-replies {
    margin-left: 52px;
    margin-top: 12px;
    padding-left: 16px;
    border-left: 2px solid #f0f0f0;
  }
}
</style>

