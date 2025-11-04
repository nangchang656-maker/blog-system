package cn.lzx.blog.controller.api;

import cn.lzx.blog.integration.ai.ZhipuAIService;
import cn.lzx.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * AI功能测试控制器
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/test")
@RequiredArgsConstructor
@Tag(name = "AI测试接口", description = "用于测试LangChain4j集成的智普AI功能")
public class AITestController {

    private final ZhipuAIService zhipuAIService;

    /**
     * 测试AI对话
     */
    @GetMapping("/chat")
    @Operation(summary = "测试AI对话", description = "发送消息给AI并获取回复")
    public R testChat(
            @Parameter(description = "用户消息", example = "你好,请介绍一下Spring Boot")
            @RequestParam String message) {
        log.info("测试AI对话, message: {}", message);
        try {
            String response = zhipuAIService.chat(message);
            return R.success(response);
        } catch (Exception e) {
            log.error("AI对话测试失败", e);
            return R.fail("AI对话失败: " + e.getMessage());
        }
    }

    /**
     * 测试生成文章摘要
     */
    @PostMapping("/summary")
    @Operation(summary = "测试生成文章摘要", description = "根据文章内容生成摘要")
    public R testSummary(
            @Parameter(description = "文章内容")
            @RequestBody String content) {
        log.info("测试生成文章摘要, 内容长度: {}", content.length());
        try {
            String summary = zhipuAIService.generateSummary(content);
            return R.success(summary);
        } catch (Exception e) {
            log.error("生成摘要测试失败", e);
            return R.fail("生成摘要失败: " + e.getMessage());
        }
    }

    /**
     * 测试生成文章大纲
     */
    @GetMapping("/outline")
    @Operation(summary = "测试生成文章大纲", description = "根据主题生成文章大纲")
    public R testOutline(
            @Parameter(description = "文章主题", example = "Spring Boot微服务架构")
            @RequestParam String topic) {
        log.info("测试生成文章大纲, topic: {}", topic);
        try {
            String outline = zhipuAIService.generateOutline(topic);
            return R.success(outline);
        } catch (Exception e) {
            log.error("生成大纲测试失败", e);
            return R.fail("生成大纲失败: " + e.getMessage());
        }
    }

    /**
     * 测试润色文章
     */
    @PostMapping("/polish")
    @Operation(summary = "测试润色文章", description = "优化文章内容的语言和结构")
    public R testPolish(
            @Parameter(description = "原始文章内容")
            @RequestBody String content) {
        log.info("测试润色文章, 内容长度: {}", content.length());
        try {
            String polished = zhipuAIService.polishContent(content);
            return R.success(polished);
        } catch (Exception e) {
            log.error("润色文章测试失败", e);
            return R.fail("润色文章失败: " + e.getMessage());
        }
    }
}
