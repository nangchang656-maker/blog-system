<template>
  <div class="statistics-view">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>数据统计</span>
          <el-button type="primary" :icon="Refresh" @click="loadStatistics">刷新</el-button>
        </div>
      </template>

      <div v-loading="loading" class="statistics-content">
        <!-- 文章统计 -->
        <el-row :gutter="20" class="statistics-row">
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                  <el-icon :size="32"><Document /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">文章总数</div>
                  <div class="stat-value">{{ statistics.articleCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                  <el-icon :size="32"><Check /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">已发布</div>
                  <div class="stat-value">{{ statistics.publishedArticleCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);">
                  <el-icon :size="32"><Edit /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">草稿</div>
                  <div class="stat-value">{{ statistics.draftArticleCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 用户统计 -->
        <el-row :gutter="20" class="statistics-row">
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);">
                  <el-icon :size="32"><User /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">用户总数</div>
                  <div class="stat-value">{{ statistics.userCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #30cfd0 0%, #330867 100%);">
                  <el-icon :size="32"><UserFilled /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">正常用户</div>
                  <div class="stat-value">{{ statistics.normalUserCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);">
                  <el-icon :size="32"><UserFilled /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">禁用用户</div>
                  <div class="stat-value">{{ statistics.disabledUserCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 评论统计 -->
        <el-row :gutter="20" class="statistics-row">
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);">
                  <el-icon :size="32"><ChatDotRound /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">评论总数</div>
                  <div class="stat-value">{{ statistics.commentCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%);">
                  <el-icon :size="32"><Check /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">正常评论</div>
                  <div class="stat-value">{{ statistics.normalCommentCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);">
                  <el-icon :size="32"><Warning /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">待审核</div>
                  <div class="stat-value">{{ statistics.hiddenCommentCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 访问统计 -->
        <el-row :gutter="20" class="statistics-row">
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #a1c4fd 0%, #c2e9fb 100%);">
                  <el-icon :size="32"><View /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">总访问量</div>
                  <div class="stat-value">{{ statistics.totalViewCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #ff9a56 0%, #ff6a88 100%);">
                  <el-icon :size="32"><Star /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">总点赞数</div>
                  <div class="stat-value">{{ statistics.totalLikeCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);">
                  <el-icon :size="32"><Collection /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">总收藏数</div>
                  <div class="stat-value">{{ statistics.totalCollectCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 分类标签统计 -->
        <el-row :gutter="20" class="statistics-row">
          <el-col :span="12">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                  <el-icon :size="32"><Folder /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">分类总数</div>
                  <div class="stat-value">{{ statistics.categoryCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="stat-card">
              <div class="stat-item">
                <div class="stat-icon" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);">
                  <el-icon :size="32"><PriceTag /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-label">标签总数</div>
                  <div class="stat-value">{{ statistics.tagCount }}</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Refresh,
  Document,
  Check,
  Edit,
  User,
  UserFilled,
  ChatDotRound,
  Warning,
  View,
  Star,
  Collection,
  Folder,
  PriceTag
} from '@element-plus/icons-vue'
import { getStatistics, type Statistics } from '@/api/admin'

const loading = ref(false)
const statistics = ref<Statistics>({
  articleCount: 0,
  publishedArticleCount: 0,
  draftArticleCount: 0,
  userCount: 0,
  normalUserCount: 0,
  disabledUserCount: 0,
  commentCount: 0,
  normalCommentCount: 0,
  hiddenCommentCount: 0,
  totalViewCount: 0,
  totalLikeCount: 0,
  totalCollectCount: 0,
  categoryCount: 0,
  tagCount: 0
})

const loadStatistics = async () => {
  loading.value = true
  try {
    const data = await getStatistics()
    statistics.value = data
  } catch (error) {
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped lang="scss">
.statistics-view {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .statistics-content {
    .statistics-row {
      margin-bottom: 20px;
    }

    .stat-card {
      .stat-item {
        display: flex;
        align-items: center;
        gap: 20px;

        .stat-icon {
          width: 64px;
          height: 64px;
          border-radius: 12px;
          display: flex;
          align-items: center;
          justify-content: center;
          color: white;
        }

        .stat-info {
          flex: 1;

          .stat-label {
            font-size: 14px;
            color: #909399;
            margin-bottom: 8px;
          }

          .stat-value {
            font-size: 28px;
            font-weight: 600;
            color: #303133;
          }
        }
      }
    }
  }
}
</style>

