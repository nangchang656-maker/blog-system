<template>
  <div class="home-container">
    <!-- 示例：文章列表使用 Grid 布局 -->
    <div class="article-grid">
      <el-card v-for="i in 6" :key="i" class="article-card">
        <h3>文章标题 {{ i }}</h3>
        <p>这是文章摘要内容...</p>
        <div class="meta">
          <span>作者</span>
          <span>2025-01-01</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
// 后续从 API 获取文章列表数据
</script>

<style scoped>
.home-container {
  width: 100%;
}

/* Grid 多列布局 - 自适应 */
.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  width: 100%;
}

.article-card {
  cursor: pointer;
  transition: transform 0.3s;
}

.article-card:hover {
  transform: translateY(-5px);
}

.article-card h3 {
  margin: 0 0 10px;
  font-size: 18px;
  color: #303133;
}

.article-card p {
  margin: 0 0 15px;
  color: #606266;
  line-height: 1.6;
}

.meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

/* 响应式：平板 2列 */
@media screen and (max-width: 1024px) {
  .article-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* 响应式：手机 1列 */
@media screen and (max-width: 768px) {
  .article-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
}
</style>
