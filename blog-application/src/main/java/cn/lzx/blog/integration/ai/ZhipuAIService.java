package cn.lzx.blog.integration.ai;

import com.zhipu.oapi.ClientV4;
import com.zhipu.oapi.service.v4.model.ChatCompletionRequest;
import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import com.zhipu.oapi.service.v4.model.ModelApiResponse;

import cn.lzx.blog.config.ai.ZhipuAIProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 智普AI服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZhipuAIService {

    private final ClientV4 zhipuClient;
    private final ZhipuAIProperties properties;

    /**
     * 生成文章摘要
     *
     * @param content 文章内容
     * @return 文章摘要
     */
    public String generateSummary(String content) {
        try {
            String prompt = String.format(AIConstants.PROMPT_SUMMARY, content);
            return invokeAI(prompt);
        } catch (Exception e) {
            log.error("生成文章摘要失败", e);
            return AIConstants.DEFAULT_SUMMARY;
        }
    }

    /**
     * 润色文章内容
     *
     * @param content 原始内容
     * @return 润色后的内容
     */
    public String polishContent(String content) {
        try {
            String prompt = String.format(AIConstants.PROMPT_POLISH, content);
            return invokeAI(prompt);
        } catch (Exception e) {
            log.error("润色文章内容失败", e);
            throw new RuntimeException(AIConstants.ERROR_FAILED, e);
        }
    }

    /**
     * 生成文章大纲
     *
     * @param topic 文章主题
     * @return Markdown格式的大纲
     */
    public String generateOutline(String topic) {
        try {
            String prompt = String.format(AIConstants.PROMPT_OUTLINE, topic);
            return invokeAI(prompt);
        } catch (Exception e) {
            log.error("生成文章大纲失败", e);
            throw new RuntimeException(AIConstants.ERROR_FAILED, e);
        }
    }

    /**
     * 调用智普AI
     *
     * @param prompt 提示词
     * @return AI响应内容
     */
    private String invokeAI(String prompt) {
        // 构建消息列表
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), prompt);
        messages.add(userMessage);

        // 构建请求
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model(properties.getModel())
                .stream(Boolean.FALSE)  // 非流式调用,一次性返回完整结果
                .messages(messages)
                .build();

        // 调用API
        ModelApiResponse response = zhipuClient.invokeModelApi(request);

        // 提取响应内容
        if (response != null && response.getData() != null
                && response.getData().getChoices() != null
                && !response.getData().getChoices().isEmpty()) {
            String content = response.getData().getChoices().get(0).getMessage().getContent().toString();
            log.info("AI响应成功, 内容长度: {}", content.length());
            return content;
        }

        throw new RuntimeException("AI响应为空");
    }
}
