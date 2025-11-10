package cn.lzx.blog.mapper;

import cn.lzx.entity.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评论Mapper接口
 *
 * @author lzx
 * @since 2025-11-05
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 查询指定父评论的深度（从根评论到父评论的层级数）
     * 通过递归查询父评论链来计算深度
     *
     * @param parentId 父评论ID
     * @return 深度（1表示一级评论，2表示二级评论，以此类推）
     */
    @Select("SELECT COUNT(*) + 1 FROM (SELECT @id := parent_id FROM comment, (SELECT @id := #{parentId}) AS tmp WHERE @id != 0 AND @id IS NOT NULL) AS depth")
    Integer getDepthByParentId(@Param("parentId") Long parentId);

    /**
     * 根据文章ID查询根评论列表（一级评论）
     *
     * @param articleId 文章ID
     * @return 根评论列表
     */
    @Select("SELECT * FROM comment WHERE article_id = #{articleId} AND parent_id = 0 AND deleted = 0 AND status = 1 ORDER BY create_time DESC")
    List<Comment> selectRootCommentsByArticleId(@Param("articleId") Long articleId);

    /**
     * 根据根评论ID查询所有子评论（按创建时间正序）
     *
     * @param rootId 根评论ID
     * @return 子评论列表
     */
    @Select("SELECT * FROM comment WHERE root_id = #{rootId} AND parent_id != 0 AND deleted = 0 AND status = 1 ORDER BY create_time ASC")
    List<Comment> selectRepliesByRootId(@Param("rootId") Long rootId);

    /**
     * 增加评论点赞数
     *
     * @param commentId 评论ID
     * @return 更新的记录数
     */
    @Update("UPDATE comment SET like_count = like_count + 1 WHERE id = #{commentId}")
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 减少评论点赞数
     *
     * @param commentId 评论ID
     * @return 更新的记录数
     */
    @Update("UPDATE comment SET like_count = like_count - 1 WHERE id = #{commentId} AND like_count > 0")
    int decrementLikeCount(@Param("commentId") Long commentId);
}

