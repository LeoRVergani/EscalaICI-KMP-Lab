# FASE 14H — Gate de contrato: pacote oficial (Dashboard) × leitura (KMP)

## Contexto

Antes de alterar o KMP nesta fase, esta spec fixa — campo a campo, por leitura
direta do código dos dois repositórios (não por suposição) — o que o
Dashboard passou a escrever em `workspaces/ici-dev/revisions/{n}/{coleção}`
nas quatro rodadas da FASE 14H (`escala-dashboard`, branch
`feature/fase-14h-dashboard-parser-oncall-push`) e o que o KMP hoje lê.
Complementa a auditoria já feita na FASE 14f-3 (spec 64), que permanece
válida — esta spec só documenta o que mudou desde então.

## 1. Tabela campo a campo

| Entidade / coleção | Campo | Tipo | Obrigatório | Produtor (Dashboard) | Consumidor (KMP) | Situação |
|---|---|---|---|---|---|---|
| `members` | `corporateLogin` | string | sim | `buildFromSchedule.ts` (`corporateEmailFromLogin`) | `OrganizationRepositories.kt` (`loginByMemberId`) | OK — corrigido na FASE 14f-3, sem mudança nesta fase |
| `teams` | `acronym`, `active` | string/bool | não | escrito sempre | `Team` (KMP) não tem esses campos | lacuna pré-existente (spec 64), fora de escopo |
| `schedulePeriods` | `groupId` | string\|null | só plantão | **novo nesta fase** (`buildFromSchedule.ts`) | **não lido ainda** | **gate: Checkpoint C precisa consumir** |
| `scheduleAssignments` | `groupId` | string\|null | só plantão | **novo nesta fase** | **não lido ainda** | **gate: Checkpoint C precisa consumir** |
| `scheduleAssignments` | `assignmentType` | enum string | sim | `'WORK_SHIFT'\|'OFF'\|'VACATION'\|'ABSENCE'\|'TRAINING'\|'OTHER'` (inalterado) | `toShiftType()`/`shiftTypeFrom()` (duplicado em `FirebaseSources.kt` e `DemoPublicationDtos.kt`) só reconhecem `OFF→FOLGA`, `VACATION→FERIAS`, `WORK_SHIFT→`lookup por `shiftName` | ver item 2 abaixo |
| `scheduleAssignments` | `shiftName` (quando `assignmentType==='OFF'`) | string\|null | não | **novo nesta fase**: `'BH'`, `'Aniversário'` ou `null` (folga comum) — `offShiftName()` | **não distinguido** — todo `OFF` vira `ShiftType.FOLGA` | **gate: Checkpoint C precisa distinguir BH/Aniversário/folga comum** |
| `scheduleAssignments` | `shiftName` (quando `assignmentType==='OTHER'`) | string | sim quando `OTHER` | **novo nesta fase**: `'Sem dado importado'` (buraco de importação) ou `'Trabalho sem turno localizado (<código>)'` (inconsistência bloqueada no servidor, só chega ao KMP se alguém tentar um DRY_RUN que falhe — não deve aparecer numa revisão realmente publicada, já que o servidor bloqueia o COMMIT) | `ShiftType.INDEFINIDO`/`ShiftType.INCONSISTENCIA` já existem como enum, mas não são alimentados a partir deste campo | **gate: mapear para os enums já existentes, ver item 2** |
| `scheduleChangeRequests` | (todos os campos) | — | não | escrito pela correção pontual FASE 14G (motivo, resolução) | KMP não lê esta coleção hoje | fora de escopo desta fase (não é consumido pela UI) |
| `team_manager_assignments` | `role` | enum | não | 6 valores possíveis | 4 colapsam em `OTHER` | lacuna pré-existente (spec 64), fora de escopo |

## 2. Resolução: mapeamento de `assignmentType`/`shiftName` → `ShiftType` (KMP)

O KMP **já tem** todos os enums necessários em `ShiftType`
(`model/ScheduleModels.kt`): `MANHA, TARDE, NOITE, MADRUGADA, FOLGA, FERIAS,
BH, ANIVERSARIO, HORA_EXTRA, AFASTAMENTO, INCONSISTENCIA, INDEFINIDO` — o
problema é que as duas funções de conversão (`FirebaseSources.kt:209`
`toShiftType()` e `DemoPublicationDtos.kt:206` `shiftTypeFrom()`, ambas
duplicadas, mesma lógica) só implementam 3 ramos. Nova regra a implementar
nas duas (ou consolidar em uma função só, reduzindo a duplicação — decisão
do Checkpoint C):

```
assignmentType == 'WORK_SHIFT' → lookup por shiftName (já existe)
assignmentType == 'VACATION'   → FERIAS (já existe)
assignmentType == 'ABSENCE'    → AFASTAMENTO (já existe, hoje não mapeado — conferir)
assignmentType == 'OFF':
  shiftName == 'BH'            → BH
  shiftName == 'Aniversário'   → ANIVERSARIO
  shiftName == null/outro      → FOLGA
assignmentType == 'OTHER':
  shiftName == 'Sem dado importado'                         → INDEFINIDO
  shiftName começa com 'Trabalho sem turno localizado'      → INCONSISTENCIA
  outro                                                     → INDEFINIDO (fallback seguro)
assignmentType == 'TRAINING'   → não há enum específico hoje; usar INDEFINIDO ou HORA_EXTRA conforme fizer mais sentido (decisão do Checkpoint C, documentar a escolha)
```

Nenhuma migração de nome é necessária — os enums já existem, só a leitura
precisa ser completada. Isso resolve, no consumidor, a mesma granularidade
que o produtor já preserva desde a Rodada 1 do Checkpoint B.

## 3. Resolução: `groupId` (plantão)

`OnCallPeriod`/`OnCallAssignment` já existem em `model/DomainModels.kt`
(KMP), sem campo de grupo. Adicionar:

```kotlin
data class OnCallGroup(val id: String, val teamId: String, val name: String, val active: Boolean)
```

e `groupId: String? = null` em `OnCallPeriod`/`OnCallAssignment` (default
`null` para não quebrar nenhuma leitura/teste existente que ainda não
popula esse campo). Fonte remota (`FirebaseSources.kt`/
`FirebaseOnCallSource`) passa a ler `groupId` do documento Firestore
(campo string opcional) e propagar para o modelo.

## 4. Nenhuma mudança de schema remoto real nesta execução

Esta spec só documenta o contrato já publicado pelo Dashboard (nas branches
de trabalho, ainda sem merge/publicação real) — nenhum documento Firestore
real (`ici-dev`, revisões 1/2) é alterado por esta spec ou pelo Checkpoint
C. A leitura remota do KMP contra revisões futuras (3+) só fará sentido
quando uma publicação real acontecer — fora do escopo autorizado nesta
execução.

## 5. Divergências resolvidas vs. pendentes

- **Resolvida** (FASE 14f-3, spec 64): `corporateLogin` nunca lido no
  caminho remoto — corrigido, sem mudança nesta fase.
- **Pendente, escopo desta fase**: `groupId` (plantão) e granularidade de
  `assignmentType`/`shiftName` (BH/Aniversário/inconsistência) — endereçadas
  no Checkpoint C.
- **Pendente, fora de escopo** (documentadas, não corrigidas): `teams.acronym`/
  `active`, `team_manager_assignments.role` (4 valores colapsando em
  `OTHER`), `schedule_periods.updatedAt` nunca escrito. Nenhuma dessas afeta
  a resolução de identidade ou a leitura de escala/plantão — mantidas como
  lacunas conhecidas, mesma decisão da spec 64.
