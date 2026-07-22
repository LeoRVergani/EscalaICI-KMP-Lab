# 62 — Estabilização Android/Web do Ambiente Demo (FASE 14E)

## Contexto

Teste manual real no emulador (checkpoint FASE 14c-5B, `workspaces/demo-v1` revisão 3,
Firestore já ativo em `escala-ici-dev`) encontrou seis problemas concretos na visão
administrativa Demo introduzida naquela fase. Esta fase corrige a causa raiz de cada um,
sem mexer em publicação, Firestore ou no Dashboard.

## 1. Safe area / insets

**Causa raiz confirmada:** nenhuma tela do app tratava `WindowInsets` — grep por
`WindowInsets|statusBarsPadding|safeContentPadding|safeDrawingPadding` em todo
`composeApp/src/androidMain` e `composeApp/src/commonMain` não retornava nenhum resultado.
`MainActivity.kt` não chamava `enableEdgeToEdge()`/configurava insets, e cada tela usava só
padding fixo em dp.

**Correção:** `ui/components/PremiumBackground.kt` (`LabPremiumBackground`, wrapper
compartilhado por praticamente toda tela do app) passou a envolver o conteúdo em
`Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`. Isso corrige a maioria das telas
de um só lugar. `PlantaoScreen.kt` e `ShiftSwapScreen.kt` tinham seu próprio `LazyColumn`
fora de `LabPremiumBackground` e precisaram do mesmo modifier aplicado localmente.

No Android, isso empurra o conteúdo para baixo da status bar/notch e acima da navigation
bar. No Web/Wasm, `WindowInsets.safeDrawing` resolve para zero (comportamento padrão do
Compose Multiplatform fora do Android) — nenhum espaçamento extra foi adicionado nem
nenhum código condicional por plataforma.

Confirmado visualmente no emulador: título "Ambiente Demo" e botão "Voltar" agora aparecem
claramente abaixo da status bar, e "Voltar" é clicável.

## 2. Navegação (Voltar / Android Back / troca de persona)

Novo mecanismo `platform/PlatformBackHandler.kt` (`expect`/`actual`): Android usa
`androidx.activity.compose.BackHandler`; Web/Wasm é no-op (não existe gesto de voltar
físico no navegador).

Nova lógica pura e testável, `ui/DemoNavigationDecision.kt`
(`decideDemoBackNavigation`/`DemoBackNavigationTarget`):

- visão administrativa Demo → Android Back volta para a tela de entrada;
- escala de uma persona Demo → Android Back volta para a visão administrativa (sem
  encerrar MSAL/`CorporateAuthState` — são independentes, como já documentado na spec 56
  seção 12);
- tela empilhada (Plantão/Trocas) aberta → Android Back fecha só a tela empilhada,
  tomando precedência sobre os dois casos acima (guarda `enabled = stackedScreen == null`
  no handler de persona-Demo evita os dois handlers disputarem o mesmo evento).

O botão "Sair (login de teste)" do Perfil, quando uma persona Demo está selecionada, agora
sai da persona (volta para a administração) em vez de encerrar a sessão MSAL — logout real
continua sendo uma ação distinta, inalterada.

Confirmado no emulador: Android Back navega persona → administração → tela de entrada
corretamente, com a sessão MSAL (`Conta corporativa autenticada`) preservada em todas as
etapas.

## 3. "Equipe não localizada na escala" sempre aparecia

**Causa raiz confirmada:** `identity/OrganizationRepositories.kt`,
`scheduleSummaryForMember`, nunca preenchia `ShiftDay.teamMembers` (default
`emptyList()`). `ui/TodayTab.kt`/`ui/ScheduleTab.kt` mostravam essa string sempre que
`teamMembers` estava vazio — ou seja, sempre, mesmo com equipe/membership perfeitamente
resolvidos. Não era uma falha de resolução real.

**Correção:** `scheduleSummaryForMember` agora calcula, para cada dia, quais outros membros
da mesma equipe também têm um turno trabalhado (`isWorkShift == true`) na mesma data
(usando `data.scheduleAssignments`, que já contém as atribuições de todos os membros do
workspace, não só do membro pedido). O texto de fallback (quando genuinamente não há
colega escalado naquele dia — situação legítima) mudou de "Equipe não localizada na
escala" para "Nenhum colega escalado neste dia" nos dois arquivos.

