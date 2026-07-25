# Roadmap — Escala ICI KMP Lab

Este roadmap segue o plano de fases descrito em
`EscalaSOC/docs/spec/32-KMP-LAB-VISUAL-XLS-WEB-APRESENTACAO.md` (repositório
Android principal, apenas como referência de spec — este laboratório vive em
`EscalaICI-KMP-Lab` e não altera nada em `EscalaSOC/`).

Status possíveis: `TODO`, `IN_PROGRESS`, `DONE`.

## FASE 14J.1 — Visualizar como colaborador (contexto administrativo de apresentação)

- **Status:** DONE (local) — spec 68, branch `feature/fase-14j1-admin-view-as-member`. Novo
  `viewedMemberId`/`viewedSummary` (`App.kt`) e `effectiveMemberId`/`effectiveSummary` -
  administrador autorizado (mesma autorização de Ambiente Demo, `demoAccessGranted`) visualiza
  qualquer colaborador ativo da própria equipe com os dados reais dele (Hoje/Escala/Alertas/
  Perfil/Plantão/Trocas), sem trocar conta Microsoft, sem segunda autenticação, sem escrita em
  nome do colaborador. Identidade autenticada sempre preservada e visível (banner + card
  "Identidade da escala" nunca substituído). Roster vem de `loadTeamRoster` (novo hook,
  reaproveita `RemoteFirstDemoMemberRepository.getMembersByTeam` já existente). Nunca oferecida
  dentro do Ambiente Demo (evita misturar Demo/Oficial). Notificações protegidas
  (`notificationSourceSummary` sempre real, nunca da persona).
- **Achado relacionado corrigido no mesmo checkpoint**: card "Hoje" mostrava "Sem turnos futuros
  neste período" mesmo quando o motivo real era simplesmente hoje/amanhã ser folga
  (`relevantShift()` só considera turnos de trabalho); e `WeekSummaryCard` destacava o dia errado
  na tira semanal (fallback para o primeiro dia da janela em vez de "hoje" real). Ambos corrigidos
  em `TodayTab.kt` - o card de folga agora usa o mesmo layout visual do turno normal.
- 285 → 298 testes JVM, 275 → 291 Wasm/Chromium, 0 falhas. Validado manualmente no emulador (SSO
  MSAL real): busca, troca entre 3 colaboradores diferentes, retorno, logout/login sem persistir
  persona. `versionCode`/`versionName`: `32/0.7.18` → `33/0.7.19`.

## FASE 14J — Sessão persistente, Demo autorizado, trocas, contrato global + bugfix do gate de entrada

