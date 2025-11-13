package cn.lzx.blog.config.es;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Elasticsearch 配置属性
 *
 * @author lzx
 * @since 2025-11-01
 */
@Data
@Component
@ConfigurationProperties(prefix = "spring.elasticsearch")
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

    /**
     * ES连接URI（用于ElasticsearchConfig，这里不直接使用，但保持配置一致性）
     */
    private String uris;

    /**
     * ES用户名（可选，如果ES启用了安全功能）
     */
    private String username;

    /**
     * ES密码（可选，如果ES启用了安全功能）
     */
    private String password;
}
