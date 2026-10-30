package com.oficina.mecanica.infrastructure.messaging;

import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropriedadesMensageria.class)
public class MensageriaConfig {

    @Bean
    public SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient) {
        return SqsTemplate.builder()
            .sqsAsyncClient(sqsAsyncClient)
            .configureDefaultConverter(conversor -> conversor.doNotSendPayloadTypeHeader())
            .configure(opcoes -> opcoes.queueNotFoundStrategy(QueueNotFoundStrategy.FAIL))
            .build();
    }
}
