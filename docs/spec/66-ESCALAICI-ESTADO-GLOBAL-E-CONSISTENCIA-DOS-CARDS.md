# FASE 14I — Estado único observável e consistência dos cards

**Status:** aprovado para implementação (Checkpoint A)
**Escopo:** `EscalaICI-KMP-Lab` (Android + Web/Wasm), `commonMain`
**Motivação:** dois bugs reais, confirmados por teste manual do usuário no
APK `0.7.14` (release, FASE 14H) e reproduzidos de novo por mim nesta fase,
no emulador, com sessão MSAL real e dado remoto real (`ici-dev`, revisão 2)
— não hipóteses, reprodução direta.

## 0. Checkpoint 0 — reprodução confirmada nesta fase

Antes de qualquer linha de código, instalei o APK release já existente da
FASE 14H (`composeApp/build/outputs/apk/release/composeApp-release.apk`,
`versionCode 28`/`versionName 0.7.14`, assinatura verificada com
`apksigner`) num emulador limpo (`EscalaSOC_API_37`), com a sessão MSAL
corporativa real já restaurada (`lvergani@ici.tec.br`), e usei **MINHA
ESCALA** (dado real, revisão remota 2 de `ici-dev`, período `2026-06-25` a
`2026-07-26` — confirmando que a FASE 14H genuinamente não publicou
nenhuma revisão nova, exatamente como esperado).

| Problema relatado | Classificação |
|---|---|
| Pausa configurada (10:30) não aparece nos cards "Pausa" (Hoje) e "Resumo" (Perfil) — só a janela genérica 09:00–11:45 | **Reproduzido na 0.7.14** — bug de código, não depende de revisão |
| "Próximos alarmes" mostra corretamente início 10:30/fim 10:45 | **Já correto** — o plano de notificação em si nunca teve o bug |
| Card "Próximo turno" (Hoje) mistura colegas de todos os turnos no campo "Com:" | **Reproduzido na 0.7.14** — bug de código |
| Card "Detalhe do dia" (Escala) mistura colegas de todos os turnos | **Reproduzido na 0.7.14** — bug de código |
| Card "Quem trabalha nesse dia" separa corretamente Md/M/T/N | **Já correto**, não regredir |
| Período remoto `2026-06-25` a `2026-07-26`, resumo `25d`/`1d`/`150h` | **Dependente da revisão remota 2** — nenhuma correção de código nesta fase muda isso; só uma futura revisão 3 publicada mudaria os dados. Não confundir com a fixture sintética de 5 folgas usada nos testes automatizados da FASE 14H (cenário diferente, nunca publicado de verdade). |

## 1. Causa raiz exata (lida no código, não suposição)

### 1.1 Pausa

`model/TemporalRules.kt`:
```kotlin
data class PausePresentation(val scheduledLabel: String?, val windowStart: String, val windowEnd: String) {
    val displayTitle: String get() = if (scheduledLabel != null) "Pausa programada" else "Janela de pausa"
    val displayValue: String get() = scheduledLabel ?: "$windowStart–$windowEnd"
}

fun pauseFor(shift: ShiftOccurrence?): PausePresentation? {
    val window = pauseWindowFor(shift) ?: return null
    return PausePresentation(scheduledLabel = null, windowStart = window.startLabel, windowEnd = window.endLabel)
}
```
`PausePresentation.scheduledLabel` **já existe exatamente para este
propósito** (mostrar o horário efetivo distinto da janela) — mas
`pauseFor()` sempre passa `scheduledLabel = null`, porque nunca recebe
`NotificationSettings.pauseCustomTime`. Os dois call sites afetados:
- `ui/TodayTab.kt:287` (`PauseCard`): `pauseFor(summary.relevantShift(now))`
  — `TodayTab` **não recebe `NotificationSettings` como parâmetro**, só
  `ProfileTab` recebe.
- `ui/ProfileTab.kt:121`: `pauseFor(relevantShift)` — aqui
  `notificationSettings` **já está em escopo** (usado logo abaixo, linha
  343, para os chips de horário), mas não é passado para `pauseFor()`.
  Consumido em `ui/ProfileTab.kt:403` (`pause?.displayValue`, card
  "Resumo").

Não há duplicação de modelo a "remover" — `ScheduleSummary` não tem campos
`pauseLabel`/`pauseOffsetLabel`/`pauseWindowStart`/`pauseWindowEnd` (não
existem no código atual, confirmado por busca; a premissa do prompt sobre
esses campos não se aplica a este repositório — só `TemporalRules.kt`
define a apresentação de pausa, um único lugar). O problema é
exclusivamente **falta de um parâmetro não usado** em uma função pura já
corretamente desenhada.

