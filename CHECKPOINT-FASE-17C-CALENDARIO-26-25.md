# CHECKPOINT FASE 17C — Calendário Operacional 26→25

Branch `feature/fase-17c-calendario-26-25`, base `23b1de1`. Migra a aba Escala do modelo mês civil (paginação com chevrons) para a competência operacional real, em `commonMain`, válida para Android e Web.

---

## ANTES

`ScheduleTab.kt` mantinha `visibleMonth: LabYearMonth` (estado local), `firstMonth`/`lastMonth` (derivados de `sortedDays`), `moveMonth(offset)` (troca `visibleMonth` e reseleciona o primeiro dia do novo mês), e `MonthNavButton`/`ChevronLeft`/`ChevronRight` para navegar entre meses civis dentro da MESMA competência. `PeriodCalendarView` recebia um único `LabYearMonth` e desenhava sempre uma grade de um mês civil (`yearMonth.firstDayOffsetSunday()`/`lengthOfMonth()`), nunca cobrindo o período real cross-mês de uma vez.

## DEPOIS

`ScheduleTab.kt` usa `summary.periodStart`/`summary.periodEnd` (a competência operacional real) como única fonte da verdade, sem noção de "mês visível". `PeriodCalendarView` recebe `periodStart`/`periodEnd` e desenha uma única grade contínua via `buildPeriodCalendarGrid()` (novo, `model/PeriodCalendarGrid.kt`). Sem chevrons, sem paginação, sem `moveMonth`.

---

## 1. Preflight

Branch `feature/fase-17c-calendario-26-25`, HEAD `23b1de1`, working tree limpo, `git diff --check` OK — confirmado antes de qualquer alteração.

## 2. Comportamento antigo

Documentado na seção "ANTES" acima. Auditoria confirmou os símbolos exatos pedidos: `visibleMonth` (`ScheduleTab.kt`, estado `remember`), `firstMonth`/`lastMonth` (derivados de `sortedDays.firstNotNullOfOrNull`/`lastOrNull`), `moveMonth()` (função local), `LabYearMonth` (tipo usado em toda a navegação), `PeriodCalendarView` (já existia, mas por mês civil), `ChevronLeft`/`ChevronRight` (ícones dos botões de navegação).

## 3. Causa conceitual

O calendário tratava cada mês civil (julho, agosto) como uma "página" navegável dentro da mesma competência publicada — mas não existe "competência anterior/seguinte" de verdade carregada (o repository só resolve a competência PUBLICADA vigente, FASE 15). Os chevrons davam a falsa impressão de navegação entre meses quando na realidade só reselecionavam o primeiro dia do mês civil dentro dos MESMOS dados já carregados.

## 4. Fonte periodStart/periodEnd

`ScheduleSummary.periodStart`/`periodEnd` (`model/ScheduleModels.kt`) já existiam, mas com default `days.mapNotNull{it.date}.min/maxOrNull()` — ou seja, derivados da lista de dias, não do backend. Para o caminho Firebase, `EscalaIciScheduleMapper.map()` **não** setava esses dois campos explicitamente (só `periodLabel`), então caíam nesse default. **Corrigido nesta fase**: `EscalaIciScheduleMapper.map()` agora passa `periodStart = LabDate.parseIso(turnosMes.periodoInicio)` e `periodEnd = LabDate.parseIso(turnosMes.periodoFim)` explicitamente — a mesma fonte que `periodLabel` já usava. Teste novo (`map_setsPeriodStartEnd_fromBackendFields_notFromDiasMinMax`) prova que isso vale mesmo quando `dias` só tem um dia no MEIO do período (nem o primeiro nem o último) — se o mapper ainda derivasse de `days`, o teste falharia. Caminho XLS/mock não foi tocado — continua usando o default do data class (correto lá, pois não há campos de backend equivalentes).

## 5. Helper do calendário

`model/PeriodCalendarGrid.kt` (novo, `commonMain`, puro, sem dependência de Compose):
- `buildPeriodCalendarGrid(start: LabDate, end: LabDate): List<PeriodCalendarCell>` — grade única, preenchida com `null` antes/depois para alinhar domingo→sábado, tamanho sempre múltiplo de 7.
- `competenciaHeaderLabel(start: LabDate, end: LabDate): String` — título da competência (3 casos, ver seção 8).

