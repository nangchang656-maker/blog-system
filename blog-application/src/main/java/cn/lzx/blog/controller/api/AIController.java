package cn.lzx.blog.controller.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.lzx.blog.dto.AIRequestDTO;
import cn.lzx.blog.integration.ai.ZhipuAIService;
import cn.lzx.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI辅助API控制器
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Tag(name = "AI辅助模块")
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final ZhipuAIService zhipuAIService;

    /**
     * AI生成文章摘要
     */
    @Operation(summary = "AI生成文章摘要", description = "根据文章内容自动生成摘要（100字左右）")
    @PostMapping("/summary")
    public R generateSummary(@RequestBody AIRequestDTO dto) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return R.fail("内容不能为空");
        }

        log.info("AI生成摘要请求，内容长度: {}", dto.getContent().length());

        try {
            String summary = zhipuAIService.generateSummary(dto.getContent());
            return R.success(summary);
        } catch (Exception e) {
            log.error("AI生成摘要失败", e);
            return R.fail("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * AI润色文章内容
     */
    @Operation(summary = "AI润色文章内容", description = "优化文章表达，提升可读性和专业性，保持技术准确性")
    @PostMapping("/polish")
    public R polishContent(@RequestBody AIRequestDTO dto) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return R.fail("内容不能为空");
        }

        log.info("AI润色内容请求，内容长度: {}", dto.getContent().length());

        try {
            String polished = zhipuAIService.polishContent(dto.getContent());
            return R.success(polished);
        } catch (Exception e) {
            log.error("AI润色内容失败", e);
            return R.fail("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * AI生成文章大纲（基于主题）
     */
    @Operation(summary = "AI生成文章大纲", description = "根据主题生成文章结构大纲")
    @PostMapping("/outline")
    public R generateOutline(@RequestBody AIRequestDTO dto) {
        if (dto.getTopic() == null || dto.getTopic().trim().isEmpty()) {
            return R.fail("主题不能为空");
        }

        log.info("AI生成大纲请求，主题: {}", dto.getTopic());

        try {
            String outline = zhipuAIService.generateOutline(dto.getTopic());
            return R.success(outline);
        } catch (Exception e) {
            log.error("AI生成大纲失败", e);
            return R.fail("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * AI基于文章内容生成大纲
     */
    @Operation(summary = "AI基于内容生成大纲", description = "根据文章内容提取并生成文章结构大纲")
    @PostMapping("/outline-from-content")
    public R generateOutlineFromContent(@RequestBody AIRequestDTO dto) {
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            return R.fail("内容不能为空");
        }

        log.info("AI基于内容生成大纲请求，内容长度: {}", dto.getContent().length());

        try {
            String outline = zhipuAIService.generateOutlineFromContent(dto.getContent());
            return R.success(outline);
        } catch (Exception e) {
            log.error("AI基于内容生成大纲失败", e);
            return R.fail("AI服务暂时不可用，请稍后重试");
        }
    }
}