- **Status:** IN_PROGRESS — spec 67. Checkpoints B a G (sessão sem flash, Demo oculto até
  autorização + logout real, `scheduleChangeRequests` no estado global, card "Solicitações de
  troca" em Hoje, golden contract, fonte única de versão) implementados e commitados; ver spec 67
  para detalhes de cada checkpoint.
- **Checkpoint H — bugfix real relatado em produção (2026-07-24)**: usuário reportou que, a cada
  versão nova, o mesmo problema recorrente voltava — MSAL autenticava mas o app ficava preso na
  tela de login mostrando "A escala oficial ainda não foi publicada neste ambiente" em vermelho,
  mesmo com a escala já publicada segundo o relato. Causa raiz: `App.kt` nunca separava
  "identidade corporativa resolvida" de "existência de escala publicada" — ambas dependiam do
  mesmo resultado para decidir se `sessionMemberId` era setado, então a ausência de publicação
  zerava a sessão e travava o gate. Corrigido com `decideLoginEntry()`
  (`ResolvedScheduleSummaryDecision.kt`): identidade resolvida sempre concede entrada; ausência de
  publicação nunca mais zera `sessionMemberId` nem aparece como erro bloqueante — só deixa a
  escala em si desatualizada. 2 testes de regressão novos. Detalhes completos em spec 67, seção
  "Checkpoint H".
- **Achado relacionado, investigado e corrigido**: em emulador, a mesma tela reapareceu mesmo com
  o fix acima ativo — a identidade em si não resolvia (`OrganizationResolutionResult` não chegava
  a `Resolved`). Diagnóstico seguro novo (`diagnostics/ResolutionDiagnostics.kt`) revelou a causa:
  `DemoPublicationResolver.loadOneAttempt()` engolia `CancellationException` junto com qualquer
  outro `Throwable` — quando o `LaunchedEffect` de resolução era cancelado (ex.: refresh silencioso
  de token do MSAL mudando `corporateAuthState` de novo antes da leitura terminar), a corrotina já
  cancelada continuava executando e cacheava uma falha **falsa** para sempre em
  `DemoPublicationRepository`. Corrigido: `CancellationException` sempre relançada; falha genuína
  sem fixture nunca mais fica presa no cache (próxima chamada tenta de novo). Confirmado ao vivo no
  emulador: identidade resolve, usuário entra direto em Hoje. 2 testes de regressão novos. Detalhes
  em spec 67, seção "Checkpoint H".
- 274 → 282 testes JVM, 267 → 275 Wasm/Chromium, 0 falhas.
- **Checkpoint I — bugfix real relatado após teste manual (2026-07-24)**: (A) card Hoje às vezes
  desatualizado; (B) login funcionava uma vez, mas travava com o mesmo erro após logout. Causa
  raiz comum do bug A: `runCatching` em dois pontos do `LaunchedEffect` de resolução (`App.kt`)
  engolia `CancellationException` (mesmo padrão do achado do Checkpoint H, em local diferente) —
  uma corrotina cancelada continuava executando e podia sobrescrever o estado com dados obsoletos.
  Corrigido com `runCatchingCancellable()`, função genérica que sempre relança cancelamento. Causa
  raiz do bug B: `performLogout()` zerava `sessionMemberId`/`requestedEntryContext` de forma
  síncrona ANTES de `corporateAuthRepository?.signOut()` terminar — se o usuário tocasse "Entrar"
  nessa janela, `corporateAuthState` ainda estava `Authenticated` (stale) e o app pulava a chamada
  real ao MSAL, ficando preso. Corrigido movendo o reset de estado para dentro do `scope.launch`,
  após o `signOut()` real completar. Instrumentação segura nova (`logResolutionTrace()`) permitiu
  confirmar a causa ao vivo em vez de só ler código. Validado manualmente: 3 ciclos completos de
  login/logout + fechar e reabrir o app, todos com Hoje atualizado corretamente, via SSO real do
  MSAL (`login.microsoftonline.com`). 1 teste de regressão novo. Detalhes em spec 67, "Checkpoint
  I".
- 282 → 285 testes JVM, 275 → 278 Wasm/Chromium, 0 falhas. `versionCode`/`versionName`: `31/0.7.17`
  → `32/0.7.18`.

## FASE 14I — Consistência dos cards (pausa efetiva, colegas por turno) + padronização visual SOC

- **Status:** DONE (local) — spec 66. Achado real do usuário no APK
  `0.7.14` (release, FASE 14H): pausa configurada (10:30) não aparecia
  nos cards "Pausa" (Hoje)/"Resumo" (Perfil), que continuavam mostrando
  só a janela genérica 09:00–11:45 mesmo com "Próximos alarmes" já
  usando o horário certo; "Próximo turno"/"Detalhe do dia" misturavam
  colegas de todos os turnos, enquanto "Quem trabalha nesse dia" já
  separava corretamente por turno.
- **Checkpoint 0** (antes de qualquer código): instalei o APK release já
  existente da FASE 14H (`versionCode 28`/`0.7.14`, assinatura
  verificada) num emulador limpo, sessão MSAL real restaurada, dado
  real (revisão remota 2 de `ici-dev`, período `2026-06-25` a
  `2026-07-26` — confirmando que a FASE 14H genuinamente não publicou
  revisão nova) — os dois bugs reproduzidos ao vivo, exatamente como
  relatado.
- **Causa raiz exata** (lida no código): `PausePresentation.scheduledLabel`
  (`model/TemporalRules.kt`) já existia para mostrar o horário efetivo,
  mas `pauseFor()` nunca recebia `NotificationSettings.pauseCustomTime` —
  `TodayTab` nem recebia `NotificationSettings` como parâmetro.
  `teamMembers` (achatado, todos os turnos) vs `membersByShift` (correto,
  por turno) já existiam certos desde a FASE 14H — só dois cards liam o
  campo errado.
- **Decisão arquitetural registrada**: não introduzir um
  `EscalaIciAppState`/`StateFlow` novo. `App.kt` já centraliza o estado
  (`remember`/`mutableStateOf` único, consumido por todos os tabs) —
  reescrever isso para corrigir "card lendo o campo errado" seria a
  refatoração ampla que a própria fase pede para evitar. Correção: dois
  seletores puros novos (`ui/CardSelectors.kt`): `effectivePause(shift,
  settings)` (reaproveita `pauseWindowFor()`, preenche `scheduledLabel`
  quando há horário customizado válido dentro da janela) e
  `colleaguesForShift(day)` (extrai o fallback `membersByShift ?:
  teamMembers` já usado em "Quem trabalha nesse dia"). 4 call sites
  migrados (`TodayTab.PauseCard`/`NextTurnHero`,
  `ProfileTab`/`ScheduleTab.CalendarDayDetailCard`).
- Checkpoint D (estrutura canônica do parser): confirmação, não nova
  implementação — `ShiftDay.sourceStatus` já cumpre integralmente o
  papel de "código de origem preservado"; mesma tabela canônica de
  turnos/situações formalizada do lado Dashboard
  (`docs/spec/11-DASHBOARD-FASE14I-...md`), um único contrato para os
  dois repositórios.
- 8 testes novos (`CardSelectorsTest.kt`): pausa dentro/fora da janela,
  lembrete desativado, horário inválido, turno nulo; colegas por turno
  com/sem `membersByShift`. 239 → 247 testes JVM, 232 → 240 testes
  Wasm/Chromium, ambos 0 falhas. `compileDebugKotlinAndroid` e
  `compileKotlinWasmJs` também verificados.
- **Validação manual real, com o build novo (`0.7.15`/`29`)**: reproduzi
  de novo, no mesmo emulador, com a mesma sessão MSAL e o mesmo dado
  real (22/07/2026, turno Manhã, `lvergani`) — `Com:` agora mostra só
  `alamancio`; `Pausa` (Hoje) e `Resumo` (Perfil) agora mostram
  `10:30–10:45` (`Pausa programada`), com `Janela permitida: 09:00–11:45`
  preservada como texto secundário, nunca confundida com o horário
  escolhido; `Detalhe do dia` também corrigido; `Quem trabalha nesse
  dia` sem regressão. Persistência confirmada após `force-stop`+reabrir
  e após reboot real do emulador (`dumpsys alarm` mostra os alarmes de
  pausa re-registrados com `10:30`/`10:45` via `BOOT_COMPLETED`, sem
  abrir o app manualmente).
- **Web/Wasm**: `wasmJsBrowserDistribution` gerada e servida localmente,
  Chromium real confirma app íntegro, service worker ativo
  (`escala-ici-web-v7`, sem regressão), reload 100% offline continua
  funcionando. Validação interativa completa dos cards em navegador
  fresco permanece bloqueada pelo mesmo achado já documentado na FASE
  14H (login corporativo exigido mesmo para "Ambiente Demo" numa sessão
  não autenticada) — não corrigido nesta fase (fora de escopo,
  confirmado de novo, mesma decisão). A lógica dos seletores em si já é
  validada pelos 240 testes Wasm/Chromium (`CardSelectorsTest` roda no
  mesmo motor real via Karma).
- `versionCode`/`versionName`: `28`/`0.7.14` → `29`/`0.7.15`.
- Nenhuma publicação real, nenhum deploy, nenhum dado real alterado —
  nenhuma ação de publish foi executada nesta fase; uma futura revisão
  corrigida é o próximo passo natural, fora do escopo autorizado aqui.
- **Achado adicional no gate de distribuição**: `model/AppVersion.kt`
  (`CODE`/`LABEL`, fonte manual da aba Perfil e do `AppUpdateChecker`
  Android, já que KMP não gera `BuildConfig` acessível de `commonMain`)
  estava parado em `27`/`"0.7.13"` desde a FASE 14f-3 — não fora
  atualizado nem na FASE 14H nem no início desta fase, apesar de
  `build.gradle.kts` já estar em `29`/`0.7.15`. Corrigido para
  `29`/`"0.7.15"`; sem isso a aba Perfil exibiria uma versão
  desatualizada indefinidamente e o `AppUpdateChecker` nunca convergiria
  (sempre compararia contra `27`). APK release recompilado, reassinado
  e reconferido (mesma assinatura V2, 1 signer) após a correção; nova
  cópia de distribuição e novos hashes SHA-256 substituem os gerados
  antes desse achado.
- **Anomalia transitória observada no gate de distribuição**: na
  primeira reinstalação a partir de `EscalaICI-latest.apk` num emulador
  recém-iniciado, "MINHA ESCALA" retornou uma vez
  `WORKSPACE_NOT_PUBLISHED` ("A escala oficial ainda não foi publicada
  neste ambiente."). Não investiguei via logcat (app não emite log
  correlato) nem alterei nenhum dado remoto — apenas fechei/reabri o
  app e tentei de novo. Na tentativa seguinte os dados reais carregaram
  normalmente (mesmo caso `lvergani`/22-07/Manhã/`alamancio` validado
  antes), confirmando que foi uma falha transitória (rede/latência no
  primeiro request pós-instalação), não uma regressão desta fase — os
  arquivos tocados na FASE 14I não tocam `OrganizationIdentityResolver`
  nem `FirestoreRestGateway`. Versão `0.7.15` confirmada visualmente na
  aba Perfil logo em seguida, já com a conta corporativa real.
- Detalhe completo: `docs/spec/66-ESCALAICI-ESTADO-GLOBAL-E-CONSISTENCIA-DOS-CARDS.md`.

## FASE 14H — Fidelidade do parser, plantão multi-grupo, notificações Android/Web

- **Status:** DONE (local), com um bloqueio externo documentado (achado
  fora de escopo, ver abaixo) — spec 65 (gate de contrato) +
  `escala-dashboard` spec `10-DASHBOARD-FASE14H-...`.
- Baseline confirmado antes de iniciar: KMP `feature/fase-14f-msal-web-auth`
  @ `da66f77` (`versionCode` 27/`0.7.13`); Dashboard
  `feature/fase-14f-official-import-publish` @ `8837c40` (`1.15.1`).
- **Dashboard** (`feature/fase-14h-dashboard-parser-oncall-push`, `1.15.1`
  → `1.15.2`, 399 → 447 testes): parser oficial ganhou layout
  `soc-combined` (cruza planilhas Escala+Escalistas por separador
  unificado, sem ano fixo hardcoded); toda linha/dia agora sempre produz
  um registro explícito (nunca ausência silenciosa) com fallback
  `NO_DATA`/"trabalho sem turno localizado"; novo domínio de grupos de
  plantão (`OnCallGroup`, painel admin, import oficial exige grupo
  explícito para toda atribuição de plantão — nunca infere por nome de
  arquivo); novo contrato `/api/push/subscriptions` (Firebase Admin,
  dry-run — nunca envia push real). Bug real encontrado e corrigido antes
  do commit: botão de importar escala regular ficava desabilitado sempre
  que qualquer equipe de plantão existisse no workspace, mesmo sem grupo
  selecionado.
- **KMP** (`feature/fase-14h-kmp-schedule-oncall-notifications`, `0.7.13`/27
  → `0.7.14`/28, 213 → 239 testes JVM + 232 testes `wasmJsTest`/Chromium):
  - Corrigidas duas causas-raiz confirmadas por pesquisa prévia: gap do
    6x1 contava posição na lista em vez de adjacência real de data;
    agrupamento "quem trabalha comigo" ignorava o turno (misturava
    Manhã/Tarde/Noite no mesmo dia). `ShiftType` ganhou mapeamento
    explícito para `BH`/`ANIVERSARIO`/`HORA_EXTRA`/`AFASTAMENTO`/
    `INCONSISTENCIA` (antes convertidos silenciosamente para
    `INDEFINIDO`, zerando métricas).
  - Domínio de plantão multi-grupo: SOC (1 grupo, "COSI") abre direto;
    NOC (múltiplos grupos) exige seleção explícita — nunca infere grupo
    pelo nome do arquivo. Cache/UI/import escopados por `groupId`.
  - Domínio de notificações (spec 49): `NotificationSettings` único e
    consolidado (sem flags divergentes do legado), `buildNotificationPlan`
    cobre o período publicado inteiro (sem cap de 21 dias), pausa de 15min
    com janela por turno (spec 49 §2). Corrigido bug de aritmética de data
    negativa em `LabDate.plusDays`/`LabDateTime.plusMinutes` (necessário
    para "véspera" e "N min antes do turno" cruzando meia-noite).
  - Android: canal de notificação + `POST_NOTIFICATIONS` só solicitada se
    ainda não concedida (o legado pedia sempre); `AlarmManager
    .setAndAllowWhileIdle` (decisão explícita de não pedir
    `SCHEDULE_EXACT_ALARM` nesta fase — legado funcionava só com alarme
    inexato para os mesmos tipos de lembrete); `requestCode` via
    `id.hashCode()` (sem a colisão do `mod 512` do legado); receiver de
    `BOOT_COMPLETED` (corrige lacuna real do legado, que nunca sobrevivia
    a reboot).
  - Web/PWA: notificações locais via `setTimeout` com horizonte de 12h e
    reavaliação a cada 5min (nunca agenda semanas à frente — risco de
    overflow do argumento 32-bit do `setTimeout`); `service-worker.js`
    corrigido (erro real de `chrome-extension://` no cache, `CACHE_NAME`
    → v7); Push real (VAPID) fica fora por decisão já registrada na spec
    49 §10 (fase futura), não por esquecimento.
  - 2 bugs reais encontrados e corrigidos antes do commit (rodada Web):
    `scheduledCount` reportado ao usuário refletia o tamanho do plano
    inteiro em vez do que foi de fato armado no horizonte de 12h (mensagem
    numericamente enganosa); `showViaServiceWorker` gerava JS inválido no
    bundle de produção (função de corpo-expressão com conteúdo de
    statement) — quebrava `wasmJsBrowserDistribution` silenciosamente
    (nem `compileKotlinWasmJs` nem os testes unitários exercitam o
    webpack).
- Validação manual real executada (não apenas compilação):
  - **Android** (`EscalaSOC_API_37`, `-gpu host`): permissão
    `POST_NOTIFICATIONS` solicitada corretamente na primeira abertura e
    não novamente após conceder; notificação de teste real via
    `NotificationManagerCompat`; alarmes reais registrados no
    `AlarmManager` do sistema (confirmado via `dumpsys alarm`) cobrindo
    todo o período publicado (26/07 a 25/08/2026, não só 21 dias);
    disparo real confirmado para dois alarmes distintos após avançar o
    relógio do sistema (véspera às 18:00 e início de pausa às 09:00, esta
    última já reconciliada após reabrir o app); toque na notificação abre
    o app e cancela a notificação (o foco exato na data via deep-link não
    foi observável de forma isolada nesta sessão porque um `force-stop`
    usado num teste de reconciliação também limpou a sessão demo em
    memória — limitação do teste, não do código, que foi conferido por
    leitura); reboot real do emulador confirmou que o `BOOT_COMPLETED`
    reagenda o plano inteiro **sem** o app ter sido aberto manualmente
    (corrige a lacuna do legado).
  - **Web/Chromium** (build de produção `wasmJs`, headless real): bug de
    build corrigido e reverificado com rebuild completo (`BUILD
    SUCCESSFUL`); service worker registrado e ativo com `CACHE_NAME`
    `escala-ici-web-v7` confirmado ao vivo; guarda `chrome-extension://`
    confirmada no arquivo realmente servido; app recarrega e funciona
    100% offline (rede simulada offline via CDP, shell cacheado serve a
    tela de entrada normalmente); manifest PWA válido.
- **Achado real, fora do escopo desta fase, não corrigido aqui**: em uma
  sessão de navegador genuinamente não autenticada (perfil limpo, sem
  sessão MSAL prévia), o botão "AMBIENTE DEMO" de `LoginGateScreen.kt`
  chama o mesmo `signInCorporate()` do botão "MINHA ESCALA", que dispara
  `signInInteractive` (popup real do Microsoft Entra) sempre que
  `corporateAuthState` ainda não é `Authenticated` — e toda a lógica do
  `EntryContext.DEMO` em `App.kt` fica dentro do `if (state is
  CorporateAuthState.Authenticated ...)`, então o Ambiente Demo fica
  inacessível sem completar um login corporativo real antes, contradizendo
  o texto da própria tela ("este modo não depende de vínculo no workspace
  oficial"). Não é regressão desta fase (nenhuma rodada tocou
  `LoginGateScreen.kt`/essa lógica) — toda validação manual anterior
  (14c-5B, 14E) sempre partiu de uma sessão MSAL já restaurada, então esse
  caminho nunca tinha sido exercitado a frio. Por isso a validação manual
  Web desta fase não conseguiu percorrer a tela de notificações/perfil
  autenticada (nunca preenchi credenciais reais no popup — fechado sem
  interação assim que apareceu). Correção mínima sugerida para uma
  próxima rodada dedicada: o clique em "AMBIENTE DEMO" não deveria chamar
  `signInCorporate`/`signInInteractive` nenhuma vez — só `onDemo()`
  diretamente, deixando o branch `EntryContext.DEMO` (que já trata
  identidade ausente) decidir.
- `versionCode`/`versionName`: `27`/`0.7.13` → `28`/`0.7.14`.
- Nenhum dado real (XLS, Firestore de produção) foi versionado ou
  alterado; nenhum deploy ou publicação real de revisão feita nesta
  execução.
- Detalhe completo: `docs/spec/65-ESCALAICI-FASE14H-GATE-CONTRATO-PLANTAO-NOTIFICACOES.md`.

## FASE 14f-3 — Auditoria de contrato: publicacao oficial Dashboard x leitura KMP

- **Status:** DONE (local) — spec 64.
- Comparacao campo a campo entre o que `escala-dashboard` escreve em
  `workspaces/ici-dev/revisions/{n}/{colecao}` e o que o KMP le. Achado
  real corrigido: `corporateLogin` era escrito pelo Dashboard em todo
  membro mas nunca lido no caminho remoto do KMP (`loginByMemberId`
  sempre vazio) - resolucao de identidade por login sempre caia para
  comparar contra o nome de exibicao em vez do login corporativo real.
- Corrigido: `Member.corporateLogin` (novo campo), `toDemoMember()` le o
  campo, `DemoPublicationSnapshot.toData()` constroi `loginByMemberId`
  a partir dele (alinhado com o caminho de fixture, que ja fazia isso
  certo). 2 testes novos, incluindo um teste de contrato ponta a ponta
  que falha sem a correcao.
- Outras divergencias encontradas (nao corrigidas, documentadas na spec
  64 por nao afetarem resolucao de identidade hoje): `teams.acronym`/
  `active` inacessiveis no modelo KMP; 4 de 6 valores de
  `team_manager_assignments.role` colapsam para `OTHER`;
  `schedule_periods.updatedAt` sempre vazio; `entraTenantId`/
  `entraObjectId` do vinculo corporativo nunca sao persistidos pelo
  Dashboard (fallback de email/login ja resolve o caso real).
- Versao: `versionCode` 26->27, `versionName` 0.7.12->0.7.13.

## FASE 14f — MSAL Web/Wasm: autenticacao corporativa real no navegador

- **Status:** DONE (local, com gate externo pendente) — spec 63.
- Login corporativo real (MSAL) implementado no alvo Web/Wasm pela primeira vez.
  `MINHA ESCALA` e `AMBIENTE DEMO` agora aparecem e autenticam no navegador
  quando `auth-config.json.web` esta configurado (ja esta, neste ambiente).
- Decisao de arquitetura documentada com evidencia real: a via preferida
  (`@azure/msal-browser` via dependencia npm + `external class`/`@JsModule`)
  compilou mas falhou em runtime (Promise da fabrica estatica nunca resolvia
  no handler anexado); pivotada para script UMD vendorizado (`window.msal`),
  mesmo padrao ja usado no projeto para `xlsx`/`dropbox-auth.js` - validada
  isoladamente antes de integrar.
- Validado com Chromium real (CDP): popup de login abre com URL correta do
  Entra (tenant/client/scopes reais), UI mostra "Autenticando" corretamente.
  Conclusao real do login interativo (inserir credenciais) depende de uma
  pessoa - gate externo, nao automatizavel.
- Regressao Android completa sem alteracoes em `androidMain`/`identity`/
  `source` - MSAL Android, MINHA ESCALA, AMBIENTE DEMO e Back seguem
  funcionando identicamente.
- Versao: `versionCode` 24->25, `versionName` 0.7.10->0.7.11.
- Pendente (gate externo, fora do escopo desta fase): confirmar cadastro da
  redirect URI de producao HTTPS no Entra (sem dominio definitivo ainda).

## FASE 14f-2 — Correcao pos-validacao humana do MSAL Web (Ktor engine + mensagem)

- **Status:** DONE (local) — spec 63, secao "Validacao humana".
- Login MSAL Web validado por humano real em 2026-07-22 (conta
  `lvergani@ici.tec.br`, redirect URI `http://localhost:8080/` ja cadastrada
  no Entra). Duas falhas reais encontradas nessa primeira validacao (nunca
  exercitadas antes porque, sem MSAL Web, nenhuma leitura remota rodava no
  navegador):
  1. `FirestoreRestGateway` usava `HttpClient(CIO)` fixo em `commonMain` -
     CIO so funciona em JVM/Android, quebrando toda leitura Firestore em
     navegador real (`Node.js net module is not available`). Corrigido:
     `HttpClient()` sem engine explicito, `ktor-client-cio` movido para
     `androidMain`, `ktor-client-js` adicionado a `wasmJsMain`.
  2. 404 no ponteiro do workspace (publicacao oficial ainda inexistente)
     era classificado como `NETWORK_ERROR` por coincidencia textual
     ("indisponivel"), mostrando "verifique sua internet" em vez de "ainda
     nao foi publicada". Corrigido com novo `ScheduleSyncCause.WORKSPACE_NOT_PUBLISHED`.
- Apos a correcao: Ambiente Demo confirmado funcionando no navegador real
  pelo usuario (revisao 3, `objectId` batendo com `allowedDeveloperObjectIds`);
  "Minha Escala" mostra a mensagem correta ("A escala oficial ainda nao foi
  publicada neste ambiente") tanto no Web quanto no Android (emulador).
- Regressao completa: 211 testes unitarios (0 falhas), Android smoke test
  no emulador sem crash, sem alteracao em `identity`/`androidMain` alem do
  necessario para o novo valor do enum compartilhado.
- Versao: `versionCode` 25->26, `versionName` 0.7.11->0.7.12.

## FASE 14E — Estabilizacao Android/Web do Ambiente Demo (insets, dados, alertas, identidade)

- **Status:** DONE (local) — 4 rodadas de correcao (Codex, revisadas e testadas
  independentemente por mim), validacao manual real no emulador com dado remoto
  (`workspaces/demo-v1` revisao 3, Firestore ja ativo), build Web/Wasm validado no
  Chromium real.
- Motivada por teste manual real no checkpoint FASE 14c-5B: titulo/botao Voltar do
  Ambiente Demo sob a status bar, "Equipe nao localizada na escala" sempre aparecendo,
  turno "Comercial" da equipe de Seguranca virando "Turno indefinido" (zerando dias
  trabalhados/horas), "Fonte: Fonte:" duplicado, e mensagens contraditorias de
  identidade no Perfil em modo Demo.
- Insets: `LabPremiumBackground` centraliza `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`;
  `PlantaoScreen`/`ShiftSwapScreen` corrigidos localmente (nao usavam o wrapper
  compartilhado). Nenhuma mudanca de comportamento no Web (insets resolvem para zero
  fora do Android).
- Navegacao: novo `PlatformBackHandler` (`expect`/`actual`, no-op no Wasm) + logica pura
  `decideDemoBackNavigation` — Android Back navega persona -> administracao -> entrada
  sem encerrar a sessao MSAL.
- Causa raiz real (confirmada com leitura direta, somente leitura, da revisao 3):
  `scheduleSummaryForMember` nunca calculava colegas do dia (`ShiftDay.teamMembers`
  sempre vazio) e o mapeador de turno nao reconhecia `shiftName: "Comercial"` (turno
  real e valido da equipe de Seguranca), classificando como `ShiftType.INDEFINIDO`
  (`isWorkShift = false`) e zerando as metricas de dias trabalhados/horas. Corrigido nas
  duas causas; novo `ShiftType.COMERCIAL` (`isWorkShift = true`).
- `sourceFileName` (pensado para "arquivo XLS importado") estava sendo reaproveitado
  para guardar a mensagem de origem remota, causando "Fonte: Fonte: ..." duplicado e
  "Arquivo importado: ..."/"Escala salva apenas neste dispositivo" incorretos para dado
  remoto. Novo campo `ScheduleSummary.remoteSourceLabel`, distinto e correto.
- Perfil em modo Demo agora separa claramente: conta MSAL real autenticada, sessao
  administrativa Demo (papel `DEMO_DEVELOPER`), persona ficticia selecionada — sem
  mostrar a resolucao de vinculo no workspace oficial (irrelevante em modo Demo) como se
  fosse um erro.
- Alertas "Turno indefinido" consolidam em um unico item quando ha mais de 3 dias
  afetados no mesmo resumo (protecao contra spam se um turno desconhecido aparecer numa
  publicacao futura).
- Plantao ganhou um badge visual "DADOS ILUSTRATIVOS" quando nenhum relatorio real foi
  publicado, alem do aviso textual ja existente.
- `versionCode`/`versionName`: `23`/`0.7.9` -> `24`/`0.7.10`.
- Validado: `testDebugUnitTest`, `compileKotlinWasmJs`, `wasmJsTest`,
  `wasmJsBrowserDistribution`, `assembleDebug`, `assembleRelease` — todos
  `BUILD SUCCESSFUL`. Validacao manual real no emulador (screenshots, logcat sem
  erro/crash) e no Chromium real servindo a build de producao Web (sem erro de
  console).
- Limitacao conhecida, nao introduzida nesta fase: MSAL Web nao configurado para
  validacao visual completa das telas autenticadas no navegador local ad-hoc.
- Detalhe completo: `docs/spec/62-ESCALAICI-DEMO-INSETS-QUALIDADE-DADOS-E-WEB.md`.

## FASE 14c-5B — LOGIN/DEMO: causas de erro separadas, autorizacao lvergani e visao administrativa Demo

- **Status:** DONE (local) — codigo, testes automatizados e validacao manual
  no emulador concluidos. Integracao real com `workspaces/ici-dev` continua
  **bloqueada externamente**: o projeto Firebase `escala-ici-dev` retorna
  `403 SERVICE_DISABLED` (Cloud Firestore API/banco `(default)` ainda nao
  ativado no projeto). Confirmado por leitura somente-leitura direta contra
  `workspaces/ici-dev` e `workspaces/demo-v1` no inicio e no fim desta fase,
  sem qualquer mudanca no resultado.
- Teste manual anterior a esta fase (sessao MSAL ja restaurada, conta
  `lvergani@ici.tec.br`) mostrou os dois botoes falhando **depois** da
  autenticacao MSAL, na etapa de autorizacao/resolucao de dados — LOGIN:
  "Publicação corporativa indisponível ou cadastro não localizado."; DEMO:
  "Esta conta não possui acesso ao modo Demo.". Nao era falha de MSAL.
- `ScheduleSyncCause` ganhou o valor `FIRESTORE_DATABASE_DISABLED`, separado
  de `AUTH_REQUIRED`: `classifySyncFailure` detecta os marcadores reais do
  erro (`SERVICE_DISABLED`, "api has not been used", "Cloud Firestore API")
  antes de cair no 403 generico. Mensagem exclusiva ao usuario: "O banco de
  dados Firebase deste ambiente ainda não foi ativado." — nunca misturada com
  cadastro ausente, escala ausente ou acesso negado.
- `LoginGateScreen`/`App.kt` separam as causas antes agrupadas na mensagem
  generica de LOGIN: cadastro nao localizado, membro inativo, identidade
  ambigua, membership sem equipe ativa, equipe nao encontrada, multiplas
  equipes ativas, workspace divergente, Firestore desativado, permissao
  negada, rede indisponivel, dados invalidos, escala/periodo/atribuicoes
  ausentes, publicacao nunca feita neste ambiente.
- `DemoAuthorization.kt` (`isDemoAuthorizedForIdentity`): autoriza por
  `allowedDeveloperObjectIds` (path preferencial, ponteiro remoto) ou, **so
  em build de desenvolvimento**, por e-mail exato normalizado (trim +
  lowercase, sem `contains`/prefixo/dominio) igual a `lvergani@ici.tec.br`.
  No alvo Web/Wasm o fallback de e-mail fica desligado (`isDevelopmentBuild =
  false`), porque o codigo comum nao expõe uma flag debug/release equivalente
  a `BuildConfig.DEBUG` — limitacao documentada, nao e bug.
- Removido o fallback silencioso que, quando a resolucao remota falhava,
  reaproveitava `mockScheduleSummary()` (dados fake de 06–12/07) como se
  fosse uma escala real carregada. `ResolvedScheduleSummaryDecision.kt`
  centraliza essa decisao (usada por LOGIN e DEMO): sem publicacao real,
  mostra erro honesto ("Escala ativa não encontrada para esta conta."), nunca
  dado inventado.
- **Mudanca de produto no DEMO**: a conta autenticada (`lvergani`) deixou de
  ser resolvida direto para a persona fixa `DemoPersonaCatalog.personas[2]`
  ("Gestor de Segurança Demo", que nunca tem `scheduleAssignments` na fixture
  — so aparece em `team_manager_assignments`). "AMBIENTE DEMO" agora abre uma
  visao administrativa minima do workspace `demo-v1` (`DemoWorkspaceOverviewScreen`,
  papel `DEMO_DEVELOPER`): mostra a identidade autenticada separada da
  persona, origem da publicacao (remoto/fixture) + revisao + causa de
  fallback, contagem de membros, periodo ativo e as equipes. A partir dela,
  o usuario escolhe internamente uma persona operacional **com**
  `scheduleAssignments` (hoje: Analista SOC Demo 1, Analista de Segurança
  Demo 1) para "ver como" — a persona gestora, sem atribuicoes, nao aparece
  nessa lista.
- Textos dos botoes trocados para reduzir a impressao de "logar de novo":
  LOGIN → "MINHA ESCALA", DEMO → "AMBIENTE DEMO". A tela ja mostra a conta
  corporativa autenticada acima dos botoes.
- `versionCode`/`versionName`: `22`/`0.7.8` → `23`/`0.7.9`.
- Testes novos/ajustados: `ScheduleSyncCauseTest` (causa dedicada +
  regressao garantindo que "not configured" generico continua
  `AUTH_REQUIRED`), `DemoAuthorizationTest` (e-mail exato normalizado
  autorizado/rejeitado, objectId autorizado, fallback desligado fora de
  debug), `RemoteFirstDemoMemberDirectoryRepositoryTest` (parametros
  Entra propagados), `DemoPublicationResolverTest` (autorizacao com
  ponteiro remoto indisponivel cai em fixture, nao em acesso negado),
  `ResolvedScheduleSummaryDecisionTest` e `DemoWorkspaceOverviewDecisionTest`
  (visao administrativa nunca finaliza em mock nem em erro de escala pessoal
  da persona gestora; identidade `lvergani`/papel `DEMO_DEVELOPER` nunca e
  tratada como persona).
- `testDebugUnitTest`, `compileKotlinWasmJs`, `wasmJsTest` (Chromium
  headless), `wasmJsBrowserDistribution`, `assembleDebug`, `assembleRelease`
  e os 6 testes de regras Firestore (`firebase/test/firestore.rules.test.mjs`,
  regras nao alteradas nesta fase): todos `BUILD SUCCESSFUL`/verde.
- Validacao manual no emulador (`EscalaSOC_API_37`), com a sessao MSAL ja
  restaurada preservada (nenhum logout/reinstall destrutivo): LOGIN mostra a
  mensagem dedicada de Firestore desativado; DEMO abre a visao
  administrativa (origem "fixture local", causa `FIRESTORE_DATABASE_DISABLED`)
  e "Ver como" carrega a escala real da persona (periodo 26/07–25/08/2026,
  turnos reais) — nao mais o mock antigo de 06–12/07. Sem crash, sem
  travamento, logcat sem erro.
- Achado de processo (nao e bug do app): o Kotlin Language Server 1.3.13 do
  VS Code (extensao `fwcd.kotlin`) ficou com uso de memoria descontrolado
  (8+ GB, 200%+ CPU) durante indexacao e foi apontado como fator relevante
  no encerramento inesperado do VS Code por OOM killer na sessao anterior.
  Nao e falha do Gradle/app — `testDebugUnitTest` e `assembleDebug` passam
  normalmente fora do VS Code.
- Bloqueio externo desta fase (Codex): a ferramenta `codex exec` usada como
  executor de codigo atingiu o limite de uso da conta durante a rodada de
  correcao de 3 erros de compilacao (retomar somente em 25/07/2026); esses 3
  erros — todos de tipagem/nulidade, sem decisao de arquitetura — foram
  corrigidos diretamente, com resultado revisado e validado pela mesma
  suite de testes acima.
- Detalhe completo: `docs/spec/61-ESCALAICI-LEITURA-DEMO-PUBLICACAO-ATIVA.md`
  (secao FASE 14c-5B).

## FASE 14c-5A — Leitura somente-leitura da publicacao Demo ativa

- **Status:** DONE — implementacao KMP comum, testes com fakes, Web/Wasm
  compilado e fallback local preservado.
- Novo contrato remoto Demo: o app resolve `workspaces/demo-v1`, le somente
  `workspaces/demo-v1/revisions/{publicationRevision}/{collection}` e valida
  `workspaceId`/`publicationRevision` em todas as entidades antes de montar
  um snapshot de dominio.
- `DemoPublicationResolver` descarta qualquer leitura parcial, reconsulta o
  ponteiro no fim e faz retry unico se a revisao mudar durante a leitura. Uma
  segunda mudanca retorna erro controlado.
- `DemoPublicationRepository` compoe leitura remota com `DemoFixtureCache`:
  qualquer erro de rede, permissao ou formato cai para a fixture local sem
  apagar snapshot consistente. A origem exibida no contexto Demo passa a ser
  "Demo remoto rev. N" ou fixture local.
- Modelos puros receberam `publicationRevision` aditivo; novos modelos puros
  representam ponteiro de publicacao, vinculo de gestor e solicitacao de
  alteracao.
- Limitacao conhecida: as regras reais deste repo ainda negam `workspaces/**`,
  portanto a leitura remota real deve cair em `PERMISSION_DENIED` ate uma fase
  externa atualizar/deployar regras. Esta fase nao altera
  `firebase/firestore.rules`.
- `versionCode`/`versionName`: `21`/`0.7.7` → `22`/`0.7.8`.
- Detalhe completo: `docs/spec/61-ESCALAICI-LEITURA-DEMO-PUBLICACAO-ATIVA.md`.

## FASE 14c-2 — Fixtures oficiais do workspace demo-v1

- **Status:** DONE — geração determinística, integração Kotlin, revisão
  independente (Codex), validação manual em Android (emulador) e Web
  (Chromium headless via CDP), bump de versão e commit/push concluídos.
- Revisão independente (Codex) encontrou 1 achado IMPORTANTE, corrigido:
  `DemoPersona.kt` ainda tinha um campo `shortDescription` (nunca
  renderizado na UI) citando nome de equipe/papel — duplicava dado que já
  vem da fixture. Campo removido; as outras 16 checagens vieram OK.
- Validação manual confirmou nas 3 personas, em Android e Web: Analista
  SOC Demo 1 → SOC Demonstração; Analista de Segurança Demo 1 → Segurança
  da Informação Demonstração; Gestor de Segurança Demo → equipe única
  Segurança da Informação Demonstração (sem `MultipleActiveTeams`, o
  objetivo central desta fase). Sessão MSAL corporativa restaurada
  silenciosamente após force-stop, sem impacto.
- Substitui o catálogo Demo hardcoded da FASE 14c-1
  (`DemoOrganizationData.kt`, removido) por uma fonte única de verdade:
  `fixtures/demo/demo-v1-seed.json` (seed compacto, escrito à mão) →
  `scripts/generate_demo_v1.py` (gerador Python 100% determinístico, sem
  `datetime.now()`/`random`/`uuid`) → pacote completo em
  `composeApp/src/commonMain/composeResources/files/demo/demo-v1-publication-package.json`
  (recurso Compose Multiplatform) + `fixtures/demo/demo-v1-manifest.json`
  (contagens + SHA-256) → `scripts/validate_demo_v1.py` (schema + 17
  invariantes de negócio/isolamento).
- Cenário fixo: 2 times (SOC Demonstração, Segurança da Informação
  Demonstração), 5 membros, 5 vínculos de pertencimento (um time pessoal por
  membro, inclusive o gestor), 2 vínculos de gestão (mesmo gestor
  administrando as duas equipes só via `team_manager_assignments`, nunca por
  um segundo membership), período 26/07/2026–25/08/2026, 124
  `scheduleAssignments` (SOC rotativo 6x1, nunca mais de 6 dias seguidos de
  trabalho; Segurança comercial, nunca fim de semana), 3 solicitações de
  alteração (pendente/aprovada/recusada) e 1 registro de publicação
  (revisão 1). Diferente da FASE 14c-1, o gestor resolve para uma única
  equipe pessoal na fixture oficial (nunca `MultipleActiveTeams`) — esse
  estado continua coberto por um teste sintético dedicado.
- Contrato JSON (spec 57) estendido de forma aditiva com `schedulePeriod`,
  `scheduleAssignment` e `publicationRecord`.
- Achado de ambiente (não é bug, é limitação real): `Res.readBytes`
  (Compose Resources) funciona no app real (Android/Web), mas não em
  nenhum alvo de teste automatizado desta sessão (`IllegalStateException`
  em `testDebugUnitTest`, `MissingResourceException` em `wasmJsTest`) —
  confirmado com um teste "spike" descartável. Por isso a suíte separa
  lógica pura de parsing (`commonTest`, JSON inline) da leitura do arquivo
  real (`androidUnitTest`, via `java.io.File`, JVM puro).
- `testDebugUnitTest`: **153 testes, 0 falhas** (era 148 antes desta fase).
  `assembleDebug`/`assembleRelease`/`wasmJsBrowserDistribution`/`wasmJsTest`:
  todos `BUILD SUCCESSFUL`.
- Determinismo comprovado: duas execuções independentes do gerador
  produziram o pacote byte a byte idêntico (`cmp`); validador confirmou 0
  erros de schema e todas as 17 invariantes.
- Nenhuma escrita no Firebase nesta fase — publicação real fica para FASE
  14c-4 em diante (spec 58).
- Detalhe completo: `docs/spec/60-ESCALAICI-FIXTURES-OFICIAIS-DEMO-V1.md`.

## FASE 14c-1 — Resolução de identidade MSAL/Demo → member/team

- **Status:** DONE — código real, testado automaticamente e validado
  manualmente em emulador real (ver abaixo).
- Novo pacote `br.com.leorvergani.escalaici.identity` (commonMain): resolve
  `CorporateIdentity` (MSAL, já validada) ou um personagem do novo catálogo
  Demo (3 personas 100% fictícias, domínio `example.invalid`) em `member` →
  `member_team_memberships` (existia só como modelo puro desde a FASE 12b,
  agora consultado de verdade pela primeira vez) → `team`, sempre isolado por
  workspace (`ici`/`demo-v1`, nunca misturados, nunca fallback silencioso de
  um para o outro).
- Correspondência de identidade sempre exata (e-mail/UPN/login normalizados
  — trim+lowercase), nunca aproximada. 9 estados tipados de resultado
  (`Resolved`, `MemberFoundNoActiveTeam`, `MemberNotFound`, `MemberInactive`,
  `MemberIdentityAmbiguous`, `MembershipNotFound`, `TeamNotFound`,
  `MultipleActiveTeams`, `WorkspaceMismatch`, `DataSourceUnavailable`) — spec
  59 detalha cada um e a mensagem amigável correspondente.
- `Member`/`Team`/`MemberTeamMembership` ganharam campo aditivo
  `workspaceId: String? = null` (nenhum call site existente quebrado).
- UI: `LoginGateScreen` ganhou o seletor "Testar como" (3 personas Demo),
  sempre rotulado "AMBIENTE DE DEMONSTRAÇÃO", convivendo sem se confundir
  com o diálogo pré-existente "Login de teste" (que não foi alterado);
  `ProfileTab` mostra equipe/função reais quando resolvido, ou uma mensagem
  específica por estado (nunca uma mensagem genérica única).
- Achado colateral corrigido: `model/AppVersion.kt` estava desatualizado
  (`18`/`0.7.4`) desde o hotfix da spec 55 (`versionCode`/`versionName` reais
  já eram `19`/`0.7.5` no Gradle) — sincronizado no bump de versão desta
  fase.
- 29 testes novos (`commonTest/.../identity/`) cobrindo normalização,
  resolução de membro/membership/time, isolamento de workspace (com
  repositórios que contam chamadas, provando zero cruzamento
  corporativo↔Demo) e cache. Suíte completa: **148 testes Android, 0
  falhas** + **144 testes Wasm/Chromium, 0 falhas** (`wasmJsTest`, gap da
  spec 55 fechado nesta sessão).
  `:composeApp:assembleDebug`/`assembleRelease`/`wasmJsBrowserDistribution`
  também `BUILD SUCCESSFUL` (release verificado com `apksigner`, mesma
  keystore/identidade da spec 52).
- **Validação manual real no emulador** (`EscalaSOC_API_37`, Android 17):
  headless (`-no-window`) crashava consistentemente (`qemu-system-x86`
  segfault, `systemd-coredump`); resolvido rodando com o display real da
  sessão (sem `-no-window`) e `-gpu host` (GPU AMD real, em vez de
  swiftshader) — causa raiz era o backend gráfico, não a ausência de janela.
  Com uma sessão MSAL corporativa real já restaurada
  (`lvergani@ici.tec.br`): fluxo corporativo mostrou corretamente
  `MemberNotFound` (diretório honesto/vazio nesta fase), restauração
  silenciosa confirmada (force-stop + relançamento), sem crash/logout/
  fallback; fluxo Demo validado nos 3 personagens (2 resolvidos com equipe
  correta, o Gestor em `MultipleActiveTeams` por desenho); diálogo legado
  "Login de teste" confirmado intacto. **Dois bugs reais de alcançabilidade
  de UI encontrados e corrigidos** por este teste manual (não capturáveis
  pelos 148 testes automatizados): o seletor "Testar como" e a seção de
  resolução Demo em `ProfileTab` só apareciam dentro de
  `CorporateAuthState.Demo`, inatingível em qualquer dispositivo com MSAL
  configurado — corrigido para aparecer sempre que há personagem
  selecionado, independente do estado corporativo. `wasmJsTest` também
  passou a funcionar nesta sessão após o usuário instalar Chromium
  (Flatpak) e apontar `CHROME_BIN` — fecha o gap da spec 55.
- **Revisão independente final (Codex)** encontrou mais dois efeitos
  colaterais da correção acima, ambos corrigidos: `selectedDemoPersona`
  ainda era limpa sempre que `corporateAuthState != Demo` (quase sempre
  verdadeiro após a correção), podendo apagar a persona escolhida sozinha
  após a restauração silenciosa — removida essa limpeza automática; e
  `Main.kt` (Web) não recebia nenhum `organizationIdentityResolver`, então
  uma seleção Demo lá nunca resolvia — passou a receber o mesmo resolver
  honesto/vazio do Android. Suíte completa (148+144 testes) e
  `assembleDebug`/`assembleRelease`/`wasmJsBrowserDistribution`/`wasmJsTest`
  reexecutados com sucesso após as duas correções.
- `versionCode`/`versionName`: `19`/`0.7.5` → `20`/`0.7.6`. APK de release
  gerado e copiado para `EscalaICI-latest.apk`; `version.json` local
  (campos `kmp*`) atualizado. Upload para o Dropbox continua manual.
- Não implementa ainda: responsáveis por equipe, solicitações de alteração,
  Dashboard, publicação, escrita no Firestore, Firebase Authentication, área
  do gestor — tudo isso continua na spec 58 (FASE 14c-2 em diante). Nenhuma
  alteração em Gradle além do bump de versão, Firebase Rules, Dashboard,
  Entra ou parser XLS.
- Detalhe completo: `docs/spec/59-ESCALAICI-RESOLUCAO-MSAL-DEMO-MEMBER-TEAM.md`.

## FASE 14c-0 — Contrato mestre de responsáveis, aprovações e workspace de demonstração

- **Status:** DONE — documentação apenas, nenhum código funcional alterado.
- Três specs novas em `docs/spec/`: `56-GESTAO-DE-RESPONSAVEIS-E-APROVACOES.md`
  (papéis `PRIMARY_MANAGER`/`PRIMARY_APPROVER`/`BACKUP_APPROVER`/
  `SCHEDULE_EDITOR`/`PUBLISHER`/`VIEW_ONLY`, permissões independentes do
  papel, substituição temporária por vigência, modo local de teste sem MSAL
  no Dashboard, e um **ambiente de demonstração** oficial — `workspace`
  `PRODUCTION`/`DEMO` — para validar o fluxo completo Dashboard → Firebase →
  app sem tocar em dados reais), `57-MODELO-FIRESTORE-ORGANIZACAO-E-SOLICITACOES.md`
  (audita o schema universal já existente antes de propor algo novo; conclui
  que `member_team_memberships` já cobre o vínculo membro↔equipe — não cria
  `member_team_assignments`; define como novas apenas `team_manager_assignments`,
  `schedule_change_requests`, `workspaces` e `publication_records`, todas
  aditivas, com campo `workspaceId` opcional estendido às coleções
  existentes), `58-ROADMAP-RESPONSAVEIS-SOLICITACOES-E-AREA-GESTOR.md`
  (FASE 14c-1 a 14c-10, cenário de aceitação ponta a ponta `DEMO-E2E-001`).
- Novo `docs/contracts/organization-approval-v1.schema.json` (JSON Schema
  Draft 2020-12): valida a forma de `workspace`/`teams`/`members`/
  `memberTeamMemberships`/`teamManagerAssignments`/`scheduleChangeRequests`
  com dados só fictícios (domínio `example.invalid`); validado
  estruturalmente com `jsonschema` (Python) além de `json.tool`, incluindo 2
  casos negativos (recusa sem justificativa, workspace `DEMO` com efeitos
  externos habilitados) confirmados como rejeitados pelo schema.
- Índice `docs/SPECS-ESCALAICI.md` atualizado com as specs 56-58 e a
  subdivisão da FASE 14c em 14c-0..14c-10.
- Preservado sem alteração: hierarquia de Segurança (ICI→DIO→GEDSI→COSI)
  separada de Service Desk (ICI→DIO→GST→CCS→SD), já confirmada distinta no
  seed universal existente; códigos `M1`-`M4` continuam exclusivos do perfil
  N1 (`ActivityCode`/`ScheduleProfile`, não tocados); nome visível
  `Escala ICI`; nenhuma coleção existente removida/renomeada; nenhuma
  Firebase Authentication, Cloud Function ou regra nova; MSAL Android
  validado nas FASES 14b-1/14b-1a/hotfix intocado.
- Branch documental própria (`feature/fase-14c-gestao-responsaveis-aprovacoes`,
  nascida de `fix/fase-14b1c-msal-runtime-linux`, já com a validação MSAL
  Android real) — sem merge, sem alteração de Gradle/versão/dashboard/app/
  parser/autenticação.
- Próximo passo: FASE 14c-1 (spec 58), identidade MSAL/Demo resolvendo
  member/team no app.

## FASE 14a.1 — Endurecimento emergencial das regras do Firestore

- **Status:** DONE — regras e testes prontos e passando no Emulator; **não
  implantada em produção** (deploy é ação humana, documentada mas não
  executada por esta sessão).
- Motivação: as regras hoje publicadas em produção liberam leitura e
  escrita totalmente anônimas até 2026-08-04 (ver FASE 14a). Antes de
  esperar pela solução definitiva (MSAL + `user_links` + Firebase Auth no
  KMP, FASE 14d), esta correção emergencial elimina o risco mais grave:
  escrita anônima em qualquer coleção.
- `firebase/firestore.rules` reescrita: nenhuma escrita anônima em nenhuma
  coleção; leitura anônima mantida apenas nas 6 coleções que
  `FirestoreRestGateway.kt` já lê hoje sem sessão (`teams`, `members`,
  `schedule_periods`, `schedule_assignments`, `oncall_periods`,
  `oncall_assignments`); `system_admins`, `user_links`, `source_files`,
  `import_jobs` e demais coleções administrativas nunca públicas; coleções
  desconhecidas negadas tanto para leitura quanto para escrita (corrige uma
  lacuna real do rascunho anterior, que permitia leitura de qualquer nome
  de coleção para qualquer usuário autenticado).
- Achado técnico importante: testado no Emulator que uma regra que só
  libere leitura anônima de documentos `active == true` faz o Firestore
  **rejeitar a listagem inteira** (não apenas filtrar), porque o cliente
  KMP hoje lista sem nenhum `where`. Como esta fase não altera Kotlin, a
  troca (manter a leitura anônima no nível atual, incluindo documentos
  inativos/rascunho, versus quebrar a leitura do KMP agora) foi apresentada
  explicitamente ao usuário, que confirmou manter a leitura no nível atual.
  Detalhe completo, matriz de acesso e riscos residuais em
  `docs/spec/51-ESCALAICI-FIRESTORE-HARDENING-TRANSITORIO.md`.
- 22 testes no Firestore Emulator (`cd firebase && npm run test:rules`),
  cobrindo: nenhuma escrita anônima, leitura mínima preservada, coleções
  sensíveis sempre bloqueadas, coleções desconhecidas negadas, isolamento
  entre equipes, e que a autorização não depende de campos enviados pelo
  próprio cliente.
- Não implementa MSAL, `user_links` ou sincronização real — isso continua
  nas FASES 14b-14e. Não altera Kotlin, Gradle, manifest, `applicationId`,
  dependências ou o snapshot histórico da regra de produção.

## FASE 14a — Auditoria e specs finais de autenticação, sincronização, pausa e migração

- **Status:** DONE — documentação/auditoria apenas, nenhum código funcional
  alterado.
- Auditoria de leitura (três frentes paralelas, somente leitura, nenhuma
  alteração) deste repositório, do `EscalaSOC` (referência) e do Dashboard
  (referência), consolidada em relatório interno não versionado
  (`.ai-runs/fase14a-diagnostico/DIAGNOSTICO-FASE14A.md`).
- Achados centrais confirmados por leitura direta de código: (1) o texto
  "Associação corporativa ainda não configurada" em `ProfileTab.kt` é
  estático, sempre exibido, sem nenhuma condição real; (2) o login atual é
  inteiramente mockado (`LoginGateScreen.kt`), com `"lvergani"` como membro
  padrão de mock; (3) o botão/toggle de pausa é `enabled = false` fixo no
  código-fonte, sem nenhuma lógica de habilitação; (4) a leitura Firestore
  atual busca coleções inteiras e filtra no cliente, sem query real; (5) as
  regras Firestore hoje publicadas em produção liberam leitura e escrita
  totalmente livres até **4 de agosto de 2026**, sem que nenhum cliente
  (Android legado ou KMP) tenha ainda uma sessão Firebase Auth real.
- Cinco specs novas criadas em `docs/spec/`: `46-ESCALAICI-AUTENTICACAO-CORPORATIVA-MSAL-FIREBASE.md`,
  `47-ESCALAICI-VINCULO-USUARIO-MEMBRO-E-TIME.md`,
  `48-ESCALAICI-SINCRONIZACAO-ESCALA-CACHE-OFFLINE.md`,
  `49-ESCALAICI-PAUSA-15-MINUTOS-E-NOTIFICACOES.md`,
  `50-ESCALAICI-MIGRACAO-FINAL-E-PARIDADE.md` — decisões arquiteturais,
  estados tipados, matriz de paridade e critérios de aceite para as fases
  seguintes. Índice atualizado em `docs/SPECS-ESCALAICI.md`.
- Revisão independente (subagente sem memória desta sessão) das 5 specs
  quanto a contradições internas, completude e coerência com o código
  auditado — ver seção correspondente no relatório final da fase.
- Não implementa MSAL, vínculo de usuário, sincronização real, pausa
  funcional ou qualquer alteração em Kotlin, Gradle, manifest, Firebase
  Rules/Functions, Dashboard ou EscalaSOC. Sequência de implementação:
  FASE 14b (MSAL/identidade) → 14c (`user_links`) → 14d (Firebase Auth +
  Rules, antes de 2026-08-04) → 14e (sincronização) → 14f (pausa/notificações)
  → 14g (paridade) → 14h (release candidato).

## KMP-MVP-1A — Contrato universal de fontes

- **Status:** DONE
- Contratos puros em `commonMain` para escala e plantão, com metadados, estados de carregamento e invalidação independente.
- Prioridade testável: remoto válido, cache remoto, arquivo local confirmado, cache local e demonstração.
- Cache Web/Android atual adaptado sem migração ou alteração de parser.
- Firebase, MSAL, OneDrive, Dropbox novo e iOS não foram implementados.
- Próxima fase recomendada: KMP-MVP-1B, adaptador Firebase/Firestore em modo controlado atrás dos contratos.

**Nota (FASE 12a):** existe uma spec separada, ainda não implementada, sobre
generalizar o Escala ICI para múltiplos setores do ICI (não só COSI/SOC) e
tipos de escala configuráveis por equipe — ver
`EscalaSOC/docs/spec/34-ESCALAICI-UNIVERSAL-SETORES-E-TIPOS-DE-ESCALA.md`.

## FASE 12c — Pacote de specs do Escala ICI

- **Status:** DONE — specs
- Criado no repositório irmão `EscalaSOC` o pacote completo de specs 35-45,
  cobrindo Firestore universal, dashboard, importadores, leitura no app,
  MSAL/Entra, PWA/iOS, release/update, migração, cards UI/UX, testes e
  roadmap até o MVP oficial.
- Índice local resumido neste repositório: `docs/SPECS-ESCALAICI.md`.
  Conteúdo completo: `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/`.
- Esta fase é documental: ainda não implementa schema Firestore real,
  dashboard universal, leitura universal no app, parsers novos, MSAL real ou
  release.

## FASE 12a-2 — Corrige crash (OutOfMemoryError) no download de atualização

- **Status:** DONE
- Bug real reportado pelo usuário: app fechava sozinho ao baixar a v0.6.1.
  Causa confirmada por reprodução no emulador (logcat): download do APK
  carregava os 66MB inteiros num `ByteArray` (Ktor `response.body()`),
  estourando o heap (`OutOfMemoryError`, não capturado por
  `catch (Exception)`). Corrigido para download em streaming direto ao
  disco + `catch (Throwable)`, mesmo padrão do app oficial. Ver detalhe
  completo no `README.md`, seção "Validação da FASE 12a-2".
- Bug adicional encontrado e corrigido: `AppVersion.CODE`/`LABEL` não
  tinham sido atualizados na FASE 12a-1 (ficaram em `10`/`0.6.0`).
- `versionCode`/`versionName`: `11`/`0.6.1` → `12`/`0.6.2`. APK de release
  gerado e copiado para `EscalaICI-latest.apk`; falta só o usuário subir
  manualmente pro Dropbox.

## FASE 12b-3 — Limpeza visual de nomenclatura: Escala ICI

- **Status:** DONE
- Remove "KMP"/"exemplo"/"demo" sem contexto de todo texto visível ao
  usuário (label do app Android, título web/PWA, `manifest.json`, 12
  textos de tela em `TodayTab`/`AlertsTab`/`ImportTab`/`ScheduleTab`/
  `ProfileTab`/`PlantaoScreen`) — o app aparece só como **Escala ICI**
  para quem usa. `EscalaSOC` continua o nome do app Android antigo/oficial.
- Nenhum aviso de dado não-real foi escondido, só reescrito em linguagem
  neutra ("Nenhuma escala importada", "Sem plantão publicado", etc.). Ver
  tabela completa no `README.md`, seção "Validação da FASE 12b-3".
- Nenhum nome de arquivo/classe/identificador renomeado (`LabCard`,
  `LabColors`, pacote `...kmp.lab`, etc.) — fora de escopo desta fase.
- `applicationId`/`versionCode`/`versionName` mantidos
  (`br.com.leorvergani.escalaici.kmp.lab` / `12` / `0.6.2`). Sem build de
  release, sem `version.json`, sem Dropbox.

## FASE 12b-2 — Card configurável de atividade/cargo por equipe

- **Status:** DONE
- `ScheduleUiConfig` novo (`showActivityCodeCard`, `showRoleLabel`,
  `activityCardTitle`) em `ScheduleProfile.uiConfig` — o card de
  atividade/código só aparece por equipe/perfil se habilitado, nada
  hardcoded para N1 no código do app.
- `ActivityCode` ganhou `visibleInApp`/`cardTitle`/`sortOrder`; `Role`
  ganhou `roleShortName`/`roleDisplayName` (nullable, com fallback) +
  `formatMemberWithRole()` (função pura, ainda sem UI real).
- Confirmado nos mocks e em teste: `M1`-`M4`/`E`/`G`/`T`/`F`/`X`/`AUS` só
  existem amarrados ao perfil N1 (`scheduleProfileId =
  "profile-n1-matrix"`), nunca ao SOC. SOC/Administrativo com
  `showActivityCodeCard = false`; N1 com `true`.
- Ver detalhe completo no `README.md`, seção "Validação da FASE 12b-2".
- Não mexeu em Firebase, Firestore Rules, dashboard, MSAL, parsers,
  update de APK, Dropbox ou scripts de release.
- `versionCode`/`versionName`: mantidos (`12`/`0.6.2`) — sem build de
  release, sem `version.json`, sem Dropbox.

## FASE 12b — Modelos universais de organização e escala no commonMain

- **Status:** DONE
- Primeiro código real da série `FASE 12.x` (spec 34 acima). Novo arquivo
  `model/UniversalOrgModels.kt`: `Organization`, `OrgUnit`/`OrgUnitType`,
  `Role`, `MemberTeamMembership`, `ScheduleProfile`/`ScheduleProfileType`/
  `SchedulePeriodMode`, `ActivityCode`/`ActivityCodeType`,
  `BusinessHoursRule`, `TwelveByThirtySixRule` — todos aditivos, nenhum
  modelo existente (`Team`, `Member`, `SchedulePeriod`, `ScheduleAssignment`,
  `OnCallPeriod`, `OnCallAssignment`, `ShiftType`) foi alterado.
- Mocks novos em `MockSchedule.kt` (ICI/GEDSI/COSI/N1, equipes SOC/N1, os 11
  códigos reais da equipe N1, 3 `ScheduleProfile`) — ainda não conectados à
  UI, mesmo padrão dos mocks puros da FASE 9c.
- 5 testes novos em `UniversalOrgModelsTest.kt` — ver detalhe completo no
  `README.md`, seção "Validação da FASE 12b".
- Não mexeu em Firebase, Firestore Rules, dashboard, MSAL, parsers
  (SOC/Plantão), UI, update de APK, Dropbox ou scripts de release.
- `versionCode`/`versionName`: mantidos (`11`/`0.6.1`).

## FASE 11.0 — Fundação de rede (Ktor)

- **Status:** DONE
- Nota: abre a série `FASE 11.x` — integrações reais (Dropbox, parser XLS,
  MSAL) do app Android de produção `EscalaSOC`. Plano completo e estado
  atual para retomada por outra IA/sessão em
  `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`.
- Adicionado `io.ktor:ktor-client-core` + `io.ktor:ktor-client-cio` em
  `commonMain` (engine CIO cobre Android/Web/Native/JVM com uma única
  dependência, sem `expect/actual` de engine).
- `ktor = "3.5.1"` inicial quebrou o build Web/Wasm (stdlib não resolvida) —
  downgrade para `ktor = "3.3.0"` (mesma linha do Kotlin 2.2 do projeto)
  resolveu.
- Nova regra: `versionCode`/`versionName` do lab sobem a cada sub-fase
  (`1`/`0.1.0-lab` → `2`/`0.1.1-lab`).
- Build Android + Web/Wasm + `testDebugUnitTest` verificados.

## FASE 11.2e — Release local padronizado + upload sempre manual

- **Status:** DONE
- Padroniza o processo de release do EscalaICI: pasta oficial
  `/home/lvergani/Downloads/dropbox_update_scripts` (mesma do EscalaSOC),
  APK sempre nomeado `EscalaICI-latest.apk`, `version.json` compartilhado
  (campos `kmp*`, FASE 11.2d).
- Novo script `gerar_update_escalaici_local.sh` (fora deste repositório,
  na pasta de release) — copia o APK, confere o `version.json`, imprime
  instruções de upload manual, **nunca chama a API do Dropbox**. Scripts
  antigos do EscalaSOC ganharam avisos deixando claro que não servem para
  o EscalaICI.
- Confirmado via `apksigner verify`: build limpa continua assinada com a
  mesma keystore de sempre (`escalaici-kmp-lab.jks`, FASE 11.0c). APK
  release reinstalado por cima do debug no emulador sem precisar
  desinstalar — prova que a mesma chave já cobre debug+release.
- `versionCode`/`versionName`: mantidos em `10`/`0.6.0` (sem mudança de
  código, só padronização de processo).
- Evidência: seção "Validação da FASE 11.2e" no `README.md`.

## FASE 11.2d — Atualização real do app via Dropbox

- **Status:** DONE no código; **PENDENTE** confirmação de ponta a ponta
  (depende do usuário adicionar os campos `kmp*` no `version.json`
  existente do app oficial, ver `docs/PENDENCIAS-EXTERNAS.md`).
- Botão "Atualizar aplicativo" (Perfil) real: verifica, baixa e instala
  APK novo, reaproveitando o mesmo `version.json` do app Android oficial
  (`DropboxCloudConfig.APP_UPDATE_MANIFEST_URL`) com campos novos
  (`kmpVersionCode`/`kmpVersionName`/`kmpApkUrl`/`kmpChangelog`) —
  confirmado que o parser oficial (`org.json.JSONObject` manual) ignora
  chaves desconhecidas, então isso não quebra o app oficial.
- Porte fiel do `AppUpdateManager.kt` real: mesmo algoritmo (baixa
  manifesto → compara versionCode → checa permissão "instalar apps
  desconhecidos" → baixa APK pro cache → abre instalador via
  `FileProvider`/`ACTION_VIEW`). Web retorna `NotSupported` (sem conceito
  de instalar APK no navegador).
- Novo `AndroidManifest.xml`: `REQUEST_INSTALL_PACKAGES` + `FileProvider`
  + `res/xml/file_paths.xml`.
- Testado no emulador com a rede real: buscou o `version.json` de
  produção de verdade, não achou os campos `kmp*` ainda (esperado) e
  mostrou "Você já está usando a versão mais recente." — confirma a
  chamada de rede, o parser e a UI funcionando ponta a ponta.
- APK de release gerado, copiado para
  `~/Downloads/EscalaICI-KMP-Lab-latest.apk`.
- Evidência: seção "Validação da FASE 11.2d" no `README.md`.
- `versionCode`/`versionName`: `9`/`0.5.0` → `10`/`0.6.0`.

## FASE 11.2c — Importação real de Plantão + mais correções de fidelidade

- **Status:** DONE
- Novo `model/PlantaoWorkbookParser.kt`, regras portadas exatamente do
  parser oficial (`PlantaoWorkbookParser.kt`, `EscalaSOC`): busca de
  cabeçalho por colunas plantonista/data início/data fim em qualquer aba,
  regex de data+hora idêntica, mesmas 3 validações de linha (vazia
  ignorada, incompleta com aviso, fim≤início com aviso), mesma ordenação,
  mesmo erro exato quando nada é encontrado. Botão "Importar relatório" na
  tela Plantão (mesmo seletor de arquivo real já usado pela escala). 7
  testes novos.
- Nova âncora de "hoje" real (`platform/CurrentDate.kt`,
  `todayLabDate()`, Android `java.time.LocalDate.now()` / Web `new Date()`
  via JS): `nextShift`/`nextRest` e a seleção inicial da aba Escala agora
  refletem a data real do dispositivo, não mais o primeiro dia da lista
  importada — consistente entre as abas Hoje e Escala.
- Resumo do período agora filtra os dias para o ciclo fixo 26→25 (ancorado
  em "hoje" real) antes de contar trabalho/folga/horas — proteção extra
  que o app real não tem explicitamente (confia na leitura fixa de 30
  linhas), sem mudar o resultado do arquivo real de hoje.
- 6 sugestões de horário de pausa (a cada 30min, igual ao app real) em vez
  de 1 fixo + botão desabilitado.
- Ícone do clima trocado de emoji para `Icon` vetorial (emoji podia não
  renderizar no Compose Web/Wasm, sem fonte de emoji colorida por
  padrão).
- Logo do cabeçalho (`SocLogo.kt`/`LabShieldLogo`) trocado do escudo
  azul/roxo com "S" (porte literal do app real) para um mini calendário
  sem letra, já que este app representa o ICI inteiro agora, não só o SOC
  — mesmo motivo/estilo do ícone do launcher da FASE 11.0b.
- Testado: 28 testes (0 falhas) + validação manual completa no emulador.
- Evidência: seção "Validação da FASE 11.2c" no `README.md`.
- `versionCode`/`versionName`: `8`/`0.4.1` → `9`/`0.5.0`.

## FASE 11.2b — Fidelidade visual/funcional ao app Android real

- **Status:** DONE
- Auditoria tela a tela contra o app real (`EscalaSOC`, só leitura)
  encontrou 4 cards genuinamente extras (sem equivalente real) e uma
  divergência de cálculo, ambos apontados pelo usuário:
  - Removidos: `"Migração KMP"` (Perfil), `"Dia sem escala"` + `"Lista do
    mês"` + lista dia-a-dia (`ShiftDayRow`) inteira (Escala — principal
    fonte de cards extras), aviso `"Dados de plantão (exemplo)"`
    (Plantão).
  - Corrigido: `"Janela permitida"` da pausa agora usa a fórmula real do
    app (`PauseWindow.kt`: início do turno +120min a +285min) por tipo de
    turno, em vez de repetir o texto de offset fixo. Novo
    `ScheduleSummary.pauseWindowStart`/`pauseWindowEnd`.
- 21 testes (0 falhas, nenhum dependia do que foi removido). Validado
  visualmente no emulador.
- Evidência: seção "Validação da FASE 11.2b" no `README.md`.
- `versionCode`/`versionName`: `7`/`0.4.0` → `8`/`0.4.1`.

## FASE 11.2 — Parser compartilhado alinhado com o oficial

- **Status:** DONE
- `model/LabWorkbookParser.kt` (Android + Web, código próprio): ranges
  fixos de linha/coluna (Escalistas: nomes desde linha 2, data fixa na
  linha 2, status em `3..32`; Escala: 30 linhas fixas `2..31`) em vez de
  auto-detecção; validação de calendário real nas datas (`31/02` rejeitado,
  `29/02` só em ano bissexto); assimetria de separadores replicada
  (colaborador: 4 separadores, equipe: 3, sem `;`); novo campo
  `ShiftDay.label` reproduzindo o `labelFor` oficial (BH → "Banco de
  horas", Aniversário → "Folga aniversário", Folga com status → "Folga /
  X", "Trabalho sem turno localizado").
- Limitação conhecida documentada (não corrigida): sem atalho para
  células de data POI reais — o pipeline sempre passa por
  `DataFormatter`/SheetJS. Não afeta o arquivo real usado hoje.
- 8 testes novos em `commonTest` (`LabWorkbookParserTest.kt`, 21 testes no
  total, 0 falhas via `testDebugUnitTest`).
- Testado manualmente no emulador com a escala real do Dropbox: mesmos 30
  dias/colaboradores de antes (sem regressão), rótulo "Folga / DF"
  confirmado visualmente na aba Escala.
- Evidência: seção "Validação da FASE 11.2" no `README.md`.
- `versionCode`/`versionName`: `6`/`0.3.1` → `7`/`0.4.0`.

## FASE 11.1b — Dropbox real na Web (OAuth) + remoção de linguagem mock/demo/POC

- **Status:** DONE — usuário confirmou em 2026-07-11 que o download real
  do Dropbox está funcionando na Web após cadastrar o escopo `sharing.read`
  e o redirect URI no App Console.
- Projeto reenquadrado como oficial (não mais "laboratório"/POC
  descartável) — textos de UI, launcher, título da aba e nome do PWA
  trocados de "mock"/"demo"/"POC"/"Lab" para linguagem neutra. Nome de
  pasta/pacote/`applicationId` **não** mudou (decisão explícita).
- Dropbox real na Web: troca do link direto (bloqueado por CORS) pela API
  oficial do Dropbox via OAuth PKCE — popup (não navega a página inteira),
  `dropbox-callback.html`, token em `localStorage` com renovação automática,
  chamada a `sharing/get_shared_link_file`. Android continua no link direto,
  sem mudança.
- Testado com Chromium headless (Playwright): popup abre com os parâmetros
  OAuth corretos, mas o Dropbox recusa com `scope_not_granted` — falta
  habilitar o escopo `sharing.read` no App Console do App Key
  `5by0pkzt2bgx95g` e registrar o redirect URI
  `http://localhost:8080/dropbox-callback.html`. Só o dono da conta Dropbox
  pode fazer isso.
- Android: regressão confirmada no emulador, sem mudança de comportamento.
- Evidência: seção "Validação da FASE 11.1b" no `README.md`.
- `versionCode`/`versionName`: `5`/`0.3.0-lab` → `6`/`0.3.1`.

## FASE 11.1 — Download real da escala via Dropbox

- **Status:** DONE
- `platform/RemoteBytesDownloader.kt` (`expect downloadBytes`): Android via
  Ktor, Web/Wasm via `fetch` nativo (`remote-download.js`) — não via engine
  Ktor CIO, que não rejeitava a coroutine de forma confiável no Wasm quando
  o `fetch` interno falhava.
- `repository/DropboxScaleRepository.kt` baixa o mesmo link Dropbox do app
  Android real (`model/RemoteScaleConfig`) e alimenta o parser
  compartilhado (`LabWorkbookParser`) via `platform/WorkbookBytesReader.kt`
  (reaproveita Apache POI/SheetJS já usados pelo seletor de arquivo local).
- Botão "Procurar escalas (Dropbox)" na aba Importar, com spinner e card de
  erro amigável (nunca quebra a tela).
- **Android: real de ponta a ponta** (testado no emulador, dados reais da
  produção). **Web/Wasm: bloqueado por CORS** (testado com Chromium
  headless/Playwright, `net::ERR_FAILED` — limitação de plataforma, não
  bug daqui).
- Evidência: seção "Validação da FASE 11.1 — download real da escala via
  Dropbox" no `README.md`.
- `versionCode`/`versionName`: `4`/`0.2.1-lab` → `5`/`0.3.0-lab`.

## FASE 11.0c — Chave de assinatura própria + APK de release

- **Status:** DONE
- Keystore `escalaici-kmp-lab.jks` + `keystore.properties` (raiz do
  projeto, gitignored) — mesma chave usada em debug e release
  (`signingConfigs`/`buildTypes` em `composeApp/build.gradle.kts`, mesmo
  padrão do `EscalaSOC/app/build.gradle.kts`).
- `./gradlew :composeApp:assembleRelease` gera
  `composeApp-release.apk`, copiado para
  `~/Downloads/EscalaICI-KMP-Lab-latest.apk` — para instalar lado a lado
  com o app oficial no celular (`applicationId` já é diferente, não
  conflita).
- Detalhes completos (formato do `keystore.properties`, onde tudo fica) em
  `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md` §10.
- `versionCode`/`versionName`: `3`/`0.2.0-lab` → `4`/`0.2.1-lab`.

## FASE 11.0b — Ícone próprio do lab (calendário)

- **Status:** DONE
- Icone escudo (SOC) trocado por um mini calendario proprio (sem texto),
  mesma paleta/gradiente do tema, no PWA (`icon.svg`/`icon-maskable.svg` +
  PNGs regerados) e no Android (primeiro adaptive icon customizado do lab —
  antes o app rodava com o icone padrao do AGP, sem `android:icon` no
  manifest).
- `versionCode`/`versionName`: `2`/`0.1.1-lab` → `3`/`0.2.0-lab`.
- Validado visualmente no emulador (dock/launcher).

## FASE 10.0 — Split mecânico do App.kt (porte visual completo)

- **Status:** DONE
- Nota: nova série `FASE 10.x`, para paridade visual **completa** com o app
  Android real (correção de escopo pedida pelo usuário — a aproximação da
  FASE 9c-1 não é suficiente). Plano completo em
  `/home/lvergani/.claude/plans/humble-spinning-yao.md`.
- `App.kt` (1831 linhas) splitado em: `ui/theme/{LabColors,LabTheme,ShiftColors}.kt`,
  `ui/components/{PremiumBackground,LabCard,PageList}.kt`, `ui/util/StringFormatting.kt`,
  `ui/{LoginGateScreen,TodayTab,ScheduleTab,ImportTab,AlertsTab,ProfileTab}.kt`,
  e `ui/App.kt` (shell: `EscalaIciLabApp`, `LabTab`, `BottomNav`).
- Split mecânico: só `package`/`import`/`private`→`internal`/movimentação de
  bloco — nenhuma mudança de cor, texto ou comportamento, exceto `HeroCard`
  ganhar um parâmetro `gradient` com valor padrão idêntico ao anterior
  (habilita a correção da FASE 10.8 sem mudar nada agora).
- Evidência: build `assembleDebug` + `wasmJsBrowserDistribution` +
  `testDebugUnitTest` (13 testes) passando sem alteração de resultado.

## FASE 10.1 — Design system exato (cores, tipografia, shapes)

- **Status:** DONE
- `ui/theme/LabColors.kt`: paleta expandida de 8 para 24 cores, porte 1:1 de
  `ui/theme/Color.kt` do app real (`primaryContainer`, `onPrimaryContainer`,
  `secondary`, `tertiaryContainer`, `errorContainer` etc.).
- `ui/theme/LabTypography.kt` (novo): porte literal dos 12 `TextStyle` de
  `SocTypography`.
- `ui/theme/LabShapes.kt` (novo): porte literal de `PremiumShapes`
  (`cardLarge/cardMedium/cardSmall/button/chip/navPill`).
- `ui/theme/LabTheme.kt`: `LabColorScheme` expandido para os 26 parâmetros de
  `darkColorScheme`, mapeamento idêntico a `SocDarkColorScheme` (confirmado
  lendo `Theme.kt` real linha a linha).
- `ui/theme/ShiftColors.kt`: `shiftContainerColor()`/`shiftOnColor()` novos,
  com os valores exatos de `Color.kt` (não apenas alpha da cor base — vários
  containers de turno usam hex distintos, ex. `ShiftManhaContainer` é
  `#713F12`, não uma variação de `#FACC15`).
- `LabShapes` aplicado aos componentes compartilhados (`LabCard`, `HeroCard`,
  badge, nav pill do `BottomNav`) — mesmos valores numéricos de antes, só
  passou a referenciar o token em vez do `dp` solto.
- `MaterialTheme` do app agora usa `typography = LabTypography` além do
  `colorScheme`.

Limites assumidos:

- shapes bespoke específicas de cada aba (ex. pills com 15dp/18dp) não foram
  migradas para `LabShapes` — só os valores que batem exatamente com os 6
  tokens oficiais do app real;
- containers/on-colors de alerta (`AlertInfo`/`AlertWarning`/etc.) ainda não
  foram portados com os valores exatos do real — fica para a FASE 10.8
  (polimento da aba Alertas), que já vai mexer nessa aba de qualquer forma;
- nenhum arquivo do app Android principal foi alterado.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.2 — Shield logo real + ícone PWA

- **Status:** DONE
- `ui/components/SocLogo.kt` (novo): `LabShieldLogo`/`LabAppTitle`, porte
  literal do `Canvas`/`Path` do `SocShieldLogo`/`SocAppTitle` reais (mesmas
  coordenadas fracionárias, gradiente azul→roxo, "S" traçado, linha
  diagonal). Ainda não usado nos headers das abas (isso é a FASE 10.4) — só
  criado.
- `icons/icon.svg`/`icon-maskable.svg`: substituído o design genérico
  ("computador" + texto "ICI") pelo shield real, usando os paths exatos de
  `ic_launcher_foreground.xml`/`ic_launcher_background.xml` (viewBox 108,
  mesmo grupo `scale(0.78) translate(12,12)`). A versão maskable usa a
  mesma proporção de safe-zone do contrato de adaptive icon do Android
  (círculo de 66dp em 108dp = 61% — `scale(0.61) translate(21,21)`).
- 4 PNGs (`icon-192/512.png`, `icon-maskable-192/512.png`) regerados com
  `rsvg-convert` (mesmo pipeline da FASE 9g, sem dependência nova).
- Ícones conferidos visualmente (renderizados e inspecionados nesta sessão):
  shield fiel ao launcher real, versão maskable com margem de segurança
  visível em todos os lados.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando; ícones novos incluídos na distribuição.

## FASE 10.3 — LoginGateScreen fiel ao LoginScreen real

- **Status:** DONE
- `ui/LoginGateScreen.kt` reescrita como porte literal de `ui/auth/LoginScreen.kt`
  (app real): mesmo layout (`Column` centralizada, `widthIn(max=400.dp)`,
  scroll vertical), mesmos textos ("Escala ICI", botão "Login", "Será aberta
  a autenticação Microsoft corporativa", "Modo Demo"), mesmo fluxo de dialog
  ("Teste SOC A"/"Teste SOC B"/"Aprovador SOC"/"Cancelar").
- Diferença inevitável (sem MSAL/Firestore no laboratório): "Login" simula
  uma tentativa (`isLoggingIn` + `CircularProgressIndicator`, igual ao real)
  e sempre termina em mensagem inline "Login corporativo indisponível nesta
  POC" — em vez de silenciosamente não fazer nada, explica o motivo. "Modo
  Demo" seleciona um dos 3 membros mock (`mockTeamMembers()`) em vez de um
  `DemoUser` real.
- Removido o ícone `Icons.Default.Security` genérico que a versão anterior
  (FASE 9f) tinha — o `LoginScreen` real não usa nenhum ícone/logo, só texto.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.4 — Navegação em pilha + PremiumHeader

- **Status:** DONE
- `ui/components/CollaboratorBadge.kt` (novo): `LabCollaboratorAvatar`/
  `SelectedCollaboratorBadge`, porte literal de `CollaboratorComponents.kt`
  real (avatar circular com gradiente azul→roxo).
- `ui/components/PremiumHeader.kt` (novo): `LabPremiumHeader`, porte literal
  de `PremiumHeader` real (logo+"Escala ICI", sino de notificações, chip
  "Plantão", badge do colaborador selecionado). Usado agora como **primeiro
  item nas 5 abas** (Hoje/Escala/Importar/Alertas/Perfil) — confirmado lendo
  `TodayScreen.kt`/`CalendarScreen.kt`/`ImportScaleScreen.kt`/
  `AlertsScreen.kt`/`SettingsScreen.kt` reais, todos chamam
  `PremiumHeader(onOpenPlantao = onOpenPlantao)` como primeiro item.
- **Correção de fidelidade**: `PageList` não recebe mais `title`/`subtitle`
  — o app real não tem um título de página genérico além do
  `PremiumHeader` (confirmado lendo os 5 arquivos de tela reais); o título
  grande "Escala"/"Importar"/etc. que o laboratório mostrava era uma
  invenção da FASE 9c-1, não existe no app real.
- `App.kt`: `StackedScreen` (enum `PLANTAO`/`SWAP`) + navegação em pilha —
  quando uma tela empilhada está aberta, o bottom nav some (igual ao app
  real) e um placeholder "Em construção" é mostrado (será substituído pelas
  telas reais nas FASES 10.10/10.11).
- `ProfileTab`: "Ver minhas solicitações" deixou de ser `DisabledAction` e
  agora abre de fato a tela (placeholder) de Trocas de escala.
- Removido o indicador de debug "mock N" do header antigo de Hoje — não
  existe no app real; o botão "Voltar para mock" (aba Importar) já cumpre
  esse papel.

**Verificação visual pendente**: mesma limitação da FASE 10.1 — o emulador
local segue sem espaço (`INSTALL_FAILED_INSUFFICIENT_STORAGE` mesmo após
liberar cache, `/data` ~92% cheio apesar do APK ter ~100MB e sobrarem
~480MB livres — parece ser um limite de threshold do AVD, não falta real de
espaço). Fidelidade conferida lendo os 5 arquivos de tela reais linha a
linha; ainda falta uma checagem visual em dispositivo/emulador com mais
espaço ou no navegador.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.5 — Polimento fino da aba Hoje

- **Status:** DONE
- Comparado `TodayTab.kt` linha a linha com `ui/home/TodayScreen.kt` real.
- `NextTurnHero`: agora trata de fato o caso `nextShift == null` (fiel ao
  real) — "Importe uma escala" + texto explicativo + `ImportVisualButton`
  (chip "Importar escala" com seta), sem chip de clima/linha de equipe
  (que só existem quando há turno). Caso populado: "Analista:"/"Com:" viram
  texto anotado com o rótulo em azul (`#60A5FA`) Black, igual ao real
  (`heroMetaText`).
- `WeatherChip`: emoji "☀️" (32sp) em vez do ícone `Icons.Default.Cloud` —
  o real não usa nenhum ícone Material aqui, só emoji.
- `WeekSummaryCard`: gradiente/borda trocados para os valores do
  `TodayCard` real (`#0B1B2C,#0D1A2D` / `SocPrimary@0.42`, antes usava os
  defaults genéricos do `LabCard`); badge agora é `"demo"` só quando não
  importado (removida a tag `"xls"`, que não existe no real).
- `EventsCard`: mesmo ajuste de gradiente/borda; textos de evento
  reescritos via `ShiftDay?.eventLabel()` igual ao real ("Não encontrado"
  em vez de "--/-- · Sem turno").
- `PauseCard`: corrigida a cor da borda para o teal literal `#14B8A6`
  (o real usa essa cor fixa pro card, diferente do `SocTertiary`/
  `LabColors.tertiary` #18A874 usado só no ícone).
- `PeriodSummary`: título/subtítulo agora distinguem demo ("RESUMO DA
  SEMANA"/"Demonstração" + badge "demo") de real ("RESUMO DO PERÍODO"/
  período) — o real nunca mostra "RESUMO DO PERÍODO" em modo demo.
- `MetricCard`: dimensões exatas do real (altura 118dp não 112dp, ícone
  20dp não 19dp, box do ícone 36dp não 34dp, alpha do gradiente 0.12 não
  0.13).
- `onImportClick` novo: o botão da hero vazia agora navega de fato para a
  aba Importar (`activeTab = LabTab.Importar` no shell).

Limite assumido (documentado, não corrigido): o real usa um `TodayCard`
privado com variações "compact" via `BoxWithConstraints` para telas
estreitas (`QuickEventsCard`/`PauseCard` lado a lado, métricas em
`LazyRow`) — o laboratório roda sempre num container largo
(`widthIn(max=760.dp)`), então essas variações de largura estreita não se
aplicam e não foram portadas.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.6 — Polimento fino da aba Escala

- **Status:** DONE
- Comparado `ScheduleTab.kt` linha a linha com `ui/calendar/CalendarScreen.kt`
  real.
- `LabCard.title` virou opcional (`String? = null`, pulando a linha de
  cabeçalho quando nulo) — o `SocCard` real é um card "nu" sem título
  embutido; alguns cards do laboratório (o novo card de demonstração da
  Escala) precisam desse comportamento.
- Novo `DemoCalendarCard` ("Demonstração" + texto explicativo + badge),
  mostrado quando `!summary.isImported`, igual ao topo do
  `DemoCalendarContent` real (antes o laboratório não tinha esse aviso).
- `ShiftTurnoTab`: shape corrigido de `RoundedCornerShape(14.dp)` para
  `LabShapes.chip` (12dp) — o real usa `PremiumShapes.chip` aqui.
- `TurnoNamesColumn`: traço "sem escalado" trocado de hífen `-` para
  travessão `—`, igual ao real.
- `CalendarDayDetailCard`: marcador de turno agora fica dentro de um "halo"
  quadrado 46dp/cardSmall com fundo `color@0.16` (antes era só o círculo de
  44dp solto); título separado em duas linhas (data completa, depois
  tipo/horário do turno) igual ao real; alpha do divisor corrigido de 0.30
  para 0.22.
- `InlineLegendCard`: grade responsiva por `BoxWithConstraints` (2/3/4
  colunas conforme a largura) igual ao real, em vez de sempre 3 colunas
  fixas — relevante porque o app roda em telas de celular estreitas na
  prática.

Limites assumidos (documentados, não portados): botão de "Atualizar"
(`Icons.Default.Refresh`) do cabeçalho do mês real não existe no
laboratório (não há sincronização/backend a atualizar); o real trava o mês
do calendário demo em julho/2026 sem navegação — o laboratório manteve a
navegação de mês habilitada mesmo em modo demo, por ser mais útil para
apresentação; a heurística real de rótulo "Observação do analista" vs
"Equipe" nas notas do dia (baseada em conter o nome do colaborador) não foi
portada — mantido o rótulo genérico "Observação".

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.7 — Polimento fino da aba Importar

- **Status:** DONE
- `ImportTab.kt` reescrita como porte de `ui/settings/ImportScaleScreen.kt`
  real: título/subtítulo simples "Importar escala" (sem hero/banner — o
  real não tem hero aqui, só texto), `LocalFileCard` clicável por inteiro
  (ícone `FolderOpen`/`Error` conforme estado, `SuccessStatusPill` "Escala
  analisada e salva com sucesso"), `ScaleSummaryCard` com diagnóstico
  OK/Atenção por aba (`DiagnosticLine`) e seções rotuladas
  ("Abas encontradas", "Colaborador selecionado", "Dias processados",
  "Status", "Colaboradores encontrados"), `IdentifiedCollaboratorCard`
  ("Identidade da escala") e `CloudFileCard` ("Arquivo em nuvem" com
  "Escolher arquivo"/"Procurar escalas").
- "Procurar escalas" (Dropbox) fica desabilitado — fora de escopo do
  laboratório.
- **Mantido, mas documentado como acréscimo do laboratório**: o seletor de
  colaborador da pré-visualização (`CollaboratorPreviewCard`) não existe no
  app real (lá a identidade vem do login, não de escolha manual) — é uma
  peça própria do laboratório para demonstrar a leitura de múltiplos
  escalistas do XLS, mantida por valor de demonstração.
- Limite assumido: não há estado "Loading" (a leitura do laboratório é
  síncrona via callback da plataforma, sem uma etapa de cópia assíncrona
  como no real).

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.8 — Polimento + correção do gradiente da aba Alertas

- **Status:** DONE
- **Bug corrigido** (identificado ainda na fase de design, confirmado por
  grep no real): `AlertsHero` usava o gradiente/textura do `HeroCard`
  compartilhado (o mesmo de Hoje/Importar); o real usa gradiente próprio
  `#0B274F,#111A31,#24104D`, shape `cardLarge` (20dp, não 12dp), borda
  `primary@0.30` (não a borda azul-acinzentada fixa do hero comum) e **sem
  nenhuma textura** — por isso `AlertsHero` deixou de reaproveitar o
  `HeroCard` e virou um `Surface`+`Box` bespoke, fiel ao real.
- `CountBadge`: corrigido para mostrar a contagem **total** de alertas em
  branco (46dp, `cardSmall`) — a versão anterior mostrava só a contagem de
  críticos com cor condicional, que não é o que o real faz.
- Shapes corrigidas para os tokens exatos do real: `AlertSummaryCard`
  (`cardMedium`, 16dp, era 12dp), `AlertFilterRow` (`chip`, 12dp, era
  18dp), `PremiumAlertCard` (`cardMedium`, era 12dp), `SeverityBadge`
  (`chip`, era 14dp).
- `AlertContextLine` (novo): replica a lógica real — alerta com título
  "Fonte" mostra período+analista; qualquer outro mostra "Fonte: arquivo".

Limites assumidos (documentados, não portados):
- o real mostra um estado vazio distinto (`DemoAlertsEmptyState`, zero
  alertas) quando não há escala importada; o laboratório optou por manter
  a geração de alertas também em modo demo (usando os dias mock), decisão
  já tomada na FASE 9c-1 e mantida aqui de propósito — mostra a lógica de
  alertas "funcionando de verdade" na demonstração, em vez de uma tela
  vazia;
- o reformato de mensagem por regex (`formatAlertMessage`, específico dos
  templates do `GenerateScaleAlertsUseCase` real) não foi portado, pois os
  templates do `GenerateLabAlerts` do laboratório já têm formato próprio.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.9 — Polimento fino da aba Perfil

- **Status:** DONE
- Comparado `ProfileTab.kt` com `ui/settings/SettingsScreen.kt` real.
- Adicionado título de seção "Perfil" (titleLarge, bold) logo após o
  `LabPremiumHeader` — porte do `SectionHeader("Perfil")` real (lido
  diretamente `ui/components/Layout.kt`).
- Card "Perfil selecionado": trocado o box quadrado com iniciais por
  `LabCollaboratorAvatar` (avatar circular com gradiente azul→roxo, mesmo
  componente usado no `PremiumHeader` desde a FASE 10.4) — o real usa
  `CollaboratorAvatar` aqui, não um ícone genérico. Card virou "nu" (sem
  título embutido no `LabCard`, `title = null`), igual ao `SocCard` real;
  adicionadas as linhas "Período"/"Fonte" (que o real mostra) mantendo
  também e-mail/time (informação própria do modelo do laboratório).
- Shapes corrigidas para os tokens exatos: `ProfileMetric` (`cardSmall`,
  era `RoundedCornerShape(12.dp)` solto) e `ProfileChip`/`CollaboratorChip`
  (`chip`, 12dp, era 14dp).
- Chips de antecedência de notificação expandidos para as 6 opções reais
  ("No horário", "5/10/15/30 min antes", "1h antes", em 2 linhas de 3),
  antes só 3 opções.

Limites assumidos (documentados, não portados): `AdminScaleCard`/
`UserMicrosoftAccountCard`/`DemoModeCard` reais têm fluxos condicionais
completos de login ADM/MSAL/Dropbox — o laboratório mantém a versão visual
estática já existente desde a FASE 9c-1 (sem login real, por definição do
escopo do laboratório); `PauseSettingsCard` real calcula janela permitida
dinâmica por tipo de turno — o laboratório mantém os valores já calculados
em `summary.pauseLabel`/`pauseOffsetLabel`.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.10 — Tela Plantão nova (mock)

- **Status:** DONE
- `ui/PlantaoScreen.kt` (novo): porte de `ui/plantao/PlantaoScreen.kt` real
  — `PlantaoHeroCard`, banner informativo ("Dados de plantão (mock)" no
  lugar do botão de importar relatório real), `PlantaoMonthHeader` +
  `PlantaoCalendarGrid` (marcador roxo nos dias com plantão, reaproveitando
  `LabDate`/`LabYearMonth`), `PlantaoDayDetailCard`.
- Reaproveita 100% os modelos/mocks já existentes da FASE 9c:
  `OnCallPeriod`, `OnCallAssignment`, `OnCallStatus`,
  `mockOnCallAssignments()` — engordado de 1 para 4 registros (um `ACTIVE`,
  dois `SCHEDULED`, um `COMPLETED`) para o calendário não ficar vazio.
- **Decisão assumida** (documentada no plano): sem `kotlinx-datetime` no
  projeto, "agora" é decidido pelo campo `status` já mockado (`ACTIVE`) em
  vez de comparar com a data real do sistema — o registro `ACTIVE` também
  serve de referência visual no calendário (destaque de borda, análogo ao
  "hoje" do real).
- Substituído no shell (`App.kt`): o placeholder "Em construção" do botão
  "Plantão" do header agora abre esta tela de verdade.

Limites assumidos: sem importação de relatório real (não existe
`PlantaoWorkbookParser`/file picker de plantão no laboratório — só o botão
"Plantão" no header e o calendário/detalhe do dia).

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## FASE 10.11 — Tela Trocas de escala nova (mock)

- **Status:** DONE
- `ui/ShiftSwapScreen.kt` (novo): porte de `ui/swap/ShiftSwapScreen.kt`
  real — seções "Recebidos"/"Enviados" (`ShiftSwapSectionCard` equivalente
  inline), `ShiftSwapRequestCard` (nome do colega, "Seu turno: X",
  "Turno de Y: Z", "Time: X", "Status: X", "Criada em: X"), botões
  Aceitar/Recusar (recebidos, quando `PENDENTE_TECNICO_DESTINO`) ou
  Cancelar (enviados). Sem Firestore — aceitar/recusar/cancelar mutam o
  status **em memória** (`remember { mutableStateOf(...) }`), sem
  persistência real.
- **Extensão de modelo** (única fase que mexeu em `DomainModels.kt`):
  `SwapStatus` foi de 4 para os 6 valores textuais reais
  (`PENDENTE_TECNICO_DESTINO`, `AGUARDANDO_COORDENADOR`, `APROVADA`,
  `RECUSADA_TECNICO_DESTINO`, `RECUSADA_COORDENADOR`, `CANCELADA`);
  `ShiftSwapRequest` ganhou campos opcionais com default
  (`requesterName`, `targetName`, `requesterShiftType`, `targetShiftType`,
  `teamName`, `createdAt`) — todos com valor padrão, então
  `MockShiftSwapRepository`/testes existentes continuam compilando sem
  alteração (só uma referência a `SwapStatus.PENDING` no teste precisou
  virar `SwapStatus.PENDENTE_TECNICO_DESTINO`).
- `mockShiftSwapRequests()` engordado de 1 para 3 pedidos, cobrindo
  "recebido pendente", "enviado pendente" e "já aprovado" (usando
  lvergani/alamancio/altaborda, os 3 membros mock já mapeados no login
  fake da FASE 10.3).
- Substituído no shell (`App.kt`): "Ver minhas solicitações" (Perfil) agora
  abre esta tela de verdade, filtrando por `summary.member.id`. Removido o
  `StackedScreenPlaceholder` (código morto — as duas telas empilhadas
  agora são reais).

Limites assumidos: sem Firestore/backend — ações alteram só o estado local
da tela (não persistem entre reaberturas); sem verificação de "logado"
(a tela só é alcançável depois do login fake, então essa checagem do real
não se aplica).

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando (só 1 linha ajustada por causa da
renomeação de `SwapStatus`); APK debug e distribuição Web/Wasm continuam
compilando.

## FASE 10.12 — Consolidação (checklist de paridade visual completa)

- **Status:** DONE
- Nova seção "Checklist de paridade visual completa (FASE 10)" no
  `README.md`, resumindo o resultado das FASES 10.0–10.11: tabela de
  design system, tabela por tela (Login/Hoje/Escala/Importar/Alertas/
  Perfil/Plantão/Trocas de escala) e limites conhecidos aceitos de
  propósito.
- Verificação final de runtime: dev server Web/Wasm subido e validado via
  `curl` (HTTP 200 em `index.html`/`manifest.json`/`composeApp.js`),
  processo encerrado ao final. Verificação em emulador Android não foi
  possível nesta sessão (mesma limitação de armazenamento do AVD já
  registrada nas FASES 10.1/10.4).
- Plano completo desta série de fases:
  `/home/lvergani/.claude/plans/humble-spinning-yao.md`.
- Confirmado `EscalaSOC` (app Android principal) sem nenhuma alteração em
  toda a série 10.0–10.12 (`git status` limpo antes/depois de cada commit).

Com isso, a série **FASE 10.0–10.12** (porte visual completo, corrigindo o
escopo da aproximação da FASE 9c-1) está concluída: design system exato,
logo/ícone reais, login/header/navegação fiéis, as 5 abas revisadas
linha a linha contra o código real, e as 2 telas que faltavam (Plantão,
Trocas de escala) criadas como mock.

## FASE 9g (spec 27) — PWA real (manifest, ícones, service worker, cache offline)

- **Status:** DONE
- `manifest.json`: ícones PNG reais (`icon-192.png`, `icon-512.png`,
  `icon-maskable-192.png`, `icon-maskable-512.png`) além do SVG existente,
  mais `id`/`lang`/`orientation`.
- `service-worker.js`: `CACHE_NAME` v2, fallback offline de navegação
  (cai no `index.html` cacheado quando rede e cache falham).
- `index.html`: meta tags `apple-mobile-web-app-*` e `apple-touch-icon`.
- Ícones gerados localmente via `rsvg-convert` (sem dependência nova).
- Evidência: seção "Validação da FASE 9g — PWA real (manifest, ícones,
  service worker, cache offline)" no `README.md`.

## FASE 9f (spec 27) — Login fake, lista de escala e calendário (Compose Multiplatform)

- **Status:** DONE
- `LoginGateScreen` em `ui/App.kt`: seleção de colaborador demo via
  `MockMemberRepository` (FASE 9e) antes de liberar a navegação principal.
- `InMemoryAuthSessionRepository` (novo, em `repository/MockRepositories.kt`)
  controla a sessão fake; botão "Sair (login fake)" na aba `Perfil` encerra
  a sessão.
- UI 100% em `commonMain` (Compose Multiplatform) — mesmo código roda em
  Android e Web/Wasm, já preparado para reaproveitar em iOS (FASE 9h).
- Lista de escala e calendário já existiam desde a FASE 9c-1 (aba `Escala`);
  esta fase supria apenas a peça que faltava (login).
- Evidência: seção "Validação da FASE 9f — login fake, lista de escala e
  calendário" no `README.md`.

## FASE 9e (spec 27) — Contratos de repository em `commonMain`

- **Status:** DONE
- `repository/Repositories.kt`: `ScheduleRepository`, `MemberRepository`,
  `TeamRepository`, `OnCallRepository`, `ShiftSwapRepository`,
  `AuthSessionRepository`, `LocalCacheRepository` (interfaces puras,
  `suspend fun`, sem SDK de plataforma).
- `repository/MockRepositories.kt`: implementações mock/em memória para
  todos os contratos, reaproveitando os mocks da FASE 9c.
- Testes novos em `commonTest` (`MockRepositoriesTest.kt`, 7 testes, 0
  falhas via `testDebugUnitTest`).
- Evidência: seção "Validação da FASE 9e — contratos de repository em
  `commonMain`" no `README.md`.

## FASE 9d (spec 27) — Regras puras de resumo da semana e alertas em `commonMain`

- **Status:** DONE
- `model/ScheduleRules.kt`: `WeekSummary`/`weekSummaryOf`, `ScheduleAlert`/
  `ScheduleAlertSeverity`/`ScheduleAlertRules`, operando sobre
  `ScheduleAssignment` (modelo puro da FASE 9c), independentes da UI mock.
- `LabDate.parseIso` adicionado em `model/ScheduleModels.kt`.
- Testes unitários novos em `commonTest` (`ScheduleRulesTest.kt`), validados
  via `testDebugUnitTest` (6 testes, 0 falhas).
- Evidência: seção "Validação da FASE 9d — regras puras (resumo da semana e
  alertas) em `commonMain`" no `README.md`.

## FASE 9c (spec 27) — Modelos puros reais em `commonMain`

- **Status:** DONE
- Nota: numeração distinta da série `FASE 9c-0`..`9c-7` abaixo, que segue a
  spec `32-KMP-LAB-VISUAL-XLS-WEB-APRESENTACAO.md`. Esta entrada corresponde
  à FASE 9c definida na spec `27-KMP-PWA-IOS-ESTRATEGIA.md` (repositório
  Android principal), seção 4: "Extrair modelos e regras puras para
  `shared/commonMain`".
- Modelos criados em `model/DomainModels.kt`: `SchedulePeriod`,
  `ScheduleAssignment`, `OnCallPeriod`, `OnCallAssignment`,
  `ShiftSwapRequest`, `ImportJob`, `SourceFileRecord`, e os enums
  `MemberRole`, `AssignmentSource`, `ImportStatus`, `OnCallStatus`,
  `SwapStatus`, `ScheduleSourceType`.
- `Member`/`Team` evoluídos (campos adicionais com valor padrão) em
  `model/ScheduleModels.kt`; mocks novos em `model/MockSchedule.kt`.
- Evidência: seção "Validação da FASE 9c — modelos puros reais em
  `commonMain`" no `README.md`.

## FASE 9b — Laboratório base

- **Status:** DONE
- Target Android + Web/Wasm funcionando, `commonMain` com mock, PWA básico.
- Evidência: commit `c84ebf7`, seção "Validação da FASE 9b" no `README.md`.

## FASE 9c-0 — Spec da POC apresentável

- **Status:** DONE
- Spec `32-KMP-LAB-VISUAL-XLS-WEB-APRESENTACAO.md` criada no repo Android
  (documentação apenas, fora deste repositório).

## FASE 9c-1 — Visual alinhado ao app Android atual

- **Status:** DONE
- Tema Material 3 com paleta `Soc`, navegação inferior (`Hoje`, `Escala`,
  `Importar`, `Alertas`, `Perfil`), cards no padrão visual do app real.
- Evidência: seção "Validação da FASE 9c-1 visual" no `README.md`.

## FASE 9c-2 — Modelos mínimos de importação em `commonMain`

- **Status:** DONE
- `WorkbookImportModels.kt` (`ImportedWorkbook`, `ImportedSheet`,
  `WorkbookImportResult`, `ScheduleImportPreview`) e `ScheduleModels.kt`.

## FASE 9c-3 — File picker Android

- **Status:** DONE
- `platform/WorkbookImportLauncher.android.kt`: seletor de documento +
  leitura `.xls`/`.xlsx` via Apache POI.

## FASE 9c-4 — File picker Web/Wasm

- **Status:** DONE
- `platform/WorkbookImportLauncher.wasmJs.kt`: seleção via navegador +
  leitura `.xls`/`.xlsx` via SheetJS (carregado por CDN no `index.html`).

## FASE 9c-5 — Parser XLS experimental

- **Status:** DONE
- `model/LabWorkbookParser.kt`: lê abas `Escala` e `Escalistas`, gera
  `ScheduleSummary`, avisos e erros. Parser próprio do laboratório, não
  reaproveita nem substitui o parser oficial Android.

## FASE 9c-6 — Conectar dados importados na UI

- **Status:** DONE
- Aba `Importar` com pré-visualização real, seletor de escalista lido da
  planilha, botão "Usar dados importados", alertas (`model/LabAlerts.kt`)
  derivados do resumo importado (descanso < 11h, regra 6x1, inconsistências).

## FASE 9c-7 — README / checklist de apresentação

- **Status:** DONE
- Seção "Checklist de apresentação (FASE 9c-7)" no `README.md`: passo a
  passo de demo para Android e Web/Wasm, roteiro de navegação pelas abas,
  critérios de aceite da spec 32 §12 marcados e limites conhecidos
  consolidados num único lugar.

## Fora do escopo deste laboratório (lembrete)

Login MSAL real, Firebase real, sync global, OneDrive, publicação em loja,
dashboard React, troca real de escala, notificações reais, iOS compilável,
migração do app Android real para KMP. Download real da escala via Dropbox
passou a ser real no Android desde a `FASE 11.1` (bloqueado por CORS no
Web/Wasm — ver seção da `FASE 11.1` acima).
