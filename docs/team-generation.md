# Geração e balanceamento de times

Contexto: implementado em `src/main/java/com/bernardo/geradortimes/team/service/TeamService.java` (método `generate`). Este documento explica, em português simples, como o algoritmo funciona para que novos membros da equipe entendam rapidamente.

## Entradas esperadas
- `matchId` (UUID): partida alvo.
- `lineMemberIds` (List<Long>): IDs de membros do clube que jogarão na linha.
- `goalkeeperMemberIds` (List<Long>): IDs de membros do clube que jogarão no gol.
- `maxLinePlayers` (int >= 1): tamanho máximo de jogadores de linha por time.

## Pré-validações e normalização
- Garante que `maxLinePlayers >= 1` e que existam pelo menos 2 IDs em `lineMemberIds`.
- Remove `null` e IDs duplicados de ambas as listas; se encontrar, lança BAD_REQUEST.
- Proíbe a sobreposição entre line e goleiro (mesmo ID não pode estar nos dois).
- Carrega todos os membros do clube pelos IDs; se algum ID não existir, retorna BAD_REQUEST listando os faltantes.
- Verifica autorização: somente DIRECTOR do clube da partida pode gerar times.

## Cálculo do número e tamanho dos times
- `teamCount = max(2, round(lineCount / maxLinePlayers))`.
  - Ex.: 15 linhas com max 5 => 3 times; 16 com max 5 => 3 times (6/5/5).
- `teamSizes`: divide `lineCount` entre os times, distribuindo o resto do módulo nos primeiros times.

## Pontuação dos jogadores (balanceamento)
- Cada membro recebe um score somando três métricas normalizadas (0 a 1):
  - `rating` (campo `rating`, default 0 se nulo).
  - `timesChampion`.
  - `timesMvp`.
- Se todos tiverem o mesmo valor em uma métrica, o peso dessa métrica vira 0 para evitar divisão por zero.
- Resultado: lista `ScoredMember(memberId, score)`.

## Distribuição dos jogadores de linha
- Ordena `ScoredMember` de linha em ordem decrescente de score (tie-break pelo ID).
- Cria um bucket por time (`TeamBucket`), cada um sabe seu tamanho alvo.
- Usa **snake draft**: em rodadas pares os buckets recebem na ordem reversa (último → primeiro), em rodadas ímpares na ordem direta (primeiro → último).
- Se o bucket natural da rodada estiver cheio, escolhe o mais fraco disponível.
- Efeito: o time que pegou o melhor jogador da rodada recebe o pior da rodada seguinte, promovendo equilíbrio mais justo que o greedy puro.

## Tratamento de goleiros
- Calcula score para todos os goleiros, ordena do mais forte ao mais fraco.
- Atribui cada goleiro ao bucket mais fraco que ainda não tem goleiro (1 por time).
- Quando todos os times já têm goleiro, os excedentes vão para `unassignedGoalkeeperMemberIds` e são persistidos com `teamId = null`.

## Persistência
- Antes de gerar, limpa dados anteriores da partida (`matchParticipantRepository.deleteByMatchId`, `teamRepository.deleteByMatchId`).
- Cria os times (entidade `Team`) com `matchId` e `clubJerseyId = null` e salva.
- Cria participantes (`MatchParticipant`) para cada jogador de linha e goleiro:
  - posição `LINE` ou `GOAL`
  - `teamId` conforme bucket; goleiros não atribuídos ficam com `teamId = null`.
- Salva todos os participantes e retorna `GenerateTeamsResponseDTO` contendo:
  - `teamCount`
  - times gerados (`teamId`, `lineMemberIds`, `goalkeeperMemberId`)
  - `unassignedGoalkeeperMemberIds`

## Regras de erro principais (HTTP 400)
- `matchId` inexistente.
- `maxLinePlayers < 1`.
- Menos de 2 jogadores de linha.
- IDs `null` ou duplicados.
- ID presente em linha e goleiro ao mesmo tempo.
- Membros do clube não encontrados.
- “not enough buckets to place members” (defensivo; acontece se `teamSizes` for inconsistente).

## Por que o balanceador funciona
- Usa score contínuo somando três sinais de habilidade/experiência.
- Sempre coloca o próximo jogador mais forte no time atualmente mais fraco, equilibrando tanto a soma de score quanto a contagem de jogadores.
- Tie-break determinístico por `teamId` evita variação entre execuções com dados iguais.

## Como ler o código rapidamente
- Ponto de entrada: `TeamService.generate`.
- Métodos utilitários relevantes:
  - `normalizeIds`, `union`, `computeTeamCount`, `computeTeamSizes`
  - `scoreMembers` (normalização + soma)
  - `pickBucketForNextPlayer` (heurística de fraqueza)
  - Classe interna `TeamBucket` guarda alocação e score acumulado.

## Sugestões de melhoria futura
- Tornar pesos configuráveis para rating/champion/mvp.
- Permitir informar `teamCount` diretamente (em vez de derivar de `maxLinePlayers`).
- Distribuir goleiros mesmo quando a contagem não bate, priorizando um goleiro por time e deixando excedentes como não atribuídos.
