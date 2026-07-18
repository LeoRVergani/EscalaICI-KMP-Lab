# Spec 56 — Gestão de responsáveis, aprovadores e ambiente de demonstração

**Status:** proposta; nenhuma linha de código funcional criada por esta spec
**Escopo:** Dashboard (React, repositório irmão), Escala ICI KMP (Android + Web/PWA), Firestore universal
**Fase:** FASE 14c-0 (contrato mestre, documental) — implementação prevista para FASE 14c-1 em diante (spec 58)
**Depende de:** spec 35 do EscalaSOC (Firestore universal), specs 46/47/50/53 deste repositório (MSAL, `user_links`, matriz de paridade), spec 51 (regras transitórias)
**Não implementa:** MSAL no Dashboard, Cloud Functions, Firebase Authentication no KMP, Firestore Rules novas, dashboard, app, parser, Gradle ou versão

## 1. Objetivo

Definir o contrato conceitual e de dados para que o Escala ICI (Dashboard +
app) saiba, para cada equipe, **quem é responsável por ela**, **quem pode
aprovar mudanças de escala**, e como um colaborador **solicita uma alteração**
e um responsável **aprova ou recusa**. Esta spec também define um **ambiente
de demonstração** oficial (`workspaceType: DEMO`) para validar esse fluxo
ponta a ponta (Dashboard → Firebase → app) sem tocar em nenhuma equipe ou
colaborador real.

Esta spec é a continuação natural de:

```text
Dashboard publica a organização
        ↓
App identifica colaborador e equipe (spec 46/47)
        ↓
App descobre o responsável/aprovador da equipe (esta spec)
        ↓
Colaborador solicita alteração (esta spec)
        ↓
Gestor recebe e aprova ou recusa (esta spec)
```

## 2. Escopo

- Modelo conceitual de responsáveis/aprovadores por equipe (vínculo, papéis,
  permissões, vigência, substituição).
- Modelo conceitual de solicitação de alteração de escala.
- Modelo conceitual do ambiente de demonstração (`workspace`), usado para
  testar o fluxo completo sem dados reais.
- Fluxo de publicação transitório (Dashboard → Express local → Firebase Admin
  → Firestore), sem Cloud Functions.
- Estados típicos de resolução no app (membro/equipe/responsável encontrados
  ou não).
- Evolução prevista de uma "área do gestor" dentro do mesmo aplicativo
  Escala ICI.

## 3. Não escopo (fica para fases posteriores, ver spec 58)

- Implementação de qualquer tela, endpoint, Cloud Function ou regra real.
- MSAL no Dashboard (continua login manual/local nesta fase e nas seguintes,
  até a FASE 14c-10).
- Aplicação automática de uma solicitação aprovada diretamente na escala sem
  confirmação humana (primeira versão exige um passo de confirmação
  explícita, seção 11).
- Notificações push/e-mail reais.
- Um segundo aplicativo para gestores (explicitamente rejeitado, seção 10).
- Alteração de `firestore.rules` publicadas (spec 51 continua vigente até a
  FASE 14d).

## 4. Termos oficiais

Para não prender o domínio à palavra "chefe" (que descreve apenas um caso
específico e culturalmente carregado), o termo interno é **responsável** ou,
em inglês/código, **manager assignment**. A interface do colaborador pode
continuar simples ("Responsável da equipe"); o modelo interno é que precisa
ser mais preciso.

| Termo interno | Uso |
|---|---|
| `member` | Colaborador cadastrado (coleção `members`, já existente). |
| `team` | Equipe (coleção `teams`, já existente). |
| `member_team_membership` | Vínculo membro↔equipe já existente (spec 35, `member_team_memberships`) — pertencimento comum, não gestão. |
| `team manager assignment` | Vínculo de **gestão/aprovação** de um `member` sobre um ou mais `team` (novo, coleção `team_manager_assignments`, spec 57). |
| `schedule change request` | Solicitação de alteração de escala feita por um `member` (novo, coleção `schedule_change_requests`, spec 57). |
| `workspace` | Ambiente lógico de dados: `PRODUCTION` (`ici`) ou `DEMO` (`demo-v1`) — seção 12. |

Nunca usar "chefe"/"boss" como nome de campo, papel ou coleção — apenas como
possível rótulo livre de exibição, se algum dia necessário.