### 1.2 Colegas por turno

Confirmado por leitura + reprodução visual:
- `ui/TodayTab.kt:138` (`NextTurnHero`, card "Próximo turno"):
  `day.teamMembers.joinToString(", ")`.
- `ui/ScheduleTab.kt:405` (`RealDayDetailCard`, card "Detalhe do dia"):
  `day.teamMembers.joinToString(", ")`.
- `ui/ScheduleTab.kt:451-465` (`TeamOnDutyCard`, card "Quem trabalha nesse
  dia" — **já correto**):
  ```kotlin
  val membersByShift = if (day.membersByShift.isNotEmpty()) day.membersByShift
      else if (day.type.isWorkShift) mapOf(day.type to (listOf(selectedCollaborator) + day.teamMembers))
      else emptyMap()
  ```

Confirmado por reprodução visual (22/07/2026, turno Manhã, usuário
`lvergani`): `teamMembers` = 7 nomes (todos os turnos do dia, sem o
próprio usuário — `teamMembers` já exclui o usuário logado, isso já
funciona certo); `membersByShift[MANHA]` = `[alamancio]` (só o colega do
mesmo turno, também já sem o próprio usuário). **Não é preciso adicionar
lógica de "excluir o usuário atual"** — os dois campos já vêm assim do
domínio (`scheduleSummaryForMember`, corrigido na FASE 14H rodada 2); a
única correção necessária é **qual campo cada card lê**.

## 2. Decisão arquitetural: seletores, não reescrita de estado

O prompt desta fase sugere um `EscalaIciAppState`/`StateFlow` novo. Decisão
desta spec, registrada explicitamente (seção 12 do prompt original já pede
"evite uma refatoração big-bang; migre por seletores e testes" — esta
decisão só torna essa recomendação concreta):

**Não introduzir uma nova classe de estado global.** `ui/App.kt` já é,
na prática, a única fonte de verdade — é um único `@Composable` que
possui, via `remember`/`mutableStateOf`, todo o estado hoje consumido por
todos os tabs (`summary`, `appNotificationSettings`, `firebaseOnCall`,
`firebaseOnCallGroups`, `sessionMemberId`, `organizationResolutionResult`,
etc. — confirmado por leitura completa do arquivo na FASE 14H). O defeito
real nunca foi "estado espalhado" — foi **um card específico não receber
um pedaço do estado que já existe centralizado** (`TodayTab` sem
`NotificationSettings`) **ou usar o campo errado do mesmo modelo já
centralizado** (`teamMembers` em vez de `membersByShift`). Introduzir uma
classe `EscalaIciAppState`/`StateFlow` nova, com eventos
(`SessionRestored`, `ScheduleLoaded`, etc.), seria reescrever uma
arquitetura que já funciona (testada em 239+232 testes, validada
manualmente 2 vezes) para resolver um problema que não é de arquitetura —
é de **plumbing incompleto + card lendo o campo errado**. Isso seria
exatamente a "refatoração ampla fora do objetivo" que o próprio prompt
proíbe.

Em vez disso: **um módulo novo de seletores puros**, testáveis,
consumidos por todo card que precise da mesma regra — nenhuma lógica
recalculada duas vezes de formas diferentes. Isso satisfaz literalmente o
requisito funcional (seção 6-8 do prompt: "todos os cards devem consumir o
mesmo estado ou seletores derivados... nenhum card deve recalcular a
mesma regra de maneira diferente") sem o risco/custo de uma reescrita
completa.

## 3. Seletores novos (`ui/CardSelectors.kt`, `commonMain`)

```kotlin
fun effectivePause(shift: ShiftOccurrence?, settings: NotificationSettings): PausePresentation?
```
- Chama `pauseWindowFor(shift)` (já existe, inalterado).
- Se `settings.notifyPause` e `settings.pauseCustomTime` não for nulo e
  parsear para um minuto dentro da janela (`parseNotificationMinute`, já
  existe em `NotificationPlan.kt`): `scheduledLabel = "$custom–$customEnd"`
  (`customEnd` = custom + 15min, mesma duração fixa já usada em
  `PauseDurationMinutes`).
- Caso contrário (sem `pauseCustomTime`, ou fora da janela — decisão
  explícita, nunca silenciosa: se o turno mudou e o horário deixou de ser
  válido, cai no fallback documentado de mostrar só a janela, igual ao
  comportamento atual): `scheduledLabel = null` (comportamento atual,
  preservado).
- `pauseFor(shift)` (a função antiga) **permanece intacta** — nenhum
  call site que não precisa de horário customizado quebra.

```kotlin
fun colleaguesForShift(day: ShiftDay): List<String>
```
- `day.membersByShift[day.type] ?: day.teamMembers` — mesma regra de
  fallback já usada em `TeamOnDutyCard`, extraída para reutilização (não
  duplicar o `if/else` em mais um lugar).

## 4. Consumidores a migrar (Checkpoint C)

| Card | Arquivo | Antes | Depois |
|---|---|---|---|
| Próximo turno — "Com:" | `TodayTab.kt:138` | `day.teamMembers` | `colleaguesForShift(day)` |
| Pausa (Hoje) | `TodayTab.kt:287` | `pauseFor(shift)` | `effectivePause(shift, notificationSettings)` — **exige nova param `notificationSettings: NotificationSettings` em `TodayTab`**, propagada de `App.kt` (já tem `appNotificationSettings` em escopo) |
| Detalhe do dia — "Com:" | `ScheduleTab.kt:405` | `day.teamMembers` | `colleaguesForShift(day)` |
| Resumo (Perfil) | `ProfileTab.kt:121,403` | `pauseFor(relevantShift)` | `effectivePause(relevantShift, notificationSettings)` (`notificationSettings` já está em escopo aqui) |
| Quem trabalha nesse dia | `ScheduleTab.kt:451` | já correto | sem mudança funcional; pode opcionalmente passar a chamar `colleaguesForShift` internamente por consistência de código, sem alterar o resultado |

`TodayTab` ganha o parâmetro novo; `App.kt` passa
`notificationSettings = appNotificationSettings` no call site (já existe
essa variável, só falta o argumento).

## 5. Compatibilidade

- `pauseCustomTime == null` (usuário nunca configurou, ou cache antigo
  sem o campo): `effectivePause` retorna exatamente o mesmo resultado que
  `pauseFor` hoje — nenhuma regressão visual para quem nunca mexeu na
  pausa customizada.
- `membersByShift` vazio (cache anterior à FASE 14H rodada 2, ou dado sem
  a estrutura nova): `colleaguesForShift` cai em `day.teamMembers` — mesmo
  comportamento (com o bug antigo) só nesse caso legado, nunca uma
  exceção.
- Nenhuma mudança de schema/serialização em cache (Android
  `SharedPreferences`, Web `localStorage`) — os dois seletores são puros,
  não persistem nada novo.

## 6. Checkpoint D — estrutura canônica do parser (confirmação, não nova implementação)

Auditoria (não requer mudança de código nesta fase, apenas confirmação
formal): `ShiftDay.sourceStatus: String?` (já existe desde antes desta
fase) **já cumpre integralmente** o papel de "código de origem preservado"
pedido pelo prompt (`assignmentType`≈`ShiftType`, `sourceStatusCode`≈
`sourceStatus`, ambos já distintos e ambos já preservados por todo o
pipeline — parser, os dois caches locais, DTO remoto). A mesma auditoria e
as mesmas tabelas de turnos/situações canônicas (`Md`/`M`/`T`/`N` e
`DU`/`DF`/`BH`/`AN`/`X`/`#`/`Folga`/`HE`) já estão formalizadas em
`docs/spec/11-DASHBOARD-FASE14I-CONTRATO-LEGENDAS-CONTABILIDADE-FOLGAS.md`
(escrita nesta mesma fase, lado Dashboard) — esta spec KMP não duplica
aquela tabela, só referencia: o contrato é um só, vale para os dois
repositórios. Nenhum parser KMP precisa mudar nesta fase para satisfazer
o Checkpoint D — a estrutura já existe; o que faltava era a UI (Checkpoint
C) e a padronização visual (Checkpoint E, Dashboard).

## 7. Critérios de aceite desta spec

1. `effectivePause`/`colleaguesForShift` são funções puras, testadas
   isoladamente (sem Compose), cobrindo os casos de compatibilidade da
   seção 5.
2. Após a migração (Checkpoint C), selecionar pausa `10:30` no Perfil e,
   **sem reiniciar o app**, navegar para Hoje mostra `10:30–10:45`; voltar
   ao Perfil mostra `10:30–10:45` no card "Resumo"; a janela `09:00–11:45`
   continua visível como texto secundário ("Janela permitida"/"Permitido
   entre"), nunca removida, nunca confundida com o horário escolhido.
3. Card "Próximo turno" e "Detalhe do dia" mostram só colegas do mesmo
   turno do usuário (validado com o mesmo cenário real: 22/07, Manhã,
   `lvergani` → só `alamancio`).
4. Card "Quem trabalha nesse dia" não regride (continua mostrando os 4
   grupos).
5. Nenhum teste dos 239 (JVM) + 232 (Wasm) existentes quebra.
