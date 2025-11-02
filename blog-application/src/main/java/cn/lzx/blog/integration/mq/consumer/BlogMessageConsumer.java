package cn.lzx.blog.integration.mq.consumer;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import cn.lzx.blog.integration.mq.MQConstant;
import lombok.extern.slf4j.Slf4j;

/**
 * 博客消息消费者示例
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = MQConstant.Topic.BLOG_TOPIC, consumerGroup = MQConstant.ConsumerGroup.BLOG_CONSUMER_GROUP)
public class BlogMessageConsumer implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        try {
            log.info("收到博客消息: {}", message);
            // 在这里处理博客相关的业务逻辑
            // 例如：更新缓存、发送通知等
        } catch (Exception e) {
            log.error("处理博客消息失败: {}", message, e);
            // 消息处理失败，可以记录到数据库或告警
        }
    }
}
