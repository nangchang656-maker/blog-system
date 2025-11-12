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

    /**
     * 根据ID列表批量查询分类
     *
     * @param ids 分类ID列表
     * @return 分类信息列表
     */
    java.util.List<CategoryVO> getCategoryListByIds(java.util.List<Long> ids);

    /**
     * 根据ID列表批量查询分类并转换为Map
     *
     * @param ids 分类ID列表
     * @return 分类信息Map，key为分类ID
     */
    java.util.Map<Long, CategoryVO> getCategoryMapByIds(java.util.List<Long> ids);
}
