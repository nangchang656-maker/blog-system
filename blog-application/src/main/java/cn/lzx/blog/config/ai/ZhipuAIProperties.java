package cn.lzx.blog.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智普AI配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhipu.ai")
public class ZhipuAIProperties {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * 模型名称
     */
    private String model = "glm-4";

    /**
     * 请求超时时间(毫秒)
     */
    private Integer timeout = 30000;
}
