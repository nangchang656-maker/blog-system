package cn.lzx.blog.config.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * RocketMQ 配置类
 * 
 * 配置信息展示和初始化确认
 * 可按需添加更多的 RocketMQ 配置（如自定义生产者、消费者、事务监听器等）
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rocketmq.name-server")
public class RocketMQConfig {

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("RocketMQ 配置初始化完成");
        log.info("NameServer: 通过 application.yml 配置");
        log.info("Producer Group: blog-producer-group");
        log.info("========================================");
    }
}
