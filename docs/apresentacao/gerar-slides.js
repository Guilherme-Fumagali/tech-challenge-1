const PptxGenJS = require("pptxgenjs");

const PAPER  = "F5F2ED";
const INK    = "2A2E33";
const MUTED  = "767E86";
const RULE   = "D8D3CA";
const PINE   = "1E6E63";
const OCHRE  = "C46B2E";

const SERIF = "Georgia";
const SANS  = "Segoe UI";
const MONO  = "Consolas";

const pptx = new PptxGenJS();
// LAYOUT_16x9 do pptxgenjs é 10 x 5,625in — pequeno demais para esta grade.
pptx.defineLayout({ name: "TC16x9", width: 13.333, height: 7.5 });
pptx.layout = "TC16x9";
pptx.author = "Danilo Canato · Guilherme Fumagali";
pptx.title = "Oficina Mecânica API — Tech Challenge Fase 2";

const W = 13.333;
const M = 0.9;
const CW = W - M * 2;

function novoSlide(notas) {
  const s = pptx.addSlide();
  s.background = { color: PAPER };
  if (notas) s.addNotes(notas);
  return s;
}

// Rodapé: quem fala, em que minuto, e o número do slide.
function rodape(s, quem, secao, tempo, n) {
  s.addShape(pptx.ShapeType.rect, {
    x: M, y: 6.78, w: CW, h: 0.012, fill: { color: RULE },
  });
  s.addText(
    [
      { text: quem, options: { color: PINE, bold: true } },
      { text: "   ·   ", options: { color: RULE } },
      { text: secao, options: { color: MUTED } },
      { text: "   ·   ", options: { color: RULE } },
      { text: tempo, options: { color: MUTED } },
    ],
    { x: M, y: 6.9, w: CW - 0.6, h: 0.3, fontFace: SANS, fontSize: 10, valign: "middle" }
  );
  s.addText(String(n), {
    x: W - M - 0.6, y: 6.9, w: 0.6, h: 0.3,
    fontFace: SANS, fontSize: 10, color: MUTED, align: "right", valign: "middle",
  });
}

// Título + linha de apoio, o topo de toda página de conteúdo.
function cabecalho(s, titulo, lede) {
  s.addText(titulo, {
    x: M, y: 0.62, w: CW, h: 0.95,
    fontFace: SERIF, fontSize: 34, bold: true, color: INK, valign: "bottom",
  });
  s.addShape(pptx.ShapeType.rect, {
    x: M, y: 1.72, w: 1.1, h: 0.045, fill: { color: PINE },
  });
  if (lede) {
    s.addText(lede, {
      x: M, y: 1.95, w: CW * 0.78, h: 0.7,
      fontFace: SANS, fontSize: 15, color: MUTED, lineSpacing: 24, valign: "top",
    });
  }
}

// Bullets: quadradinho de acento + frase forte + complemento em cinza.
function bullets(s, itens, y0) {
  let y = y0;
  itens.forEach((it) => {
    s.addShape(pptx.ShapeType.rect, {
      x: M, y: y + 0.16, w: 0.11, h: 0.11, fill: { color: it.quente ? OCHRE : PINE },
    });
    s.addText(
      [
        { text: it.forte, options: { bold: true, color: INK } },
        it.resto ? { text: "  " + it.resto, options: { color: MUTED } } : { text: "" },
      ],
      {
        x: M + 0.3, y: y, w: CW - 0.35, h: it.alto || 0.72,
        fontFace: SANS, fontSize: 14, lineSpacing: 22, valign: "top",
      }
    );
    y += it.alto || 0.72;
    y += 0.16;
  });
}

// Cartão simples: rótulo, afirmação, explicação. Sem moldura pesada.
function cartoes(s, cards, y0, h) {
  const gap = 0.4;
  const cw = (CW - gap * (cards.length - 1)) / cards.length;
  cards.forEach((c, i) => {
    const x = M + i * (cw + gap);
    s.addShape(pptx.ShapeType.rect, {
      x: x, y: y0, w: cw, h: 0.04, fill: { color: c.quente ? OCHRE : PINE },
    });
    s.addText(c.rotulo.toUpperCase(), {
      x: x, y: y0 + 0.18, w: cw, h: 0.25,
      fontFace: SANS, fontSize: 9.5, bold: true, color: MUTED, charSpacing: 1.4,
    });
    s.addText(c.titulo, {
      x: x, y: y0 + 0.46, w: cw, h: 0.62,
      fontFace: SERIF, fontSize: 17, bold: true, color: INK, lineSpacing: 22, valign: "top",
    });
    s.addText(c.texto, {
      x: x, y: y0 + 1.1, w: cw, h: h - 1.1,
      fontFace: SANS, fontSize: 12.5, color: MUTED, lineSpacing: 19, valign: "top",
    });
  });
}

