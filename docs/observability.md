# Observabilidade — Guia para leigos

Este documento explica, de forma simples, como a observabilidade do sistema funciona. O objetivo é que você entenda **o que** cada peça faz, **por que** ela existe e **como** usar.

---

## 1. O que é observabilidade?

Observabilidade é a capacidade de **entender o que está acontecendo dentro do sistema em produção** — sem precisar entrar no servidor e ler logs manualmente.

Quando um usuário reclama de algo, a observabilidade responde perguntas como:

- Essa requisição funcionou? Onde ela travou?
- O email de verificação foi enviado?
- A aplicação está lenta? Desde quando?
- A memória está acabando?
- Um serviço caiu de madrugada?

---

## 2. Os três pilares

Toda a observabilidade gira em torno de **três tipos de dados**:

| Pilar | O que é | Analogia | Exemplo |
|-------|---------|----------|---------|
| **Logs** | Registros de eventos que a aplicação escreve | Diário de bordo | `INFO - Email de confirmacao enviado com sucesso` |
| **Métricas** | Números medidos ao longo do tempo | Painel do carro (velocímetro, temperatura) | Memória usada, requisições por segundo, latência |
| **Traces** | O caminho completo de uma requisição pelo sistema | Rastreamento de encomenda dos Correios | Requisição de login passou por: NGINX → API → Banco → RabbitMQ → email-service |

**Nenhum pilar sozinho conta a história toda.** Juntos, eles mostram o quadro completo.

---

## 3. Arquitetura

```
                        ┌──────────────────────────────┐
  Frontend (Vercel) ───►│          NGINX (:443)         │
  geniofc.com.br        └──────────────┬───────────────┘
                                       │
                        ┌──────────────▼───────────────┐
                        │      gerador-times (API)      │  ── instrumentada com OTel
                        │      porta 8080 (interna)     │
                        └──────────┬───────────┬───────┘
                                   │           │
                          RabbitMQ │           │ JDBC (PostgreSQL em outra VM)
                          (CloudAMQP)          │
                                   │           │
                        ┌──────────▼───────────┴───────┐
                        │      email-service           │  ── instrumentada com OTel
                        │      (consome filas)         │
                        └──────────┬──────────────────┘
                                   │ SMTP
                            Brevo (envia emails)

 ══════════════════════ Observabilidade (tudo na VM) ══════════════════════

  apps ──(OTLP HTTP)──► otel-collector ──(gRPC)──► tempo     → traces
                           │            ──► prometheus      → métricas
                           │            ──► loki            → logs (via OTLP)
                           │
  promtail (lê logs dos  ──┴──► loki                          → logs (via arquivos)
  containers Docker)

  prometheus ──► alertmanager  (dispara alertas)
  grafana ──► consulta tempo + loki + prometheus (visualização única)
```

### Resumo em uma frase

**As aplicações "contam" o que fazem (OTel) → o coletor recebe e distribui → os bancos de dados especializados guardam (Tempo/Loki/Prometheus) → o Grafana mostra tudo em um lugar só.**

---

## 4. O que cada peça faz

### 4.1 OpenTelemetry (OTel) — o "contador de histórias"

- É um **padrão aberto** para instrumentação (coleta de telemetria).
- No nosso caso, usamos o **OTel Java Agent**: um arquivo `.jar` que é carregado junto com a aplicação (`-javaagent:opentelemetry-javaagent.jar`) e **instrumenta automaticamente** tudo (HTTP, banco, RabbitMQ, JVM) — **sem precisar escrever código manual**.
- O agent gera os 3 pilares: traces, métricas e logs.

**Onde vive:** dentro do container de cada aplicação (`gerador-times` e `email-service`).

### 4.2 OTel Collector — o "porteiro"

- Recebe a telemetria das aplicações (protocolo OTLP, portas 4317/4318).
- **Distribui** cada tipo de dado para o banco certo:
  - traces → Tempo
  - métricas → Prometheus (via endpoint que o Prometheus coleta)
  - logs → Loki
