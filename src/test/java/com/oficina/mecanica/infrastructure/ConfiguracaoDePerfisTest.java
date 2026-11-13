package com.oficina.mecanica.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguracaoDePerfisTest {

    private final YamlPropertySourceLoader leitor = new YamlPropertySourceLoader();

    @Test
    void segredoJwtSemValorPadraoForaDosPerfisLocalETeste() throws IOException {
        assertThat(valor("application.yml", "jwt.secret")).isEqualTo("${JWT_SECRET}");
        assertThat(valor("application.yml", "app.seguranca.chave-interna")).isEqualTo("${CHAVE_INTERNA:}");
        assertThat(valor("application-local.yml", "jwt.secret")).startsWith("${JWT_SECRET:");
        assertThat(valor("application-local.yml", "app.seguranca.chave-interna"))
            .isEqualTo("${CHAVE_INTERNA:chave-interna-local}");
    }

    @Test
    void perfilLocalApontaOSqsParaOLocalstackComCredenciaisEstaticas() throws IOException {
        assertThat(valor("application-local.yml", "spring.cloud.aws.sqs.endpoint"))
            .isEqualTo("${SQS_ENDPOINT:http://localhost:4566}");
        assertThat(valor("application-local.yml", "spring.cloud.aws.credentials.access-key")).isEqualTo("test");
        assertThat(valor("application.yml", "spring.cloud.aws.sqs.endpoint")).isNull();
    }

    private String valor(String arquivo, String propriedade) throws IOException {
        return leitor.load(arquivo, new ClassPathResource(arquivo)).stream()
            .map(fonte -> fonte.getProperty(propriedade))
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .map(Object::toString)
            .orElse(null);
    }
}