// Fluxo horizontal em caixinhas monoespaçadas.
// Mede a largura natural e encolhe tudo junto se não couber — nada pode vazar pela direita.
function fluxo(s, etapas, y0) {
  const H = 0.42;
  const CHAR = 0.082; // largura média de um caractere Consolas 10.5pt, em polegadas
  const PAD = 0.42;
  const SETA = 0.36;

  const natural = etapas.map((p) => p.t.length * CHAR + PAD);
  const totalNatural =
    natural.reduce((a, b) => a + b, 0) + (etapas.length - 1) * SETA;

  const k = Math.min(1, CW / totalNatural);
  const larguras = natural.map((w) => w * k);
  const seta = SETA * k;
  const fonte = 10.5 * k;
  const total = larguras.reduce((a, b) => a + b, 0) + (etapas.length - 1) * seta;

  let x = M + (CW - total) / 2;

  etapas.forEach((p, i) => {
    const w = larguras[i];
    s.addShape(pptx.ShapeType.rect, {
      x: x, y: y0, w: w, h: H,
      fill: { color: PAPER },
      line: { color: p.quente ? OCHRE : RULE, width: p.quente ? 1.5 : 1 },
    });
    s.addText(p.t, {
      x: x, y: y0, w: w, h: H,
      fontFace: MONO, fontSize: fonte, color: p.quente ? OCHRE : INK,
      align: "center", valign: "middle",
    });
    x += w;
    if (i < etapas.length - 1) {
      s.addText("→", {
        x: x, y: y0, w: seta, h: H,
        fontFace: SANS, fontSize: 12, color: MUTED, align: "center", valign: "middle",
      });
      x += seta;
    }
  });
}

// Bloco de código / terminal.
function codigo(s, linhas, x, y, w, h) {
  s.addShape(pptx.ShapeType.rect, { x: x, y: y, w: w, h: h, fill: { color: "ECE8E1" } });
  s.addShape(pptx.ShapeType.rect, { x: x, y: y, w: 0.035, h: h, fill: { color: OCHRE } });
  s.addText(linhas, {
    x: x + 0.28, y: y + 0.18, w: w - 0.5, h: h - 0.36,
    fontFace: MONO, fontSize: 11, color: INK, lineSpacing: 18, valign: "top",
  });
}

// Passos numerados da demo. Altura por passo cresce quando a linha quebra —
// altura fixa fazia o passo longo invadir o seguinte.
function passos(s, itens, y0) {
  const LARG = CW - 0.5;
  const CPL = 92; // caracteres que cabem numa linha de Segoe UI 14pt nessa largura
  let y = y0;
  itens.forEach((it, i) => {
    const linhas = Math.max(1, Math.ceil(it.length / CPL));
    const h = linhas * 0.28 + 0.1;
    s.addText(String(i + 1), {
      x: M, y: y, w: 0.32, h: 0.32,
      fontFace: MONO, fontSize: 12, color: OCHRE, bold: true, align: "left", valign: "top",
    });
    s.addText(it, {
      x: M + 0.4, y: y, w: LARG, h: h,
      fontFace: SANS, fontSize: 14, color: INK, lineSpacing: 20, valign: "top",
    });
    y += h + 0.22;
  });
}

/* ─────────────────────────── 1 · Abertura ─────────────────────────── */
{
  const s = novoSlide(
    "Boa tarde. Somos o Danilo e o Guilherme, e este é o Tech Challenge da Fase 2: a API de gestão de uma oficina mecânica de médio porte.\n\n" +
      "Na Fase 1 entregamos o MVP em Clean Architecture. A Fase 2 pediu duas coisas: evoluir as regras de negócio e entregar a cadeia de infraestrutura inteira — container, Kubernetes, Terraform e CI/CD.\n\n" +
      "É isso que vamos mostrar. E vamos mostrar rodando."
  );
  s.addShape(pptx.ShapeType.rect, { x: M, y: 1.9, w: 1.6, h: 0.06, fill: { color: OCHRE } });
  s.addText("Oficina Mecânica API", {
    x: M, y: 2.15, w: CW, h: 1.1,
    fontFace: SERIF, fontSize: 46, bold: true, color: INK, valign: "middle",
  });
  s.addText(
    "Gestão de ordens de serviço para uma oficina de médio porte.\nDa regra de negócio ao cluster, com a esteira inteira no meio.",
    { x: M, y: 3.3, w: CW * 0.7, h: 0.9, fontFace: SANS, fontSize: 16, color: MUTED, lineSpacing: 26 }
  );
  s.addText("Danilo Canato          Guilherme Fumagali", {
    x: M, y: 4.6, w: CW, h: 0.35,
    fontFace: SANS, fontSize: 13, bold: true, color: INK,
  });
  s.addText("Tech Challenge · Fase 2 · PosTech FIAP — Arquitetura de Software", {
    x: M, y: 5.0, w: CW, h: 0.3,
    fontFace: SANS, fontSize: 11.5, color: MUTED,
  });
}

