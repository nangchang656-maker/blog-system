package cn.lzx.blog.task;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import cn.lzx.blog.mapper.ArticleMapper;
import cn.lzx.entity.Article;
import cn.lzx.enums.RedisKeyEnum;
import cn.lzx.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务类
 * 实现缓存清理和热门文章排行功能
 *
 * @author lzx
 * @since 2025-11-04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTask {

    private final RedisUtil redisUtil;
    private final ArticleMapper articleMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 缓存清理任务
     * 每天凌晨2点执行，清理Redis中过期的缓存数据
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanExpiredCache() {
        log.info("开始执行缓存清理任务...");
        long startTime = System.currentTimeMillis();
        int cleanedCount = 0;

        try {
            // 清理文章详情缓存（blog:cache:article:*）
            String articleCachePattern = "blog:cache:article:*";
            cleanedCount += cleanCacheByPattern(articleCachePattern);

            // 清理用户信息缓存（blog:cache:user:*）
            String userCachePattern = "blog:cache:user:*";
            cleanedCount += cleanCacheByPattern(userCachePattern);

            long endTime = System.currentTimeMillis();
            log.info("缓存清理任务完成，清理了 {} 个过期缓存，耗时 {} ms", cleanedCount, (endTime - startTime));
        } catch (Exception e) {
            log.error("缓存清理任务执行失败", e);
        }
    }

    /**
     * 热门文章排行任务
     * 每天凌晨1点执行，统计文章浏览量并生成热门文章排行榜
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void updateHotArticlesRank() {
        log.info("开始执行热门文章排行任务...");
        long startTime = System.currentTimeMillis();

        try {
            String hotArticlesKey = RedisKeyEnum.KEY_HOT_ARTICLES.getKey();

            // 1. 清空旧的排行榜
            redisUtil.delete(hotArticlesKey);
            log.debug("已清空旧的排行榜");

            // 2. 查询所有已发布的文章（按浏览量降序）
            LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Article::getStatus, 1) // 只查询已发布的文章
                    .orderByDesc(Article::getViewCount)
                    .last("LIMIT 100"); // 只取前100篇热门文章

            java.util.List<Article> articles = articleMapper.selectList(wrapper);

            // 3. 将文章ID和浏览量存入Redis ZSet
            int count = 0;
            for (Article article : articles) {
                if (article.getViewCount() != null && article.getViewCount() > 0) {
                    redisUtil.zAdd(hotArticlesKey, article.getId().toString(), article.getViewCount().doubleValue());
                    count++;
                }
            }

            long endTime = System.currentTimeMillis();
            log.info("热门文章排行任务完成，更新了 {} 篇文章到排行榜，耗时 {} ms", count, (endTime - startTime));
        } catch (Exception e) {
            log.error("热门文章排行任务执行失败", e);
        }
    }

    /**
     * 根据模式清理缓存
     * 清理已过期但还未被Redis自动删除的缓存key，释放内存空间
     *
     * @param pattern 缓存key模式
     * @return 清理的缓存数量
     */
    private int cleanCacheByPattern(String pattern) {
        int count = 0;
        try {
            // 使用SCAN命令遍历匹配的key（避免阻塞，适合生产环境）
            ScanOptions options = ScanOptions.scanOptions()
                    .match(pattern)
                    .count(100) // 每次扫描100个key
                    .build();

            try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    String key = cursor.next();
                    // 检查key的过期时间
                    // TTL = -2: key不存在
                    // TTL = -1: key存在但没有设置过期时间（永不过期）
                    // TTL = 0: key已过期但可能还未被删除
                    // TTL > 0: key存在且未过期
                    Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);

                    // 只清理已过期（TTL = 0）的key，避免误删永不过期的key
                    if (ttl != null && ttl == 0) {
                        // key已过期，删除它以释放内存
                        if (stringRedisTemplate.delete(key)) {
                            count++;
                            log.debug("清理过期缓存: {}", key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("清理缓存模式 {} 时出错", pattern, e);
        }
        return count;
    }
}
