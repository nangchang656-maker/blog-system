package cn.lzx.blog.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.lzx.blog.mapper.CategoryMapper;
import cn.lzx.blog.service.CategoryService;
import cn.lzx.blog.vo.CategoryVO;
import cn.lzx.entity.Category;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 分类Service实现类
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryVO> getCategoryList() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSort);

        List<Category> categories = categoryMapper.selectList(wrapper);

        return categories.stream()
                .map(category -> CategoryVO.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .sort(category.getSort())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public CategoryVO getCategoryById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            return null;
        }

        return CategoryVO.builder()
                .id(category.getId())
                .name(category.getName())
                .sort(category.getSort())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long getOrCreateCategoryByName(String name) {
        // 先查询是否存在同名分类
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, name);
        Category existCategory = categoryMapper.selectOne(wrapper);

        if (existCategory != null) {
            log.debug("分类已存在: {}", name);
            return existCategory.getId();
        }

        // 不存在则创建新分类
        Category newCategory = Category.builder()
                .name(name)
                .sort(0)
                .build();

        // TODO: 定时任务清理长期不使用的分类
        categoryMapper.insert(newCategory);
        log.info("创建新分类: {}, ID: {}", name, newCategory.getId());

        return newCategory.getId();
    }

    @Override
    public List<CategoryVO> getCategoryListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Category> categories = categoryMapper.selectBatchIds(ids);
        return categories.stream()
                .map(category -> CategoryVO.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .sort(category.getSort())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, CategoryVO> getCategoryMapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return categoryMapper.selectBatchIds(ids)
                .stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        category -> CategoryVO.builder()
                                .id(category.getId())
                                .name(category.getName())
                                .sort(category.getSort())
                                .build()));
    }
}
