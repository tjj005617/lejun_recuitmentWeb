package com.interview.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * RabbitMQ 配置
 * 聊天消息通过队列投递，保证消息不丢失
 * 延迟消息用于 ACK 超时检测
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 聊天消息 ====================

    /** 聊天交换机 */
    public static final String CHAT_EXCHANGE = "chat.exchange";

    /** 聊天消息队列 */
    public static final String CHAT_QUEUE = "chat.message.queue";

    /** 路由键 */
    public static final String CHAT_ROUTING_KEY = "chat.message";

    /** 死信队列 */
    public static final String CHAT_DLQ = "chat.message.dlq";

    /** 死信路由键 */
    public static final String CHAT_DLQ_ROUTING_KEY = "chat.message.dlq";

    // ==================== 延迟消息（ACK 超时检测） ====================

    /** 延迟交换机（需要 rabbitmq_delayed_message_exchange 插件） */
    public static final String DELAYED_EXCHANGE = "chat.delayed.exchange";

    /** ACK 超时队列 */
    public static final String ACK_TIMEOUT_QUEUE = "chat.ack.timeout.queue";

    /** ACK 超时路由键 */
    public static final String ACK_TIMEOUT_ROUTING_KEY = "chat.ack.timeout";

    // ==================== 聊天消息配置 ====================

    /** 聊天交换机 */
    @Bean
    public DirectExchange chatExchange() {
        return ExchangeBuilder.directExchange(CHAT_EXCHANGE).durable(true).build();
    }

    /** 聊天消息队列（持久化 + 死信） */
    @Bean
    public Queue chatQueue() {
        return QueueBuilder.durable(CHAT_QUEUE)
                .withArgument("x-dead-letter-exchange", CHAT_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CHAT_DLQ_ROUTING_KEY)
                .build();
    }

    /** 死信队列 */
    @Bean
    public Queue chatDlq() {
        return QueueBuilder.durable(CHAT_DLQ).build();
    }

    /** 绑定：聊天交换机 → 聊天队列 */
    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue()).to(chatExchange()).with(CHAT_ROUTING_KEY);
    }

    /** 绑定：聊天交换机 → 死信队列 */
    @Bean
    public Binding chatDlqBinding() {
        return BindingBuilder.bind(chatDlq()).to(chatExchange()).with(CHAT_DLQ_ROUTING_KEY);
    }

    // ==================== 延迟消息配置 ====================

    /** 延迟交换机（类型: x-delayed-message） */
    @Bean
    public CustomExchange delayedExchange() {
        return new CustomExchange(
            DELAYED_EXCHANGE,
            "x-delayed-message",
            true,
            false,
            Map.of("x-delayed-type", "direct")
        );
    }

    /** ACK 超时队列 */
    @Bean
    public Queue ackTimeoutQueue() {
        return QueueBuilder.durable(ACK_TIMEOUT_QUEUE).build();
    }

    /** 绑定：延迟交换机 → ACK 超时队列 */
    @Bean
    public Binding ackTimeoutBinding() {
        return BindingBuilder.bind(ackTimeoutQueue()).to(delayedExchange()).with(ACK_TIMEOUT_ROUTING_KEY).noargs();
    }

    // ==================== 通用配置 ====================

    /** JSON 消息转换器 */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /** RabbitTemplate 使用 JSON 转换器 */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
