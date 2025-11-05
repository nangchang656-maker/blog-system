-- =====================================================
-- 文章模块数据表更新脚本
-- =====================================================
-- 说明：修复 article_tag 表结构，增加自增主键id
-- 日期：2025-11-04
-- =====================================================

USE blog_db;

-- 1. 备份旧表数据
CREATE TABLE IF NOT EXISTS `article_tag_backup` AS SELECT * FROM `article_tag`;

-- 2. 删除旧表
DROP TABLE IF EXISTS `article_tag`;

-- 3. 重新创建表（增加id主键）
CREATE TABLE `article_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`) USING BTREE COMMENT '联合唯一索引（防重复关联）',
  INDEX `idx_tag_id` (`tag_id`) USING BTREE COMMENT '标签ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章标签关联表';

-- 4. 恢复数据（如果有备份数据）
-- INSERT INTO `article_tag` (`article_id`, `tag_id`, `create_time`)
-- SELECT `article_id`, `tag_id`, `create_time` FROM `article_tag_backup`;

-- 5. 验证数据
-- SELECT COUNT(*) FROM article_tag;

-- 6. 确认无误后删除备份表
-- DROP TABLE IF EXISTS `article_tag_backup`;

-- =====================================================
-- 优化建议：增加复合索引（生产环境可选）
-- =====================================================

-- 文章表：优化查询已发布文章+按时间排序
ALTER TABLE `article` ADD INDEX `idx_status_create` (`status`, `create_time` DESC) COMMENT '状态+时间复合索引';

-- 文章表：优化查询分类下的已发布文章
ALTER TABLE `article` ADD INDEX `idx_category_status` (`category_id`, `status`) COMMENT '分类+状态复合索引';

-- =====================================================
-- 初始化测试数据（可选）
-- =====================================================

-- 插入更多分类数据
INSERT IGNORE INTO `category` (`name`, `sort`) VALUES
('Spring框架', 6),
('微服务架构', 7),
('DevOps', 8),
('算法与数据结构', 9),
('系统设计', 10);

-- 插入更多标签数据
INSERT IGNORE INTO `tag` (`name`) VALUES
('Docker'),
('Kubernetes'),
('Elasticsearch'),
('RocketMQ'),
('Nginx'),
('Git'),
('Linux'),
('面试'),
('设计模式'),
('性能优化');

-- 查看当前数据统计
SELECT
    'category' AS table_name, COUNT(*) AS record_count FROM category
UNION ALL
SELECT 'tag', COUNT(*) FROM tag
UNION ALL
SELECT 'user', COUNT(*) FROM user
UNION ALL
SELECT 'article', COUNT(*) FROM article;
