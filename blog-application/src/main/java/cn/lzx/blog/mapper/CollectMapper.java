package cn.lzx.blog.mapper;

import cn.lzx.entity.Collect;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 收藏Mapper接口
 *
 * @author lzx
 * @since 2025-11-04
 */
@Mapper
public interface CollectMapper extends BaseMapper<Collect> {

    /**
     * 查询收藏记录（包括已逻辑删除的）
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 收藏记录
     */
    @Select("SELECT * FROM collect WHERE user_id = #{userId} AND article_id = #{articleId} LIMIT 1")
    Collect selectIncludeDeleted(@Param("userId") Long userId,
                                  @Param("articleId") Long articleId);

    /**
     * 恢复已逻辑删除的收藏记录
     *
     * @param userId    用户ID
     * @param articleId 文章ID
     * @return 更新的记录数
     */
    @Update("UPDATE collect SET deleted = 0, create_time = NOW() WHERE user_id = #{userId} AND article_id = #{articleId} AND deleted = 1")
    int restoreDeleted(@Param("userId") Long userId,
                       @Param("articleId") Long articleId);
}
