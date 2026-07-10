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