Confirmado com dado remoto real (revisão 3): a escala de "Analista de Segurança Demo 1"
agora mostra "Com: Analista de Segurança Demo 2" nos dias em que os dois trabalham juntos.

## 4. Turno "Comercial" da equipe de Segurança virava "Turno indefinido"

**Causa raiz confirmada** por leitura direta (somente leitura, sem autenticação) de
`workspaces/demo-v1/revisions/3/schedule_assignments`: os membros da equipe de Segurança
(`member-demo-seguranca-01`/`02`) têm `assignmentType: "WORK_SHIFT"`,
`shiftName: "Comercial"` — um turno comercial real e válido. O mapeador de turno
(`source/DemoPublicationDtos.kt` `shiftTypeFrom`, e a função irmã `identity/DemoFixturePackage.kt`
`fixtureShiftType`) só reconhecia `madrugada`/`manhã`/`tarde`/`noite` — qualquer outro nome
caía em `ShiftType.INDEFINIDO` (`isWorkShift = false`). Como
`ScheduleSummary.workedDays`/`restDays`/`totalHours` contam por `isWorkShift`, todo dia
"Comercial" (na prática, trabalhado) virava folga — daí o "0 dias trabalhados, 31 folgas,
0 horas" relatado no teste manual.

**Correção:** novo `ShiftType.COMERCIAL` (`model/ScheduleModels.kt`), `isWorkShift = true`,
horário `08:00-18:00` (baseado no `startTime`/`endTime` reais da fixture local de
Segurança). Mapeamento `"comercial" -> ShiftType.COMERCIAL` adicionado em
`shiftTypeFrom`/`fixtureShiftType` **e** em `source/FirebaseSources.kt`
(`toShiftType`, fonte Firebase legada separada, mesma classe de bug). Cor própria em
`ui/theme/ShiftColors.kt` (as três funções de cor). Janela de pausa em
`model/LabWorkbookParser.kt` (pausa 10:00-12:45, mesmo padrão proporcional dos outros
turnos). `ShiftType.INDEFINIDO` continua existindo para nomes de turno genuinamente
desconhecidos — não foi removido.

Confirmado no emulador com dado remoto real: "PRÓXIMO TURNO: Comercial, 08:00 - 18:00",
resumo do período "22d Trabalho / 9d Folgas / 132h" (22 × 6h), badges "Co" na semana.

## 5. Alertas: "Fonte: Fonte:" duplicado e possível spam de "Turno indefinido"

**Causa raiz confirmada:** `scheduleSummaryForMember` gravava a mensagem de origem
("Fonte: publicacao remota rev. 3", já uma frase pronta) dentro de `sourceFileName` — um
campo pensado para nome de arquivo XLS importado. `ui/AlertsTab.kt`
(`AlertContextLine`) e `ui/ProfileTab.kt` (duas linhas) prefixavam/rotulavam esse valor de
novo, produzindo "Fonte: Fonte: publicacao remota rev. 3" e "Arquivo importado: Fonte:
publicacao remota rev. 3" (rótulo errado — não era um arquivo importado). Isso também
fazia `ScheduleSummary.isImported` (`sourceFileName != null`) ficar `true` para dado
remoto, mostrando "Escala salva apenas neste dispositivo" quando na verdade veio do
Firestore.

