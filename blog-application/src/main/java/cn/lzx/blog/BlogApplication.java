package cn.lzx.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 个人博客系统启动类
 * <p>
 * 架构说明：
 * - /api/**    前台接口（游客访问：浏览文章、评论、点赞等）
 * - /admin/**  后台接口（博主管理：发布文章、审核评论等，需JWT鉴权）
 * <p>
 * @author lzx
 */
@SpringBootApplication(exclude = {
    ElasticsearchDataAutoConfiguration.class,
    ElasticsearchRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration.class,
    // 暂时排除RocketMQ自动配置，待实际使用时再启用
    org.apache.rocketmq.spring.autoconfigure.RocketMQAutoConfiguration.class
})
@ComponentScan(basePackages = {"cn.lzx.blog", "cn.lzx"})
@MapperScan("cn.lzx.blog.mapper")
@EnableScheduling
public class BlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("个人博客系统启动成功！");
        System.out.println("接口文档地址: http://localhost:8088/swagger-ui.html");
        System.out.println("Druid监控: http://localhost:8088/druid (admin/admin123)");
        System.out.println("========================================\n");
    }
}
