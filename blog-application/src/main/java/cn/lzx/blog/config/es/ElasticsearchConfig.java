package cn.lzx.blog.config.es;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;

/**
 * Elasticsearch 7.17 配置类
 *
 * 兼容性说明：
 * - Spring Boot 3.2.12 默认支持 ES 8.x
 * - 本项目使用 ES 7.17.18，直接使用 elasticsearch-java 客户端（不使用 spring-data-elasticsearch）
 * - 添加 X-Elastic-Product 头部以解决 ES 7.17 与新版本客户端的兼容性问题
 * - 使用 co.elastic.clients:elasticsearch-java:7.17.18 和
 * org.elasticsearch.client:elasticsearch-rest-client:7.17.18
 *
 * @author lzx
 * @since 2025-11-01
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    /**
     * 创建 Elasticsearch RestClient (底层客户端)
     * 添加兼容性头部以支持 ES 7.17
     * 支持可选的用户名密码认证
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
        Header[] headers = new Header[] {
                new BasicHeader("X-Elastic-Product", "Elasticsearch")
        };

        // 构建 RestClientBuilder
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
                .setDefaultHeaders(headers);

        // 如果配置了用户名和密码，添加认证
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        return builder.build();
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
