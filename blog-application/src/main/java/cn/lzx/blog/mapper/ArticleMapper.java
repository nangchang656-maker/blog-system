package cn.lzx.blog.mapper;

import cn.lzx.entity.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文章Mapper接口
 *
 * @author lzx
 * @since 2025-11-04
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 增加浏览量
     *
     * @param id 文章ID
     * @return 影响行数
     */
    int incrementViewCount(@Param("id") Long id);

    /**
     * 增加点赞数
     *
     * @param id 文章ID
     * @return 影响行数
     */
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 减少点赞数
     *
     * @param id 文章ID
     * @return 影响行数
     */
    int decrementLikeCount(@Param("id") Long id);

    /**
     * 增加评论数
     *
     * @param id 文章ID
     * @return 影响行数
     */
    int incrementCommentCount(@Param("id") Long id);

    /**
     * 增加收藏数
     *
     * @param id 文章ID
     * @return 影响行数
     */
    int incrementCollectCount(@Param("id") Long id);

    /**
     * 减少收藏数
     *
     * @param id 文章ID
     * @return 影响行数
     */
    int decrementCollectCount(@Param("id") Long id);
}