- Pode filtrar, transformar e enriquecer dados no meio do caminho (processors).

**Onde vive:** container `otel-collector` na VM.

### 4.3 Tempo — o "banco de traces"

- Armazena traces (o caminho completo de cada requisição) de forma eficiente.
- Um trace é uma **árvore de spans**: cada span é um passo (chamada HTTP, query SQL, envio de mensagem).
- Retenção: 7 dias.

**Onde vive:** container `tempo`.

### 4.4 Loki — o "banco de logs"

- Armazena logs de forma econômica (só indexa labels, não o texto todo).
- Recebe logs por dois caminhos:
  1. **Promtail** lê os logs dos containers Docker e envia para o Loki.
  2. O OTel Collector pode enviar logs via OTLP (ainda não ligado nos apps).
- Retenção: 7 dias.

**Onde vive:** containers `loki` e `promtail`.

### 4.5 Prometheus — o "banco de métricas"

- **Coleta (scrape)** métricas de tempos em tempos (a cada 15s) de um endpoint HTTP.
- No nosso caso, coleta do OTel Collector (`otel-collector:8889`), que expõe as métricas das aplicações.
- **Avalia regras de alerta** (ex: "5xx acima de 5% por 5 minutos") e, quando disparam, envia para o Alertmanager.
- PromQL é a linguagem de consulta (ex: `jvm_memory_used_bytes`).

**Onde vive:** container `prometheus`.

### 4.6 Alertmanager — o "sino de alarme"

- Recebe alertas do Prometheus e gerencia o ciclo de vida deles:
  - **Agrupamento**: junta alertas parecidos (evita 10 notificações para o mesmo problema).
  - **Silêncios**: permite silenciar alertas temporariamente.
  - **Notificações**: envia para canais (Slack, email, Telegram, webhook).
- Hoje está **sem canal de notificação externo** — os alertas ficam visíveis na UI do Alertmanager. Adicionar um canal é só colocar a config no `alertmanager-config.yml`.

**Onde vive:** container `alertmanager`.

### 4.7 Grafana — o "painel de controle"

- A **única interface** que você usa no dia a dia.
- Conecta-se aos 3 bancos (Tempo, Loki, Prometheus) e permite:
  - Explorar traces (Explore → Tempo)
  - Buscar logs (Explore → Loki)
  - Consultar métricas (Explore → Prometheus)
  - Ver dashboards prontos (JVM, HTTP, Database)
  - **Correlacionar**: abrir um trace e pular para os logs daquele `trace_id`.

**Onde vive:** container `grafana`.

---

## 5. A jornada de uma requisição

Exemplo: usuário cadastra o email (fluxo que gera tudo).

### 5.1 Como o trace é gerado

```
Usuário → geniofc.com.br → NGINX → gerador-times (span: HTTP POST /api/users/email)
                                          │
                                          ├─► PostgreSQL (span: INSERT/SELECT)
                                          │
                                          └─► RabbitMQ (span: publish user.email-verification)
                                                        │
                                                        ▼ (trace context via headers)
                                              email-service (span: process message)
                                                        │
                                                        └─► Brevo SMTP (span: send email)
```

- Cada passo é um **span**. Todos os spans juntos formam o **trace**.
- O agent OTel **propaga o trace_id automaticamente**: quando o `gerador-times` publica no RabbitMQ, o `trace_id` vai junto no cabeçalho da mensagem. O `email-service` lê o cabeçalho e continua o **mesmo trace**.
- Resultado: um único `trace_id` aparece nos dois serviços (verificamos isso em produção: `b053a005e1836fa19983df02efb2ba33`).

### 5.2 Como o log se conecta ao trace

- O agent OTel injeta `trace_id` e `span_id` no contexto de log (MDC do SLF4J).
- Nós configuramos o padrão de log para exibi-los:

