# Spec 57 — Modelo Firestore: responsáveis, solicitações e workspace

**Status:** proposta; nenhuma linha de código funcional criada por esta spec
**Escopo:** Firestore universal (spec 35 do EscalaSOC + specs 46/47/51 deste repositório)
**Fase:** FASE 14c-0 (contrato mestre, documental)
**Princípio inegociável:** estender sem substituir — nenhuma coleção existente é removida, renomeada ou tem campo obrigatório removido.

## 1. Auditoria do que já existe (antes de propor qualquer coleção nova)

Confirmado por leitura direta de `docs/firebase/firestore-universal-schema.json`,
`docs/firebase/firestore-universal-seed-example.json`,
`composeApp/.../model/UniversalOrgModels.kt`, `SPEC-DB-UNIVERSAL-TIMES-E-ESCALAS.md`
e `firebase/firestore.rules`:

| Coleção existente | Cobre | Não cobre |
|---|---|---|
| `organizations`, `org_units` | Hierarquia ICI → DIO → GEDSI → COSI (Segurança) e ICI → DIO → GST → CCS → SD (Service Desk) — **já separadas em ramos distintos**, confirmado no seed (`gerencia-data-center-seguranca`/COSI vs. `gerencia-suporte-tecnico`/CCS/SD). | Quem administra/aprova por equipe. |
| `teams` | Equipe, `orgUnitId`, `scheduleProfileId`, e já tem `adminEmails`/`approverEmails` (campos de e-mail livre, usados hoje por `isTeamAdmin()` nas Rules, spec 51). | Vínculo estruturado por `memberId` (não e-mail), papéis diferenciados, múltiplos times por responsável, vigência, substituto. |
| `members` | Cadastro do colaborador. | Nada de gestão — não deve carregar "quem é meu chefe" como campo próprio (ver spec 56 seção 7). |
| `member_team_memberships` | Vínculo membro↔equipe: pertencimento, `roleId` (cargo de exibição), vigência, `isPrimary`. | Isto é **pertencimento**, não **gestão** — um `PRIMARY_APPROVER` não é um "cargo de exibição", é uma permissão operacional. Não reaproveitado para gestão (ver decisão seção 2). |
| `roles` | Cargo/função de exibição (`roleDisplayName`, `roleShortName`), com `scope` `GLOBAL\|ORG_UNIT\|TEAM`. | Permissões concretas (aprovar, publicar, editar) — `roles` é rótulo, não capacidade. |
| `dashboard_permissions` | Acesso administrativo ao **Dashboard** por `principalEmail`, com `permissions: READ\|EDIT_MEMBERS\|EDIT_PROFILES\|IMPORT\|PUBLISH\|ADMIN`, escopo `ORGANIZATION\|ORG_UNIT\|TEAM`. | Não é sobre aprovar solicitações de escala nem sobre "quem aparece como responsável no app" — é sobre quem pode editar o Dashboard. Conceito adjacente, não duplicado (seção 2.1). |
| `schedule_change_events` | Auditoria **depois do fato** (evento já aplicado: `CREATED\|UPDATED\|SWAPPED\|CANCELLED\|PUBLISHED\|REIMPORTED`). | Não é uma solicitação **pendente de decisão humana** — é o log do que já aconteceu. Uma solicitação aprovada e aplicada deve gerar um `schedule_change_events`, não substituí-lo. |
| `shift_swap_requests` | Troca direta entre dois colegas, com aprovação do responsável via `managerUids`. | Não cobre pedidos que não são troca com colega específico (`SHIFT_CHANGE`, `DAY_OFF_CHANGE`, `SCHEDULE_CORRECTION`, `OTHER`). |
| `firestore.rules` (`isTeamAdmin`) | Autorização de escrita hoje é só por `teams.adminEmails` (e-mail em array). | Não sabe nada sobre `team_manager_assignments` — reconciliação fica para a FASE 14d (seção 8). |

**Decisão**: `member_team_assignments` (vínculo membro↔equipe genérico) **não
é criado** — `member_team_memberships` já resolve isso. O que é novo é
`team_manager_assignments` (vínculo de **gestão/aprovação**, semântica
diferente de pertencimento) e `schedule_change_requests` (solicitação
pendente, semântica diferente de evento já ocorrido).

### 1.1 Por que `team_manager_assignments` não é apenas `dashboard_permissions`

São conceitos adjacentes, não o mesmo dado:

