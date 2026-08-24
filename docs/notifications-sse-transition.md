# Notificações — Migração de polling para SSE (virtual threads)

Este documento é um guia para agentes de IA (e desenvolvedores) que precisem
migrar o sistema de notificações de **polling** (estado atual) para **SSE
(Server-Sent Events)** com **virtual threads**. Explica o estado atual, o
objetivo, o passo a passo e as armadilhas.

> Estado atual: o frontend consome notificações via polling do endpoint
> `GET /api/notifications`. A entrega é *pull*: o cliente repete a requisição
> em intervalos fixos. Este documento descreve como evoluir para *push*.

---

## 1. Estado atual (polling)

- **Backend**: `NotificationService` (`com.bernardo.geradortimes.notification`),
  `NotificationController` (`GET /api/notifications`, `PATCH /api/notifications/{id}/read`),
  entidade `Notification` e `NotificationRepository`.
- **Produção de eventos**: `ClubMembershipService` chama `notificationService.create(...)`
  quando cria/decide uma solicitação de ingresso.
- **RabbitMQ**: `ClubMembershipRequestedProducer` publica `ClubMembershipRequestedEvent`
  na exchange `club.events` (routing-key `club.membership-requested`).
- **Frontend (Angular)**: um `notification.service.ts` faz `list(unread)` em loop
  (30–60s), tipicamente só enquanto a tela está aberta.

### Por que polling foi escolhido primeiro

Simples, sem infra nova, CPU desprezível com poucos usuários concorrentes.
A fronteira de mudança fica isolada em `NotificationService` + `notification.service.ts`,
então a troca por SSE não afeta o restante do fluxo.

---

## 2. Por que migrar para SSE

- **Menos CPU/requests**: polling gera N requisições por intervalo por usuário,
  cada uma com validação de JWT + query. SSE mantém uma única conexão por usuário
  e só consulta o banco quando há evento.
- **Tempo real**: a notificação aparece sem esperar o próximo ciclo de polling.
- **Sem Redis**: com 1 instância (free tier), o fan-out é feito pelo RabbitMQ já
  existente → um consumidor empurra o evento para o `SseEmitter` do usuário correto.

---

## 3. Pré-requisitos

1. **Virtual threads**: habilitar em `application.properties` (Spring Boot 4 + Java 21):

   ```properties
   spring.threads.virtual.enabled=true
   ```

   Isso faz o Tomcat processar requests em virtual threads, então cada conexão SSE
   ocupa um virtual thread barato (KBs) em vez de um thread de SO.

2. **1 instância** (requisito atual): o registro de emitters é em memória
   (`ConcurrentHashMap`). Se escalar para >1 instância, o push precisa de um
   barramento externo (ver seção 7).

3. **Evitar `synchronized`** no caminho do SSE: block `synchronized` "pina" o
   virtual thread ao carrier thread e anula o benefício. Usar `ReentrantLock` ou
   estruturas sem bloqueio.

---

## 4. Backend — passo a passo

### 4.1 `SseEmitterRegistry`

Novo componente que guarda um `SseEmitter` por usuário:

```java
@Component
public class SseEmitterRegistry {

    private static final long TIMEOUT_MS = 60_000L;

    private final Map<UUID, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(UUID userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.put(userId, emitter);
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));
        return emitter;
    }

    public void push(UUID userId, NotificationResponseDTO notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
        } catch (Exception e) {
            emitters.remove(userId, emitter);
        }
    }
}
```

### 4.2 Consumidor RabbitMQ (fan-out sem Redis)

```java
@Component
public class NotificationEventConsumer {

    private final NotificationService notificationService;
    private final SseEmitterRegistry registry;

    // @RabbitListener(queues = "${app.rabbitmq.notification.queue}")
    public void onMembershipRequested(ClubMembershipRequestedEvent event) {
        // 1. persistir a Notification (reaproveita NotificationService.create)
        // 2. resolver o userId destinatário (event.directorUserId)
        // 3. registry.push(userId, dto)
    }
}
```

> Alternativa sem novo consumer: chamar `registry.push(...)` diretamente dentro de
> `NotificationService.create` — mais simples, porém acopla a persistência ao push.
> Para desacoplar, prefira o consumidor na fila.

### 4.3 Endpoint de stream

```java
@GetMapping(value = "/api/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter stream() {
    UUID userId = currentUserService.requireUserId();
    return registry.register(userId);
}
```

### 4.4 Heartbeat

Enviar um comentário periódico para manter a conexão viva e detectar clientes mortos:

```java
@Scheduled(fixedRate = 25_000)
public void heartbeat() {
    emitters.forEach((userId, emitter) -> {
        try {
            emitter.send(SseEmitter.event().comment("keep-alive"));
        } catch (Exception e) {
            emitters.remove(userId, emitter);
        }
    });
}
```

---

## 5. Frontend — passo a passo

Substituir o polling por `EventSource` com reconexão automática:

```typescript
// notification-stream.service.ts
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class NotificationStreamService {
  connect(onNotification: (n: NotificationDTO) => void): () => void {
    const es = new EventSource('/api/notifications/stream', { withCredentials: true });

    es.addEventListener('notification', (event: MessageEvent) => {
      onNotification(JSON.parse(event.data));
    });

    es.onerror = () => {
      // EventSource tenta reconectar sozinho; aqui só loga/controla backoff.
    };

    return () => es.close();
  }
}
```

Considerações:

- `withCredentials: true` para enviar o refresh cookie (se usar cookie de sessão);
  caso contrário, o `EventSource` nativo **não** envia o header `Authorization` —
  use um token em query param (menos seguro) ou um interceptor/`fetch`-based SSE.
- Manter `markRead` via `PATCH /api/notifications/{id}/read` como hoje.
- Usar `switchMap`/`takeUntilDestroyed` para abrir/fechar a conexão com o ciclo
  de vida do componente.

---

## 6. Armadilhas conhecidas

- **Proxy/nginx com buffer**: `text/event-stream` precisa de `proxy_buffering off`
  e `proxy_read_timeout` alto, senão a conexão é fechada/travada.
- **Reconexão**: `EventSource` reconecta automaticamente, mas sem backoff pode
  causar "thundering herd" após queda geral. Adicionar backoff exponencial.
- **Pinning de virtual threads**: qualquer `synchronized` no fluxo do push
  (`registry.push`, `create`, serialização) neutraliza o ganho.
- **Escala horizontal**: `ConcurrentHashMap` local não funciona com 2+ instâncias.
  Nesse caso, usar Redis pub/sub ou o próprio RabbitMQ para rotear o evento à
  instância que segura a conexão.
- **Auth no SSE**: o `EventSource` nativo não manda `Authorization`. Planejar a
  autenticação (cookie vs token) antes de implementar.

---

## 7. Checklist de aceite

- [ ] Notificação aparece na tela sem refresh manual (sem polling).
- [ ] Conexão reconecta após queda/restart do backend.
- [ ] Sem vazamento de emitter (timeout/error removem do registry).
- [ ] `spring.threads.virtual.enabled=true` ativo e sem `synchronized` no fluxo.
- [ ] `markRead` continua funcionando e reflete o estado no stream.
- [ ] Testes de integração cobrem o stream (ou ao menos o `NotificationService`).

---

## 8. Rollback

Se algo quebrar, basta voltar o frontend a usar `GET /api/notifications` (polling)
e deixar o endpoint `/stream` desligado — o backend de notificações (tabela +
RabbitMQ) permanece o mesmo.
