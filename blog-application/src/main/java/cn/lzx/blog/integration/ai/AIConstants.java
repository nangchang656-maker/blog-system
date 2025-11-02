package cn.lzx.blog.integration.ai;

/**
 * AI服务常量
 */
public class AIConstants {

    /**
     * 生成文章摘要的提示词模板
     */
    public static final String PROMPT_SUMMARY = """
            请为以下文章生成一段100字左右的摘要。要求:
            1. 概括文章核心内容
            2. 语言简洁专业
            3. 突出技术要点
            4. 不要包含"本文"、"文章"等字眼

            文章内容:
            %s
            """;

    /**
     * 润色文章内容的提示词模板
     */
    public static final String PROMPT_POLISH = """
            请优化以下文章内容。要求:
            1. 保持原意和技术准确性
            2. 提升语言流畅度和专业性
            3. 优化段落结构和逻辑
            4. 修正语法和标点错误
            5. 保留Markdown格式

            文章内容:
            %s
            """;

    /**
     * 生成文章大纲的提示词模板
     */
    public static final String PROMPT_OUTLINE = """
            请为主题"%s"生成一篇技术博客的文章大纲。要求:
            1. 使用Markdown格式
            2. 包含3-5个主要章节
            3. 每个章节包含2-4个小节
            4. 内容结构清晰,逻辑合理
            5. 适合技术博客风格
            """;

    /**
     * 默认摘要(AI调用失败时使用)
     */
    public static final String DEFAULT_SUMMARY = "这是一篇精心编写的技术文章,欢迎阅读。";

    /**
     * AI请求超时错误信息
     */
    public static final String ERROR_TIMEOUT = "AI服务响应超时,请稍后重试";

    /**
     * AI请求失败错误信息
     */
    public static final String ERROR_FAILED = "AI服务暂时不可用,请稍后重试";
}
