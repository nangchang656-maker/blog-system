package cn.lzx.blog.integration.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * RocketMQ 消息生产者
 */
@Slf4j
@Component
public class RocketMQProducer {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    /**
     * 发送同步消息
     *
     * @param topic 主题
     * @param message 消息内容
     */
    public void sendMessage(String topic, Object message) {
        try {
            rocketMQTemplate.syncSend(topic, message);
            log.info("发送同步消息成功, topic: {}, message: {}", topic, message);
        } catch (Exception e) {
            log.error("发送同步消息失败, topic: {}, message: {}", topic, message, e);
        }
    }

    /**
     * 发送同步消息（带Tag）
     *
     * @param topic 主题
     * @param tag 标签
     * @param message 消息内容
     */
    public void sendMessageWithTag(String topic, String tag, Object message) {
        try {
            String destination = topic + ":" + tag;
            rocketMQTemplate.syncSend(destination, message);
            log.info("发送同步消息成功, topic: {}, tag: {}, message: {}", topic, tag, message);
        } catch (Exception e) {
            log.error("发送同步消息失败, topic: {}, tag: {}, message: {}", topic, tag, message, e);
        }
    }

    /**
     * 发送异步消息
     *
     * @param topic 主题
     * @param message 消息内容
     */
    public void sendAsyncMessage(String topic, Object message) {
        rocketMQTemplate.asyncSend(topic, message, new org.apache.rocketmq.client.producer.SendCallback() {
            @Override
            public void onSuccess(org.apache.rocketmq.client.producer.SendResult sendResult) {
                log.info("发送异步消息成功, topic: {}, message: {}, msgId: {}",
                    topic, message, sendResult.getMsgId());
            }

            @Override
            public void onException(Throwable e) {
                log.error("发送异步消息失败, topic: {}, message: {}", topic, message, e);
            }
        });
    }

    /**
     * 发送延迟消息
     *
     * @param topic 主题
     * @param message 消息内容
     * @param delayLevel 延迟级别 (1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h)
     */
    public void sendDelayMessage(String topic, Object message, int delayLevel) {
        try {
            Message<Object> msg = MessageBuilder.withPayload(message).build();
            rocketMQTemplate.syncSend(topic, msg, 3000, delayLevel);
            log.info("发送延迟消息成功, topic: {}, message: {}, delayLevel: {}", topic, message, delayLevel);
        } catch (Exception e) {
            log.error("发送延迟消息失败, topic: {}, message: {}, delayLevel: {}", topic, message, delayLevel, e);
        }
    }

    /**
     * 发送单向消息（不关心发送结果，性能最高）
     *
     * @param topic 主题
     * @param message 消息内容
     */
    public void sendOneWayMessage(String topic, Object message) {
        try {
            rocketMQTemplate.sendOneWay(topic, message);
            log.info("发送单向消息, topic: {}, message: {}", topic, message);
        } catch (Exception e) {
            log.error("发送单向消息失败, topic: {}, message: {}", topic, message, e);
        }
    }
}
