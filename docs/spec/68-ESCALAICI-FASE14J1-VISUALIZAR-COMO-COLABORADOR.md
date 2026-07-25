# FASE 14J.1 — Visualizar como colaborador (contexto administrativo de apresentação)

**Status:** implementado
**Escopo:** `EscalaICI-KMP-Lab` (Android + Web/Wasm), `commonMain`
**Base:** `feature/fase-14j-session-demo-swaps-contract` @ `versionCode 32`/`versionName 0.7.18`
**Branch:** `feature/fase-14j1-admin-view-as-member`

## 1. Objetivo

Um administrador autorizado pode selecionar qualquer colaborador ativo da própria equipe/período já
carregado e visualizar o app (Hoje, Escala, Alertas, Plantão, Trocas, Perfil) exatamente com os
dados daquele colaborador — uma troca de **contexto de apresentação**, nunca de identidade,
permissão ou autenticação.

## 2. Modelo de identidade

Duas identidades sempre coexistem, nunca se confundem:

- **Identidade autenticada** (`sessionMemberId` em `App.kt`) — resolvida pelo login MSAL real.
  Nunca é sobrescrita pela visualização. Toda autorização e qualquer ação sensível futura sempre
  usam este valor.
- **Colaborador visualizado** (`viewedMemberId`, novo) — `String?`, `null` quando não há
  visualização ativa.
- **Contexto efetivo de apresentação**: `ViewAsIdentityContext.effectiveMemberId =
  viewedMemberId ?: authenticatedMemberId` (`ui/AdminViewAsMember.kt`). Usado **só** para decidir
  qual `ScheduleSummary` mostrar nas telas — nunca para autorização.

Na prática, `App.kt` mantém `summary` (sempre o resumo REAL do usuário autenticado, obtido no
login) e um novo `viewedSummary: ScheduleSummary?` (resumo do colaborador visualizado, buscado sob
demanda). `val effectiveSummary = viewedSummary ?: summary` é o valor passado para `TodayTab`,
`ScheduleTab`, `AlertsTab`, `ProfileTab`, `PlantaoScreen` (via `teamId`) e `ShiftSwapScreen` (via
`currentMemberId`) — nunca `summary` diretamente, exceto na aba Importar (ação sensível, sempre a
escala real).

## 3. Autorização

Reaproveita a autorização administrativa já existente (`demoAccessGranted`, calculada por
`isDemoAuthorizedForIdentity`/claims do backend) através de `isViewAsMemberAuthorized()`
(`AdminViewAsMember.kt`) — nenhuma comparação de e-mail nova. Além disso, a função fica indisponível
quando uma persona do Ambiente Demo está ativa (`selectedDemoPersona != null` ou
`requestedEntryContext == EntryContext.DEMO`): `organizationResolutionResult` (usado para
autorização/roster) fica obsoleto assim que uma persona Demo é escolhida, e oferecer o seletor
nesse momento misturaria Demo com Oficial.

## 4. Fonte dos colaboradores

Novo hook `loadTeamRoster: suspend (workspaceId, teamId) -> List<Member>` (parâmetro novo de
`EscalaIciLabApp`), reaproveitando o `MemberRepository.getMembersByTeam()` já existente
(`RemoteFirstDemoMemberRepository`, o mesmo usado por `DefaultOrganizationIdentityResolver`) —
nenhuma fonte paralela. `collaboratorOptionsForViewAs()` (função pura) filtra membros ativos,
exclui o próprio administrador autenticado e ordena alfabeticamente. `filterViewAsCollaborators()`
busca por nome, login corporativo ou equipe (nunca e-mail completo). Como o roster é sempre da
equipe do PRÓPRIO administrador (`summary.team`, nunca `effectiveSummary.team`), visualização
aninhada (visualizar-enquanto-visualiza) não é possível.

## 5. Troca de contexto e busca do resumo

Um único `LaunchedEffect(viewedMemberId, corporateDataSourceState?.publicationRevision,
organizationResolutionResult)` cobre seleção inicial, troca de contexto e "membro deixou de
existir":

1. Se `viewedMemberId == null`, `viewedSummary` fica `null` (sem visualização).
2. Se a identidade real deixou de resolver (`organizationResolutionResult` não é mais `Resolved` —
   ex.: logout), a visualização é encerrada junto.
3. Checagem rápida local (`shouldClearViewAsOnPublicationChange`, sem round-trip de rede): se
   `workspaceId`/`teamId`/`publicationRevision` mudaram desde a seleção, encerra.
4. Busca `loadPublishedScheduleSummary(workspaceId, viewedMemberId)` (o MESMO hook já usado para o
   próprio usuário). Se vier `null` (publicação atualizada sem esse membro), encerra. Caso
   contrário, `viewedSummary` é atualizado.

Todas as chamadas usam `runCatchingCancellable` (mesma guarda do Checkpoint I) — cancelamento nunca
vira uma troca parcial aplicada.

## 6. Interface

- **Entrada**: card "Acesso administrativo" em `ProfileTab` (junto de "Ambiente Demo"), botão
  "Visualizar como colaborador".
- **Seletor** (`AdminViewAsMemberScreen.kt`, novo `StackedScreen.ADMIN_VIEW_AS`): busca, lista
  ordenada, equipe/período no topo.
- **Banner persistente** (`ViewAsBanner`): "Modo de visualização administrativa" / "Você está
  visualizando a escala de {nome}. Sua conta continua sendo {nome real}." + botão "Voltar para
  minha visualização". Aparece em Hoje, Escala, Perfil (dentro do `Column` da aba ativa) e em
  Plantão/Trocas (`StackedScreen`, banner adicionado acima da tela empilhada).
