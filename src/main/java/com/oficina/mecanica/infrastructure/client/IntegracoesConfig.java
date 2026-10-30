package com.oficina.mecanica.infrastructure.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PropriedadesIntegracoes.class)
public class IntegracoesConfig {

    public static final String EXECUTOR = "executorIntegracoes";

    @Bean(EXECUTOR)
    public ThreadPoolTaskExecutor executorIntegracoes() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("integracao-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        return executor;
    }

    @Bean(BillingRestClient.FONTE)
    public RestClient restClientBilling(RestClient.Builder builder, PropriedadesIntegracoes propriedades) {
        return construir(builder, propriedades.billing().url(), propriedades);
    }

    @Bean(ExecutionRestClient.FONTE)
    public RestClient restClientExecution(RestClient.Builder builder, PropriedadesIntegracoes propriedades) {
        return construir(builder, propriedades.execution().url(), propriedades);
    }

    private static RestClient construir(RestClient.Builder builder, String url, PropriedadesIntegracoes propriedades) {
        var configuracao = ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(propriedades.timeoutConexao())
            .withReadTimeout(propriedades.timeoutLeitura());
        return builder.clone()
            .baseUrl(url)
            .requestFactory(ClientHttpRequestFactoryBuilder.jdk().build(configuracao))
            .build();
    }
}
