-- 修复点赞和收藏表的唯一索引问题
-- 问题：使用逻辑删除时，原有唯一索引不包含deleted字段，导致取消点赞/收藏后再次操作时会报唯一键冲突

-- 1. 修复 like_record 表
-- 删除原有唯一索引
ALTER TABLE `like_record` DROP INDEX `uk_user_target`;

-- 创建包含deleted字段的唯一索引
-- 只对未删除的记录(deleted=0)建立唯一约束
ALTER TABLE `like_record` ADD UNIQUE INDEX `uk_user_target_deleted`(`user_id`, `target_id`, `type`, `deleted`);

-- 2. 修复 collect 表
-- 删除原有唯一索引
ALTER TABLE `collect` DROP INDEX `uk_user_article`;

-- 创建包含deleted字段的唯一索引
-- 只对未删除的记录(deleted=0)建立唯一约束
ALTER TABLE `collect` ADD UNIQUE INDEX `uk_user_article_deleted`(`user_id`, `article_id`, `deleted`);

-- 3. 同样修复 follow 表（预防相同问题）
ALTER TABLE `follow` DROP INDEX `uk_user_follow`;
ALTER TABLE `follow` ADD UNIQUE INDEX `uk_user_follow_deleted`(`user_id`, `follow_user_id`, `deleted`);