**Correção:** novo campo `ScheduleSummary.remoteSourceLabel` (sem o prefixo "Fonte: "
embutido), distinto de `sourceFileName` (que voltou a significar só "arquivo XLS
importado"). Novas funções puras testáveis em `ui/ScheduleSourceDisplayDecision.kt`
(`scheduleContextSourceLine`, `profileScheduleStatusLine`, `profileScheduleSourceLine`,
`scheduleGenerationSourceKind`, `hasPublishedOrImportedSchedule`) decidem o texto certo em
cada um dos pontos de exibição, sem nunca duplicar "Fonte:".

`model/LabAlerts.kt` (`generateInconsistencyAlerts`) ganhou uma salvaguarda: mais de 3 dias
com `ShiftType.INDEFINIDO` no mesmo resumo consolidam em um único alerta ("Turno indefinido
em vários dias") em vez de um alerta por dia — proteção contra spam se um nome de turno
genuinamente desconhecido aparecer numa publicação futura. Com 3 ou menos, o comportamento
anterior (um alerta por dia) é preservado.

Confirmado no emulador: aba Alertas mostra só 2 itens ("Fonte da escala", "Regra 6x1 dentro
do limite"), com "Fonte: publicacao remota rev. 3" aparecendo uma única vez.

## 6. Identidade contraditória no Perfil em modo Demo

**Causa raiz confirmada:** `ui/App.kt` resolve a identidade corporativa oficial
(`resolver.resolveCorporateIdentity`) sempre que `CorporateAuthState.Authenticated`,
independente de `requestedEntryContext` ser `LOGIN` ou `DEMO`. `ui/ProfileTab.kt` sempre
mostrava o resultado dessa resolução oficial (`OrganizationResolutionBody`) junto com a
seção de persona Demo — em modo Demo, isso produzia "conta corporativa autenticada" +
"cadastro ainda não foi localizado na organização" (resultado esperado da tentativa
oficial, irrelevante em modo Demo) + "AMBIENTE DE DEMONSTRAÇÃO / Personagem: X"
simultaneamente, parecendo um erro real.

**Correção:** nova função pura testável `ui/ProfileIdentityDecision.kt`
(`decideProfileIdentityPresentation`): quando uma persona Demo está selecionada, oculta
`OrganizationResolutionBody` e mostra em vez disso um rótulo claro ("Sessão administrativa
Demo (DEMO_DEVELOPER)") mais uma frase explícita
(`DemoOfficialWorkspaceNotRequiredMessage`: "Este modo não depende de vínculo no workspace
oficial; os dados abaixo são da persona fictícia selecionada."). Em modo LOGIN (sem persona
selecionada), o comportamento anterior é preservado integralmente.

Confirmado no emulador: Perfil mostra "Nome: Leonardo Rodrigo Vergani / Login:
lvergani@ici.tec.br" (identidade real) + "Sessão administrativa Demo (DEMO_DEVELOPER)" +
"AMBIENTE DE DEMONSTRAÇÃO / Personagem: Analista de Segurança Demo 1" + a frase de
"não é erro no modo Demo" — sem nenhuma menção a cadastro não localizado.

## 7. Plantão — dados ilustrativos pouco visíveis

`ui/PlantaoScreen.kt` já mostrava um aviso textual honesto quando nenhum relatório foi
publicado, mas só como uma linha pequena. Adicionado um badge visual proeminente
("DADOS ILUSTRATIVOS", mesmo padrão visual de outros badges do app) no hero da tela, mais
uma frase fixa deixando explícito que "Plantão é um relatório separado da escala
principal". Nova função pura `shouldShowIllustrativeBadge(isImported: Boolean)`. O badge
desaparece assim que há dado real (cache, Firebase ou XLS importado).

## Matriz Android/Web

| Item | Android | Web/Wasm |
| --- | --- | --- |
| Insets | Corrigido e confirmado no emulador (título/Voltar abaixo da status bar) | `WindowInsets.safeDrawing` resolve para zero por padrão no Compose Multiplatform fora do Android; sem regressão visual observada na build de produção servida localmente |
| Android Back | Confirmado (persona → admin → entrada, MSAL preservado) | N/A (não existe gesto físico de voltar no navegador; `PlatformBackHandler` é no-op) |
| Resolução de colegas/turno Comercial/alertas/identidade | Lógica 100% em `commonMain`, testada em `commonTest`, confirmada no emulador com dado remoto real | Mesma lógica `commonMain` — `wasmJsTest` cobre os mesmos testes; não foi possível validar visualmente as telas autenticadas porque MSAL Web não está configurado para o servidor estático local ad-hoc usado nesta validação (gap pré-existente, documentado em fases anteriores, não uma regressão desta fase) |
| Build/testes | `testDebugUnitTest`, `assembleDebug`, `assembleRelease`: verde | `compileKotlinWasmJs`, `wasmJsTest`, `wasmJsBrowserDistribution`: verde; app carrega sem erro de console no Chromium real |

## Testes adicionados

- `DemoWorkspaceOverviewDecisionTest`: `decideDemoBackNavigation` (overview → entrada;
  persona → overview).
- `DemoScheduleSummaryRepositoryTest` (novo): colegas do mesmo time/data em
  `teamMembers`; dia sem colega mantém `teamMembers` vazio; resumo só com "Comercial"
  conta dias trabalhados/horas corretamente.
- `DemoPublicationResolverTest`/`DemoFixtureParsingTest`: mapeamento de "Comercial"
  (variações de maiúsculas/minúsculas) e de nome de turno desconhecido.
- `LabAlertsTest` (novo): 1-3 dias indefinidos → um alerta por dia; mais de 3 → alerta
  consolidado único.
- `ProfileIdentityDecisionTest` (novo): modo Demo oculta resolução oficial e mostra a
  explicação; modo LOGIN preserva o comportamento anterior.
- `ScheduleSourceDisplayDecisionTest` (novo): nunca duplica "Fonte:"; XLS real continua
  mostrando "Arquivo importado: <nome>".
- `PlantaoDisplayDecisionTest` (novo): badge ilustrativo aparece/desaparece conforme
  `isImported`.

Todos os testes já existentes foram preservados; nenhuma cobertura foi reduzida.

## Critérios de aceite

- [x] Título/botão Voltar do Ambiente Demo visíveis e clicáveis sob status bar Android.
- [x] Android Back navega corretamente entre persona/administração/entrada sem encerrar MSAL.
- [x] Colegas do dia resolvidos corretamente com dado remoto real; mensagem honesta quando
      não há colega.
- [x] Turno "Comercial" reconhecido como turno trabalhado; métricas de dias/horas corretas.
- [x] Nenhuma duplicação de "Fonte:"; nenhum spam de alertas "Turno indefinido".
- [x] Perfil em modo Demo não mostra mais mensagens contraditórias sobre cadastro oficial.
- [x] Plantão sinaliza visualmente dados ilustrativos.
- [x] `testDebugUnitTest`, `wasmJsTest`, `wasmJsBrowserDistribution`, `assembleDebug`,
      `assembleRelease` verdes.
- [x] Validação manual real no emulador Android (screenshots, logcat sem erro).
- [x] Build de produção Web/Wasm carregando sem erro de console no Chromium real.
- [ ] Validação visual das telas autenticadas no navegador (bloqueada por MSAL Web não
      configurado para o ambiente local ad-hoc — gap pré-existente).

## Fora do escopo desta fase

- Configuração de MSAL Web para validação visual completa no navegador.
- Qualquer escrita, publicação ou deploy no Firebase.

## Nota pós-validação: acesso ao Ambiente Demo em build release (2026-07-21)

Após a distribuição do build de release 0.7.10 gerado nesta fase (primeira vez que um
release chegou a ser testado fora do ambiente de desenvolvimento), o acesso ao Ambiente
Demo passou a falhar com "Esta conta não possui acesso ao modo Demo." para uma conta que
antes conseguia entrar. Não foi uma regressão de código desta fase: `isDemoAuthorizedForIdentity`
(`identity/DemoAuthorization.kt`, já existente desde a FASE 14c-5B) só libera acesso por duas vias:

1. `objectId` (Entra ID) presente em `workspaces/demo-v1.allowedDeveloperObjectIds` no Firestore;
2. fallback temporário `BuildConfig.DEBUG == true` + e-mail fixo `lvergani@ici.tec.br`.

A via 2 nunca funciona em build de release (`BuildConfig.DEBUG = false`), e o campo da via 1
estava vazio — por isso ninguém conseguia entrar em um release antes desta correção.

**Correção aplicada:** o campo `allowedDeveloperObjectIds` foi populado manualmente via
Firebase Console (não há escrita de cliente possível: `firestore.rules` bloqueia
`allow write: if false` incondicionalmente em `/workspaces/**`, e não há credencial de
Admin SDK configurada neste ambiente) com o `objectId` real da conta, confirmado por leitura
somente-leitura via REST e por um log de diagnóstico temporário (adicionado e revertido no
mesmo ciclo, sem alterar o código versionado) que capturou o claim `oid` retornado pelo MSAL.
Nenhum código foi alterado; nenhuma escrita de publicação/dado de negócio foi feita — apenas
este campo de controle de acesso interno.

**Pendência real:** a via 2 (fallback por e-mail fixo) continua marcada no código como
temporária e deve ser removida quando `allowedDeveloperObjectIds` for a única fonte de
verdade, com um processo (ou UI administrativa) para adicionar contas sem depender de edição
manual no Console.
- Alterações no Dashboard ou no EscalaSOC.
