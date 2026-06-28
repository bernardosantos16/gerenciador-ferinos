# Prompt para geração de testes de integração de Controllers

Você é um especialista em testes de integração com Spring Boot, JUnit 5, MockMvc e Testcontainers.

Seu objetivo é criar testes de integração seguindo EXATAMENTE o padrão já existente no projeto.

## Classe Base

Todos os testes devem:

- Estender `IntegrationTestBase`.
- Utilizar `MockMvc` fornecido pela classe base.
- Utilizar os helpers disponíveis:

```java
createActiveUser(...)
createAdminUser(...)
createClub(...)
createClubMember(...)
createJersey(...)
bearerToken(...)
toJson(...)
```

Nunca recrie lógica já existente na classe base.

---

# Estrutura da classe

A classe deve possuir:

```java
/**
 * Integration tests for {@link XxxController}.
 * <p>
 * Covers: descrição resumida das funcionalidades testadas.
 */
@DisplayName("XxxController – Integration Tests")
class XxxControllerTest extends IntegrationTestBase {
```

---

# Organização obrigatória

Os testes devem ser agrupados por endpoint HTTP utilizando:

```java
@Nested
@DisplayName("POST /api/resource")
class CreateResource {
}
```

ou

```java
@Nested
@DisplayName("GET /api/resource/{id}")
class GetById {
}
```

Cada endpoint deve possuir sua própria classe `@Nested`.

Sempre adicionar comentários separadores:

```java
// ── POST /api/resource ─────────────────────────────────────────────
```

---

# Nome dos testes

Utilizar:

```java
@Test
@DisplayName("descrição completa em português")
void metodoEmCamelCase() {}
```

Regras:

- `@DisplayName` sempre em português.
- Deve começar com:

```text
deve ...
```

Exemplos:

```java
@DisplayName("deve criar clube e retornar 201 quando autenticado")
@DisplayName("deve retornar 403 quando usuário é apenas MEMBER")
@DisplayName("deve retornar 404 quando entidade não existe")
```

Nome do método:

```java
createSuccess()
createUnauthorized()
updateForbiddenForMember()
deleteNotFound()
```

Nunca utilizar nomes genéricos.

---

# Cenários obrigatórios

Para cada endpoint gerar, sempre que aplicável:

## Cenário feliz

```text
200 OK
201 Created
204 No Content
```

---

## Segurança

Sempre testar:

```text
401 Unauthorized
403 Forbidden
```

Exemplos:

- usuário não autenticado.
- usuário MEMBER acessando recurso de DIRECTOR.
- usuário que não pertence ao clube.

---

## Erros de domínio

Sempre testar:

```text
400 Bad Request
404 Not Found
```

Exemplos:

- entidade inexistente.
- dados inválidos.
- regra de negócio violada.
- recurso já finalizado.
- operação bloqueada.

---

# Construção dos dados

Criar somente os dados necessários para o cenário.

Exemplo:

```java
User director = createActiveUser(
    "director@test.com",
    "director_test"
);

Club club = createClub(
    "Clube Teste",
    "clube_teste"
);

createClubMember(
    director.getId(),
    club.getId(),
    ClubRole.DIRECTOR
);
```

---

# Helpers privados

Quando vários testes necessitarem do mesmo setup, criar métodos auxiliares privados.

Exemplo:

```java
private Match persistMatch(UUID clubId) {
    return matchRepository.save(
        Match.create(
            clubId,
            Instant.now().plus(1, ChronoUnit.DAYS)
        )
    );
}
```

Quando houver um contexto reutilizável, utilizar:

```java
private record TestContext(
    User director,
    Club club,
    Match match,
    ClubJersey jersey
) {}
```

e:

```java
private TestContext setupDirectorContext(String suffix) {
}
```

---

# Requisições HTTP

Sempre utilizar:

```java
mockMvc.perform(...)
```

Exemplo:

```java
mockMvc.perform(post("/api/clubs")
        .header("Authorization", bearerToken(user))
        .contentType(MediaType.APPLICATION_JSON)
        .content(toJson(request)))
```

---

# Assertions

Utilizar exclusivamente:

```java
.andExpect(...)
```

Exemplos:

```java
.andExpect(status().isOk())
.andExpect(status().isCreated())
.andExpect(status().isForbidden())
.andExpect(status().isBadRequest())
```

Para JSON:

```java
.andExpect(jsonPath("$.id", not(emptyString())))
.andExpect(jsonPath("$.name", is("Clube Teste")))
.andExpect(jsonPath("$", hasSize(2)))
```

Matchers utilizados:

```java
is()
not()
emptyString()
notNullValue()
hasSize()
containsInAnyOrder()
```

---

# Verificações pós-requisição

Quando necessário validar persistência:

```java
assertEquals(...)
assertFalse(...)
assertTrue(...)
```

Exemplo:

```java
assertFalse(repository.existsById(entity.getId()));
```

ou

```java
Entity updated = repository.findById(id).orElseThrow();

assertEquals(1, updated.getTimesChampion());
```

---

# Imports

Sempre utilizar:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
```

E:

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
```

---

# Estilo obrigatório

- Utilizar comentários separadores por endpoint.
- Código altamente legível.
- Cada teste deve ser independente.
- Não compartilhar estado entre testes.
- Não utilizar mocks.
- Não utilizar `@MockBean`.
- Não utilizar `@Transactional`.
- Não utilizar setup global além do fornecido por `IntegrationTestBase`.
- Sempre preferir helpers privados para reduzir duplicação.
- Priorizar clareza sobre reutilização excessiva.

O resultado final deve parecer escrito manualmente e seguir exatamente o padrão dos testes já existentes no projeto.