| | `dashboard_permissions` | `team_manager_assignments` |
|---|---|---|
| Responde | "Quem pode editar o quê no Dashboard?" | "Quem é o responsável visível de uma equipe no app, e quem pode aprovar solicitações?" |
| Chave principal | `principalEmail` (transitório, até MSAL no Dashboard) | `managerMemberId` (sempre por `member`, nunca por e-mail) |
| Escopo | `ORGANIZATION \| ORG_UNIT \| TEAM` (amplo, cadastro/import/publish) | Sempre por `teamId` individual, com papel e permissões específicas de escala/aprovação |
| Consumido por | Dashboard (controle de acesso a telas) | App (exibição do responsável) + Dashboard (fila de aprovação) |

Um mesmo `member` pode ter as duas coisas ao mesmo tempo (é comum que um
`PRIMARY_APPROVER` também tenha `dashboard_permissions` para publicar), mas
uma não deriva da outra automaticamente — cadastros continuam
independentes nesta fase. Unificação futura, se fizer sentido, é uma
decisão explícita de uma fase posterior, não desta.

## 2. Regra de migração

1. Nenhuma coleção existente é apagada ou renomeada.
2. Novos campos em coleções existentes (`workspaceId`, seção 7) são sempre
   opcionais/aditivos — documentos antigos continuam válidos sem eles.
3. Leitores (app e Dashboard) tratam a ausência de `workspaceId` como
   `"ici"`/`PRODUCTION` — nunca uma migração retroativa em massa nesta fase.
4. `schemaVersion` das coleções novas começa em `1`.
5. Nenhuma coleção desta spec é lida hoje pelo app (`FirestoreRestGateway.kt`
   continua sem mudança até a FASE 14c-4/14c-5, spec 58).

## 3. Coleção nova — `team_manager_assignments`

**Finalidade:** vínculo de gestão/aprovação de um `member` sobre um ou mais
`team`. Fonte de verdade de "quem é responsável por esta equipe e o que essa
pessoa pode fazer".

**ID:** `{workspaceId}_{teamId}_{managerMemberId}_{sequencial}` (ex.:
`ici_team-example-soc_member-example-manager_01`) — permite mais de um
vínculo do mesmo par membro/equipe ao longo do tempo (ex.: encerrar um
`PRIMARY_APPROVER` e abrir outro depois), sem reescrever o histórico.

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `id` | string | sim | Ver formato acima. |
| `workspaceId` | string | sim | `"ici"` (produção) ou `"demo-v1"` (demonstração) — spec 56 seção 12. |
| `managerMemberId` | string | sim | Referência a `members/{id}`. |
| `teamId` | string | sim | Referência a `teams/{id}`. Um vínculo cobre **uma** equipe; um responsável de várias equipes tem um documento por equipe. |
| `role` | string | sim | Um de `PRIMARY_MANAGER \| PRIMARY_APPROVER \| BACKUP_APPROVER \| SCHEDULE_EDITOR \| PUBLISHER \| VIEW_ONLY` (spec 56 seção 6). |
| `permissions` | map<string, boolean> | sim | Chaves fixas: `viewTeamSchedule`, `viewTeamMembers`, `editTeamSchedule`, `approveScheduleChanges`, `publishSchedule`, `manageTeamAssignments`. Todas obrigatoriamente presentes (mesmo que `false`) — nunca omitir uma chave assumindo `false` implícito. |
| `active` | boolean | sim | Desativação lógica (spec 56 seção 8). |
| `validFrom` | date string (`YYYY-MM-DD`) | sim | Início da vigência. |
| `validTo` | date string \| null | não | Fim da vigência; `null` = sem data de término. |
| `createdAt` | Timestamp | sim | Criação do documento. |
| `updatedAt` | Timestamp | sim | Última escrita. |
| `createdBy` | string | sim | E-mail/login de quem cadastrou (auditoria, mesmo padrão de `user_links.linkedBy`, spec 47). |
| `schemaVersion` | number | sim | `1`. |

**Índices esperados:**

```text
team_manager_assignments: teamId + active
team_manager_assignments: managerMemberId + active
team_manager_assignments: workspaceId + teamId + active
```

**Leitura:** qualquer usuário autenticado (Dashboard) ou leitura mínima
anônima temporária, se algum dia o app precisar ler diretamente (decisão
adiada — nesta fase o app ainda não lê Firestore fora das 6 coleções da
spec 51; ler `team_manager_assignments` no app entra em vigor só na FASE
14c-5, spec 58, e reavalia se precisa de sessão real primeiro).
**Escrita:** apenas `isSystemAdmin()` ou um responsável com
`manageTeamAssignments == true` na própria equipe — regra concreta de Rules
fica para a FASE 14d/14c-3 (implementação), não para esta spec.

