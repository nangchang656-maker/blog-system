package cn.lzx.blog.config.es;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch 配置属性
 *
 * @author lzx
 * @since 2025-11-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    /**
     * 文章索引名称
     */
    private String articleIndex = "blog_article";

    /**
     * 分片数量
     */
    private Integer numberOfShards = 1;

    /**
     * 副本数量
     */
    private Integer numberOfReplicas = 1;
}
