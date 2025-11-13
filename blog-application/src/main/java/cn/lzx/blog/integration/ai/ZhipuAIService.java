package cn.lzx.blog.integration.ai;

import org.springframework.stereotype.Service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
            // 清理AI返回内容中的说明文字和代码块标记
            String cleaned = cleanAIContent(response);
            log.info("润色文章内容成功, 原始长度: {}, 润色后长度: {}, 清理后长度: {}",
                    content.length(), response.length(), cleaned.length());
            return cleaned;
        } catch (Exception e) {
            log.error("润色文章内容失败", e);
            throw new RuntimeException(AIConstants.ERROR_FAILED, e);
        }
    }

    /**
     * 清理AI返回的内容，移除说明文字和代码块标记
     *
     * @param content AI返回的原始内容
     * @return 清理后的内容
     */
    private String cleanAIContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return content;
        }

        String cleaned = content.trim();

        // 移除markdown代码块标记
        cleaned = cleaned.replaceAll("(?i)^```(?:markdown)?\\s*\\n?", "");
        cleaned = cleaned.replaceAll("(?i)\\n?```\\s*$", "");

        // 移除常见的AI说明文字（这些说明通常在内容末尾）
        String[] tailPatterns = {
                "\\n以上对原文进行了[^。]*。?\\s*$",
                "\\n以上内容已[^。]*。?\\s*$",
                "\\n已对[^。]*进行[^。]*。?\\s*$",
                "\\n优化后的内容如下[：:]\\s*$",
                "\\n以上内容在保留[^。]*基础上[^。]*。?\\s*$",
                "\\n以上内容在保留原文[^。]*基础上[^。]*。?\\s*$",
                "以上对原文进行了[^。]*。?\\s*$",
                "以上内容已[^。]*。?\\s*$",
                "已对[^。]*进行[^。]*。?\\s*$",
                "以上内容在保留[^。]*基础上[^。]*。?\\s*$",
                "以上内容在保留原文[^。]*基础上[^。]*。?\\s*$",
                // 匹配完整的长说明段落
                "\\n以上内容[^。]*保留[^。]*基础上[^。]*优化[^。]*修正[^。]*同时[^。]*保留[^。]*格式[^。]*。\\s*$",
        };

        for (String pattern : tailPatterns) {
            cleaned = cleaned.replaceAll("(?i)" + pattern, "").trim();
        }

        // 特殊处理：如果末尾有很长的说明段落，尝试移除
        java.util.regex.Pattern longTailPattern = java.util.regex.Pattern.compile(
                "\\n([^。]{50,}(?:保留|优化|修正|流畅性|段落结构|Markdown格式)[^。]{0,100}。?\\s*)$",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = longTailPattern.matcher(cleaned);
        if (matcher.find()) {
            String tailText = matcher.group(1);
            // 检查是否包含多个说明性关键词
            String[] keywords = { "保留", "优化", "修正", "流畅性", "段落结构", "Markdown格式",
                    "技术准确性", "语言流畅度", "专业性", "语法", "标点" };
            long keywordCount = java.util.Arrays.stream(keywords)
                    .filter(tailText::contains)
                    .count();

            // 如果包含3个或以上关键词，且文字较长，很可能是说明文字
            if (keywordCount >= 3 && tailText.length() > 50) {
                cleaned = cleaned.substring(0, matcher.start()).trim();
            }
        }

        return cleaned.trim();
    }

    /**
     * 生成文章大纲（基于主题）
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
     * 基于文章内容生成大纲
     *
     * @param content 文章内容
     * @return Markdown格式的大纲
     */
    public String generateOutlineFromContent(String content) {
        try {
            String prompt = String.format(AIConstants.PROMPT_OUTLINE_FROM_CONTENT, content);
            String response = chatLanguageModel.generate(prompt);
            // 清理AI返回内容中的说明文字和代码块标记
            String cleaned = cleanAIContent(response);
            log.info("基于内容生成文章大纲成功, 内容长度: {}, 大纲长度: {}", content.length(), cleaned.length());
            return cleaned;
        } catch (Exception e) {
            log.error("基于内容生成文章大纲失败", e);
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
