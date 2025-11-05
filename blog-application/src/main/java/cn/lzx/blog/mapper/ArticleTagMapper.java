package cn.lzx.blog.mapper;

import cn.lzx.entity.ArticleTag;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章标签关联Mapper接口
 *
 * @author lzx
 * @since 2025-11-04
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {
}
