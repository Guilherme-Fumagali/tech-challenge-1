package com.oficina.mecanica.infrastructure.messaging;

import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.support.converter.MessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropriedadesMensageria.class)
public class MensageriaConfig {

    @Bean
    public MessagingMessageConverter<Message> conversorMensagensSqs() {
        var conversor = new SqsMessagingMessageConverter();
        conversor.setPayloadTypeMapper(mensagem -> null);
        conversor.doNotSendPayloadTypeHeader();
        return conversor;
    }

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient, MessagingMessageConverter<Message> conversor) {
        return SqsTemplate.builder()
            .sqsAsyncClient(sqsAsyncClient)
            .messageConverter(conversor)
            .configure(opcoes -> opcoes.queueNotFoundStrategy(QueueNotFoundStrategy.FAIL))
            .build();
    }
}
