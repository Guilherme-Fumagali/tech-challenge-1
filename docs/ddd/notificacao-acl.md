# Serviço de notificação e Anti-Corruption Layer

O envio do orçamento ao cliente é feito por um serviço de notificação, modelado no Event Storming como sistema externo. Este documento descreve como a integração foi isolada do domínio.

## Histórico

Na Fase 1, o Event Storming registrou o serviço de notificação como **Hot Spot**: o canal (e-mail, SMS ou push) ainda não estava definido. Por isso, o MVP implementou apenas a fronteira da integração e um adaptador que registra a notificação em log. O critério de aceite era atendido pelo fluxo `POST /api/ordens/{id}/gerar-orcamento`, que muda o status para `AGUARDANDO_APROVACAO`, e pela consulta pública `GET /api/ordens/{id}/status`.

Na Fase 2, foi implementado o envio de e-mail por SMTP (`SmtpNotificacaoService`, com MailHog nos ambientes de desenvolvimento e demonstração), incluindo os links de aprovação externa. O adaptador de log continua disponível.

## Anti-Corruption Layer

No Context Map, o serviço de notificação é um sistema externo com relacionamento ACL:

```
[Bounded Context: Oficina] ──ACL──> [Sistema Externo: Notificação]
```

O ACL impede que tipos e conceitos do provedor externo sejam usados nas regras de negócio. Ele é implementado como uma porta de saída na camada de aplicação, com dois adaptadores na infraestrutura, selecionados pela propriedade `app.notificacao.canal`:

```
application/port/NotificacaoService.java                  fronteira do ACL, na linguagem do domínio
infrastructure/notification/LogNotificacaoService.java     canal=log (padrão, uso em desenvolvimento)
infrastructure/notification/SmtpNotificacaoService.java    canal=smtp
```

A porta de saída não depende de framework nem de provedor:

```java
public interface NotificacaoService {
    void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao);
}
```

O adaptador SMTP obtém o e-mail do cliente, monta a mensagem com os links de aprovação e reprovação e a envia com `JavaMailSender`:

```java
@Component
@ConditionalOnProperty(prefix = "app.notificacao", name = "canal", havingValue = "smtp")
public class SmtpNotificacaoService implements NotificacaoService {

    @Override
    public void notificarOrcamentoPendente(UUID osId, UUID clienteId, BigDecimal valorTotal, String tokenAprovacao) {
        var cliente = clienteRepository.buscarPorId(clienteId).orElseThrow(...);
        mailSender.send(mensagem);
    }
}
```

O adaptador de log implementa a mesma interface e apenas registra a notificação. Como ele inclui o token de aprovação na mensagem de log, seu uso é restrito ao desenvolvimento; nos ambientes da AWS, o canal configurado é `smtp`.

## Fluxo de chamada

```
GerarOrcamentoUseCase
  ├─ os.gerarOrcamento(validadeToken)   regra de domínio; gera o token de aprovação externa
  ├─ repository.salvar(os)              porta de persistência
  └─ notificacaoService.notificarOrcamentoPendente(...)
       └─ LogNotificacaoService ou SmtpNotificacaoService, conforme app.notificacao.canal
```

## Arquivos

| Arquivo | Camada | Papel |
|---|---|---|
| `application/port/NotificacaoService.java` | aplicação | fronteira do ACL |
| `application/usecase/ordemservico/GerarOrcamentoUseCase.java` | aplicação | consumidor da porta |
| `infrastructure/notification/LogNotificacaoService.java` | infraestrutura | adaptador de log |
| `infrastructure/notification/SmtpNotificacaoService.java` | infraestrutura | adaptador SMTP |

Para integrar outro canal (SES, SNS ou um provedor de SMS), basta criar uma nova implementação de `NotificacaoService` com `@ConditionalOnProperty` para o valor desejado de `app.notificacao.canal`, sem alterar regras de negócio.
