# Gerador de Times (API)

API REST para gerenciar usuarios, clubes, partidas e gerar times balanceados para jogos de futebol. Inclui autenticacao JWT, controle de acesso por papel (DIRECTOR/MEMBER) e documentacao OpenAPI.

## Principais recursos
- Cadastro e autenticacao de usuarios.
- Clubes, membros e camisas por clube.
- Partidas e participantes.
- Geracao automatica de times balanceados por score (rating + historico).
- Swagger UI integrado.

## Tecnologias
- Java 21
- Spring Boot 4
- Spring MVC, Data JPA, Validation, Security
- PostgreSQL + Flyway
- JWT (access/refresh) + cookies HttpOnly
- OpenAPI (springdoc)

## Requisitos
- Java 21
- Maven (ou `./mvnw`)
- PostgreSQL

## Configuracao
As variaveis abaixo sao lidas de variaveis de ambiente (ou `.env` quando suportado pela sua execucao local).

Obrigatorias:
- `JDBC_URL`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `ARGON_PEPPER`
- `AUTH_COOKIE_REFRESH_NAME`
- `AUTH_COOKIE_PATH`
- `AUTH_COOKIE_SECURE`
- `AUTH_COOKIE_SAMESITE`

Recomendadas (com default em `application.properties`):
- `JWT_SECRET` (default: `dev-secret-change-me`)
- `JWT_ISSUER` (default: `geradortimes`)
- `JWT_ACCESS_TTL` (default: `15m`)
- `JWT_REFRESH_TTL` (default: `30d`)
- `JWT_REFRESH_SALT` (default: `dev-refresh-salt-change-me`)

### Supabase
Para usar Supabase, configure as variaveis abaixo (elas tem prioridade sobre `JDBC_URL/POSTGRES_*`):
- `SUPABASE_JDBC_URL` (ex.: `jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require`)
- `SUPABASE_DB_USER` (normalmente `postgres`)
- `SUPABASE_DB_PASSWORD`

## Rodando localmente
```bash
./mvnw spring-boot:run
```

Swagger UI:
- `http://localhost:8080/swagger-ui/index.html`

## Testes
```bash
./mvnw test
```

## Visao geral da API
Prefixo base: `/api`

Auth:
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

Usuarios:
- `POST /api/users` (publico)
- `GET /api/users`
- `GET /api/users/{id}`
- `DELETE /api/users/{id}`

Clubes:
- `POST /api/clubs`
- `GET /api/clubs/{id}`
- `POST /api/clubs/{clubId}/members`
- `POST /api/clubs/{clubId}/jerseys`
- `GET /api/clubs/{clubId}/jerseys`
- `DELETE /api/clubs/{clubId}/jerseys/{jerseyId}`

Partidas:
- `POST /api/matches`
- `GET /api/matches?clubId=...`
- `GET /api/matches/{id}`
- `GET /api/matches/{id}/participants`
- `DELETE /api/matches/{id}`

Times:
- `POST /api/teams`
- `GET /api/teams?matchId=...`
- `GET /api/teams/{id}`
- `PATCH /api/teams/{id}/jersey`
- `POST /api/teams/generate`
- `DELETE /api/teams/{id}`

## Geracao de times (detalhes)
O algoritmo de balanceamento está descrito em [docs/team-generation.md](docs/team-generation.md).

## Observacoes
- As rotas protegidas exigem `Authorization: Bearer <token>`.
- CORS está liberado para `http://localhost:4200` (ajuste em `SecurityConfig` se necessário).
