# Runbook — Healthcheck falhando

**Alerta:** `AWS-OficinaAPI-<Amb>-Health-Uptime-Critical`
**Dispara quando:** 2 checagens sintéticas consecutivas falham.

## O que significa

O monitor sintético bate em `/actuator/health` pelo API Gateway, a cada 5 minutos, de 3
localizações. Duas falhas seguidas significam **pelo menos 10 minutos de indisponibilidade**.

O caminho testado é o completo: API Gateway → VPC Link → NLB → NodePort → pod. Qualquer
elo quebrado derruba o check.

## Impacto

Indisponibilidade da API para o cliente final. É o alerta mais grave do conjunto.

## Diagnóstico — de fora para dentro

Testar cada elo na ordem, para isolar onde parou.

1. **Gateway responde?**

```bash
API_URL=$(aws ssm get-parameter --name /oficina/prod/api-gateway-url --query 'Parameter.Value' --output text)
curl -si "$API_URL/actuator/health" | head -5
```

2. **Pods de pé e prontos?**

```bash
kubectl get pods -n oficina -o wide
kubectl get deployment/oficina-api -n oficina
```

3. **Aplicação responde dentro do cluster?** Isola gateway/NLB de aplicação:

```bash
kubectl run curl-teste --rm -it --restart=Never --image=curlimages/curl -n oficina \
  -- curl -s http://oficina-api/actuator/health
```

4. **Target group saudável?** É o elo mais fácil de esquecer:

```bash
TG=$(aws elbv2 describe-target-groups --names oficina-api-tg-prod \
       --query 'TargetGroups[0].TargetGroupArn' --output text)
aws elbv2 describe-target-health --target-group-arn "$TG"
```

5. **Banco alcançável?** `/actuator/health` inclui o datasource — banco fora derruba o check
   mesmo com pod de pé.

```bash
aws rds describe-db-instances --db-instance-identifier oficina-api-db-prod \
  --query 'DBInstances[0].DBInstanceStatus' --output text
```

## Causas mais prováveis

| Onde quebrou | Sintoma | Ação |
|---|---|---|
| Pod | `CrashLoopBackOff` | `kubectl logs --previous`; provável falha de migration ou secret |
| Target group | Targets `unhealthy` | Nó novo não registrado — conferir `aws_autoscaling_attachment` |
| Banco | Pod de pé, health `DOWN` | Ver status do RDS e security group |
| Deploy | Falhou no meio do rollout | `kubectl rollout undo` |
| Nó | Nó `NotReady` | `kubectl describe node` |

## Mitigação

- **Rollout ruim:**
  ```bash
  kubectl rollout undo deployment/oficina-api -n oficina
  ```
- **Pod travado:**
  ```bash
  kubectl rollout restart deployment/oficina-api -n oficina
  ```
- **Target não registrado:** conferir se o NodePort 30080 está no security group do cluster.

## Quando escalar

Imediatamente. Este alerta é Critical por definição — indisponibilidade não espera
diagnóstico completo. Mitigar primeiro, investigar com o serviço no ar.
