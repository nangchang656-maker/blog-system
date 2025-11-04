package cn.lzx.blog.integration.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 智普AI服务类 - 使用LangChain4j
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZhipuAIService {

    private final ChatLanguageModel chatLanguageModel;

    /**
     * 生成文章摘要
     *
     * @param content 文章内容
     * @return 文章摘要
     */
    public String generateSummary(String content) {
        try {
            String prompt = String.format(AIConstants.PROMPT_SUMMARY, content);
            String response = chatLanguageModel.generate(prompt);
            log.info("生成文章摘要成功, 内容长度: {}", response.length());
            return response;
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
            String response = chatLanguageModel.generate(prompt);
            log.info("润色文章内容成功, 原始长度: {}, 润色后长度: {}", content.length(), response.length());
            return response;
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
            String response = chatLanguageModel.generate(prompt);
            log.info("生成文章大纲成功, 主题: {}, 大纲长度: {}", topic, response.length());
            return response;
        } catch (Exception e) {
            log.error("生成文章大纲失败, 主题: {}", topic, e);
            throw new RuntimeException(AIConstants.ERROR_FAILED, e);
        }
    }

    /**
     * 通用AI对话接口
     *
     * @param userMessage 用户消息
     * @return AI回复
     */
    public String chat(String userMessage) {
        try {
            String response = chatLanguageModel.generate(userMessage);
            log.info("AI对话成功, 问题长度: {}, 回复长度: {}", userMessage.length(), response.length());
            return response;
        } catch (Exception e) {
            log.error("AI对话失败", e);
            throw new RuntimeException(AIConstants.ERROR_FAILED, e);
        }
    }
}
