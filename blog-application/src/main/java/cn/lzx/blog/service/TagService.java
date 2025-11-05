package cn.lzx.blog.service;

import cn.lzx.blog.vo.TagVO;

import java.util.List;

/**
 * 标签Service接口
 *
 * @author lzx
 * @since 2025-11-04
 */
public interface TagService {

    /**
     * 获取所有标签列表
     *
     * @return 标签列表
     */
    List<TagVO> getTagList();

    /**
     * 获取热门标签
     *
     * @param limit 限制数量
     * @return 热门标签列表
     */
    List<TagVO> getHotTags(Integer limit);

    /**
     * 根据文章ID获取标签列表
     *
     * @param articleId 文章ID
     * @return 标签列表
     */
    List<TagVO> getTagsByArticleId(Long articleId);

    /**
     * 根据名称列表获取或创建标签
     *
     * @param names 标签名称列表
     * @return 标签ID列表
     */
    List<Long> getOrCreateTagsByNames(List<String> names);
}
