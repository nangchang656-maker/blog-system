package cn.lzx.blog.dto;

import lombok.Data;

/**
 * AI请求DTO
 *
 * @author lzx
 * @since 2025-11-04
 */
@Data
public class AIRequestDTO {

    /**
     * 内容（用于生成摘要、润色等）
     */
    private String content;

    /**
     * 主题（用于生成大纲）
     */
    private String topic;
}