```
2026-08-12 23:36:20.273 INFO [trace_id=b053a005...] [span_id=d76f0ac0...] EmailService - Email enviado com sucesso
```

- No Grafana, abrindo um trace, o botão **"Logs for this span"** busca no Loki todas as linhas com aquele `trace_id`.

### 5.3 Como as métricas são geradas

- O agent OTel conta/mede tudo em segundo plano: memória JVM, duração de cada requisição HTTP (histograma), conexões do banco etc.
- A cada 60 segundos, exporta essas métricas para o Collector.
- O Collector as expõe na porta 8889, e o Prometheus coleta a cada 15 segundos.
- Os dashboards do Grafana consultam o Prometheus (PromQL).

---

## 6. Como acessar

Tudo roda na VM e **nada de observabilidade é exposto à internet**. O acesso é por **túnel SSH** (porta local → localhost da VM).

### Túnel completo (Grafana + Alertmanager + Prometheus)

```bash
ssh -i ~/.oracle/geradortimes-server/ssh-key-2026-08-07.key \
  -L 3001:localhost:3000 \
  -L 9093:localhost:9093 \
  -L 9090:localhost:9090 \
  ubuntu@147.15.72.190
```

> A porta local `3001` é usada porque a `3000` costuma estar ocupada (havia um Grafana local na sua máquina).

| Interface | URL | Para quê |
|-----------|-----|----------|
| **Grafana** | `http://localhost:3001` | Tudo: dashboards, traces, logs, métricas |
| **Alertmanager** | `http://localhost:9093` | Alertas disparando, silêncios |
| **Prometheus** | `http://localhost:9090/alerts` | Estado das regras de alerta |

---

## 7. O que ver no Grafana (roteiro rápido)

### 7.1 Dashboards (o dia a dia)

**Dashboards →** escolha um:

- **JVM Overview** — memória heap, threads, GC, CPU de cada serviço
- **HTTP Overview** — requisições por segundo, latência p95, erros 5xx
- **Database Overview** — pool de conexões do PostgreSQL

### 7.2 Traces (investigar uma requisição)

1. **Explore** → seletor de data source → `Tempo`
2. Tipo de query: **Search**
3. `service.name = gerador-times` → **Run query**
4. Clique num trace → veja o diagrama de spans (cada passo com sua duração)
5. Clique num span → **Logs for this span** → logs correlacionados daquele trace

### 7.3 Logs (buscar eventos)

1. **Explore** → `Loki`
2. Query: `{container="gerador-times-app"} |= "erro"` (filtrar linhas contendo "erro")
3. Ou simplesmente: `{container="email-service"}` para ver tudo do email-service

### 7.4 Métricas (consultas livres)

1. **Explore** → `Prometheus`
2. Exemplos:
   - `jvm_memory_used_bytes` — memória usada
   - `rate(http_server_request_duration_seconds_count[5m])` — requisições por segundo
   - `db_client_connections_pending_requests` — fila do pool de banco

---

## 8. Os alertas

Definidos em `deploy/observability/prometheus-alert-rules.yml`:

| Alerta | Dispara quando | Severidade |
|--------|----------------|-----------|
| `ServiceDown` | Um serviço para de exportar métricas (caiu) por 2 min | critical |
| `HighErrorRate` | Respostas 5xx > 5% das requisições por 5 min | warning |
| `HighLatency` | Latência p95 > 2 segundos por 5 min | warning |
| `DBConnectionPoolExhausted` | Requisições aguardando conexão no banco por 5 min | warning |
| `HighHeapUsage` | Heap da JVM > 95% por 10 min | warning |

**Para adicionar notificações** (Slack/email/Telegram): edite `deploy/observability/alertmanager-config.yml` e adicione um `receiver` (há um exemplo comentado no arquivo).

---

## 9. Arquivos importantes

