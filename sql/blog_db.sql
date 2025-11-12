/*
 Navicat Premium Data Transfer

 Source Server         : wsl
 Source Server Type    : MySQL
 Source Server Version : 80044
 Source Host           : 172.28.131.158:3306
 Source Schema         : blog_db

 Target Server Type    : MySQL
 Target Server Version : 80044
 File Encoding         : 65001

 Date: 10/11/2025 17:46:28
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for article
-- ----------------------------
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '作者ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标题',
  `cover_image` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '封面图',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `summary` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '摘要',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `view_count` int NULL DEFAULT 0 COMMENT '浏览量',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NULL DEFAULT 0 COMMENT '评论数',
  `collect_count` int NULL DEFAULT 0 COMMENT '收藏数',
  `is_top` tinyint NULL DEFAULT 0 COMMENT '是否置顶：0否，1是',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0草稿，1已发布,  4屏蔽',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_is_top`(`is_top` ASC) USING BTREE,
  INDEX `idx_category_status`(`category_id` ASC, `status` ASC) USING BTREE COMMENT '分类+状态复合索引'
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of article
-- ----------------------------
INSERT INTO `article` VALUES (13, 3, '秋风起', 'http://localhost:9000/blog-files/covers/user_3_temp_1762741721268.jpg', '1.  秋风拂过街巷，卷着金黄落叶翩跹。它掠过树梢沙沙作响，携走暑气，将桂香揉进风里，给人间捎来清冽，也催熟了枝头饱满的果实。\n2.  秋风带着凉意漫过田野，稻浪翻涌成金涛。它吻红枫叶，摇落银杏，轻拍窗台，送来远处果园的甜香，让整个世界浸在清爽的诗意中。\n3.  秋风起时天渐朗，它穿梭在林间，把绿叶染成斑斓色彩。风里藏着草木的干爽气息，拂过脸颊，驱散烦闷，留下满心澄澈与安宁。\n4.  秋风像温柔的信使，掠过荷塘残叶，穿过晾晒的谷物。它卷起枯叶铺成地毯，携着流云赶路，悄悄把秋的静谧与丰盈洒满大地。\n5.  秋风掠过屋顶瓦檐，摇响了挂着的风铃。它裹着薄凉，吹散最后一丝暑热，催得菊花绽放，每一缕风都藏着秋日独有的清欢。', '', 16, 27, 1, 3, 1, 0, 1, 0, '2025-11-10 10:31:06', '2025-11-10 09:33:05');

-- ----------------------------
-- Table structure for article_tag
-- ----------------------------
DROP TABLE IF EXISTS `article_tag`;
CREATE TABLE `article_tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_article_tag`(`article_id` ASC, `tag_id` ASC) USING BTREE COMMENT '联合唯一索引（防重复关联）',
  INDEX `idx_tag_id`(`tag_id` ASC) USING BTREE COMMENT '标签ID索引'
) ENGINE = InnoDB AUTO_INCREMENT = 26 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of article_tag
-- ----------------------------
INSERT INTO `article_tag` VALUES (25, 13, 21, '2025-11-10 17:09:36');

-- ----------------------------
-- Table structure for category
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of category
-- ----------------------------
INSERT INTO `category` VALUES (15, 'test-title', 0, 0, '2025-11-05 16:29:49', '2025-11-10 11:34:09');
INSERT INTO `category` VALUES (16, '秋天', 1, 0, '2025-11-10 10:31:06', '2025-11-10 10:31:06');

-- ----------------------------
-- Table structure for collect
-- ----------------------------
DROP TABLE IF EXISTS `collect`;
CREATE TABLE `collect`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_article_deleted`(`user_id` ASC, `article_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_article_id`(`article_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of collect
-- ----------------------------
INSERT INTO `collect` VALUES (8, 3, 13, 0, '2025-11-10 03:01:12');

-- ----------------------------
-- Table structure for comment
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `article_id` bigint NOT NULL COMMENT '文章ID',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `root_id` bigint NULL DEFAULT 0 COMMENT '根评论ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父评论ID（0表示一级评论）',
  `to_user_id` bigint NULL DEFAULT NULL COMMENT '回复的用户ID',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `status` tinyint NULL DEFAULT 1 COMMENT '审核状态：1正常显示，2已隐藏-待审核',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_article_id`(`article_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_root_id`(`root_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '评论表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of comment
-- ----------------------------
INSERT INTO `comment` VALUES (1, 3, 13, '秋天的风冷冽!', 0, 0, NULL, 1, 1, 0, '2025-11-10 11:01:54', '2025-11-10 11:35:26');
INSERT INTO `comment` VALUES (2, 3, 13, '嘿嘿,有点冷呢!', 1, 1, 3, 0, 1, 0, '2025-11-10 11:06:53', '2025-11-10 11:06:53');
INSERT INTO `comment` VALUES (3, 4, 13, '哦呦喂~', 0, 0, NULL, 1, 1, 0, '2025-11-10 11:37:10', '2025-11-10 03:37:32');

-- ----------------------------
-- Table structure for follow
-- ----------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `follow_user_id` bigint NOT NULL COMMENT '关注的用户ID',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_follow_deleted`(`user_id` ASC, `follow_user_id` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_follow_user_id`(`follow_user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '关注表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of follow
-- ----------------------------

-- ----------------------------
-- Table structure for like_record
-- ----------------------------
DROP TABLE IF EXISTS `like_record`;
CREATE TABLE `like_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `target_id` bigint NOT NULL COMMENT '目标ID（文章ID或评论ID）',
  `type` tinyint NOT NULL COMMENT '类型：1文章，2评论',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_target_deleted`(`user_id` ASC, `target_id` ASC, `type` ASC, `deleted` ASC) USING BTREE,
  INDEX `idx_target`(`target_id` ASC, `type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '点赞记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of like_record
-- ----------------------------
INSERT INTO `like_record` VALUES (9, 3, 13, 1, 0, '2025-11-10 03:01:15');
INSERT INTO `like_record` VALUES (10, 3, 1, 2, 0, '2025-11-10 11:06:36');
INSERT INTO `like_record` VALUES (11, 4, 3, 2, 0, '2025-11-10 11:37:33');

-- ----------------------------
-- Table structure for tag
-- ----------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 22 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of tag
-- ----------------------------
INSERT INTO `tag` VALUES (20, 'test-tag', 0, '2025-11-05 16:29:49', '2025-11-10 11:34:25');
INSERT INTO `tag` VALUES (21, '秋风', 0, '2025-11-10 10:31:06', '2025-11-10 10:31:06');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `intro` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个人简介',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_email`(`email` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, 'admin', '$2a$10$FlkNg4nVG33a30mWTVxvmeJ12f./CbRGTsncghPfuLmoomtqaNqU6', 'http://localhost:9000/blog-files/avatars/user_1_avatar.jpg?t=1762745089872', 'admin', 'admin@blog.com', NULL, '', 1, 0, '2025-11-10 03:23:07', '2025-11-10 11:24:52');
INSERT INTO `user` VALUES (3, 'lzx', '$2a$10$FlkNg4nVG33a30mWTVxvmeJ12f./CbRGTsncghPfuLmoomtqaNqU6', 'http://localhost:9000/blog-files/avatars/user_3_avatar.jpg', 'lzx', '2823980197@qq.com', NULL, '', 1, 0, '2025-11-09 11:58:22', '2025-11-10 11:34:52');
INSERT INTO `user` VALUES (4, 'll', '$2a$10$FlkNg4nVG33a30mWTVxvmeJ12f./CbRGTsncghPfuLmoomtqaNqU6', 'http://localhost:9000/blog-files/avatars/user_4_avatar.jpg', 'll', NULL, NULL, '', 1, 0, '2025-11-10 03:36:18', '2025-11-10 11:37:24');

SET FOREIGN_KEY_CHECKS = 1;