- **Saída**: botão do banner, "Encerrar visualização" no card administrativo do Perfil, ou logout
  real (`performLogout()` zera `viewedMemberId`/`viewedSummary` explicitamente, além da limpeza
  reativa do item 2 acima).

## 7. Segurança

- `notificationSourceSummary` (novo parâmetro de `ProfileTab`, default `summary`): o plano de
  notificação (`buildNotificationPlan`) SEMPRE usa o resumo real do administrador
  (`App.kt` passa `notificationSourceSummary = summary` explicitamente), nunca os turnos do
  colaborador visualizado — sem isso, alterar preferências de notificação enquanto visualiza
  agendaria alarmes baseados no turno da persona.
- `Sair` sempre executa o logout real (nunca "sai só da visualização") — states explícitos e
  redundantes já limpam `viewedMemberId` em ambos os caminhos.
- Nenhuma escrita existe hoje para `ScheduleChangeRequest`/aprovação (mesma decisão honesta do
  Checkpoint F: "sem botões de ação funcionais") — não há ação sensível a proteger além da
  notificação acima; a regra fica documentada para quando escrita real existir.
- `viewedMemberId` nunca é persistido (`localDataCache`/`NotificationSettingsStore`) — só estado em
  memória do Compose, morre com o processo/logout.

## 8. Achado relacionado corrigido no mesmo checkpoint

Durante a validação manual, dois problemas pré-existentes no card "Hoje" foram identificados e
corrigidos (mesmos arquivos, `TodayTab.kt`):

- **Mensagem "Sem turnos futuros neste período" incorreta em dia de folga**: `relevantShift()` só
  considera turnos de TRABALHO; quando não há nenhum (ex.: hoje/amanhã é folga), a mensagem
  genérica aparecia mesmo havendo dado real. Corrigido: quando existe uma folga relevante
  (`ScheduleSummary.nextRest()`, já existente), o hero card usa o MESMO layout do turno normal
  (rótulo + tipo + cor + data), nunca o bloco vazio de "importar escala" (esse continua reservado
  para quando genuinamente não há publicação/importação).
- **`WeekSummaryCard` destacava o dia errado**: usava `nextShift(today)?.date` para decidir o dia
  selecionado na tira semanal, com fallback para `index == 0` quando `null` (todo dia de folga) —
  nunca "hoje" de verdade. Corrigido para `day.date == today` diretamente, sempre respeitando o
  status global do app.

## 9. Testes

`AdminViewAsMemberTest.kt` (13 testes): autorização (2), filtragem de roster — ativos/exclui
admin/ordenação alfabética (3), busca por nome/login/equipe (1), `effectiveMemberId` com e sem
visualização (2), limpeza por troca de revisão/workspace/equipe/sem-troca/sem-persona-selecionada
(5).

Testes de UI (Compose) não foram escritos — o projeto não tem harness de teste de Compose
(Robolectric/`ComposeTestRule`) configurado; os dois achados do item 8 e a integração completa da
feature foram confirmados por validação manual ao vivo (seção 10), não por leitura de código.

282 → 298 testes JVM (285 do Checkpoint I + 13 novos), 275 → 291 Wasm/Chromium, 0 falhas.

## 10. Validação manual (emulador Android, sessão MSAL real via SSO)

1. Sessão restaurada → Hoje com dado real (revisão 2, 26 dias, 1 `scheduleChangeRequest`).
2. Perfil → "Visualizar como colaborador" → roster real da equipe SOC/NOC (8 colaboradores,
   alfabético) → busca "luiz" filtra para 1 resultado (`luizneto`) → seleção.
3. Banner correto; Hoje mostra turno "Noite 19:00-01:00" de `luizneto`, colega real
   (`dschlottag`), semana toda diferente da do admin; Perfil mostra "luizneto" no topo mas
   "Identidade da escala" continua "Leonardo Rodrigo Vergani" (identidade real preservada).
4. "Voltar para minha visualização" → dados reais do admin restaurados, banner some.
5. Segunda troca (`dschlottag`, colaborador diferente) → nenhum dado de `luizneto` remanescente.
6. Logout enquanto visualizava `dschlottag` → tela de login limpa.
7. Terceiro login (SSO real) → conta real restaurada, **nenhuma persona restaurada**.
8. Card "Hoje é dia de folga" (achado do item 8) confirmado visualmente antes e depois do fix;
   `WeekSummaryCard` destacando "Sáb 25/07" (hoje real) em vez de "Dom 19/07".

## 11. Validação Web/Wasm

`wasmJsBrowserDistribution` servido localmente, aberto em Chromium real headless: tela de entrada
carrega limpa, trace de estado aparece no console (`[Web] resolutionEffect ...`), zero erro. Login
MSAL interativo Web não foi exercitado nesta rodada (exigiria OAuth real); mesma limitação já
registrada em checkpoints anteriores.

## 12. Critérios de aceite

1. Função só visível para identidade com `demoAccessGranted == true`, nunca fora do Ambiente Demo.
2. Lista mostra só membros ativos da equipe/revisão atual do administrador, nunca de outro
   workspace/revisão/período, nunca o próprio administrador.
3. Troca de colaborador atualiza atomicamente Hoje/Escala/Alertas/Perfil/Plantão/Trocas via
   `effectiveSummary`; nenhum dado do colaborador anterior permanece.
4. Identidade autenticada nunca é substituída visualmente sem indicação clara (banner + card
   "Identidade da escala" sempre real).
5. Notificações nunca são agendadas com os turnos do colaborador visualizado.
6. Logout e novo login nunca restauram a persona visualizada.
7. Nenhum dos 285 testes JVM / 275 Wasm pré-existentes quebra (298/291 no total, com os novos).
