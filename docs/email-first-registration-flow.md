# Fluxo de Cadastro Email-First — Guia para o Client (Angular)

## Visão geral

O cadastro acontece em **3 etapas**:

```
[1] Usuário informa email        →  POST /api/users/email
[2] Usuário digita código OTP de 6 dígitos →  POST /api/users/verify-email
[3] Usuário preenche nome, nickname e senha →  POST /api/users
```

**Fluxo inspirado no Jira (Atlassian):** O OTP é consumido na etapa 2, e um **JWT de registro** é emitido. A etapa 3 usa esse JWT (não o OTP). O email do usuário é extraído do JWT — o client não precisa reenviá-lo.

---

## Endpoints

### 1. Enviar código de verificação (OTP)

```
POST /api/users/email
Content-Type: application/json
```

**Request body:**

```json
{
  "login": "joao@example.com"
}
```

**Validação no client antes de enviar:**
- Campo obrigatório, email válido, máx. 100 caracteres.

**Respostas possíveis:**

| HTTP | Significado | Ação no client |
|------|-------------|----------------|
| `204 No Content` | Código enviado | Avançar para tela de digitar código |
| `400 Bad Request` | Email inválido | Exibir mensagem de erro do campo |
| `409 Conflict` | Email já cadastrado | Exibir "Este email já está em uso" |
| `429 Too Many Requests` | Rate limit (3 req/min por IP) | Exibir "Muitas tentativas. Aguarde 1 minuto." |

**IMPORTANTE:** Chamadas repetidas dentro de 5 minutos com o mesmo email são ignoradas silenciosamente (retornam 204). O mesmo código de 6 dígitos permanece válido.

**Validade do OTP:** 5 minutos.

---

### 2. Verificar OTP e obter token de registro

```
POST /api/users/verify-email
Content-Type: application/json
```

**Request body:**

```json
{
  "login": "joao@example.com",
  "token": "123456"
}
```

**Validação no client antes de enviar:**
- `login`: obrigatório, email válido
- `token`: obrigatório, exatamente 6 dígitos numéricos

**Respostas possíveis:**

| HTTP | Significado | Ação no client |
|------|-------------|----------------|
| `200 OK` | OTP válido, token de registro gerado | Armazenar `registrationToken` e avançar para formulário de cadastro |
| `400 Bad Request` | Dados inválidos | Exibir erro de validação |
| `404 Not Found` | Código inválido/expirado/já usado | Exibir "Código inválido ou expirado" |

**Response body (200):**

