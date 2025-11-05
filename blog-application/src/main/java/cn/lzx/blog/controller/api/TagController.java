package cn.lzx.blog.controller.api;

import cn.lzx.annotation.NoLogin;
import cn.lzx.blog.service.TagService;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签API控制器
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Tag(name = "标签模块")
@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 获取所有标签列表
     */
    @NoLogin
    @Operation(summary = "获取所有标签列表")
    @GetMapping("/list")
    public R getTagList() {
        List<TagVO> list = tagService.getTagList();
        return R.success(list);
    }

    /**
     * 获取热门标签
     */
    @NoLogin
    @Operation(summary = "获取热门标签")
    @GetMapping("/hot")
    public R getHotTags(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        List<TagVO> list = tagService.getHotTags(limit);
        return R.success(list);
    }

    /**
     * 根据文章ID获取标签列表
     */
    @NoLogin
    @Operation(summary = "根据文章ID获取标签列表")
    @GetMapping("/article/{articleId}")
    public R getTagsByArticleId(@PathVariable("articleId") Long articleId) {
        List<TagVO> list = tagService.getTagsByArticleId(articleId);
        return R.success(list);
    }
}
