# Roadmap — Escala ICI KMP Lab

Este roadmap segue o plano de fases descrito em
`EscalaSOC/docs/spec/32-KMP-LAB-VISUAL-XLS-WEB-APRESENTACAO.md` (repositório
Android principal, apenas como referência de spec — este laboratório vive em
`EscalaICI-KMP-Lab` e não altera nada em `EscalaSOC/`).

Status possíveis: `TODO`, `IN_PROGRESS`, `DONE`.

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

Login MSAL real, Firebase real, sync global, Dropbox/OneDrive, publicação em
loja, dashboard React, troca real de escala, notificações reais, iOS
compilável, migração do app Android real para KMP.
