package cn.lzx.blog.config.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 智普AI配置类 - 使用LangChain4j集成
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ZhipuAIConfig {

    private final ZhipuAIProperties properties;

    /**
     * 创建智普AI聊天模型
     * 使用LangChain4j的统一接口ChatLanguageModel
     */
    @Bean
    public ChatLanguageModel zhipuAiChatModel() {
        log.info("初始化智普AI聊天模型 (LangChain4j), model={}", properties.getModel());

        return ZhipuAiChatModel.builder()
                .apiKey(properties.getApiKey())
                .model(properties.getModel())
                .temperature(properties.getTemperature())
                .topP(properties.getTopP())
                .callTimeout(Duration.ofMillis(properties.getTimeout()))
                .connectTimeout(Duration.ofMillis(properties.getTimeout()))
                .readTimeout(Duration.ofMillis(properties.getTimeout()))
                .writeTimeout(Duration.ofMillis(properties.getTimeout()))
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
