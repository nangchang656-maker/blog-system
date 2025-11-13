package cn.lzx.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 文章ES文档实体类
 * 用于Elasticsearch索引中的文档结构
 *
 * @author lzx
 * @since 2025-11-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 文章ID
     */
    private Long id;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 标签名称（用空格连接，用于搜索）
     */
    private String tags;

    /**
     * 标签ID列表
     */
    @JsonProperty("tagIds")
    private List<Long> tagIds;

    /**
     * 分类ID
     */
    @JsonProperty("categoryId")
    private Long categoryId;

    /**
     * 分类名称
     */
    @JsonProperty("categoryName")
    private String categoryName;

    /**
     * 用户ID（作者ID）
     */
    @JsonProperty("userId")
    private Long userId;

    /**
     * 作者名称
     */
    @JsonProperty("authorName")
    private String authorName;

    /**
     * 封面图
     */
    @JsonProperty("coverImage")
    private String coverImage;

    /**
     * 浏览量
     */
    @JsonProperty("viewCount")
    private Integer viewCount;

    /**
     * 点赞数
     */
    @JsonProperty("likeCount")
    private Integer likeCount;

    /**
     * 评论数
     */
    @JsonProperty("commentCount")
    private Integer commentCount;

    /**
     * 状态：0草稿，1已发布
     */
    private Integer status;

    /**
     * 创建时间（ES中存储为字符串格式：yyyy-MM-dd HH:mm:ss）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("createTime")
    private String createTime;

    /**
     * 更新时间（ES中存储为字符串格式：yyyy-MM-dd HH:mm:ss）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("updateTime")
    private String updateTime;
}