| Arquivo | O que configura |
|---------|-----------------|
| `deploy/observability/otel-collector-config.yaml` | Recebe OTLP e roteia para Tempo/Prometheus/Loki |
| `deploy/observability/tempo-config.yaml` | Banco de traces (retenção 7 dias) |
| `deploy/observability/loki-config.yaml` | Banco de logs (retenção 7 dias) |
| `deploy/observability/prometheus-config.yaml` | Scrape do collector + regras de alerta |
| `deploy/observability/prometheus-alert-rules.yml` | As 6 regras de alerta |
| `deploy/observability/alertmanager-config.yml` | Roteamento/notificações de alertas |
| `deploy/observability/promtail-config.yaml` | Coleta logs dos containers Docker |
| `deploy/observability/grafana/datasources.yml` | Conexões do Grafana (Tempo/Loki/Prometheus + correlação) |
| `deploy/observability/grafana/dashboards/*.json` | Os 3 dashboards |
| `Dockerfile` (apps) | Baixa e carrega o OTel Java Agent |
| `deploy/docker-compose.yml` | Orquestra todos os containers |

---

## 10. Troubleshooting rápido

```bash
# Ver se todos os containers estão de pé
docker ps

# Logs de cada peça
docker logs otel-collector
docker logs tempo
docker logs loki
docker logs prometheus
docker logs alertmanager
docker logs grafana

# Aplicar mudanças nos arquivos de config e recriar
DOCKER_USERNAME=bernardo16 docker compose -f /home/ubuntu/docker-compose.yml up -d
```

**Armadilhas conhecidas (já mordemos):**

- **Bind mount obsoleto**: quando o CI reescreve um arquivo montado (ex: `nginx.conf`), o container pode continuar vendo a versão antiga. Solução: `docker compose up -d --force-recreate <serviço>`.
- **`$$` no provisionamento do Grafana**: o Grafana interpreta `${...}` como variável de ambiente ao provisionar. Para usar `${__trace.traceId}` literal, escreva `$${__trace.traceId}` no YAML.
- **Label `job` sobrescrito**: o Prometheus sobrescreve o label `job` do scrape — `honor_labels: true` preserva o `job` original de cada serviço.
- **Métricas de messaging ausentes**: o OTel agent não emite métricas de RabbitMQ (publish/process) com Spring AMQP 4.0. Os traces funcionam; as métricas não. Por isso o dashboard de banco substituiu o de RabbitMQ.

---

## 11. Glossário

| Termo | Significado simples |
|-------|---------------------|
| **OTel / OpenTelemetry** | Padrão de instrumentação que gera traces, métricas e logs |
| **OTLP** | Protocolo de transporte da telemetria (HTTP ou gRPC) |
| **Trace** | O caminho completo de uma requisição pelo sistema |
| **Span** | Um passo dentro de um trace (ex: uma query SQL) |
| **trace_id / span_id** | Identificadores que ligam logs ↔ traces ↔ serviços |
| **PromQL** | Linguagem de consulta do Prometheus |
| **LogQL** | Linguagem de consulta do Loki |
| **Histograma** | Métrica que agrupa medidas em faixas (permite calcular p95) |
| **p95** | 95% das requisições são mais rápidas que esse valor |
| **Scrape** | A coleta periódica que o Prometheus faz de um endpoint |
| **MDC** | Contexto de log do Java onde o agent injeta trace_id/span_id |
| **Retenção** | Por quanto tempo os dados ficam guardados (7 dias aqui) |

---

## 12. Evoluções futuras (quando quiser)

- **Notificações de alerta** (Slack/Telegram/email) no Alertmanager
- **Logs via OTLP** (os apps enviam logs direto ao Collector — ativar `OTEL_LOGS_EXPORTER=otlp`)
- **Grafana Alloy** no lugar de Promtail + Collector (consolidação futura)
- **Dashboards de negócio** (ex: emails enviados por dia, cadastros por dia)
- **Retenção maior** (ajustar `block_retention` no Tempo e `retention_period` no Loki)