Reaproveita `LabDate.plusDays()` (já existente em `TemporalRules.kt`) e `LabDate.dayOfWeekSundayIndex()` (já existente) — nenhuma lógica de data duplicada.

## 6. Estrutura PeriodCalendarCell

```kotlin
data class PeriodCalendarCell(val date: LabDate?)
```
`date == null` é preenchimento (filler) — nunca clicável, nunca um dia real.

## 7. Células filler

`PeriodDayCell` (Composable já existente, inalterado) já tratava `date == null` como `Spacer` vazio, não clicável, sem marcador — comportamento preservado sem mudança de código nessa função. Testes puros confirmam: filler antes nunca é uma data real, filler depois nunca é uma data real, tamanho da grade sempre múltiplo de 7 (5 casos testados).

## 8. Header da competência

`competenciaHeaderLabel()` testado nos 3 casos pedidos:
- Mesmo mês/ano → `"Agosto de 2026"`.
- Meses diferentes, mesmo ano → `"Julho — agosto de 2026"`.
- Meses e anos diferentes → `"Dezembro de 2026 — janeiro de 2027"`.

Usa `LabDate.MonthLongNames` (já existente, multiplataforma, sem `java.time`) — nenhum nome de mês hardcoded fora dessa lista já usada em outros lugares do app.

## 9. Remoção de visibleMonth

Removido de `ScheduleTab.kt`. Substituído por leitura direta de `summary.periodStart`/`periodEnd` a cada recomposição — não há mais "mês visível" como conceito de estado.

## 10. Remoção de firstMonth/lastMonth

Removidos — não fazem sentido sem `moveMonth`/chevrons. `summary.contains(today)` (já existente, usa `periodStart`/`periodEnd`) continua sendo o único gate para o botão "Ir para hoje".

## 11. Remoção de moveMonth

Removida a função e todo o estado que ela mutava.

## 12. Remoção dos chevrons

`MonthNavButton` (Composable) removido inteiramente. Imports `ChevronLeft`/`ChevronRight` removidos. Novo header (`CompetenciaHeader`) é só um `Column` centralizado com título + subtítulo, sem botões de navegação. Confirmado visualmente (Chromium real, seção 21): sem setas ‹ › na tela.

`NAVEGACAO_ENTRE_COMPETENCIAS_PENDENTE` registrado — não implementada navegação real entre competências (exigiria repository/query por competência, fora do escopo desta fase).

## 13. Seleção inicial

`initialScheduleDate(summary, today)` (já existente, `ScheduleModels.kt`) **não foi alterada** — continua: hoje se dentro do período, senão `periodStart` como fallback. Coberto pelos testes já existentes (`ScheduleDateNavigationTest.kt`, não tocados, ainda passando).

## 14. Seleção de dia

`selectedDate` (estado local) preservado sem mudança de comportamento — clique numa célula com `date != null` atualiza a seleção; células filler (`date == null`) não disparam `onDateClick` (`cell.date?.let(onDateClick)`). Confirmado via Chromium real: clique no dia 11 moveu a seleção corretamente (screenshot).

## 15. Detalhe do dia

`CalendarDayDetailCard` **não foi alterada** (função preservada integralmente) — mesmo card, mesmos campos (dia da semana, data, código, descrição, horário, colegas no turno, botão "Solicitar troca" desabilitado). Confirmado via Chromium: card reage à nova seleção (cor do ícone mudou de amarelo/Manhã para verde/Folga ao selecionar o dia 11).

## 16. Sem horário de trabalho

`ShiftType.timeRange` (enum fixo, compartilhado com Hoje/Alertas — **não tocado**, fora do escopo) já nunca produz `"00:00"`/`"--:--"`/`"null"` para tipos sem horário — mostra um rótulo com sentido (`"Descanso"`, `"Férias"`, `"Banco de horas"`, etc.) em vez do literal "Sem horário de trabalho". Comportamento pré-existente, já compatível com a proibição da seção 18 do prompt (nunca mostra os literais proibidos), mesmo sem usar exatamente essa frase — não alterado para não tocar em `ShiftType`, que é usado por `TodayTab`/`AlertsTab` (explicitamente protegidos nesta fase).