/* ─────────────────────────── 2 · Escopo ─────────────────────────── */
{
  const s = novoSlide(
    "Cinco entregas. Três regras novas de domínio; a aplicação containerizada; um cluster Kubernetes com escalabilidade automática; a infraestrutura descrita em Terraform; e uma esteira de CI/CD que vai do commit ao deploy.\n\n" +
      "Eu cubro a aplicação, o Guilherme cobre a infraestrutura. Cada um com uma demo ao vivo."
  );
  cabecalho(s, "O que a Fase 2 pediu", "Cinco entregas. As duas primeiras são minhas; as três últimas, do Guilherme.");
  bullets(
    s,
    [
      { forte: "Novas regras de negócio.", resto: "Priorização da fila, exclusão lógica e aprovação externa por token." },
      { forte: "A aplicação containerizada.", resto: "Imagem enxuta, sem ferramenta de build e sem root." },
      { forte: "Kubernetes com escalabilidade automática.", resto: "Probes, requests e limits, e um HPA que reage à carga de verdade." },
      { forte: "A infraestrutura descrita em Terraform.", resto: "Dois cenários: um cluster local e um na nuvem." },
      { forte: "Uma esteira que vai do commit ao cluster.", resto: "Testes, segurança, imagem publicada e deploy." },
    ],
    2.95
  );
  rodape(s, "Danilo", "Escopo", "0:40 → 1:20", 2);
}

/* ─────────────────────────── 3 · Domínio ─────────────────────────── */
{
  const s = novoSlide(
    "A primeira regra: a listagem de OS é ordenada por prioridade de negócio, não por data. Dentro do mesmo status, as mais antigas primeiro — quem esperou mais aparece antes.\n\n" +
      "Segunda: OS Finalizada e Entregue somem da listagem. Exclusão lógica — o registro fica no banco para o relatório de tempo médio, só não polui a fila de quem está no balcão.\n\n" +
      "Terceira, e a mais interessante: aprovação externa por token. Ao gerar o orçamento, a OS ganha um token único, de uso único e com expiração, e o cliente recebe um e-mail de verdade. Ele aprova sem ter login — o endpoint é público, mas só aceita aquele token, para aquela OS, uma vez só.\n\n" +
      "E reprovar não é só mudar status: estorna o estoque das peças já baixadas."
  );
  cabecalho(s, "Três regras novas", "Todas nascem da mesma pergunta: o que a pessoa no balcão precisa ver primeiro?");
  bullets(
    s,
    [
      {
        forte: "A fila é ordenada por prioridade, não por data.",
        resto: "Em Execução vem antes de Aguardando Aprovação, que vem antes de Em Diagnóstico. No mesmo status, quem esperou mais aparece antes.",
      },
      {
        forte: "OS entregue some da fila, mas não do banco.",
        resto: "Exclusão lógica. O registro continua lá para o relatório de tempo médio — só para de poluir o trabalho de quem está atendendo.",
      },
      {
        forte: "O cliente aprova o orçamento sem ter login.",
        resto: "Um token único por OS, de uso único e com prazo, entregue por e-mail. O endpoint é público, mas só aceita aquele token, para aquela OS, uma vez.",
        quente: true,
      },
      {
        forte: "Reprovar devolve as peças ao estoque.",
        resto: "Não é só uma troca de status: o que já tinha sido baixado volta.",
        alto: 0.5,
      },
    ],
    2.75
  );
  fluxo(
    s,
    [
      { t: "Recebida" },
      { t: "Em Diagnóstico" },
      { t: "Aguardando Aprovação", quente: true },
      { t: "Em Execução" },
      { t: "Finalizada" },
      { t: "Entregue" },
    ],
    6.15
  );
  rodape(s, "Danilo", "Domínio", "1:20 → 2:40", 3);
}

