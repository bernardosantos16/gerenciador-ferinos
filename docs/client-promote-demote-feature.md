# Prompt — Client (Angular): promover/rebaixar membro de clube

> Prompt para o agente responsável pelo frontend Angular (repositório separado) implementar
> a nova funcionalidade de promover um membro a diretor e rebaixar um diretor a membro.

## Contexto

O backend agora expõe dois novos endpoints para alterar a função de um membro de clube
(`DIRECTOR` <-> `MEMBER`). Antes, o client **não tinha** essa capacidade, então esta é uma
mudança **puramente aditiva**: nenhuma chamada existente do client é alterada.

## Contrato da API (novos endpoints)

Ambos exigem `Authorization: Bearer <access_token>` e que o usuário logado seja **DIRECTOR**
do clube.

### 1. Promover membro a diretor

```
PATCH /api/clubs/{clubId}/members/{memberId}/promote
```

| Resposta | Significado |
|----------|-------------|
| `204 No Content` | Sucesso (membro virou diretor). |
| `401` | Não autenticado. |
| `403` | Usuário logado não é DIRECTOR do clube. |
| `404` | Membro não existe (ou pertence a outro clube). |
| `409` | Membro já é diretor **ou** não possui usuário vinculado (`userId == null`). |

### 2. Rebaixar diretor a membro

```
PATCH /api/clubs/{clubId}/members/{memberId}/demote
```

| Resposta | Significado |
|----------|-------------|
| `204 No Content` | Sucesso (diretor virou membro). |
| `401` | Não autenticado. |
| `403` | Usuário logado não é DIRECTOR do clube. |
| `404` | Membro não existe (ou pertence a outro clube). |
| `409` | Membro já é MEMBER, não possui usuário vinculado, **ou é o dono do clube** (criador). |

## Mudança no modelo de resposta de membro

O DTO `ClubMemberResponseDTO` (retornado por `GET /api/clubs/{clubId}/members` e
`GET /api/clubs/{clubId}/members/{memberId}`) ganhou um campo novo:

```json
{
  "id": 12,
  "userId": "c0a8012e-6f1f-4b4b-9f5e-7a8b9c0d1e2f",   // null se o membro não tem conta
  "name": "João",
  "rating": 4,
  "timesMvp": 2,
  "timesChampion": 1,
  "teamId": null,
  "clubRole": "MEMBER",                                  // "DIRECTOR" | "MEMBER"
  "isOwner": false                                       // true apenas para o criador do clube
}
```

Campos relevantes para a lógica de UI:

- `userId` — `null` quando o membro foi cadastrado manualmente (sem conta). Não pode virar diretor.
- `clubRole` — `"DIRECTOR"` ou `"MEMBER"`.
- `isOwner` — `true` somente para o membro que criou o clube.

## Regras de negócio (obrigatórias)

1. **Confirmação antes de promover**: ao clicar em "Tornar diretor", exibir um diálogo/modal
   de confirmação (ex.: "Tornar <nome> diretor do clube?" com Cancelar/Confirmar). Só chamar a
   API após confirmação explícita.

2. **Exibir "Tornar diretor"** somente quando, para o membro:
   - `userId != null` (o membro tem conta de usuário), **e**
   - `clubRole == "MEMBER"`.

3. **Não exibir nenhum botão de ação** (nem "Tornar diretor", nem "Rebaixar") para o membro
   que criou o clube (`isOwner == true`). A linha dele não deve ter nenhum controle de
   promoção/rebaixamento.

4. **Exibir "Rebaixar a membro"** (demote) para diretores que **não** são o dono
   (`clubRole == "DIRECTOR"` e `isOwner == false`). Para o rebaixamento não é obrigatória a
   confirmação, mas é recomendado um diálogo de confirmação também.

5. **Visibilidade geral**: os controles de promover/rebaixar só devem aparecer quando o usuário
   logado é DIRECTOR do clube (mesma regra já usada para "remover membro"). Membros que não são
   diretores não veem essas ações.

## Tratamento de erros

- `409` — exibir mensagem amigável (ex.: "Este membro já é diretor", "Este membro não possui
  conta vinculada", "Não é possível rebaixar o dono do clube").
- `403`/`404` — manter o tratamento já existente no client para esses códigos.
- Após sucesso (`204`), atualizar a lista de membros (re-fetch ou atualização local) para
  refletir o novo `clubRole`.

## Convenções do client

- Seguir os padrões de componentes, serviços HTTP e formulários/diálogos já existentes no
  projeto Angular (não criar novos padrões).
- Reutilizar o serviço de membros existente; adicionar os dois métodos (`promote`, `demote`)
  seguindo o estilo dos métodos atuais.
- Textos em português, consistentes com o restante da interface.
