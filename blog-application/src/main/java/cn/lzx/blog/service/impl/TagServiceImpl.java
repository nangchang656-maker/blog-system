package cn.lzx.blog.service.impl;

import cn.lzx.blog.mapper.TagMapper;
import cn.lzx.blog.service.TagService;
import cn.lzx.blog.vo.TagVO;
import cn.lzx.entity.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签Service实现类
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    @Override
    public List<TagVO> getTagList() {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getCreateTime);

        List<Tag> tags = tagMapper.selectList(wrapper);

        return tags.stream()
                .map(tag -> TagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TagVO> getHotTags(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        List<Tag> tags = tagMapper.selectHotTags(limit);

        return tags.stream()
                .map(tag -> TagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TagVO> getTagsByArticleId(Long articleId) {
        List<Tag> tags = tagMapper.selectByArticleId(articleId);

        return tags.stream()
                .map(tag -> TagVO.builder()
                        .id(tag.getId())
                        .name(tag.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> getOrCreateTagsByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = new ArrayList<>();

        for (String name : names) {
            // 先查询是否存在同名标签
            LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Tag::getName, name);
            Tag existTag = tagMapper.selectOne(wrapper);

            if (existTag != null) {
                log.debug("标签已存在: {}", name);
                tagIds.add(existTag.getId());
            } else {
                // 不存在则创建新标签
                Tag newTag = Tag.builder()
                        .name(name)
                        .build();
                tagMapper.insert(newTag);
                log.info("创建新标签: {}, ID: {}", name, newTag.getId());
                tagIds.add(newTag.getId());
            }
        }

        return tagIds;
    }
}