/* ─────────────────────────── 4 · Arquitetura ─────────────────────────── */
{
  const s = novoSlide(
    "A arquitetura não mudou — e esse é o ponto: as três regras novas entraram sem tocar em infraestrutura.\n\n" +
      "O domínio é Java puro, zero Spring. A aplicação orquestra os casos de uso. A infraestrutura tem Spring, JPA, REST, Security.\n\n" +
      "O e-mail é o exemplo mais claro. No Context Map, a notificação é um sistema externo, e a fronteira é um Anti-Corruption Layer: uma porta de saída escrita na linguagem do negócio. Dois adaptadores a implementam — um que só loga, outro que manda SMTP — e a escolha é uma property, não uma decisão de código.\n\n" +
      "Na Fase 1 o canal ficou como Hot Spot em aberto porque o negócio ainda não tinha decidido. Na Fase 2 plugamos o SMTP: nenhuma regra de negócio mudou, só apareceu um adaptador novo. É o ACL fazendo o trabalho dele."
  );
  cabecalho(
    s,
    "A arquitetura aguentou",
    "As três regras novas entraram sem que uma linha de infraestrutura mudasse. É o teste que uma Clean Architecture precisa passar."
  );
  codigo(
    s,
    "// Application — a fronteira do ACL,\n// escrita na linguagem do negócio\n\npublic interface NotificacaoService {\n    void notificarOrcamentoPendente(\n        UUID osId, UUID clienteId,\n        BigDecimal valorTotal,\n        String tokenAprovacao);\n}",
    M,
    3.0,
    6.0,
    2.5
  );
  const x2 = M + 6.4;
  const w2 = CW - 6.4;
  s.addText("NA FASE 1", {
    x: x2, y: 3.0, w: w2, h: 0.25, fontFace: SANS, fontSize: 9.5, bold: true, color: MUTED, charSpacing: 1.4,
  });
  s.addText("O canal ficou em aberto de propósito", {
    x: x2, y: 3.26, w: w2, h: 0.35, fontFace: SERIF, fontSize: 16, bold: true, color: INK,
  });
  s.addText(
    "O negócio ainda não tinha decidido entre e-mail, SMS ou push. Fixar um canal em código seria dívida técnica no dia zero — então construímos só a fronteira.",
    { x: x2, y: 3.66, w: w2, h: 0.9, fontFace: SANS, fontSize: 12.5, color: MUTED, lineSpacing: 19 }
  );
  s.addText("NA FASE 2", {
    x: x2, y: 4.72, w: w2, h: 0.25, fontFace: SANS, fontSize: 9.5, bold: true, color: OCHRE, charSpacing: 1.4,
  });
  s.addText("SMTP plugado, domínio intacto", {
    x: x2, y: 4.98, w: w2, h: 0.35, fontFace: SERIF, fontSize: 16, bold: true, color: INK,
  });
  s.addText(
    "Apareceu um adaptador novo. Nenhum caso de uso, nenhuma entidade e nenhuma regra de negócio foram alterados.",
    { x: x2, y: 5.38, w: w2, h: 0.9, fontFace: SANS, fontSize: 12.5, color: MUTED, lineSpacing: 19 }
  );
  rodape(s, "Danilo", "Arquitetura", "2:40 → 3:50", 4);
}

/* ─────────────────────────── 5 · Demo 1 ─────────────────────────── */
{
  const s = novoSlide(
    "DEMO 1 — 2:30. É o trecho mais fácil de estourar: ensaie.\n\n" +
      "Deixe cliente e veículo já criados ANTES de gravar.\n\n" +
      "Ao adicionar a peça, repare no estoque caindo — decremento atômico, ACID, sem race condition.\n\n" +
      "No MailHog: \"isto é o que o cliente recebe\".\nNo aprovar-externo: \"sem login, só o token\".\nRepita o POST para mostrar que o token já queimou.\n\n" +
      "Se o tempo apertar, corte o passo 6."
  );
  s.addShape(pptx.ShapeType.rect, { x: M, y: 0.62, w: 1.55, h: 0.32, fill: { color: OCHRE } });
  s.addText("DEMO 1", {
    x: M, y: 0.62, w: 1.55, h: 0.32,
    fontFace: SANS, fontSize: 11, bold: true, color: PAPER, align: "center", valign: "middle", charSpacing: 2,
  });
  s.addText("Do orçamento ao e-mail, e do e-mail à aprovação", {
    x: M, y: 1.1, w: CW, h: 0.75,
    fontFace: SERIF, fontSize: 32, bold: true, color: INK, valign: "middle",
  });
  passos(
    s,
    [
      "Login → JWT. As rotas administrativas exigem token.",
      "Abrir a OS, iniciar o diagnóstico, adicionar um serviço e uma peça — e ver o estoque cair na hora.",
      "Gerar o orçamento. O status vai para Aguardando Aprovação.",
      "Abrir o MailHog: o e-mail chegou, com o token. É exatamente isto que o cliente recebe.",
      "Aprovar pelo endpoint público, sem header de autenticação. Sem login. Só o token.",
      "Repetir o mesmo POST — e falhar. Uso único: o token já queimou.",
      "Listar as ordens e mostrar a fila ordenada por prioridade.",
    ],
    2.25
  );
  rodape(s, "Danilo", "Ao vivo", "3:50 → 6:20", 5);
}

