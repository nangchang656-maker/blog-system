package cn.lzx.blog.service;

import cn.lzx.blog.dto.admin.ArticleCategoryUpdateDTO;
import cn.lzx.blog.dto.admin.ArticleStatusUpdateDTO;
import cn.lzx.blog.dto.admin.ArticleTagUpdateDTO;
import cn.lzx.blog.dto.admin.CategoryManageDTO;
import cn.lzx.blog.dto.admin.CommentStatusUpdateDTO;
import cn.lzx.blog.dto.admin.TagManageDTO;
import cn.lzx.blog.vo.CategoryVO;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.blog.vo.admin.CommentManageVO;
import cn.lzx.blog.vo.admin.StatisticsVO;
import cn.lzx.blog.vo.admin.UserManageVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 管理员Service接口
 *
 * @author lzx
 * @since 2025-11-06
 */
public interface AdminService {

    /**
     * 更新文章分类（分类重划分）
     *
     * @param dto 文章分类更新DTO
     */
    void updateArticleCategory(ArticleCategoryUpdateDTO dto);

    /**
     * 更新文章标签（标签综合修改）
     *
     * @param dto 文章标签更新DTO
     */
    void updateArticleTags(ArticleTagUpdateDTO dto);

    /**
     * 分页查询文章列表（管理员，包含所有状态）
     *
     * @param current 当前页
     * @param size    每页大小
     * @param keyword 关键词（文章标题）
     * @param status  状态筛选（0草稿，1已发布，4已屏蔽，可为空）
     * @return 文章列表
     */
    Page<cn.lzx.blog.vo.ArticleListVO> getArticleList(Long current, Long size, String keyword, Integer status);

    /**
     * 更新文章状态（屏蔽/取消屏蔽）
     *
     * @param dto 文章状态更新DTO
     */
    void updateArticleStatus(ArticleStatusUpdateDTO dto);

    /**
     * 分页查询用户列表（访客管理）
     *
     * @param current 当前页
     * @param size    每页大小
     * @param keyword 关键词（用户名/昵称/邮箱）
     * @return 用户列表
     */
    Page<UserManageVO> getUserList(Long current, Long size, String keyword);

    /**
     * 更新用户状态（启用/禁用）
     *
     * @param userId 用户ID
     * @param status  状态：1正常，0禁用
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 分页查询评论列表（评论管理）
     *
     * @param current 当前页
     * @param size    每页大小
     * @param status  状态：1正常，2已隐藏-待审核（可为空）
     * @param keyword 关键词（评论内容/文章标题）
     * @return 评论列表
     */
    Page<CommentManageVO> getCommentList(Long current, Long size, Integer status, String keyword);

    /**
     * 更新评论状态
     *
     * @param dto 评论状态更新DTO
     */
    void updateCommentStatus(CommentStatusUpdateDTO dto);

    /**
     * 删除评论（管理员）
     *
     * @param commentId 评论ID
     */
    void deleteComment(Long commentId);

    /**
     * 获取数据统计
     *
     * @return 数据统计VO
     */
    StatisticsVO getStatistics();

    // ==================== 分类管理 ====================

    /**
     * 获取所有分类列表
     *
     * @return 分类列表
     */
    java.util.List<CategoryVO> getCategoryList();

    /**
     * 创建分类
     *
     * @param dto 分类管理DTO
     * @return 分类ID
     */
    Long createCategory(CategoryManageDTO dto);

    /**
     * 更新分类
     *
     * @param dto 分类管理DTO
     */
    void updateCategory(CategoryManageDTO dto);

    /**
     * 删除分类
     *
     * @param categoryId 分类ID
     */
    void deleteCategory(Long categoryId);

    // ==================== 标签管理 ====================

    /**
     * 获取所有标签列表
     *
     * @return 标签列表
     */
    java.util.List<TagVO> getTagList();

    /**
     * 创建标签
     *
     * @param dto 标签管理DTO
     * @return 标签ID
     */
    Long createTag(TagManageDTO dto);

    /**
     * 更新标签
     *
     * @param dto 标签管理DTO
     */
    void updateTag(TagManageDTO dto);

    /**
     * 删除标签
     *
     * @param tagId 标签ID
     */
    void deleteTag(Long tagId);
}