## 17. Quem trabalha nesse dia

`TeamOnDutyCard` **não foi alterada**. Continua usando `EscalaIciScheduleMapper.quemTrabalhaPorTurno(teamSnapshot, dataIso)` quando há `TeamScheduleSnapshot` carregado (caminho Firebase), com fallback para `day.membersByShift`/`day.teamMembers` (caminho XLS/mock). Nenhuma nova query por clique — o snapshot já vem carregado de fora (`App.kt`), como antes.

## 18. TeamScheduleSnapshot

Não alterado. Cache compartilhado (`TeamScheduleRepository`) preservado — a mudança de calendário não introduziu nenhuma leitura adicional.

## 19. Responsividade Android

`PeriodDayCell` (inalterado) já usa `Modifier.weight(1f)` por coluna dentro de `Row.fillMaxWidth()` — 7 colunas sempre, sem scroll horizontal, independente da largura da tela (320/360/412dp) já era o comportamento antes e continua sendo, já que a mecânica de célula não mudou, só a fonte de quantas linhas renderizar. Título usa `maxLines = 1` + `TextOverflow.Ellipsis` (herdado do header antigo) — não quebra feio em telas estreitas.

## 20. Responsividade Web

Mesmo `commonMain`, mesmo Composable — nenhuma implementação paralela para Web. Confirmado via Chromium real (viewport 1280×720, seção 21): calendário centralizado, 7 colunas, sem overflow horizontal.

## 21. Chromium real

Playwright + Chromium real, servido via `python3 -m http.server 8099` (não `file://`). Sequência: app abre → clique em "Modo demonstração" → aba "Escala". Resultado:
- App inicia sem erros de console.
- Login e Demo continuam funcionando e separados (Demo é um link distinto na tela de login, inalterado desde a FASE 17B).
- **0 ocorrências de "Node.js net module"** — a correção da FASE 17B.1 não foi afetada.
- Header mostra `"Julho de 2026"` / `"26 jun. - 25 jul. 2026"` (dados do mock — ver nota abaixo), **sem chevrons**.
- Grade única contínua: 6 (segunda, selecionado, hoje fora do período → fallback correto) até 12 (domingo, linha seguinte) — sem paginação artificial entre meses.
- Clique no dia 11 moveu a seleção e atualizou "Detalhe do dia" corretamente (cor do ícone mudou de Manhã/amarelo para Folga/verde).

**Nota:** o fixture de demonstração (`MockSchedule.kt`, não alterado nesta fase) tem uma inconsistência pré-existente: `periodLabel` diz `"26 jun. - 25 jul. 2026"` mas os `days` reais só cobrem 06/07–12/07 (dentro de julho) — então o header mostra `"Julho de 2026"` (mês único), não o exemplo cross-mês 26/07→25/08 pedido na seção 29 do prompt. Não ajustei esse fixture (fora do escopo de "migrar o calendário"; alterar dados de demo é uma decisão de conteúdo, não de mecânica). **A cobertura do caso 26/07→25/08 (e da virada de ano 26/12→25/01) está provada pelos 12 testes puros de `PeriodCalendarGridTest.kt`**, que testam exatamente esses períodos — mais confiável que uma captura de tela específica, e sem depender de dados de demo artificiais.

## 22. Testes novos

- `PeriodCalendarGridTest.kt` (12 testes): competência cruzando meses, virada de ano, período começando domingo/segunda/meio da semana, filler antes/depois nunca é data real, tamanho sempre múltiplo de 7 (5 períodos testados), nenhuma data duplicada/perdida, header nos 3 casos (mesmo mês, meses diferentes mesmo ano, meses e anos diferentes).
- `EscalaIciScheduleMapperTest.kt` (+1 teste): `periodStart`/`periodEnd` vêm de `periodoInicio`/`periodoFim`, não de `dias`.

`ScheduleDateNavigationTest.kt` (seleção inicial today-dentro/fora do período) **não precisou de teste novo** — comportamento não alterado, continua coberto pelos testes já existentes.

## 23. Total de testes