/* ─────────────────────────── 6 · Container ─────────────────────────── */
{
  const s = novoSlide(
    "A imagem é um Dockerfile multi-stage: o primeiro estágio compila com Maven, o segundo carrega só o JRE e o JAR.\n\n" +
      "O que vai pra produção não tem Maven, não tem código-fonte, não tem cache de build — e roda como usuário não-root, com healthcheck.\n\n" +
      "Para o dia a dia, um docker compose up sobe API, Postgres e MailHog."
  );
  cabecalho(s, "A imagem carrega só o que roda", "Duas etapas: uma compila, a outra é a que vai pra produção.");
  codigo(
    s,
    "# etapa 1 — compila\nFROM maven:3.9-eclipse-temurin-21 AS builder\nRUN mvn package -DskipTests\n\n# etapa 2 — o que realmente vai pra produção\nFROM eclipse-temurin:21-jre-alpine\nRUN adduser -S spring\nCOPY --from=builder /app/target/*.jar app.jar\nUSER spring\nHEALTHCHECK CMD wget -qO- /actuator/health",
    M,
    2.95,
    6.6,
    2.75
  );
  const x2 = M + 7.0;
  const w2 = CW - 7.0;
  const itens = [
    { forte: "Sem Maven, sem fonte, sem cache.", resto: "A imagem final tem só o JRE e o JAR." },
    { forte: "Usuário não-root.", resto: "E um healthcheck declarado no próprio container." },
    { forte: "O dia a dia é separado.", resto: "docker compose up sobe API, Postgres e MailHog." },
  ];
  let y = 3.0;
  itens.forEach((it) => {
    s.addShape(pptx.ShapeType.rect, { x: x2, y: y + 0.15, w: 0.1, h: 0.1, fill: { color: PINE } });
    s.addText(
      [
        { text: it.forte, options: { bold: true, color: INK } },
        { text: "  " + it.resto, options: { color: MUTED } },
      ],
      { x: x2 + 0.28, y: y, w: w2 - 0.3, h: 0.85, fontFace: SANS, fontSize: 13, lineSpacing: 20, valign: "top" }
    );
    y += 0.95;
  });
  rodape(s, "Guilherme", "Container", "6:20 → 7:00", 6);
}

/* ─────────────────────────── 7 · Kubernetes ─────────────────────────── */
{
  const s = novoSlide(
    "Um namespace só: Postgres, MailHog e a API.\n\n" +
      "A API tem readiness e liveness separadas — a readiness segura o tráfego até o Spring subir, a liveness reinicia o pod travado.\n\n" +
      "Requests e limits de CPU e memória. E é por isso que o autoscaling funciona: sem requests, o HPA não tem denominador e não calcula porcentagem nenhuma.\n\n" +
      "HPA de 2 a 8 réplicas, mirando 70% de CPU e 80% de memória.\n\n" +
      "Os secret.yaml versionados têm só REPLACE_ME — os valores reais são renderizados pelo Terraform. Nunca há segredo commitado.\n\n" +
      "E o metrics-server precisou ser adicionado à mão: nem o kind nem o EKS o trazem, e sem ele o HPA lê <unknown> para sempre."
  );
  cabecalho(s, "O que faz o autoscaling funcionar", "Não é o HPA sozinho. São três peças, e faltar uma delas quebra tudo em silêncio.");
  cartoes(
    s,
    [
      {
        rotulo: "Réplicas",
        titulo: "De 2 a 8, pela carga",
        texto: "O HPA mira 70% de CPU e 80% de memória. Sobe quando aperta, desce quando alivia — com atraso proposital, para não oscilar.",
      },
      {
        rotulo: "A peça que todo mundo esquece",
        titulo: "Sem requests, não há conta",
        texto: "O HPA calcula uso sobre o que o pod pediu. Sem requests declarados, ele não tem denominador — e simplesmente não escala.",
        quente: true,
      },
      {
        rotulo: "Probes",
        titulo: "Uma segura, a outra cura",
        texto: "A readiness segura o tráfego até o Spring terminar de subir. A liveness reinicia o pod que travou.",
      },
    ],
    2.75,
    2.15
  );
  bullets(
    s,
    [
      {
        forte: "O metrics-server é responsabilidade nossa.",
        resto: "Nem o kind nem o EKS o trazem de fábrica. Sem ele, o HPA lê <unknown> para sempre e nunca escala.",
        alto: 0.62,
      },
      {
        forte: "Nenhum segredo commitado.",
        resto: "Os secret.yaml do repositório têm só REPLACE_ME. Os valores reais são renderizados pelo Terraform, no apply.",
        alto: 0.62,
      },
    ],
    5.15
  );
  rodape(s, "Guilherme", "Kubernetes", "7:00 → 8:10", 7);
}