## 5. Modelo conceitual — membro e equipe (existente, não alterado)

`members` e `teams` já existem (`SPEC-DB-UNIVERSAL-TIMES-E-ESCALAS.md`,
spec 35 do EscalaSOC). O vínculo comum membro↔equipe (pertencimento, cargo de
exibição, vigência) já existe em `member_team_memberships`
(`UniversalOrgModels.kt`, `docs/firebase/firestore-universal-schema.json`).
Esta spec **não recria** esse vínculo — ele já resolve "colaborador X
pertence à equipe Y, com cargo Z, desde a data W".

O que não existe é o vínculo de **gestão/aprovação**: "colaborador X é
responsável pela equipe Y, pode aprovar trocas, é o principal ou o substituto".
É isso que a seção 6 define.

## 6. Papéis de gestão (`TeamManagerRole`)

```text
PRIMARY_MANAGER      — responsável principal da equipe (gestor operacional)
PRIMARY_APPROVER      — aprovador principal de solicitações de escala
BACKUP_APPROVER       — aprovador substituto (férias, ausência, licença)
SCHEDULE_EDITOR        — pode editar a escala da equipe, sem aprovar solicitações
PUBLISHER              — pode publicar a escala da equipe no Firestore
VIEW_ONLY              — observador (ex.: coordenação superior), sem permissão de escrita
```

Um mesmo `member` pode ter **mais de um papel na mesma equipe** (ex.:
`PRIMARY_MANAGER` + `PRIMARY_APPROVER` simultaneamente) e **papéis
diferentes em equipes diferentes** (ex.: `PRIMARY_APPROVER` no SOC e
`BACKUP_APPROVER` na Segurança da Informação). O papel é sempre um atributo
do **vínculo** (`team_manager_assignment`), nunca do `member` isoladamente —
o mesmo `member` pode ser `VIEW_ONLY` em uma equipe e `PRIMARY_APPROVER` em
outra.

### 6.1 Permissões (`TeamManagerPermission`)

As permissões são **independentes do papel** — o papel é só um rótulo de
exibição/atalho de cadastro; quem decide o que a pessoa pode de fato fazer é
o conjunto de permissões concedido no vínculo:

```text
viewTeamSchedule          — ver a escala da equipe
viewTeamMembers           — ver a lista de colaboradores da equipe
editTeamSchedule          — editar assignments da equipe
approveScheduleChanges     — aprovar/recusar schedule_change_requests
publishSchedule            — publicar uma nova revisão da escala no Firestore
manageTeamAssignments       — cadastrar/editar outros team_manager_assignments da equipe
```

Um `PRIMARY_APPROVER` recém-cadastrado, por padrão de UI, tende a ganhar
`viewTeamSchedule` + `viewTeamMembers` + `approveScheduleChanges`, mas **o
Dashboard sempre mostra e permite editar as seis permissões individualmente**
— nunca uma tela que só deixa escolher o papel e deriva o resto de forma
oculta. Isso evita, por exemplo, que um `VIEW_ONLY` acabe com
`publishSchedule` só porque alguém assumiu que o nome do papel bastava.

## 7. Um responsável, vários times / vários responsáveis, um time

- Um `member` pode ter um `team_manager_assignment` ativo para **cada** equipe
  que administra — não há limite de quantas equipes um responsável cobre.
- Uma equipe pode ter **mais de um** `team_manager_assignment` ativo
  simultaneamente (ex.: um `PRIMARY_APPROVER` e um `BACKUP_APPROVER`, ou dois
  `PRIMARY_MANAGER` durante uma transição de cargo).
- Não há uma coleção "responsável único da equipe" — a pergunta "quem é o
  responsável do SOC hoje" é sempre uma **consulta** (`team_manager_assignments`
  filtrado por `teamId` + `active == true`), nunca um campo único
  sobrescrito em `teams`.

### 7.1 Precedência

Quando mais de um vínculo ativo poderia resolver uma solicitação (ex.: um
`PRIMARY_APPROVER` e um `BACKUP_APPROVER` simultaneamente ativos), a ordem de
precedência para decidir **quem aparece como "o responsável" na tela do
colaborador** é:

