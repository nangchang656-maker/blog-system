-- 为article表添加outline字段（文章大纲）
-- 执行时间：2025-11-10

ALTER TABLE `article` 
ADD COLUMN `outline` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文章大纲（Markdown格式）' AFTER `summary`;