**Histórico:** desativar nunca apaga (`active = false`, documento
preservado). Trocar de responsável = desativar o vínculo antigo e criar um
novo, nunca sobrescrever `managerMemberId` no mesmo documento.

**Exemplo fictício (workspace de produção):**

```json
{
  "id": "ici_soc_member-example-manager_01",
  "workspaceId": "ici",
  "managerMemberId": "member-example-manager",
  "teamId": "team-example-soc",
  "role": "PRIMARY_APPROVER",
  "permissions": {
    "viewTeamSchedule": true,
    "viewTeamMembers": true,
    "editTeamSchedule": false,
    "approveScheduleChanges": true,
    "publishSchedule": false,
    "manageTeamAssignments": false
  },
  "active": true,
  "validFrom": "2026-07-01",
  "validTo": null,
  "createdAt": "server timestamp",
  "updatedAt": "server timestamp",
  "createdBy": "admin@example.invalid",
  "schemaVersion": 1
}
```

**Exemplo fictício (workspace Demo, um responsável para duas equipes):**

```json
[
  {
    "id": "demo-v1_team-demo-soc_member-demo-gestor-seguranca_01",
    "workspaceId": "demo-v1",
    "managerMemberId": "member-demo-gestor-seguranca",
    "teamId": "team-demo-soc",
    "role": "PRIMARY_APPROVER",
    "permissions": {
      "viewTeamSchedule": true, "viewTeamMembers": true,
      "editTeamSchedule": false, "approveScheduleChanges": true,
      "publishSchedule": false, "manageTeamAssignments": false
    },
    "active": true, "validFrom": "2026-07-01", "validTo": null,
    "createdAt": "server timestamp", "updatedAt": "server timestamp",
    "createdBy": "demo-admin@example.invalid", "schemaVersion": 1
  },
  {
    "id": "demo-v1_team-demo-seguranca_member-demo-gestor-seguranca_01",
    "workspaceId": "demo-v1",
    "managerMemberId": "member-demo-gestor-seguranca",
    "teamId": "team-demo-seguranca",
    "role": "PRIMARY_APPROVER",
    "permissions": {
      "viewTeamSchedule": true, "viewTeamMembers": true,
      "editTeamSchedule": false, "approveScheduleChanges": true,
      "publishSchedule": false, "manageTeamAssignments": false
    },
    "active": true, "validFrom": "2026-07-01", "validTo": null,
    "createdAt": "server timestamp", "updatedAt": "server timestamp",
    "createdBy": "demo-admin@example.invalid", "schemaVersion": 1
  }
]
```

**Consultas do Dashboard:** listar todos os vínculos de uma equipe (tela
"Responsáveis cadastrados"); listar todas as equipes de um responsável
("Minhas equipes" no futuro modo gestor do Dashboard, se vier a existir).
**Consultas do app:** resolver o(s) responsável(is) ativo(s) de
`memberId` autenticado → `teamId` → `team_manager_assignments` (spec 56
seção 7.1, precedência).

## 4. Coleção nova — `schedule_change_requests`

**Finalidade:** solicitação de alteração de escala feita por um colaborador,
endereçada ao responsável resolvido no momento da criação.

**ID:** `{workspaceId}_{teamId}_{requesterMemberId}_{timestamp ou sequencial}`.

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `id` | string | sim | Ver formato acima. |
| `workspaceId` | string | sim | `"ici"` ou `"demo-v1"`. |
| `requesterMemberId` | string | sim | Referência a `members/{id}`. |
| `requesterTeamId` | string | sim | Equipe do solicitante no momento da criação. |
| `assignedManagerMemberId` | string | sim | Fotografia do responsável resolvido (spec 56 seção 9.3) — **imutável após criação**. |
| `schedulePeriodId` | string | sim | Referência a `schedule_periods/{id}`. |
| `assignmentId` | string | não | Referência a `schedule_assignments/{id}`, quando aplicável (`SHIFT_CHANGE`/`DAY_OFF_CHANGE`/`SCHEDULE_CORRECTION`). |
| `requestType` | string | sim | `SHIFT_CHANGE \| DAY_OFF_CHANGE \| SWAP_WITH_MEMBER \| SCHEDULE_CORRECTION \| OTHER`. |
| `status` | string | sim | `DRAFT \| PENDING \| APPROVED \| REJECTED \| CANCELLED \| EXPIRED`. |
| `reason` | string | sim | Motivo declarado pelo solicitante. |
| `createdAt` | Timestamp | sim | Criação. |
| `resolvedAt` | Timestamp \| null | não | Quando saiu de `PENDING`. |
| `resolvedByMemberId` | string \| null | não | Quem decidiu (deve ter `approveScheduleChanges == true` no momento da decisão). |
| `resolutionNote` | string \| null | condicional | Obrigatório quando `status == REJECTED` (spec 56 seção 9.4). |
| `schemaVersion` | number | sim | `1`. |