1. `PRIMARY_MANAGER` com `approveScheduleChanges == true`, se só houver um.
2. `PRIMARY_APPROVER` ativo.
3. `BACKUP_APPROVER` ativo, apenas se não houver `PRIMARY_APPROVER` ativo no
   momento (ver seção 8, substituição temporária).
4. Se houver mais de um `PRIMARY_APPROVER`/`PRIMARY_MANAGER` ativo ao mesmo
   tempo (situação transitória, ex. troca de gestor em andamento), a tela do
   colaborador mostra o de `validFrom` mais recente; o Dashboard mostra
   **todos**, sem ocultar a ambiguidade do administrador.

A precedência é só para **exibição** — uma `schedule_change_request` nova
sempre grava `assignedManagerMemberId` resolvido no momento da criação
(seção 9.3), nunca recalcula depois.

## 8. Férias e substituição temporária

Substituição temporária **não move nem edita** o vínculo do responsável
principal — ela é um **novo vínculo `BACKUP_APPROVER`** com `validFrom`/
`validTo` cobrindo o período de ausência. Quando o período termina
(`validTo` no passado), o vínculo do substituto naturalmente deixa de estar
"ativo por vigência" e a precedência (seção 7.1) volta a resolver para o
`PRIMARY_APPROVER` original — sem nenhuma ação manual de "restaurar".

`active` (booleano) e vigência (`validFrom`/`validTo`) são **dimensões
independentes**: `active == false` é uma decisão administrativa explícita
("este vínculo não vale mais, mesmo dentro da janela de datas"); estar fora
da janela `validFrom`/`validTo` é uma condição temporal automática. Um
vínculo só conta como "responsável de fato agora" quando as duas condições
são verdadeiras: `active == true` **e** a data atual está dentro de
`[validFrom, validTo ou infinito)`.

## 9. Solicitação de alteração de escala

### 9.1 Tipos iniciais (`ScheduleChangeRequestType`)

```text
SHIFT_CHANGE        — troca de turno num dia específico
DAY_OFF_CHANGE       — alteração de folga
SWAP_WITH_MEMBER      — troca com outro colaborador (complementa shift_swap_requests já existente, não substitui)
SCHEDULE_CORRECTION   — correção de um erro na escala publicada
OTHER                 — qualquer outro motivo, com `reason` livre obrigatório
```

`SWAP_WITH_MEMBER` não duplica `shift_swap_requests` (já existente,
`SPEC-DB-UNIVERSAL-TIMES-E-ESCALAS.md` seção 4) — é o **motivo declarado**
de uma solicitação dirigida ao responsável, útil quando a equipe ainda não
usa o fluxo de troca direta entre colegas. As duas coleções podem coexistir;
reconciliar isso definitivamente (uma virar caso especial da outra) fica
para uma fase posterior, fora do escopo desta spec.

### 9.2 Estados (`ScheduleChangeRequestStatus`)

```text
DRAFT       — rascunho, ainda não enviado ao responsável
PENDING     — enviado, aguardando decisão
APPROVED    — aprovado pelo responsável
REJECTED    — recusado (exige `resolutionNote`, seção 9.4)
CANCELLED   — cancelado pelo próprio solicitante antes de uma decisão
EXPIRED     — período/assignment referenciado não existe mais, ou prazo administrativo esgotado
```

### 9.3 Fotografia do responsável no momento da criação

`assignedManagerMemberId` (e, no workspace Demo, também `assignedTeamId`,
seção 12) é **gravado no momento da criação da solicitação**, resolvido pela
precedência da seção 7.1, e nunca recalculado depois. Se o responsável da
equipe mudar enquanto a solicitação está `PENDING`, a solicitação **não**
migra silenciosamente para o novo responsável — ela continua endereçada a
quem estava designado quando foi criada. Um responsável novo vê apenas as
solicitações criadas depois de sua designação; solicitações antigas
pendentes de um responsável desativado aparecem para o administrador do
sistema como uma fila que precisa de atenção manual (reatribuição explícita,
não implementada nesta fase — registrado como lacuna conhecida).

### 9.4 Recusa exige justificativa

Uma transição para `REJECTED` sempre exige `resolutionNote` preenchido — o
Dashboard não permite recusar sem motivo. Aprovação não exige nota (mas
aceita uma opcional).

