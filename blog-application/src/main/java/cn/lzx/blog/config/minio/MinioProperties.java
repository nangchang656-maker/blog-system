package cn.lzx.blog.config.minio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * minio属性类
 */
@Data
// 通过指定的前缀，绑定配置文件中的配置
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * 端点
     */
    private String endpoint;

    private String accessKey;

    private String secretKey;

    /**
     * 桶名称
     */
    private String bucketName;
}