**394 testes, 8 skipped, 0 failures, 0 errors** (368 da FASE 17B + 26 novos: 13 novos × 2 plataformas, JVM e `wasmJsBrowserTest`).

## 24. Builds

- `./gradlew :composeApp:testDebugUnitTest :composeApp:wasmJsTest --rerun-tasks` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:assembleDebug` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:assembleRelease` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:wasmJsBrowserDistribution` → BUILD SUCCESSFUL, testado em Chromium real (seção 21).
- `git diff --check` → limpo.

## 25. Arquivos novos

- `composeApp/src/commonMain/kotlin/.../model/PeriodCalendarGrid.kt`
- `composeApp/src/commonTest/kotlin/.../model/PeriodCalendarGridTest.kt`

## 26. Arquivos modificados

- `composeApp/src/commonMain/kotlin/.../ui/ScheduleTab.kt` — remoção de mês civil/chevrons, nova grade por competência.
- `composeApp/src/commonMain/kotlin/.../firebase/EscalaIciScheduleMapper.kt` — `periodStart`/`periodEnd` explícitos do backend.
- `composeApp/src/commonTest/kotlin/.../firebase/EscalaIciScheduleMapperTest.kt` — 1 teste novo.

**Nenhum arquivo da FASE 17B foi tocado**: `PlatformHttpClient.kt`/`.android.kt`/`.wasmJs.kt`, `CoordinatorFactory.kt`, `RemoteDtoMappers.kt` e o fail-fast em `build.gradle.kts` permanecem exatamente como ficaram após aquela fase (confirmado por `git status --short` não os listar).

## 27. CHECKPOINT

Este arquivo.

## 28. Marcadores (estado ao final da FASE 17C, antes da validação visual/17C-FINAL)

- `PARIDADE_CALENDARIO_26_25_PENDENTE` → **`PARIDADE_CALENDARIO_26_25_IMPLEMENTADA`**
- `PARIDADE_CALENDARIO_26_25_VALIDACAO_VISUAL_PENDENTE` — **criada**, aguardando teste visual do usuário antes de considerar `VALIDADA_USUARIO`.
- `NAVEGACAO_ENTRE_COMPETENCIAS_PENDENTE` — **criada** (seção 12).
- `PARIDADE_REGRAS_TROCA_GLOBAL_PENDENTE` — mantida, inalterada.
- `INTEGRACAO_STAGING_BROWSER_REAL = PENDENTE` — mantida, inalterada (fora do escopo desta fase).
- `WEB_HTTP_ENGINE_INCOMPATIVEL_COM_BROWSER = RESOLVIDO` — mantida, confirmada ainda válida (seção 21).
- `GRADLE_LOCAL_FIREBASE_PROPERTIES_AUSENTE_QUEBRA_BUILD = RESOLVIDO` — mantida, inalterada.

## 29. Versionamento (estado na FASE 17C, antes do bump)

Confirmado: `versionCode 15` / `versionName "0.7.1"` — sem mudança nesta fase (`composeApp/build.gradle.kts` não editado nesta fase).

## 30-31. `git status --short` / `git diff --check`

Ver relatório final da FASE 17C.

## 32. Git (estado ao final da FASE 17C)

commit: NÃO
push: NÃO
merge: NÃO

---

# FASE 17C.1 — VALIDAÇÃO VISUAL

## Android

Emulador `EscalaSOC_API_37`, 1080×2400px (~412dp), sessão Firebase real restaurada automaticamente (nenhum dado alterado), competência real **26/07/2026 → 25/08/2026**.

**Aprovado:**
- Header "Julho — agosto de 2026" / "26 jul. — 25 ago." — exatamente o formato esperado, sem chevrons, sem espaço residual.
- Grade única contínua (26/07 a 25/08), 7 colunas, domingo/sábado alinhados, fillers corretos.
- "Hoje" (10) selecionado automaticamente (dentro do período).
- Seleção imediatamente identificável (círculo azul, alto contraste).
- Dia de trabalho (10, Manhã) e dia de folga (16, "DSR - Final de Semana") visualmente distintos (marcador amarelo vs. verde, borda do card de detalhe correspondente).
- "Quem trabalha nesse dia" legível, "Leonardo Vergani (Você)" sem truncamento.
- Sem crash (`adb logcat` sem `FATAL`/exceção ligada a `br.com.leorvergani.escalaici.kmp.lab`).

## Web desktop

Chromium real, 1280×800. Mesma estrutura `commonMain`, calendário centralizado, seleção e detalhe funcionais, sem regressão do erro `Node.js net module`. O Demo usado nesse driver não reproduzia exatamente 26/07→25/08 (fixture de mock mais estreito, pré-existente) — o mesmo algoritmo foi validado com dados reais no Android e pelos 12 testes puros de `PeriodCalendarGridTest`.

## Web mobile

Chromium real, 375×812. 7 colunas mantidas, sem scroll horizontal, "Detalhe do dia"/"Quem trabalha nesse dia" legíveis e conectados. A troca de seleção por coordenada automatizada não foi confirmada nesse viewport específico (mira no canvas Wasm não acertou em 2 tentativas) — mesmo caminho funcional já comprovado em Android e Web desktop.

## Resultado da validação

**Nenhum problema CRÍTICO. Nenhum problema IMPORTANTE.** Fase aprovada sem alteração funcional.

## Marcadores atualizados

- `PARIDADE_CALENDARIO_26_25_IMPLEMENTADA` = **SIM**
- `PARIDADE_CALENDARIO_26_25_VALIDADA_USUARIO` = **SIM**
- `PARIDADE_CALENDARIO_26_25_VALIDACAO_VISUAL_PENDENTE` — **removida** (substituída pela validação acima).
- `NAVEGACAO_ENTRE_COMPETENCIAS_PENDENTE` — mantida.
- `PARIDADE_REGRAS_TROCA_GLOBAL_PENDENTE` — mantida.
- `INTEGRACAO_STAGING_BROWSER_REAL = PENDENTE` — mantida.
- `WEB_HTTP_ENGINE_INCOMPATIVEL_COM_BROWSER = RESOLVIDO` — mantida.
- `GRADLE_LOCAL_FIREBASE_PROPERTIES_AUSENTE_QUEBRA_BUILD = RESOLVIDO` — mantida.

## Backlog cosmético (registrado, não corrigido)

- **`DETALHE_DIA_EQUIPE_REDUNDANTE_PENDENTE`** — o card "Detalhe do dia" ainda pode mostrar "Equipe não localizada na escala" mesmo quando "Quem trabalha nesse dia", logo abaixo, já resolve a equipe corretamente via `TeamScheduleSnapshot`. Comportamento pré-existente, não introduzido pela FASE 17C, não bloqueia a aprovação.
- `IR_PARA_HOJE_VISIBILIDADE`
- `ATALHO_TROCA_CALENDARIO_PENDENTE`
- `PADRONIZACAO_CODIGO_MD`
- `REVISAO_INSET_BOTTOM_NAV`

---

# FASE 17C-FINAL — VERSIONAMENTO E PUBLICAÇÃO

## Versão

`versionCode 15`/`versionName "0.7.1"` → **`versionCode 16`/`versionName "0.7.2"`**. Sincronizado em `composeApp/build.gradle.kts` e `model/AppVersion.kt` (`CODE`/`LABEL`). `applicationId` inalterado (`br.com.leorvergani.escalaici.kmp.lab`).

## Validação final

- `testDebugUnitTest`/`wasmJsTest` → **394 testes, 8 skipped, 0 failures, 0 errors** (idêntico à referência da FASE 17C — nenhum teste novo pelo bump de versão).
- `assembleDebug`/`assembleRelease`/`wasmJsBrowserDistribution` → todos BUILD SUCCESSFUL.
- APK inspecionado (`aapt dump badging`): `package: name='br.com.leorvergani.escalaici.kmp.lab' versionCode='16' versionName='0.7.2'`.
- Web: `GeneratedFirebaseConfig.kt` confirmado `STAGING`/`escala-ici-staging`; `local.firebase.properties` íntegro (`md5sum` idêntico ao original); Chromium real confirmou **0 ocorrências** de `Node.js net module` após o bump.

## Git

commit: SIM
push: SIM
merge: NÃO
