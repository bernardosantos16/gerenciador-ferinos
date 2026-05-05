# TESTS_SUMMARY

## Visão geral

A suíte atual é focada em **testes de integração de controllers**.  
Os testes sobem o contexto completo do Spring Boot, executam requisições HTTP com `MockMvc` e validam resposta + persistência em banco.

Atualmente existem **90 testes** distribuídos em:

- `AuthControllerTest` (11)
- `ClubControllerTest` (27)
- `UserControllerTest` (16)
- `MatchControllerTest` (15)
- `TeamControllerTest` (21)

## Tecnologias de teste usadas

- **JUnit 5** (`org.junit.jupiter`) para estrutura e execução dos testes.
- **Spring Boot Test** (`@SpringBootTest`, `@AutoConfigureMockMvc`) para subir a aplicação em modo teste.
- **MockMvc** para testar endpoints HTTP sem servidor externo real.
- **Testcontainers (PostgreSQL)** para banco real de integração (`postgres:17-alpine`).
- **Flyway** para aplicar as mesmas migrations do projeto (`classpath:db/migration`) também nos testes.
- **MockitoBean** para mockar integrações externas (`RabbitTemplate` e `JavaMailSender`).
- **Spring Security Test** para cenários com autenticação/autorização.

## Estratégia atual de testes

### 1. Base comum

A classe `src/test/java/com/bernardo/geradortimes/support/IntegrationTestBase.java` concentra:

- bootstrap do container PostgreSQL;
- injeção dinâmica de propriedades de datasource (`@DynamicPropertySource`);
- helpers de criação de dados (`createActiveUser`, `createClub`, etc.);
- geração de token JWT para cenários autenticados;
- limpeza de dados entre testes (`@BeforeEach`).

### 2. Banco e migrations

- Os testes **não** usam schema gerado por Hibernate (`spring.jpa.hibernate.ddl-auto=none`).
- O Flyway aplica as migrations oficiais da aplicação.
- Isso evita divergência entre ambiente de teste e produção no nível de banco.

### 3. Isolamento de dependências externas

Integrações que saem do processo são mockadas:

- RabbitMQ (`RabbitTemplate`);
- envio de email (`JavaMailSender`).

Assim, os testes validam o comportamento da API sem depender de infraestrutura externa além do Docker (para o PostgreSQL de teste).

## Cobertura funcional da suíte

- **AuthController**: login, refresh token, logout, casos de erro.
- **UserController**: criação, listagem admin, busca por id, deleção, verificação de email.
- **MatchController**: criação/listagem/busca/deleção de partidas e autorização por papel.
- **TeamController**: criação/listagem/busca, atualização de camisa, geração de times, troca de jogadores, deleção.

## Como executar

Pré-requisitos:

- Java 21
- Docker ativo

Comandos úteis:

```bash
# suíte completa
./mvnw clean test

# apenas uma classe
./mvnw -Dtest=UserControllerTest test

# apenas um método específico
./mvnw -Dtest=UserControllerTest#getByIdSelf test
```

## Limitações atuais e próximos passos

- A suíte está concentrada em integração de API; ainda há espaço para:
  - testes unitários de serviços/domínio;
  - testes de contrato para integrações externas;
  - métricas de cobertura (JaCoCo) no pipeline.
- Como depende de Testcontainers, execução local e CI exigem acesso a Docker.