**Índices esperados:**

```text
schedule_change_requests: workspaceId + assignedManagerMemberId + status
schedule_change_requests: workspaceId + requesterMemberId + status
schedule_change_requests: workspaceId + requesterTeamId + status
```

**Leitura:** o próprio `requesterMemberId` (suas solicitações),
`assignedManagerMemberId` (fila de aprovação) e `isSystemAdmin()`.
**Escrita:** criação pelo próprio solicitante (`requesterMemberId ==` sessão
atual, quando existir sessão real); transição de estado apenas por quem tem
`approveScheduleChanges` na equipe (`APPROVED`/`REJECTED`), pelo próprio
solicitante (`CANCELLED`, só enquanto `PENDING`), ou automática por regra de
expiração (`EXPIRED`, mecanismo de expiração fica para implementação
futura).
**Histórico:** nunca apagar — todo estado final (`APPROVED`/`REJECTED`/
`CANCELLED`/`EXPIRED`) permanece como registro consultável.

**Exemplo fictício:**

```json
{
  "id": "ici_team-example-soc_member-example-requester_20260802",
  "workspaceId": "ici",
  "requesterMemberId": "member-example-requester",
  "requesterTeamId": "team-example-soc",
  "assignedManagerMemberId": "member-example-manager",
  "schedulePeriodId": "period-example-2026-08",
  "assignmentId": "assignment-example-2026-08-02",
  "requestType": "SHIFT_CHANGE",
  "status": "PENDING",
  "reason": "Consulta médica agendada",
  "createdAt": "server timestamp",
  "resolvedAt": null,
  "resolvedByMemberId": null,
  "resolutionNote": null,
  "schemaVersion": 1
}
```

**Consultas do Dashboard:** fila de pendentes por responsável autenticado/
selecionado (`assignedManagerMemberId + status == PENDING`); histórico por
equipe. **Consultas do app:** "minhas solicitações"
(`requesterMemberId + workspaceId`), ordenado por `createdAt` desc.

## 5. Coleção nova — `workspaces`

**Finalidade:** metadados do ambiente lógico de dados (produção ou
demonstração) — spec 56 seção 12.8.

**ID:** o próprio `workspaceId` (`ici` ou `demo-v1`).

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `workspaceId` | string | sim | Chave primária, igual ao ID do documento. |
| `workspaceType` | string | sim | `PRODUCTION \| DEMO`. |
| `scenarioId` | string \| null | não | Ex.: `"schedule-approval-basic"`. Só relevante para `DEMO`. |
| `seedVersion` | number \| null | não | Versão das fixtures carregadas (só `DEMO`). |
| `publicationRevision` | number | sim | Incrementa a cada publicação neste workspace. Começa em `0`. |
| `externalEffectsAllowed` | boolean | sim | `false` para `DEMO` (nunca notificação/e-mail/Graph real); `true` para `PRODUCTION`. |
| `notificationsEnabled` | boolean | sim | Idem — `false` para `DEMO`. |
| `updatedAt` | Timestamp | sim | Última publicação/alteração de metadado. |

**Leitura:** qualquer usuário autenticado do Dashboard; app lê só para
diagnóstico (seção "Última revisão sincronizada", spec 56 seção 12.8), nunca
obrigatório na tela principal. **Escrita:** apenas o backend Express local
(Firebase Admin) durante publicação/reset — nunca escrita direta do
navegador.

**Exemplos fictícios:**

```json
[
  {
    "workspaceId": "ici",
    "workspaceType": "PRODUCTION",
    "scenarioId": null,
    "seedVersion": null,
    "publicationRevision": 0,
    "externalEffectsAllowed": true,
    "notificationsEnabled": true,
    "updatedAt": "server timestamp"
  },
  {
    "workspaceId": "demo-v1",
    "workspaceType": "DEMO",
    "scenarioId": "schedule-approval-basic",
    "seedVersion": 1,
    "publicationRevision": 1,
    "externalEffectsAllowed": false,
    "notificationsEnabled": false,
    "updatedAt": "server timestamp"
  }
]
```

## 6. Coleção nova — `publication_records`

