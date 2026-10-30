package com.oficina.mecanica.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.testcontainers.containers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

public final class FilasDeTeste {

    public static final String OS_EVENTOS = "oficina-local-os-eventos";
    public static final String BILLING_COMANDOS = "oficina-local-billing-comandos";
    public static final String EXECUTION_COMANDOS = "oficina-local-execution-comandos";
    public static final String DLQ_OS_EVENTOS = OS_EVENTOS + "-dlq";

    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Map<String, List<MensagemRecebida>> RECEBIDAS = new ConcurrentHashMap<>();
    private static SqsClient sqs;

    private FilasDeTeste() {}

    public record MensagemRecebida(JsonNode envelope, Map<String, MessageAttributeValue> atributos) {

        public String tipo() {
            return envelope.path("tipo").asText();
        }

        public UUID mensagemId() {
            return UUID.fromString(envelope.path("mensagemId").asText());
        }

        public UUID osId() {
            return UUID.fromString(envelope.path("osId").asText());
        }
    }

    static synchronized void inicializar(LocalStackContainer localstack) {
        sqs = SqsClient.builder()
            .endpointOverride(localstack.getEndpoint())
            .region(Region.of(localstack.getRegion()))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())))
            .build();
        criarFilaComDlq(OS_EVENTOS, "2");
        criarFilaComDlq(BILLING_COMANDOS, "30");
        criarFilaComDlq(EXECUTION_COMANDOS, "30");
    }

    private static void criarFilaComDlq(String nome, String visibilidade) {
        var dlqUrl = sqs.createQueue(r -> r.queueName(nome + "-dlq")).queueUrl();
        var dlqArn = sqs.getQueueAttributes(r -> r.queueUrl(dlqUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
            .attributes().get(QueueAttributeName.QUEUE_ARN);
        sqs.createQueue(r -> r.queueName(nome).attributes(Map.of(
            QueueAttributeName.VISIBILITY_TIMEOUT, visibilidade,
            QueueAttributeName.REDRIVE_POLICY,
            "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"5\"}".formatted(dlqArn))));
    }

    public static UUID publicarEvento(String tipo, UUID sagaId, UUID osId, UUID causadaPor, Map<String, Object> dados) {
        var mensagemId = UUID.randomUUID();
        publicarEvento(mensagemId, tipo, sagaId, osId, causadaPor, dados);
        return mensagemId;
    }

    public static void publicarEvento(UUID mensagemId, String tipo, UUID sagaId, UUID osId, UUID causadaPor,
                                      Map<String, Object> dados) {
        ObjectNode envelope = JSON.createObjectNode();
        envelope.put("mensagemId", mensagemId.toString());
        envelope.put("tipo", tipo);
        envelope.put("versao", 1);
        envelope.put("sagaId", sagaId == null ? null : sagaId.toString());
        envelope.put("osId", osId.toString());
        envelope.put("causadaPor", causadaPor == null ? null : causadaPor.toString());
        envelope.put("origem", origem(tipo));
        envelope.put("ocorridoEm", Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        envelope.set("dados", JSON.valueToTree(dados));
        publicarCorpo(OS_EVENTOS, escrever(envelope), tipo);
    }

    public static void publicarCorpo(String fila, String corpo, String tipo) {
        var url = url(fila);
        sqs.sendMessage(r -> r.queueUrl(url).messageBody(corpo).messageAttributes(Map.of(
            "tipo", MessageAttributeValue.builder().dataType("String").stringValue(tipo).build(),
            "versao", MessageAttributeValue.builder().dataType("String").stringValue("1").build())));
    }

    public static MensagemRecebida aguardarMensagem(String fila, Predicate<MensagemRecebida> filtro, Duration limite) {
        return Awaitility.await().atMost(limite).pollInterval(Duration.ofMillis(200)).until(() -> {
            drenar(fila);
            return RECEBIDAS.getOrDefault(fila, List.of()).stream().filter(filtro).findFirst().orElse(null);
        }, java.util.Objects::nonNull);
    }

    public static MensagemRecebida aguardarComando(String fila, UUID osId, String tipo) {
        return aguardarMensagem(fila, m -> m.osId().equals(osId) && m.tipo().equals(tipo), Duration.ofSeconds(20));
    }

    public static List<MensagemRecebida> comandosRecebidos(String fila, UUID osId, String tipo) {
        drenar(fila);
        return RECEBIDAS.getOrDefault(fila, List.of()).stream()
            .filter(m -> m.osId().equals(osId) && m.tipo().equals(tipo))
            .toList();
    }

    public static int mensagensAproximadas(String fila) {
        var atributos = sqs.getQueueAttributes(r -> r.queueUrl(url(fila))
            .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES,
                QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE)).attributes();
        return Integer.parseInt(atributos.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
            + Integer.parseInt(atributos.get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE));
    }

    public static void esvaziar(String fila) {
        sqs.purgeQueue(r -> r.queueUrl(url(fila)));
    }

    private static synchronized void drenar(String fila) {
        var url = url(fila);
        var recebidas = sqs.receiveMessage(r -> r.queueUrl(url).maxNumberOfMessages(10).waitTimeSeconds(1)
            .messageAttributeNames("All")).messages();
        for (var mensagem : recebidas) {
            RECEBIDAS.computeIfAbsent(fila, chave -> new CopyOnWriteArrayList<>())
                .add(new MensagemRecebida(ler(mensagem.body()), mensagem.messageAttributes()));
            sqs.deleteMessage(r -> r.queueUrl(url).receiptHandle(mensagem.receiptHandle()));
        }
    }

    private static String url(String fila) {
        return sqs.getQueueUrl(r -> r.queueName(fila)).queueUrl();
    }

    private static String origem(String tipo) {
        return tipo.startsWith("Orcamento") || tipo.startsWith("Pagamento") ? "billing-service" : "execution-service";
    }

    private static JsonNode ler(String corpo) {
        try {
            return JSON.readTree(corpo);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String escrever(JsonNode no) {
        try {
            return JSON.writeValueAsString(no);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