/* ─────────────────────────── 8 · Terraform ─────────────────────────── */
{
  const s = novoSlide(
    "Dois cenários, os mesmos manifestos.\n\n" +
      "O local sobe um cluster kind, com Postgres dentro do cluster. Custo zero — é o que vou mostrar daqui a pouco.\n\n" +
      "O da AWS provisiona tudo do zero: VPC, subnets, Internet Gateway, IAM, o cluster EKS e um PostgreSQL gerenciado no RDS.\n\n" +
      "A diferença entre os dois é só onde está o banco e de onde vem a imagem. O resto é o mesmo YAML.\n\n" +
      "Escolhas de custo mínimo no cenário AWS: sem NAT Gateway, instâncias pequenas, single-AZ. É um ambiente efêmero — subir, gravar, destruir."
  );
  cabecalho(s, "Dois ambientes, os mesmos manifestos", "O YAML não sabe em que cluster está rodando. E isso é de propósito.");
  cartoes(
    s,
    [
      {
        rotulo: "environments/local",
        titulo: "Um cluster kind, custo zero",
        texto: "Um único apply cria o cluster, constrói a imagem, injeta no nó e sobe tudo. O Postgres roda dentro do cluster. É o que vou mostrar daqui a pouco.",
      },
      {
        rotulo: "environments/aws",
        titulo: "EKS e RDS, do zero",
        texto: "VPC, subnets, Internet Gateway, IAM, o cluster e um PostgreSQL gerenciado. Sem NAT Gateway, instâncias pequenas, single-AZ: um ambiente efêmero, por decisão de custo.",
      },
    ],
    3.05,
    2.2
  );
  bullets(
    s,
    [
      {
        forte: "A diferença é só o banco e a origem da imagem.",
        resto: "Namespace, probes, HPA, metrics-server — é exatamente o mesmo YAML nos dois clusters.",
      },
    ],
    5.6
  );
  rodape(s, "Guilherme", "Terraform", "8:10 → 9:00", 8);
}

/* ─────────────────────────── 9 · Bootstrap ─────────────────────────── */
{
  const s = novoSlide(
    "Uma armadilha clássica que vale a pena contar.\n\n" +
      "O state do Terraform mora num bucket S3, com lock no DynamoDB. A pergunta é: quem cria o bucket?\n\n" +
      "Se for o próprio Terraform, o primeiro apply — o que cria o bucket — ainda não tem bucket onde guardar o state. Ele ficaria em disco. E state em disco morre junto com o runner do CI: na execução seguinte, o Terraform esqueceria que o bucket existe e tentaria criá-lo de novo.\n\n" +
      "A causa é tratar um pré-requisito como se fosse um recurso. Então o bucket e a tabela são criados por um script de AWS CLI idempotente, que não tem state nenhum: pode rodar dez vezes, converge sempre no mesmo lugar. E aí ele roda tranquilo num runner efêmero.\n\n" +
      "O Terraform continua dono de tudo que o desafio pede. Só a prateleira onde ele guarda o próprio state é que fica de fora."
  );
  cabecalho(
    s,
    "Quem cria o bucket do state?",
    "O state mora no S3. Mas se o próprio Terraform criasse esse bucket, o primeiro apply não teria onde guardar o state do apply que cria o bucket."
  );
  cartoes(
    s,
    [
      {
        rotulo: "O sintoma",
        titulo: "State em disco morre com o runner",
        texto: "O runner do CI é efêmero. Na execução seguinte, o Terraform esqueceria que o bucket existe — e tentaria criá-lo de novo, e falharia.",
      },
      {
        rotulo: "A causa",
        titulo: "Um pré-requisito tratado como recurso",
        texto: "Backend não é aplicação. Bucket e tabela são criados por um script de AWS CLI idempotente: sem state, converge sempre no mesmo lugar, roda dez vezes sem susto.",
        quente: true,
      },
    ],
    3.2,
    2.1
  );
  fluxo(
    s,
    [
      { t: "bootstrap.sh", quente: true },
      { t: "S3 + DynamoDB" },
      { t: "terraform apply" },
    ],
    5.85
  );
  s.addText("O script constrói a prateleira. O Terraform guarda o state nela.", {
    x: M, y: 6.35, w: CW, h: 0.3,
    fontFace: SANS, fontSize: 12, color: MUTED, align: "center", italic: true,
  });
  rodape(s, "Guilherme", "Decisão", "9:00 → 9:50", 9);
}

