# Gerador de Times (API)

API REST para gerenciar usuarios, clubes, partidas e gerar times balanceados para jogos de futebol. Inclui autenticacao JWT, controle de acesso por papel (ADMIN/DIRECTOR/MEMBER), verificacao de email, recuperacao de senha e documentacao OpenAPI.

## Principais recursos
- Cadastro email-first com verificacao OTP e token de registro JWT.
- Autenticacao com JWT (access/refresh) via cookies HttpOnly.
- Recuperacao de senha com token por email.
- Clubes, membros e camisas por clube.
- ClubRole (DIRECTOR/MEMBER) para controle de acesso por clube.
- Partidas individuais ou em lote, com resultado (campeao/MVP).
- Geracao automatica de times balanceados por score.
- Troca de jogadores entre times apos geracao.
- RabbitMQ para eventos de email (verificacao, reset de senha).
- Swagger UI integrado.

## Tecnologias
- Java 21
- Spring Boot 4
- Spring MVC, Data JPA, Validation, Security
- PostgreSQL + Flyway
- JWT (access/refresh) + cookies HttpOnly
- Argon2 + pepper para hashing de senhas
- OpenAPI (springdoc)
- RabbitMQ (eventos de email)
- Spring Mail
- Testcontainers (testes de integracao)

## Requisitos
- Java 21
- Maven (ou `./mvnw`)
- PostgreSQL
- RabbitMQ (necessario para envio de emails)
- Docker (para executar os testes)

## Configuracao

Os ambientes ficam separados por perfil Spring:
- `dev` para desenvolvimento local
- `test` para testes automatizados
- `prod` para producao

Ativacao:
- Desenvolvimento: `SPRING_PROFILES_ACTIVE=dev`
- Testes: `SPRING_PROFILES_ACTIVE=test`
- Producao: `SPRING_PROFILES_ACTIVE=prod`

O perfil `dev` aceita defaults locais para facilitar execucao. O perfil `prod` exige todas as variaveis de ambiente.

## Variaveis de ambiente

### Obrigatorias (prod, sem default):
```
JDBC_URL (ou PG_HOST + PG_PORT + PG_DATABASE)
POSTGRES_USER
POSTGRES_PASSWORD
ARGON_PEPPER
JWT_SECRET
JWT_REFRESH_SALT
AUTH_COOKIE_REFRESH_NAME
AUTH_COOKIE_PATH
AUTH_COOKIE_SECURE
AUTH_COOKIE_SAMESITE
RABBITMQ_HOST
RABBITMQ_USERNAME
RABBITMQ_PASSWORD
```

### Recomendadas (tem default inseguro para dev):
```
JWT_ISSUER              # default: geradortimes
JWT_ACCESS_TTL          # default: 15m
JWT_REFRESH_TTL         # default: 30d
JWT_REGISTRATION_TTL    # default: 30m
```

### Opcionais:
```
PG_PORT                 # default: 5432
APP_CORS_ALLOWED_ORIGINS  # CSV de origins permitidos (default: vazio)
RABBITMQ_PORT           # default: 5672
RABBITMQ_VIRTUAL_HOST
```

## Rodando localmente

```bash
# 1. Subir PostgreSQL e RabbitMQ (ex.: Docker)
docker compose up -d

# 2. Exportar variaveis minimas
export PG_DATABASE=ferino
export POSTGRES_USER=ferino
export POSTGRES_PASSWORD=ferino
export ARGON_PEPPER=dev-pepper-local
export AUTH_COOKIE_REFRESH_NAME=refreshToken
export AUTH_COOKIE_PATH=/api/auth
export AUTH_COOKIE_SECURE=false
export AUTH_COOKIE_SAMESITE=Lax

# 3. Rodar
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Testes

Prerequisito: Docker ativo (Testcontainers sobe PostgreSQL automaticamente).

```bash
# Suite completa
./mvnw clean test

# Uma classe especifica
./mvnw -Dtest=UserControllerTest test

