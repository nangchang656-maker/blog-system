package cn.lzx.blog.config.es;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Elasticsearch 7.17 配置类
 *
 * 兼容性说明：
 * - Spring Boot 3.5.7 默认支持 ES 8.x
 * - 本项目使用 ES 7.17，需要降级 spring-data-elasticsearch 到 4.4.18
 * - 添加 X-Elastic-Product 头部以解决版本兼容性问题
 *
 * @author lzx
 * @since 2025-11-01
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUris;

    /**
     * 创建 Elasticsearch RestClient (底层客户端)
     * 添加兼容性头部以支持 ES 7.17
     */
    @Bean
    @Primary
    public RestClient restClient() {
        // 解析 URI (格式: http://localhost:9200)
        String uri = elasticsearchUris.replace("http://", "").replace("https://", "");
        String[] parts = uri.split(":");
        String host = parts[0];
        int port = Integer.parseInt(parts[1]);

        // 判断协议类型
        String scheme = elasticsearchUris.startsWith("https://") ? "https" : "http";

        // 添加兼容性头部，解决 ES 7.17 与新版客户端的兼容问题
        Header[] headers = new Header[]{
                new BasicHeader("X-Elastic-Product", "Elasticsearch")
        };

        return RestClient.builder(new HttpHost(host, port, scheme))
                .setDefaultHeaders(headers)
                .build();
    }

    /**
     * 创建 Elasticsearch Java API Client
     * 使用 7.17 版本的客户端
     */
    @Bean
    @Primary
    public RestClientTransport restClientTransport(RestClient restClient) {
        // 使用 Jackson 作为 JSON 映射器
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    /**
     * 创建 ElasticsearchClient
     */
    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient(RestClientTransport transport) {
        return new ElasticsearchClient(transport);
    }
}
