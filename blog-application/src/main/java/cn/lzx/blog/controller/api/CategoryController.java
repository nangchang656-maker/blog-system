package cn.lzx.blog.controller.api;

import cn.lzx.annotation.NoLogin;
import cn.lzx.blog.service.CategoryService;
import cn.lzx.blog.vo.CategoryVO;
import cn.lzx.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类API控制器
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Tag(name = "分类模块")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有分类列表
     */
    @NoLogin
    @Operation(summary = "获取所有分类列表")
    @GetMapping("/list")
    public R getCategoryList() {
        List<CategoryVO> list = categoryService.getCategoryList();
        return R.success(list);
    }

    /**
     * 根据ID获取分类
     */
    @NoLogin
    @Operation(summary = "根据ID获取分类")
    @GetMapping("/{id}")
    public R getCategoryById(@PathVariable("id") Long id) {
        CategoryVO category = categoryService.getCategoryById(id);
        return R.success(category);
    }
}