## 10. Interface prevista

### 10.1 Dashboard

Novo menu:

```text
Configurações
└── Responsáveis e aprovações
```

Tela "Responsáveis cadastrados": tabela com nome, login corporativo (só
exibição/busca, nunca chave), times administrados, papel, ações
ativar/desativar. Formulário "Adicionar responsável": nome, login
corporativo, e-mail corporativo, cargo exibido, times administrados (multi-
seleção), papel, as seis permissões (seção 6.1) como toggles individuais,
data inicial, data final.

Nova aba "Solicitações": filtros (pendentes/aprovadas/recusadas/todos os
times/meu time/período/colaborador), card por solicitação com nome do
colaborador, equipe, turno atual → solicitado, motivo, botões
Aprovar/Recusar/Ver escala.

### 10.2 App (Escala ICI)

```text
Minha equipe
SOC

Responsável principal
Nome do responsável — Cargo

Responsável substituto (se houver um BACKUP_APPROVER ativo)
Nome do substituto

Minhas solicitações
[ Solicitar alteração ]
```

O login corporativo e o e-mail do responsável **nunca aparecem** na tela do
colaborador comum — apenas nome, cargo e equipe (mesmo princípio de não
expor identificadores técnicos já aplicado ao `tenantId` na spec 53 seção 7).
O `memberId` do responsável é usado só internamente, para endereçar a
solicitação.

### 10.3 Área do gestor (mesmo aplicativo)

Ver spec 58 para o roadmap de fases. Regra arquitetural fixada aqui: **um
único aplicativo Escala ICI**, com a área do gestor aparecendo condicionada a
`team_manager_assignments` ativo do usuário autenticado — nunca um segundo
app. Razão: evitar duas autenticações, duas bases de código e duas rotinas
de atualização para o mesmo domínio de dados.

Primeira etapa da área do gestor: somente leitura (equipes gerenciadas,
colaboradores, escalas, solicitações). Etapa posterior: aprovar, recusar,
justificar, solicitar mais informação, aplicar alteração.

## 11. Modo de teste sem MSAL no Dashboard

Enquanto o Dashboard não tem MSAL (spec 58, FASE 14c-10), o cadastro inicial
de responsáveis e o teste do fluxo de aprovação usam um **seletor de
identidade de teste**, nunca um campo de texto livre:

```text
Modo local de teste — sem autenticação corporativa
Gestor de teste: [ Nome do responsável — login ]
```

Um campo de texto livre permitiria qualquer pessoa com acesso à página
digitar outro login e se passar por outro gestor — a seleção a partir de uma
lista de responsáveis já cadastrados no Firestore elimina esse risco de
digitação, embora **não seja autenticação real** (ver aviso obrigatório,
seção 11.1). O seletor de teste é a mesma peça reaproveitada pelo workspace
Demo (seção 12.6): "gestor de teste" em produção-sem-MSAL e "personagem
Demo" no workspace Demo são a mesma capacidade de UI, usada em dois
contextos diferentes.

### 11.1 Aviso visual obrigatório

Sempre que o modo local de teste estiver ativo (produção sem MSAL, ou
qualquer sessão de Dashboard antes da FASE 14c-10), a interface deve exibir,
de forma permanente e não descartável:

```text
Modo local de teste — sem autenticação corporativa
Este login não concede autorização real. Não deve ser usado em produção
como mecanismo de segurança.
```

Este aviso é adicional ao aviso do workspace Demo (seção 12.6) — os dois
podem aparecer juntos (Dashboard no modo Demo **e** sem MSAL) ou só um deles
(Dashboard em produção-real, mas ainda sem MSAL — situação atual).

## 12. Ambiente de demonstração (`workspace`)

### 12.1 Motivação

Testar o fluxo completo (Dashboard edita → publica → app sincroniza →
colaborador solicita → gestor aprova → Dashboard aplica e republica) exige
dados que possam ser alterados livremente, sem risco de corromper uma
equipe real. Em vez de "dados fake" descartáveis, este ambiente é uma **parte
oficial da arquitetura**: um workspace completo, isolado e reproduzível.

### 12.2 Termos oficiais

Usar na interface:

```text
Ambiente de Demonstração
Workspace de Demonstração
Equipe de Demonstração
Cenário Demo
```

