package cn.lzx.blog.integration.mq;

/**
 * RocketMQ 常量配置
 */
public class MQConstant {

    /**
     * 主题定义
     */
    public static class Topic {
        /** 博客相关主题 */
        public static final String BLOG_TOPIC = "BLOG_TOPIC";

        /** 用户相关主题 */
        public static final String USER_TOPIC = "USER_TOPIC";

        /** 评论相关主题 */
        public static final String COMMENT_TOPIC = "COMMENT_TOPIC";

        /** 邮件通知主题 */
        public static final String EMAIL_TOPIC = "EMAIL_TOPIC";
    }

    /**
     * 标签定义
     */
    public static class Tag {
        /** 博客发布 */
        public static final String BLOG_PUBLISH = "BLOG_PUBLISH";

        /** 博客删除 */
        public static final String BLOG_DELETE = "BLOG_DELETE";

        /** 用户注册 */
        public static final String USER_REGISTER = "USER_REGISTER";

        /** 用户登录 */
        public static final String USER_LOGIN = "USER_LOGIN";

        /** 评论发布 */
        public static final String COMMENT_PUBLISH = "COMMENT_PUBLISH";

        /** 发送邮件 */
        public static final String EMAIL_SEND = "EMAIL_SEND";
    }

    /**
     * 消费者组定义
     */
    public static class ConsumerGroup {
        /** 博客消费者组 */
        public static final String BLOG_CONSUMER_GROUP = "blog-consumer-group";

        /** 用户消费者组 */
        public static final String USER_CONSUMER_GROUP = "user-consumer-group";

        /** 评论消费者组 */
        public static final String COMMENT_CONSUMER_GROUP = "comment-consumer-group";

        /** 邮件消费者组 */
        public static final String EMAIL_CONSUMER_GROUP = "email-consumer-group";
    }
}
