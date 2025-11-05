package cn.lzx.blog.service;

import cn.lzx.blog.vo.CategoryVO;

import java.util.List;

/**
 * 分类Service接口
 *
 * @author lzx
 * @since 2025-11-04
 */
public interface CategoryService {

    /**
     * 获取所有分类列表
     *
     * @return 分类列表
     */
    List<CategoryVO> getCategoryList();

    /**
     * 根据ID获取分类
     *
     * @param id 分类ID
     * @return 分类信息
     */
    CategoryVO getCategoryById(Long id);

    /**
     * 根据名称获取或创建分类
     *
     * @param name 分类名称
     * @return 分类ID
     */
    Long getOrCreateCategoryByName(String name);
}