Usar internamente (nunca "fake" como termo de domínio ou de interface):

```text
workspaceId
workspaceType
scenarioId
seedVersion
publicationRevision
```

### 12.3 Dois workspaces

```text
workspaceId: ici        workspaceType: PRODUCTION   (dados reais)
workspaceId: demo-v1     workspaceType: DEMO         scenarioId: schedule-approval-basic
```

Toda entidade organizacional/operacional (`members`, `teams`,
`member_team_memberships`, `team_manager_assignments`, `schedule_periods`,
`schedule_assignments`, `schedule_change_requests`, `publication_records`)
possui um `workspaceId` — ver detalhamento de campos na spec 57. Documentos
já existentes, gravados antes desta spec, **não têm** esse campo; a ausência
é tratada como `workspaceId = "ici"` / `workspaceType = "PRODUCTION"` por
convenção de leitura (nunca por escrita retroativa em massa — ver migração,
spec 57 seção "Migração").

### 12.4 Isolamento (requisito obrigatório)

- Toda consulta é filtrada por `workspaceId` — nunca uma consulta que misture
  os dois workspaces.
- Uma publicação afeta exatamente um workspace por vez.
- Um reset (seção 12.9) só pode atingir `demo-v1` — o backend rejeita
  qualquer outro destino.
- Nenhum ID do workspace Demo coincide com um ID real (seção 12.7 — IDs
  determinísticos com prefixo `demo-`).
- Nenhuma notificação, e-mail ou chamada ao Microsoft Graph acontece a partir
  de dados do workspace Demo (`notificationsEnabled: false`,
  `externalEffectsAllowed: false`, seção 12.8).
- Nenhum gestor real é usado como aprovador de uma solicitação Demo.
- Nenhum dado Demo entra em relatório de produção.
- Uma conta corporativa MSAL autenticada **nunca** cai automaticamente no
  workspace Demo — o modo corporativo sempre resolve para `workspaceId: ici`;
  só a escolha manual e explícita de "modo demonstração" usa `demo-v1`. Não
  há fallback automático de produção para Demo em nenhuma circunstância
  (identidade não encontrada em produção gera o estado
  `AUTHENTICATED_MEMBER_NOT_FOUND`, spec 57, nunca uma troca silenciosa de
  workspace).

### 12.5 Cenário inicial (`schedule-approval-basic`)

Pensado para exercitar exatamente a ideia original desta spec — um
responsável cuidando de mais de uma equipe:

```text
Gestor de Segurança Demo (member-demo-gestor-seguranca)
├── administra: SOC Demonstração (team-demo-soc)
└── administra: Segurança da Informação Demonstração (team-demo-seguranca)

Colaboradores:
- Analista SOC Demo 1 (member-demo-soc-01)
- Analista SOC Demo 2 (member-demo-soc-02)
- Analista de Segurança Demo 1 (member-demo-seguranca-01)
- Analista de Segurança Demo 2 (member-demo-seguranca-02)
```

O cenário já inclui um período de escala determinístico e uma solicitação
`PENDING` inicial (Analista SOC Demo 1 solicitando troca de turno,
endereçada ao Gestor de Segurança Demo), para que a tela de aprovação possa
ser validada imediatamente após carregar o cenário, sem precisar criar nada
manualmente primeiro.

Todos os nomes, logins e e-mails são fictícios; e-mails usam o domínio
reservado `example.invalid` quando um formato de e-mail for necessário.

### 12.6 Personagens no app (modo Demo)

O modo demonstração do app permite selecionar um personagem:

```text
Testar como:
[ Analista SOC Demo 1 ]
[ Analista de Segurança Demo 1 ]
[ Gestor de Segurança Demo ]
```

