package com.example.config;

import com.example.utils.Const;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ消息队列配置
 */
@Configuration
public class RabbitConfiguration {

    @Bean("errorQueue")
    public Queue dlQueue() {
        return QueueBuilder
                .durable(Const.MQ_ERROR)
                .ttl(24 * 60 * 60 * 1000)
                .build();
    }

    @Bean("errorExchange")
    public Exchange dlExchange() {
        return ExchangeBuilder
                .directExchange("dlx.direct")
                .build();
    }

    @Bean
    public Binding dlBinding( @Qualifier("errorQueue") Queue errorQueue,
                            @Qualifier("errorExchange")Exchange errorExchange) {
        return BindingBuilder
                .bind(errorQueue)
                .to(errorExchange)
                .with("error-message")
                .noargs();
    }

    @Bean("mailQueue")
    public Queue queue(){
        return QueueBuilder
                .durable(Const.MQ_MAIL)
                .deadLetterExchange("dlx.direct")
                .deadLetterRoutingKey("error-message")
                .ttl(3 *60 * 60 * 1000)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
