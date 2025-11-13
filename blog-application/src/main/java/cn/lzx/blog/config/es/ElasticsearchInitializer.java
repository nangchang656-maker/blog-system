package cn.lzx.blog.config.es;

import cn.lzx.blog.service.ArticleSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch初始化器
 * 应用启动时自动创建文章索引
 *
 * @author lzx
 * @since 2025-11-01
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchInitializer implements ApplicationRunner {

    private final ArticleSearchService articleSearchService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("开始初始化Elasticsearch索引...");
            articleSearchService.initArticleIndex();
            log.info("Elasticsearch索引初始化完成");
        } catch (Exception e) {
            log.error("Elasticsearch索引初始化失败", e);
        }
    }
}

