package cn.lzx.blog.mapper;

import cn.lzx.entity.LikeRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 点赞记录Mapper接口
 *
 * @author lzx
 * @since 2025-11-04
 */
@Mapper
public interface LikeRecordMapper extends BaseMapper<LikeRecord> {

    /**
     * 查询点赞记录（包括已逻辑删除的）
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     * @return 点赞记录
     */
    @Select("SELECT * FROM like_record WHERE user_id = #{userId} AND target_id = #{targetId} AND type = #{type} LIMIT 1")
    LikeRecord selectIncludeDeleted(@Param("userId") Long userId,
                                     @Param("targetId") Long targetId,
                                     @Param("type") Integer type);

    /**
     * 恢复已逻辑删除的点赞记录
     *
     * @param userId   用户ID
     * @param targetId 目标ID
     * @param type     类型
     * @return 更新的记录数
     */
    @Update("UPDATE like_record SET deleted = 0, create_time = NOW() WHERE user_id = #{userId} AND target_id = #{targetId} AND type = #{type} AND deleted = 1")
    int restoreDeleted(@Param("userId") Long userId,
                       @Param("targetId") Long targetId,
                       @Param("type") Integer type);
}