**Finalidade:** registro de auditoria de cada publicação feita pelo backend
Express local, sem segredo (spec 56 seção 9 do prompt original: "registro de
publicação sem segredo").

**ID:** `{workspaceId}_{publicationRevision}`.

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `id` | string | sim | Ver formato acima. |
| `workspaceId` | string | sim | Workspace afetado (um único por publicação — nunca misturar `ici` e `demo-v1` no mesmo pacote, ver schema JSON). |
| `publicationRevision` | number | sim | Igual ao valor gravado em `workspaces/{workspaceId}` após a publicação. |
| `publishedAt` | Timestamp | sim | Quando ocorreu. |
| `publishedByMode` | string | sim | `LOCAL_TEST_MODE \| CORPORATE_MSAL` (spec 56 seção 11 — quase sempre `LOCAL_TEST_MODE` nesta fase). |
| `publishedByLogin` | string | não | Login/e-mail de teste selecionado (nunca uma credencial real). |
| `dryRun` | boolean | sim | `true` quando foi só validação/preview, sem escrita real. |
| `countsCreated` | number | sim | Quantidade de documentos novos. |
| `countsUpdated` | number | sim | Quantidade de documentos alterados. |
| `countsDeleted` | number | sim | Quantidade de exclusões — **sempre `0`** enquanto exclusão destrutiva estiver desabilitada (spec 56/58). |
| `source` | string | sim | `DASHBOARD_MANUAL_PUBLISH \| DEMO_RESET \| DEMO_SEED`. |
| `schemaVersion` | number | sim | `1`. |

**Leitura:** `isSystemAdmin()` e responsáveis com `publishSchedule` na
própria equipe (filtro por equipes afetadas, quando implementado).
**Escrita:** só o backend Express local, nunca o navegador.
**Histórico:** nunca apagar — é o próprio histórico.

## 7. Campo aditivo `workspaceId` nas coleções existentes

Adicionado como **campo opcional** às coleções abaixo, sem remover nem
renomear nada:

```text
teams
members
member_team_memberships
schedule_periods
schedule_assignments
oncall_periods
oncall_assignments
schedule_change_events
```

`schedule_change_events` entra nesta lista porque a FASE 14c-8 (spec 58) grava
um evento nessa coleção sempre que uma `schedule_change_requests` aprovada é
aplicada na escala — sem `workspaceId` nesse evento, um evento gerado no
workspace Demo não teria como ser isolado/filtrado separadamente do
workspace de produção, quebrando o requisito de isolamento da spec 56 seção
12.4.

Regra de leitura: ausência de `workspaceId` == `"ici"`. Regra de escrita: o
backend de publicação sempre grava `workspaceId` explicitamente em qualquer
documento novo a partir desta fase, tanto para produção quanto para Demo —
a omissão só é aceitável em documentos que já existiam antes desta spec.

## 8. Reconciliação pendente (não resolvida nesta spec, registrada)

`isTeamAdmin()` (`firebase/firestore.rules`) hoje resolve autorização de
escrita por `teams.adminEmails` (lista de e-mails), não por
`team_manager_assignments` (lista de `memberId`). As duas fontes vão
coexistir até a FASE 14d (rules definitivas): `team_manager_assignments` é a
fonte de verdade para **exibição no app e fila de aprovação**;
`adminEmails`/`approverEmails` continuam sendo a fonte para **autorização de
escrita no Firestore**, porque uma regra do Firestore não pode hoje resolver
"o e-mail autenticado corresponde a qual `memberId`" sem uma leitura extra
indexada por e-mail — o mesmo problema já registrado na spec 47 seção 7
(tensão `user_links` vs. `app_user_preferences` por e-mail). Reconciliar as
duas listas (ex.: `isTeamAdmin()` passando a fazer `get()` em
`team_manager_assignments` por `managerMemberId` resolvido de
`user_links`) é trabalho da FASE 14d, não desta fase.

## 9. Critérios de aceite

1. Nenhuma coleção existente foi removida, renomeada ou teve campo
   obrigatório removido.
2. `member_team_assignments` não foi criado — `member_team_memberships`
   (já existente) continua sendo a fonte de pertencimento membro↔equipe.
3. `team_manager_assignments` e `schedule_change_requests` têm todos os
   campos, índices, exemplos fictícios e consultas exigidos pelo prompt
   desta fase.
4. `workspaceId`/`workspaceType`/`scenarioId`/`seedVersion`/
   `publicationRevision`/`externalEffectsAllowed`/`notificationsEnabled`
   estão definidos e aplicáveis a todas as coleções relevantes.
5. Nenhum exemplo usa tenant real, client ID real, e-mail real, login real,
   hash, redirect, token, senha ou credencial Firebase.
6. A reconciliação pendente com `firestore.rules` está registrada
   explicitamente, não escondida nem resolvida por acidente.