/* ─────────────────────────── 10 · CI/CD ─────────────────────────── */
{
  const s = novoSlide(
    "A cada push: build, testes unitários e de integração com Testcontainers, cobertura no JaCoCo, OWASP Dependency-Check e o quality gate do SonarCloud. Passando, a imagem é construída, publicada no GHCR, e o deploy vai pro cluster.\n\n" +
      "Duas decisões que valem a menção.\n\n" +
      "Primeira: o Actions se autentica na AWS via OIDC. Não existe access key estática guardada em secret. O GitHub troca um token de curta duração por uma role que só este repositório consegue assumir.\n\n" +
      "Segunda: tudo que cobra dinheiro — o apply, o destroy — passa por um GitHub Environment com aprovação manual obrigatória. A pipeline para e espera um humano.\n\n" +
      "Depois de um único passo local — criar essa primeira credencial, que é impossível de automatizar, porque para criar a primeira credencial você já precisaria de uma — todo o resto é um clique no Actions."
  );
  cabecalho(s, "Do commit ao cluster", "Tudo automático, menos o que cobra dinheiro. Esse, alguém precisa aprovar.");
  fluxo(
    s,
    [
      { t: "push" },
      { t: "build + testes" },
      { t: "JaCoCo" },
      { t: "OWASP" },
      { t: "Sonar" },
      { t: "imagem" },
      { t: "aprovação humana", quente: true },
      { t: "deploy" },
    ],
    3.0
  );
  cartoes(
    s,
    [
      {
        rotulo: "Autenticação",
        titulo: "Nenhuma chave estática",
        texto: "Não há access key guardada em secret. O GitHub troca um token de curta duração por uma role que só este repositório consegue assumir — via OIDC.",
      },
      {
        rotulo: "O freio",
        titulo: "Tudo que cobra, para e espera",
        texto: "O apply e o destroy passam por um GitHub Environment com aprovação manual. Nenhum recurso pago sobe sem alguém dizer sim.",
        quente: true,
      },
    ],
    3.9,
    1.9
  );
  bullets(
    s,
    [
      {
        forte: "Um único passo roda na nossa máquina — e é irredutível.",
        resto: "Criar a primeira credencial não dá para automatizar: para criá-la, o pipeline já precisaria de uma. Feito isso, o ciclo inteiro é um clique no Actions.",
        alto: 0.62,
      },
    ],
    5.98
  );
  rodape(s, "Guilherme", "CI/CD", "9:50 → 11:00", 10);
}

/* ─────────────────────────── 11 · Demo 2 ─────────────────────────── */
{
  const s = novoSlide(
    "DEMO 2 — 2:30. O HPA é a estrela: reserve tempo pra ele.\n\n" +
      "Se o cluster já estiver criado, mostre o apply retornando sem mudanças e comente que a criação do zero leva uns 4 minutos, por causa do docker build.\n\n" +
      "Tela dividida: kubectl get hpa -w de um lado, k6 do outro.\n\n" +
      "Narre o HPA acordando: <unknown> → percentual real → passa de 70% → as réplicas sobem.\n\n" +
      "Feche assim: \"oito réplicas, o teto que definimos. Quando a carga cair, ele volta a duas sozinho — com um atraso proposital, pra não ficar batendo pra cima e pra baixo.\"\n\n" +
      "NÃO corte esta demo. Autoscaling ao vivo é requisito explícito do enunciado."
  );
  s.addShape(pptx.ShapeType.rect, { x: M, y: 0.62, w: 1.55, h: 0.32, fill: { color: OCHRE } });
  s.addText("DEMO 2", {
    x: M, y: 0.62, w: 1.55, h: 0.32,
    fontFace: SANS, fontSize: 11, bold: true, color: PAPER, align: "center", valign: "middle", charSpacing: 2,
  });
  s.addText("O HPA reagindo à carga, ao vivo", {
    x: M, y: 1.1, w: CW, h: 0.75,
    fontFace: SERIF, fontSize: 32, bold: true, color: INK, valign: "middle",
  });
  passos(
    s,
    [
      "terraform apply — cria o cluster, constrói a imagem, injeta no nó e aplica os manifestos.",
      "Listar os pods: API, Postgres e MailHog de pé. Duas réplicas da API.",
      "Port-forward e um curl no health. É a mesma API da primeira demo, agora num cluster.",
      "Tela dividida: o HPA de um lado, o k6 do outro.",
      "Subir a carga e narrar o HPA acordando — de <unknown> ao percentual real, passando de 70%, e as réplicas subindo: 2, 4, 6, 8.",
      "Oito réplicas, o teto que definimos. Quando a carga cai, ele volta a duas sozinho.",
    ],
    2.3
  );
  rodape(s, "Guilherme", "Ao vivo", "11:00 → 13:30", 11);
}

