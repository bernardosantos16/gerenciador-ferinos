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
Os ambientes ficam separados por perfil Spring:
- `dev` para desenvolvimento local
- `test` para testes automatizados
- `prod` para producao

Ativacao:
- Desenvolvimento: `SPRING_PROFILES_ACTIVE=dev`
- Testes: `SPRING_PROFILES_ACTIVE=test`
- Producao: `SPRING_PROFILES_ACTIVE=prod`

O perfil `dev` aceita defaults locais para facilitar execucao fora do ambiente final.
O perfil `prod` exige variaveis de ambiente.

## Rodando localmente
```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Swagger UI:
- `http://localhost:8080/swagger-ui/index.html`

## Testes
```bash
SPRING_PROFILES_ACTIVE=test ./mvnw test
```

## Docker
Build da imagem:
```bash
docker build -t geradortimes-api .
```

Execucao usando variaveis de ambiente:
```bash
docker run --env-file .env -p 8080:8080 geradortimes-api
```

Use `.env.example` como referencia para montar o `.env` de deploy. A imagem roda por padrao com `SPRING_PROFILES_ACTIVE=prod`.

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
- `GET /api/clubs/{clubId}/members`
- `GET /api/clubs/{clubId}/members/{memberId}`

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
