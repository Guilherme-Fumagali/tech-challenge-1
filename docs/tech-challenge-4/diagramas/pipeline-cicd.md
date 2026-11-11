# Pipeline de CI/CD de cada serviço

Mesmo desenho nos três repositórios de serviço. `Build & Test`, `SonarCloud` e `Format & validate (infra)` são checks obrigatórios do PR para `develop` e `main`.

```mermaid
---
config:
  flowchart:
    padding: 10
    nodeSpacing: 34
    rankSpacing: 38
---
flowchart TB
    push(["push / pull request"]) --> bt["Build & Test<br/>unitários, integração,<br/>BDD, JaCoCo ≥ 80%"]
    bt --> sonar["SonarCloud<br/>quality gate"]
    sonar -->|"develop / main"| img["Build & push no ECR<br/>tag = commit"]
    img --> cl["Conferir cluster<br/>(SSM)"]
    cl -->|existe| dep["Deploy no EKS<br/>Secret do SSM · kustomize<br/>rollout · smoke test"]
    cl -->|desligado| fim(["imagem publicada,<br/>deploy pulado"])
    infra(["workflow_dispatch"]) --> fv["Format & validate (infra)<br/>fmt · validate · tflint · checkov"]
    fv --> ap["Plan / Apply (env)<br/>banco, filas, role de pod"]
```

| Ambiente | Branch | Aprovação |
|---|---|---|
| homologação (`staging`) | `develop` | automática |
| produção (`prod`) | `main` | revisor obrigatório no environment |
