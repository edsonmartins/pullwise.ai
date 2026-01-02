package com.pullwise.api.config;

import jakarta.jms.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import jakarta.jms.DeliveryMode;

/**
 * Configuração do Artemis/ActiveMQ para processamento assíncrono de mensagens.
 */
@Configuration
@EnableJms
public class ArtemisConfig {

    // Queues
    public static final String REVIEW_QUEUE = "pullwise.review.queue";
    public static final String WEBHOOK_QUEUE = "pullwise.webhook.queue";
    public static final String NOTIFICATION_QUEUE = "pullwise.notification.queue";
    public static final String DLQ = "pullwise.dlq";

    @Value("${spring.activemq.broker-url}")
    private String brokerUrl;

    @Value("${spring.activemq.user}")
    private String brokerUsername;

    @Value("${spring.activemq.password}")
    private String brokerPassword;

    @Bean
    public MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        template.setDeliveryMode(DeliveryMode.PERSISTENT);
        template.setPriority(4);
        template.setTimeToLive(3600000); // 1 hora
        return template;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrency("3-10");
        factory.setSessionTransacted(true);
        factory.setAutoStartup(true);

        configurer.configure(factory, connectionFactory);
        return factory;
    }

    /**
     * Factory dedicada para reviews (maior prioridade).
     */
    @Bean("reviewListenerFactory")
    public DefaultJmsListenerContainerFactory reviewListenerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrency("5-15");
        factory.setSessionTransacted(false);
        factory.setAutoStartup(true);

        configurer.configure(factory, connectionFactory);
        return factory;
    }

    /**
     * Factory dedicada para webhooks (alta concorrência).
     */
    @Bean("webhookListenerFactory")
    public DefaultJmsListenerContainerFactory webhookListenerFactory(
            ConnectionFactory connectionFactory,
            DefaultJmsListenerContainerFactoryConfigurer configurer) {

        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrency("10-20");
        factory.setSessionTransacted(false);
        factory.setAutoStartup(true);

        configurer.configure(factory, connectionFactory);
        return factory;
    }
}
