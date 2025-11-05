package cn.lzx.blog.controller.api;

import cn.lzx.annotation.NoLogin;
import cn.lzx.blog.dto.ArticlePublishDTO;
import cn.lzx.blog.dto.ArticleQueryDTO;
import cn.lzx.blog.service.ArticleService;
import cn.lzx.blog.service.FileUploadService;
import cn.lzx.blog.service.InteractionService;
import cn.lzx.blog.vo.ArticleDetailVO;
import cn.lzx.blog.vo.ArticleListVO;
import cn.lzx.utils.R;
import cn.lzx.utils.SecurityContextUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文章API控制器
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Tag(name = "文章模块")
@RestController
@RequestMapping("/api/article")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final InteractionService interactionService;
    private final FileUploadService fileUploadService;

    /**
     * 发布文章
     */
    @Operation(summary = "发布文章")
    @PostMapping("/publish")
    public R publishArticle(@RequestBody @Valid ArticlePublishDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        Long articleId = articleService.publishArticle(userId, dto);
        return R.success(articleId);
    }

    /**
     * 上传文章封面
     */
    @Operation(summary = "上传文章封面")
    @PostMapping("/cover/upload")
    public R uploadCover(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "articleId", required = false) Long articleId) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        String fileUrl = fileUploadService.uploadArticleCover(file, userId, articleId);

        Map<String, String> result = new HashMap<>();
        result.put("url", fileUrl);

        return R.success(result);
    }

    /**
     * 更新文章
     */
    @Operation(summary = "更新文章")
    @PutMapping("/{id}")
    public R updateArticle(@PathVariable("id") Long id,
                           @RequestBody @Valid ArticlePublishDTO dto) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        articleService.updateArticle(userId, id, dto);
        return R.success("更新成功");
    }

    /**
     * 删除文章
     */
    @Operation(summary = "删除文章")
    @DeleteMapping("/{id}")
    public R deleteArticle(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        articleService.deleteArticle(userId, id);
        return R.success("删除成功");
    }

    /**
     * 获取文章列表（分页）
     * 支持分类、标签、关键词筛选，以及多种排序方式
     */
    @NoLogin
    @Operation(summary = "获取文章列表", description = "支持分页、分类筛选、标签筛选、关键词搜索、排序")
    @GetMapping("/list")
    public R getArticleList(ArticleQueryDTO queryDTO) {
        Page<ArticleListVO> page = articleService.getArticleList(queryDTO);
        return R.success(page);
    }

    /**
     * 获取文章详情
     * 自动增加浏览量，已登录用户返回点赞/收藏状态
     */
    @NoLogin
    @Operation(summary = "获取文章详情", description = "自动增加浏览量，已登录用户返回点赞/收藏状态")
    @GetMapping("/{id}")
    public R getArticleDetail(@PathVariable("id") Long id) {
        // 尝试获取当前用户ID（可能为空，表示未登录）
        Long userId = null;
        try {
            userId = SecurityContextUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录，忽略异常
            log.debug("用户未登录，查看文章详情: {}", id);
        }

        ArticleDetailVO articleDetail = articleService.getArticleDetail(id, userId);
        return R.success(articleDetail);
    }

    /**
     * 获取我的文章列表
     */
    @Operation(summary = "获取我的文章列表")
    @GetMapping("/my")
    public R getMyArticles(ArticleQueryDTO queryDTO) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        Page<ArticleListVO> page = articleService.getMyArticles(userId, queryDTO);
        return R.success(page);
    }

    /**
     * 点赞文章
     */
    @Operation(summary = "点赞文章")
    @PostMapping("/{id}/like")
    public R likeArticle(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        interactionService.likeArticle(userId, id);
        return R.success("点赞成功");
    }

    /**
     * 取消点赞文章
     */
    @Operation(summary = "取消点赞文章")
    @PostMapping("/{id}/unlike")
    public R unlikeArticle(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        interactionService.unlikeArticle(userId, id);
        return R.success("取消点赞成功");
    }

    /**
     * 收藏文章
     */
    @Operation(summary = "收藏文章")
    @PostMapping("/{id}/collect")
    public R collectArticle(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        interactionService.collectArticle(userId, id);
        return R.success("收藏成功");
    }

    /**
     * 取消收藏文章
     */
    @Operation(summary = "取消收藏文章")
    @PostMapping("/{id}/uncollect")
    public R uncollectArticle(@PathVariable("id") Long id) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        interactionService.uncollectArticle(userId, id);
        return R.success("取消收藏成功");
    }

    /**
     * 获取我的收藏列表
     */
    @Operation(summary = "获取我的收藏列表")
    @GetMapping("/my-collections")
    public R getMyCollections(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityContextUtil.getCurrentUserId();
        Page<ArticleListVO> result = interactionService.getCollectedArticles(userId, page, size);
        return R.success(result);
    }
}