/* ─────────────────────────── 12 · Qualidade ─────────────────────────── */
{
  const s = novoSlide(
    "GUILHERME: Na esteira, a cada push: quality gate do SonarCloud, cobertura pelo JaCoCo e OWASP Dependency-Check. Quatro CVEs seguem suprimidas com justificativa — são de Tomcat e Spring Security, e ainda não existe patch publicado pelos mantenedores. Estão documentadas, com data, e reavaliadas a cada fase.\n\n" +
      "DANILO: E três débitos da Fase 1 foram quitados: o MapStruct que estava declarado e nunca foi usado saiu do projeto; o construtor de nove parâmetros virou um record; e o stub de notificação virou SMTP de verdade."
  );
  cabecalho(s, "O que a esteira cobra da gente", "E o que a gente devia da fase passada.");
  cartoes(
    s,
    [
      {
        rotulo: "A cada push",
        titulo: "Sonar, JaCoCo e OWASP",
        texto:
          "Quality gate, cobertura e varredura de dependências. Quatro CVEs seguem suprimidas — com justificativa: são de Tomcat e Spring Security, e ainda não existe patch publicado. Documentadas, datadas, reavaliadas a cada fase.",
      },
      {
        rotulo: "Débitos da Fase 1",
        titulo: "Três quitados",
        texto:
          "O MapStruct estava declarado no pom e nunca tinha sido usado — saiu. O construtor de nove parâmetros virou um record. E o stub de notificação virou SMTP de verdade.",
      },
    ],
    3.1,
    2.4
  );
  rodape(s, "Danilo e Guilherme", "Qualidade", "13:30 → 14:10", 12);
}

/* ─────────────────────────── 13 · Fechamento ─────────────────────────── */
{
  const s = novoSlide(
    "Regras novas sem tocar em infraestrutura, porque a arquitetura aguentou. Infraestrutura declarada, versionada e destruível com um clique. E a esteira inteira, do commit ao cluster.\n\n" +
      "Obrigado."
  );
  s.addShape(pptx.ShapeType.rect, { x: M, y: 1.9, w: 1.6, h: 0.06, fill: { color: OCHRE } });
  s.addText("Obrigado", {
    x: M, y: 2.15, w: CW, h: 1.0,
    fontFace: SERIF, fontSize: 44, bold: true, color: INK, valign: "middle",
  });
  const fechos = [
    { forte: "Regras novas sem tocar em infraestrutura.", resto: "A arquitetura aguentou." },
    { forte: "Infraestrutura declarada e versionada.", resto: "E destruível com um clique." },
    { forte: "A esteira inteira.", resto: "Do commit ao cluster." },
  ];
  let y = 3.35;
  fechos.forEach((it) => {
    s.addShape(pptx.ShapeType.rect, { x: M, y: y + 0.14, w: 0.1, h: 0.1, fill: { color: PINE } });
    s.addText(
      [
        { text: it.forte, options: { bold: true, color: INK } },
        { text: "  " + it.resto, options: { color: MUTED } },
      ],
      { x: M + 0.28, y: y, w: CW - 0.3, h: 0.4, fontFace: SANS, fontSize: 14, valign: "top" }
    );
    y += 0.5;
  });
  s.addText("Danilo Canato          Guilherme Fumagali", {
    x: M, y: 5.5, w: CW, h: 0.35,
    fontFace: SANS, fontSize: 13, bold: true, color: INK,
  });
}

const destino = process.argv[2];
pptx.writeFile({ fileName: destino }).then(() => {
  console.log("OK →", destino);
});
