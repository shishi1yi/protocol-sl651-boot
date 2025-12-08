package cc.shaoyi.sl651.modules.protocol.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author ShaoYi
 * @Description rabbitmq配置
 * @createDate 2022年08月05日 11:02
 **/
@Configuration
@Slf4j
public class RabbitmqConfiguration {

    @Bean
    public RabbitTemplate createRabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate();
        rabbitTemplate.setConnectionFactory(connectionFactory);

        /**
         * 消息发送确认 (Publisher Confirms)
         * 从生产者(Producer) 到 交换机(exchange) 的回调
         */
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.warn("ConfirmCallback 消息未送达交换机!, 相关数据: {}, 原因: {}", correlationData, cause);
            }
        });

        // 设置开启Mandatory,才能触发回调函数,无论消息推送结果怎么样都强制调用回调函数
        rabbitTemplate.setMandatory(true);

        /**
         * 消息返回回调 (Publisher Returns)
         * 从交换机(exchange) 到 队列(queue) 失败时会被调用
         * 情况说明：
         * 新的 Spring Boot (Spring AMQP 2.4+) 版本中，RabbitTemplate 的 setReturnsCallback 方法签名发生了变化。
         * 它不再接收 5 个单独的参数（message, replyCode, replyText, exchange, routingKey），
         * 而是接收一个封装好的对象 ReturnedMessage
         * 旧版本: (message, replyCode, replyText, exchange, routingKey) -> ...
         * 新版本: (returnedMessage) -> ... 所有参数都被封装在 returnedMessage 对象中
         */
        rabbitTemplate.setReturnsCallback((ReturnedMessage returned) -> {
            log.warn("ReturnCallback 消息未送达队列!， 消息内容: {}, 回应码: {}, 回应消息: {}, 交换机: {}, 路由key值: {}",
                    returned.getMessage(),
                    returned.getReplyCode(),
                    returned.getReplyText(),
                    returned.getExchange(),
                    returned.getRoutingKey());
        });

        return rabbitTemplate;
    }

    // 创建logback日志交换机
    @Bean
    public FanoutExchange fanoutExchange(){
        return ExchangeBuilder.fanoutExchange("protocol.sl651.rabbit.log").build();
    }
}