```json
{
  "registrationToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**IMPORTANTE:** Este endpoint **consome o OTP** — ele não pode mais ser reutilizado. Em troca, retorna um **JWT de registro** (válido por 30 minutos) que deve ser usado na etapa 3.

---

### 3. Criar conta

```
POST /api/users
Content-Type: application/json
```

**Request body:**

```json
{
  "name": "João Teste",
  "nickname": "joao_teste",
  "password": "S3nh4F0rt3!",
  "registrationToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**Validação no client antes de enviar:**
- `name`: obrigatório, máx. 250 caracteres
- `nickname`: obrigatório, máx. 100 caracteres
- `password`: obrigatório, 8 a 72 caracteres
- `registrationToken`: obrigatório — **o JWT obtido na etapa 2**

**O email NÃO é enviado no body** — ele é extraído do JWT de registro.

**Respostas possíveis:**

| HTTP | Significado | Ação no client |
|------|-------------|----------------|
| `201 Created` | Conta criada com sucesso | Redirecionar para login ou logar automaticamente |
| `400 Bad Request` | Dados inválidos | Exibir erros de validação por campo |
| `404 Not Found` | Token de registro inválido/expirado | Exibir "Sessão expirada. Recomece o cadastro." |
| `409 Conflict` | Nickname já em uso | Exibir mensagem específica do campo |

**Response body (201):**

```json
{
  "id": "uuid-aqui",
  "name": "João Teste",
  "nickname": "joao_teste",
  "login": "joao@example.com"
}
```

O header `Location` contém a URL do recurso criado.

**IMPORTANTE:** O token de registro é um JWT stateless — pode ser usado apenas uma vez para criar a conta. Se o cadastro falhar por nickname duplicado (409), o mesmo token continua válido para nova tentativa com outro nickname (dentro do TTL de 30 minutos).

---

## Estrutura de erro de validação

Quando o servidor retorna `400 Bad Request` ou `409 Conflict`, o corpo segue o formato [RFC 9457 Problem Detail](https://datatracker.ietf.org/doc/html/rfc9457):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/users/email",
  "errors": [
    {
      "field": "login",
      "message": "must be a well-formed email address"
    }
  ]
}
```

O array `errors` contém objetos `{ field, message }`. O campo `field` pode ser `name`, `nickname`, `login`, `password`, `registrationToken` ou `global`.

**Mapeamento de mensagens de erro conhecidas:**

| Mensagem | Campo | Significado |
|----------|-------|-------------|
| `email already registered` | `login` | Email já cadastrado |
| `nickname already exists` | `nickname` | Apelido já em uso |
| `login already exists` | `login` | Login já em uso |
| `invalid or expired registration token` | `registrationToken` | Token de registro inválido/expirado |

---

## Rate Limiting

Apenas o endpoint `POST /api/users/email` possui rate limit:
- **3 requisições por minuto por IP**
- Exceder retorna `429 Too Many Requests`
- O client deve tratar `429` exibindo uma mensagem e **não reenviar** até o usuário solicitar explicitamente

---

## Estados e navegação do formulário

### Fluxo de navegação

1. **Tela de email:** Usuário digita email → chama `sendVerificationCode()` → em caso de sucesso (204), navega para tela de código.
2. **Tela de código:** Usuário digita 6 dígitos → chama `verifyCode()` → em caso de sucesso (200), **armazena o `registrationToken`** e navega para formulário de cadastro.
3. **Tela de cadastro:** Usuário preenche nome, nickname, senha → chama `register()` com o `registrationToken` → em caso de sucesso (201), redireciona para login.

Em caso de erro em qualquer etapa:
- `400`: exibir erros de validação por campo
- `404`: voltar para tela de email (token inválido/expirado)
- `409`: exibir conflito (email já existe, nickname já existe)
- `429`: exibir mensagem de rate limit, bloquear reenvio

### Regras importantes

- O email digitado na etapa 1 **não pode ser alterado** nas etapas 2 e 3. Ele é fixo após o envio do código.
- Se o usuário digitar email errado, deve haver um botão "Voltar e corrigir email" que retorna à etapa 1.
- O código de 6 dígitos é enviado por email. O client não tem acesso a ele — o usuário deve consultar sua caixa de entrada.
- **OTP:** validade de **5 minutos**. Após isso, o usuário deve solicitar um novo código (voltar à etapa 1).
- **JWT de registro:** validade de **30 minutos**. Emitido na etapa 2, usado na etapa 3.
- O email do usuário **não é enviado na etapa 3** — ele é extraído do JWT de registro.

---

## Resumo do fluxo de tokens

```
Etapa 1: POST /api/users/email
  → Gera OTP (6 dígitos, 5 min) → armazenado no banco (hash SHA-256)

Etapa 2: POST /api/users/verify-email  
  → Valida OTP → CONSOME o OTP → gera JWT de registro (30 min, stateless)

Etapa 3: POST /api/users
  → Valida JWT de registro → extrai email → cria usuário ACTIVE
```

**Vantagem sobre o fluxo antigo:** O OTP só precisa durar da etapa 1 até a etapa 2 (entrada do código). O JWT de registro tem 30 minutos para preenchimento dos dados, resolvendo o problema de expiração durante o cadastro.