O personagem selecionado define **apenas** a identidade dentro do workspace
`demo-v1` — não usa MSAL, não altera a sessão corporativa armazenada
(`CorporateAuthState`, spec 53), e é tecnicamente a mesma capacidade de
seleção de "membro demo" que já existe hoje (`LoginGateScreen`, "Login de
teste"), só que também passando a resolver equipe/responsável/solicitações
dentro do workspace Demo, em vez de mostrar dados soltos sem contexto
organizacional.

Aviso permanente enquanto o workspace Demo está ativo (app e Dashboard):

```text
AMBIENTE DE DEMONSTRAÇÃO
As alterações realizadas aqui não afetam equipes reais.
```

### 12.7 IDs determinísticos

O workspace em si **não** tem um ID separado — o documento é `workspaces/demo-v1`
(seção 12.3/12.8; a chave é o próprio `workspaceId`, nunca um prefixo
`workspace-` adicional). Os IDs determinísticos das entidades dentro dele
são:

```text
team-demo-soc
team-demo-seguranca
member-demo-gestor-seguranca
member-demo-soc-01
member-demo-soc-02
member-demo-seguranca-01
member-demo-seguranca-02
manager-demo-soc
manager-demo-seguranca
period-demo-2026-07
```

IDs fixos (nunca gerados aleatoriamente a cada restauração) permitem
comparar antes/depois, atualizar o mesmo documento em vez de duplicar, testar
sincronização incremental e escrever testes automatizados previsíveis.

### 12.8 Documento do workspace

Cada workspace (`ici` e `demo-v1`) é também um documento próprio (coleção
`workspaces`, spec 57), guardando metadados que não fazem sentido em nenhuma
entidade individual:

```json
{
  "workspaceId": "demo-v1",
  "workspaceType": "DEMO",
  "scenarioId": "schedule-approval-basic",
  "seedVersion": 1,
  "publicationRevision": 1,
  "externalEffectsAllowed": false,
  "notificationsEnabled": false
}
```

`publicationRevision` incrementa a cada publicação — permite ao app mostrar,
em diagnóstico (não necessariamente na tela principal), "última revisão
sincronizada" e provar que uma alteração publicada de fato chegou.

### 12.9 Restauração do cenário Demo

Endpoint futuro do backend Express local (não implementado nesta fase):

```text
POST /api/demo/reset
```

Contrato exigido:

- destino sempre fixo `demo-v1` — o backend rejeita qualquer outro valor;
- confirmação explícita antes de executar;
- apaga **somente** documentos do workspace `demo-v1`;
- recarrega as fixtures versionadas (`seedVersion`);
- gera uma nova `publicationRevision`;
- nunca toca em nenhum documento do workspace `ici`;
- registra um log sanitizado da operação (sem segredo, sem token).

Botão correspondente no Dashboard: "Restaurar cenário de demonstração", com
aviso prévio "Esta ação apagará somente os dados de demonstração. Os dados
reais não serão modificados."

## 13. Segurança transitória (reforço, ver também spec 51/57)

- O login manual/seletor de teste do Dashboard nunca é autenticação real —
  vale tanto para "gestor de teste" (produção sem MSAL) quanto para
  "personagem Demo".
- Nenhuma senha, chave, tenant real, client ID real, e-mail real ou token
  aparece nesta spec, na spec 57 ou no schema JSON (spec 57 seção
  "Exemplos").
- Toda escrita continua passando por um backend confiável (Express local +
  Firebase Admin) — nunca Firebase Admin no navegador, nunca write público
  no Firestore (reforça spec 51, que já bloqueia toda escrita anônima).
- MSAL no Dashboard é fase posterior (FASE 14c-10, spec 58) — até lá, todo
  cadastro de responsável/aprovação usa o modo local de teste desta seção.

## 14. Critérios de aceite

1. O termo "chefe" não aparece como nome de campo, papel ou coleção em
   nenhum artefato criado por esta fase.
2. O modelo permite um responsável administrar mais de uma equipe e uma
   equipe ter mais de um responsável ativo simultaneamente.
3. Substituição temporária (férias) é um vínculo novo com vigência própria,
   nunca uma edição do vínculo principal.
4. Desativar um vínculo preserva histórico (nunca exclusão física).
5. Toda solicitação grava uma fotografia do responsável no momento da
   criação, imune a reatribuições posteriores.
6. O aviso "modo local de teste — sem autenticação real" está descrito como
   obrigatório e permanente enquanto não houver MSAL no Dashboard.
7. O workspace Demo está isolado do workspace de produção em todos os
   aspectos listados na seção 12.4, e usa IDs determinísticos, nunca
   aleatórios.
8. Nenhuma linha desta spec implementa código, altera Gradle, Firebase,
   regras, dashboard, app, parser ou versão.
