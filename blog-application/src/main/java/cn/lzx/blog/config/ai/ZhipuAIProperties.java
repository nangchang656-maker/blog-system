package cn.lzx.blog.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 智普AI配置属性 - LangChain4j版本
 *
 * @author lzx
 * @since 2025-11-04
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
     * 可选值: glm-4, glm-4-plus, glm-3-turbo等
     */
    private String model = "glm-4";

    /**
     * 请求超时时间(毫秒)
     */
    private Integer timeout = 30000;

    /**
     * 温度参数(0.0-1.0)
     * 控制随机性,越高越随机,越低越确定
     */
    private Double temperature = 0.7;

    /**
     * Top P参数(0.0-1.0)
     * 控制多样性,建议与temperature只设置一个
     */
    private Double topP = 0.95;

    /**
     * 最大生成token数
     */
    private Integer maxTokens = 2000;
}