# Um metodo especifico
./mvnw -Dtest=UserControllerTest#getByIdSelf test
```

## Docker

Build da imagem:
```bash
docker build -t geradortimes-api .
```

Execucao com variaveis de ambiente:
```bash
docker run --env-file .env -p 8080:8080 geradortimes-api
```

A imagem roda por padrao com `SPRING_PROFILES_ACTIVE=prod`. Use `.env` como referencia para deploy.

## Visao geral da API

Prefixo base: `/api`. Rotas marcadas como `Auth` exigem `Authorization: Bearer <access_token>`.

### Auth (publico)
| Metodo | Rota | Descricao |
|--------|------|-----------|
| POST | `/api/auth/login` | Login; retorna access token + seta cookie refresh |
| POST | `/api/auth/refresh` | Renova access token via cookie refresh |
| POST | `/api/auth/logout` | Invalida sessao |

### Users
| Metodo | Rota | Acesso |
|--------|------|--------|
| POST | `/api/users/email` | Publico — envia OTP para verificacao de email |
| POST | `/api/users/verify-email` | Publico — valida OTP, retorna token de registro JWT |
| POST | `/api/users` | Publico — cria conta com token de registro (email-first) |
| POST | `/api/users/forgot-password` | Publico — envia token de recuperacao por email |
| POST | `/api/users/reset-password` | Publico — redefine senha com token |
| GET | `/api/users` | ADMIN — lista usuarios (paginado) |
| GET | `/api/users/{id}` | Auth — busca usuario por ID |
| DELETE | `/api/users/{id}` | Auth — deleta usuario |

### Clubs
| Metodo | Rota | Acesso |
|--------|------|--------|
| POST | `/api/clubs` | Auth — cria clube (criador vira DIRECTOR) |
| GET | `/api/clubs?clubRole=DIRECTOR\|MEMBER` | Auth — lista clubes do usuario por papel |
| GET | `/api/clubs/{id}` | Auth |
| GET | `/api/clubs/nickname/{nickname}` | Auth |
| PATCH | `/api/clubs/{id}` | DIRECTOR |
| DELETE | `/api/clubs/{id}` | DIRECTOR — soft delete |

Membros e camisas (subordinados ao clube):
| Metodo | Rota | Acesso |
|--------|------|--------|
| POST | `/api/clubs/{clubId}/members` | DIRECTOR |
| GET | `/api/clubs/{clubId}/members` | Auth |
| GET | `/api/clubs/{clubId}/members/{memberId}` | Auth |
| POST | `/api/clubs/{clubId}/jerseys` | DIRECTOR |
| GET | `/api/clubs/{clubId}/jerseys` | Auth |
| DELETE | `/api/clubs/{clubId}/jerseys/{jerseyId}` | DIRECTOR |

### Matches
| Metodo | Rota | Acesso |
|--------|------|--------|
| POST | `/api/matches` | DIRECTOR |
| POST | `/api/matches/batch` | DIRECTOR — criacao em lote recorrente |
| GET | `/api/matches?clubId=...` | Auth (MEMBER) |
| GET | `/api/matches/upcoming?clubId=...` | Auth (MEMBER) — partidas futuras |
| GET | `/api/matches/{id}` | Auth |
| GET | `/api/matches/{id}/participants` | Auth |
| PATCH | `/api/matches/{id}/result` | DIRECTOR — define campeao/MVP |
| DELETE | `/api/matches/{id}` | DIRECTOR |

### Teams
| Metodo | Rota | Acesso |
|--------|------|--------|
| POST | `/api/teams` | DIRECTOR — criacao manual |
| POST | `/api/teams/generate` | DIRECTOR — geracao automatica balanceada |
| POST | `/api/teams/swap` | DIRECTOR — troca jogadores entre times |
| GET | `/api/teams?matchId=...` | Auth |
| GET | `/api/teams/{id}` | Auth |
| PUT | `/api/teams/{id}` | DIRECTOR |
| PATCH | `/api/teams/{id}/jersey` | DIRECTOR |
| DELETE | `/api/teams/{id}` | DIRECTOR |

## Fluxo de cadastro (email-first)

1. `POST /api/users/email` — usuario informa email, recebe OTP de 6 digitos (validade 5 min).
2. `POST /api/users/verify-email` — usuario informa email + OTP, recebe JWT de registro (validade 30 min). O OTP e consumido nesta etapa.
3. `POST /api/users` — usuario envia nome, nickname, senha + registrationToken. Email e extraido do JWT.

Detalhes completos em [docs/email-first-registration-flow.md](docs/email-first-registration-flow.md).

## Geracao de times

O algoritmo de balanceamento usa snake draft baseado em score (rating + historico de campeao/MVP). Goleiros sao distribuidos apenas quando a quantidade coincide com o numero de times. A operacao e destrutiva: limpa times/participantes anteriores da partida antes de gerar os novos.

Detalhes completos em [docs/team-generation.md](docs/team-generation.md).

## Migracoes de banco

Gerenciadas pelo Flyway. Scripts em `src/main/resources/db/migration/` com padrao `V<versao>__<descricao>.sql`. Nunca edite scripts ja aplicados em producao. A aplicacao aplica as migracoes automaticamente ao iniciar.

## Observacoes
- Rotas protegidas exigem `Authorization: Bearer <token>`.
- CORS liberado para `http://localhost:4200` (padrao dev). Ajuste via `APP_CORS_ALLOWED_ORIGINS` ou `SecurityConfig`.
- `POST /api/users/email` tem rate limit de 3 req/min por IP.
- `JWT_SECRET` e `JWT_REFRESH_SALT` com defaults inseguros para dev — sempre sobrescreva em producao.
