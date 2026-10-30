package com.oficina.mecanica.infrastructure;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegracaoBase {

    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("oficina_test")
        .withUsername("oficina")
        .withPassword("oficina");

    protected static final LocalStackContainer LOCALSTACK =
        new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
            .withServices(LocalStackContainer.Service.SQS);

    protected static final WireMockServer PARTICIPANTES = new WireMockServer(WireMockConfiguration.options()
        .dynamicPort());

    static {
        Startables.deepStart(POSTGRES, LOCALSTACK).join();
        FilasDeTeste.inicializar(LOCALSTACK);
        PARTICIPANTES.start();
    }

    @Value("${jwt.secret}")
    protected String segredoJwt;

    @DynamicPropertySource
    static void registrarPropriedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.cloud.aws.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
        registry.add("app.integracoes.billing.url", PARTICIPANTES::baseUrl);
        registry.add("app.integracoes.execution.url", PARTICIPANTES::baseUrl);
    }

    protected String tokenFuncionario() {
        return token(UUID.randomUUID().toString(), "FUNCIONARIO");
    }

    protected String tokenCliente(UUID clienteId) {
        return token(clienteId.toString(), "CLIENTE");
    }

    private String token(String sub, String role) {
        var agora = Instant.now();
        return Jwts.builder()
            .subject(sub)
            .issuer("oficina-auth")
            .claim("role", role)
            .issuedAt(Date.from(agora))
            .expiration(Date.from(agora.plusSeconds(900)))
            .signWith(Keys.hmacShaKeyFor(segredoJwt.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }
}
