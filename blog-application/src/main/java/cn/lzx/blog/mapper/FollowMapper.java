package cn.lzx.blog.mapper;

import cn.lzx.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 关注Mapper接口
 *
 * @author lzx
 * @since 2025-11-05
 */
@Mapper
public interface FollowMapper extends BaseMapper<Follow> {
}
