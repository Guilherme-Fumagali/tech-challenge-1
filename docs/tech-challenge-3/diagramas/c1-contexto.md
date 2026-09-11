# C4 · Nível 1 — Contexto

Quem usa o sistema e com que sistemas externos ele conversa.

```mermaid
C4Context
    title Sistema de Ordens de Serviço da Oficina — Contexto

    Person(cliente, "Cliente", "Dono do veículo. Autentica-se pelo CPF para acompanhar suas ordens de serviço.")
    Person(atendente, "Atendente da oficina", "Abre ordens, registra diagnóstico, adiciona serviços e peças.")

    System(oficina, "Sistema da Oficina", "Gestão de ordens de serviço: abertura, diagnóstico, orçamento, execução e entrega.")

    System_Ext(email, "Servidor SMTP", "Entrega o e-mail de aprovação de orçamento ao cliente.")
    System_Ext(newrelic, "New Relic", "APM, métricas de infraestrutura, logs e alertas.")

    Rel(cliente, oficina, "Autentica por CPF e consulta OS", "HTTPS/JSON")
    Rel(atendente, oficina, "Opera as ordens de serviço", "HTTPS/JSON")
    Rel(oficina, email, "Envia orçamento com links de aprovação", "SMTP")
    Rel(email, oficina, "Cliente aprova ou reprova pelo link", "HTTPS")
    Rel(oficina, newrelic, "Traces, métricas e logs", "HTTPS/OTLP")
```

**Ponto que costuma passar despercebido:** a aprovação de orçamento é um fluxo de **volta** — o cliente clica num link do e-mail e o sistema recebe a chamada de fora, autenticada por um token de uso único, não pelo JWT de CPF.
