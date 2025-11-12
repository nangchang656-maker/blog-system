package cn.lzx.blog.controller.admin;

import cn.lzx.blog.dto.admin.ArticleCategoryUpdateDTO;
import cn.lzx.blog.dto.admin.ArticleStatusUpdateDTO;
import cn.lzx.blog.dto.admin.ArticleTagUpdateDTO;
import cn.lzx.blog.dto.admin.CategoryManageDTO;
import cn.lzx.blog.dto.admin.CommentStatusUpdateDTO;
import cn.lzx.blog.dto.admin.TagManageDTO;
import cn.lzx.blog.vo.CategoryVO;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.blog.service.AdminService;
import cn.lzx.blog.vo.admin.CommentManageVO;
import cn.lzx.blog.vo.admin.StatisticsVO;
import cn.lzx.blog.vo.admin.UserManageVO;
import cn.lzx.constants.AdminConstants;
import cn.lzx.exception.BusinessException;
import cn.lzx.utils.R;
import cn.lzx.utils.SecurityContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员控制器
 *
 * @author lzx
 * @since 2025-11-06
 */
@Slf4j
@Tag(name = "管理员模块")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * 检查管理员权限
     */
    private void checkAdminPermission() {
        Long userId = SecurityContextUtil.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
        if (!AdminConstants.isAdmin(userId)) {
            throw new BusinessException("无权限访问");
        }
    }

    // ==================== 文章管理 ====================

    /**
     * 更新文章分类（分类重划分）
     */
    @Operation(summary = "更新文章分类", description = "管理员可以重新划分文章的分类")
    @PutMapping("/article/category")
    public R updateArticleCategory(@RequestBody @Valid ArticleCategoryUpdateDTO dto) {
        checkAdminPermission();
        adminService.updateArticleCategory(dto);
        return R.success("更新成功");
    }

    /**
     * 更新文章标签（标签综合修改）
     */
    @Operation(summary = "更新文章标签", description = "管理员可以综合修改文章的标签")
    @PutMapping("/article/tags")
    public R updateArticleTags(@RequestBody @Valid ArticleTagUpdateDTO dto) {
        checkAdminPermission();
        adminService.updateArticleTags(dto);
        return R.success("更新成功");
    }

    /**
     * 分页查询文章列表（管理员，包含所有状态）
     */
    @Operation(summary = "获取文章列表", description = "管理员可以查看所有文章（包含草稿、已发布、已屏蔽）")
    @GetMapping("/articles")
    public R getArticleList(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status) {
        checkAdminPermission();
        Page<cn.lzx.blog.vo.ArticleListVO> page = adminService.getArticleList(current, size, keyword, status);
        return R.success(page);
    }

    /**
     * 更新文章状态（屏蔽/取消屏蔽）
     */
    @Operation(summary = "更新文章状态", description = "管理员可以屏蔽或取消屏蔽文章")
    @PutMapping("/article/status")
    public R updateArticleStatus(@RequestBody @Valid ArticleStatusUpdateDTO dto) {
        checkAdminPermission();
        adminService.updateArticleStatus(dto);
        return R.success("更新成功");
    }

    // ==================== 访客管理 ====================

    /**
     * 分页查询用户列表（访客管理）
     */
    @Operation(summary = "获取用户列表", description = "管理员可以查看所有用户信息")
    @GetMapping("/users")
    public R getUserList(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword) {
        checkAdminPermission();
        Page<UserManageVO> page = adminService.getUserList(current, size, keyword);
        return R.success(page);
    }

    /**
     * 更新用户状态（启用/禁用）
     */
    @Operation(summary = "更新用户状态", description = "管理员可以启用或禁用用户")
    @PutMapping("/user/{userId}/status")
    public R updateUserStatus(
            @PathVariable("userId") Long userId,
            @RequestParam("status") Integer status) {
        checkAdminPermission();
        adminService.updateUserStatus(userId, status);
        return R.success("更新成功");
    }

    // ==================== 评论管理 ====================

    /**
     * 分页查询评论列表（评论管理）
     */
    @Operation(summary = "获取评论列表", description = "管理员可以查看所有评论信息")
    @GetMapping("/comments")
    public R getCommentList(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword) {
        checkAdminPermission();
        Page<CommentManageVO> page = adminService.getCommentList(current, size, status, keyword);
        return R.success(page);
    }

    /**
     * 更新评论状态
     */
    @Operation(summary = "更新评论状态", description = "管理员可以更新评论的显示状态")
    @PutMapping("/comment/status")
    public R updateCommentStatus(@RequestBody @Valid CommentStatusUpdateDTO dto) {
        checkAdminPermission();
        adminService.updateCommentStatus(dto);
        return R.success("更新成功");
    }

    /**
     * 删除评论（管理员）
     */
    @Operation(summary = "删除评论", description = "管理员可以删除评论")
    @DeleteMapping("/comment/{commentId}")
    public R deleteComment(@PathVariable("commentId") Long commentId) {
        checkAdminPermission();
        adminService.deleteComment(commentId);
        return R.success("删除成功");
    }

    // ==================== 数据统计 ====================

    /**
     * 获取数据统计
     */
    @Operation(summary = "获取数据统计", description = "管理员可以查看系统数据统计信息")
    @GetMapping("/statistics")
    public R getStatistics() {
        checkAdminPermission();
        StatisticsVO statistics = adminService.getStatistics();
        return R.success(statistics);
    }

    // ==================== 分类管理 ====================

    /**
     * 获取所有分类列表
     */
    @Operation(summary = "获取分类列表", description = "管理员可以查看所有分类")
    @GetMapping("/categories")
    public R getCategoryList() {
        checkAdminPermission();
        List<CategoryVO> categories = adminService.getCategoryList();
        return R.success(categories);
    }

    /**
     * 创建分类
     */
    @Operation(summary = "创建分类", description = "管理员可以创建新分类")
    @PostMapping("/category")
    public R createCategory(@RequestBody @Valid CategoryManageDTO dto) {
        checkAdminPermission();
        Long categoryId = adminService.createCategory(dto);
        return R.success(categoryId);
    }

    /**
     * 更新分类
     */
    @Operation(summary = "更新分类", description = "管理员可以更新分类信息")
    @PutMapping("/category")
    public R updateCategory(@RequestBody @Valid CategoryManageDTO dto) {
        checkAdminPermission();
        adminService.updateCategory(dto);
        return R.success("更新成功");
    }

    /**
     * 删除分类
     */
    @Operation(summary = "删除分类", description = "管理员可以删除分类")
    @DeleteMapping("/category/{categoryId}")
    public R deleteCategory(@PathVariable("categoryId") Long categoryId) {
        checkAdminPermission();
        adminService.deleteCategory(categoryId);
        return R.success("删除成功");
    }

    // ==================== 标签管理 ====================

    /**
     * 获取所有标签列表
     */
    @Operation(summary = "获取标签列表", description = "管理员可以查看所有标签")
    @GetMapping("/tags")
    public R getTagList() {
        checkAdminPermission();
        List<TagVO> tags = adminService.getTagList();
        return R.success(tags);
    }

    /**
     * 创建标签
     */
    @Operation(summary = "创建标签", description = "管理员可以创建新标签")
    @PostMapping("/tag")
    public R createTag(@RequestBody @Valid TagManageDTO dto) {
        checkAdminPermission();
        Long tagId = adminService.createTag(dto);
        return R.success(tagId);
    }

    /**
     * 更新标签
     */
    @Operation(summary = "更新标签", description = "管理员可以更新标签信息")
    @PutMapping("/tag")
    public R updateTag(@RequestBody @Valid TagManageDTO dto) {
        checkAdminPermission();
        adminService.updateTag(dto);
        return R.success("更新成功");
    }

    /**
     * 删除标签
     */
    @Operation(summary = "删除标签", description = "管理员可以删除标签")
    @DeleteMapping("/tag/{tagId}")
    public R deleteTag(@PathVariable("tagId") Long tagId) {
        checkAdminPermission();
        adminService.deleteTag(tagId);
        return R.success("删除成功");
    }
}

