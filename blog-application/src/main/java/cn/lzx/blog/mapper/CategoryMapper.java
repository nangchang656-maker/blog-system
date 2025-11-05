package cn.lzx.blog.mapper;

import cn.lzx.entity.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分类Mapper接口
 *
 * @author lzx
 * @since 2025-11-04
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
