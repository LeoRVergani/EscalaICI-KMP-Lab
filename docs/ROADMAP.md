# Roadmap — Escala ICI KMP Lab

Este roadmap segue o plano de fases descrito em
`EscalaSOC/docs/spec/32-KMP-LAB-VISUAL-XLS-WEB-APRESENTACAO.md` (repositório
Android principal, apenas como referência de spec — este laboratório vive em
`EscalaICI-KMP-Lab` e não altera nada em `EscalaSOC/`).

Status possíveis: `TODO`, `IN_PROGRESS`, `DONE`.

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
