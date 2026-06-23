package com.caixacombo.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.rabbitmq.host", havingValue = "localhost", matchIfMissing = false)
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_APPROVED_QUEUE = "payment.approved.queue";
    public static final String PAYMENT_DECLINED_QUEUE = "payment.declined.queue";
    public static final String PAYMENT_CANCELLED_QUEUE = "payment.cancelled.queue";
    public static final String PAYMENT_REFUNDED_QUEUE = "payment.refunded.queue";

    @Value("${spring.rabbitmq.host:localhost}")
    private String rabbitMqHost;

    @Value("${spring.rabbitmq.port:5672}")
    private int rabbitMqPort;

    @Value("${spring.rabbitmq.username:guest}")
    private String rabbitMqUsername;

    @Value("${spring.rabbitmq.password:guest}")
    private String rabbitMqPassword;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        factory.setHost(rabbitMqHost);
        factory.setPort(rabbitMqPort);
        factory.setUsername(rabbitMqUsername);
        factory.setPassword(rabbitMqPassword);
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        return factory;
    }

    @Bean
    public DirectExchange paymentExchange() {
        return new DirectExchange(PAYMENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return QueueBuilder.durable(PAYMENT_APPROVED_QUEUE).build();
    }

    @Bean
    public Queue paymentDeclinedQueue() {
        return QueueBuilder.durable(PAYMENT_DECLINED_QUEUE).build();
    }

    @Bean
    public Queue paymentCancelledQueue() {
        return QueueBuilder.durable(PAYMENT_CANCELLED_QUEUE).build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable(PAYMENT_REFUNDED_QUEUE).build();
    }

    @Bean
    public Binding approvedBinding(Queue paymentApprovedQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentApprovedQueue)
                .to(paymentExchange)
                .with("payment.approved");
    }

    @Bean
    public Binding declinedBinding(Queue paymentDeclinedQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentDeclinedQueue)
                .to(paymentExchange)
                .with("payment.declined");
    }

    @Bean
    public Binding cancelledBinding(Queue paymentCancelledQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentCancelledQueue)
                .to(paymentExchange)
                .with("payment.cancelled");
    }

    @Bean
    public Binding refundedBinding(Queue paymentRefundedQueue, DirectExchange paymentExchange) {
        return BindingBuilder.bind(paymentRefundedQueue)
                .to(paymentExchange)
                .with("payment.refunded");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
