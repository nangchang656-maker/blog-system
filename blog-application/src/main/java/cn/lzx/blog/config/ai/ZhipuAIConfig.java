package cn.lzx.blog.config.ai;

import com.zhipu.oapi.ClientV4;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 智普AI配置类
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ZhipuAIConfig {

    private final ZhipuAIProperties properties;

    /**
     * 创建智普AI客户端
     */
    @Bean
    public ClientV4 zhipuClient() {
        log.info("初始化智普AI客户端, model={}, timeout={}ms",
                properties.getModel(), properties.getTimeout());
        return new ClientV4.Builder(properties.getApiKey()).build();
    }
}
