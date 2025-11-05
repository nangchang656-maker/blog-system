package cn.lzx.blog.mapper;

import cn.lzx.entity.Tag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签Mapper接口
 *
 * @author lzx
 * @since 2025-11-04
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * 根据文章ID查询标签列表
     *
     * @param articleId 文章ID
     * @return 标签列表
     */
    @Select("SELECT t.* FROM tag t " +
            "INNER JOIN article_tag at ON t.id = at.tag_id " +
            "WHERE at.article_id = #{articleId}")
    List<Tag> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询热门标签（按文章数量排序）
     *
     * @param limit 限制数量
     * @return 标签列表及文章数量
     */
    @Select("SELECT t.id, t.name, COUNT(at.article_id) as article_count " +
            "FROM tag t " +
            "LEFT JOIN article_tag at ON t.id = at.tag_id " +
            "GROUP BY t.id, t.name " +
            "ORDER BY article_count DESC " +
            "LIMIT #{limit}")
    List<Tag> selectHotTags(@Param("limit") Integer limit);
}
