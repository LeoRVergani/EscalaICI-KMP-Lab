# SPEC 47 — Vínculo entre identidade, membro e equipe

**Status:** proposta; nenhuma linha de código funcional criada por esta spec
**Escopo:** Firestore universal (coleção `user_links`), Dashboard, Escala ICI KMP
**Fase:** FASE 14a (documentação) — implementação prevista para FASE 14c
**Depende de:** SPEC 46 (a sessão Firebase Auth precisa existir antes de haver algo a vincular)

## 1. Diagnóstico que motiva esta spec

Não existe hoje, em nenhum dos dois repositórios (Dashboard, KMP), uma coleção
`user_links` nem equivalente. `docs/spec/SPEC-DB-UNIVERSAL-TIMES-E-ESCALAS.md:110-124`
já a definia como "etapa posterior", mas nunca foi implementada. O vínculo
identidade→membro que existe hoje é frágil: o Dashboard resolve por **e-mail
normalizado em texto livre** (`membersRepository.ts`, `teamsRepository.ts`,
`systemAdminsRepository.ts`), sem UID nem tenant — qualquer usuário cujo
e-mail mude ou cujo domínio de e-mail seja reatribuído perderia/ganharia
acesso silenciosamente. O KMP nem isso tem — o "membro atual" é escolhido
manualmente numa lista de 3 opções mock (diagnóstico, itens 2 e 3).

## 2. Modelo `CorporateIdentity`

Representa a identidade corporativa autenticada (pós-SPEC 46), antes de
qualquer resolução de membro/equipe:

```
CorporateIdentity {
  tenantId: string       // sempre d2d23346-e737-4cac-96ec-fb25e7889f01 hoje
  objectId: string       // "oid" do Azure AD, estável por usuário no tenant
  email: string          // normalizado (lowercase, trim) — só para exibição/lookup legado, nunca chave primária
  displayName: string?   // do Graph, só para exibição
  firebaseUid: string    // hash(tenantId, objectId) — igual ao UID mintado pela ponte (SPEC 46)
}
```

`tenantId`+`objectId` são a chave estável; `email` é auxiliar (compatibilidade
com o modelo atual do Dashboard, nunca a chave de vínculo definitiva).

## 3. Coleção `user_links`

```
user_links/{firebaseUid} {
  tenantId: string
  objectId: string
  email: string                 // normalizado, só para busca/exibição administrativa
  memberId: string               // aponta para members/{memberId}
  teamIds: string[]               // todas as equipes com vínculo ativo (spec 35 já previa múltiplas)
  primaryTeamId: string           // equipe principal, usada como default de UI
  roles: { [teamId: string]: string }  // função/cargo por equipe (referencia roles/{roleId} da spec 35, não duplica a definição)
  active: boolean
  createdAt: Timestamp             // já definido na spec 35 para user_links — preservado, nunca substituído
  updatedAt: Timestamp             // idem — atualizado em qualquer escrita no documento
  linkedAt: Timestamp              // quando o vínculo foi ativado (pode diferir de createdAt em reativações)
  linkedBy: string                 // e-mail do administrador que fez o vínculo (auditoria)
  deactivatedAt: Timestamp?
  deactivatedBy: string?
}
```

`linkedAt`/`linkedBy`/`deactivatedAt`/`deactivatedBy` são **aditivos** aos
campos `createdAt`/`updatedAt` já definidos para `user_links` na spec 35 —
esta spec não remove nem substitui os dois últimos, só acrescenta os
primeiros para responder a uma pergunta diferente ("quando/por quem o vínculo
foi ativado/desativado", não "quando o documento foi criado/tocado pela
última vez").

Decisões:
- Documento é indexado por `firebaseUid` (não por e-mail) — sobrevive a
  mudança de e-mail exibido, desde que `tenantId`+`objectId` não mudem.
- `teamIds` é array porque um usuário pode ter vínculo com múltiplas equipes
  (spec 35/38 já preveem isso) — `primaryTeamId` resolve qual mostrar por
  padrão sem forçar seleção manual toda vez.
- `roles` é um mapa por equipe porque a função pode diferir entre equipes
  (ex.: responsável em uma equipe, membro comum em outra) — nunca um único
  campo `role` global.
- Nunca duplicar a definição da função/cargo aqui — `roles` deste documento
  guarda só o **id/slug** de uma role definida em `roles/{roleId}` (schema
  universal, spec 35), a mesma separação entre identidade e cargo/função já
  decidida na spec 39 do EscalaSOC (autenticação ≠ autorização ≠ cargo).

## 4. Fluxo de resolução no app

1. App obtém `CorporateIdentity` (pós-SPEC 46, sessão Firebase Auth ativa).
2. App lê `user_links/{firebaseUid}`.
3. Se documento não existe, ou `active == false`: estado `IDENTITY_NOT_LINKED`
   (spec 48) — nunca cair silenciosamente em dado mock ou primeiro membro da
   lista, como acontece hoje (diagnóstico, item 2).
4. Se existe e ativo: resolve `memberId` → lê `members/{memberId}`; resolve
   `teamIds` → se mais de uma, oferece seleção de equipe (mantendo
   `primaryTeamId` como default); se `members/{memberId}` não existir mais
   (membro removido) ou `teamId` selecionado não existir mais em `teams`
   (equipe removida/inativada) → estado `TEAM_NOT_FOUND`, nunca crash (ver
   achado desta auditoria sobre `ProfileTab`/normalização de draft, mesmo
   princípio de "nunca deixar dado ausente derrubar a tela").

## 5. Usuário sem vínculo

- Estado explícito `IDENTITY_NOT_LINKED` — a tela deve mostrar quem está
  logado (nome/e-mail do Graph) e uma mensagem clara: "Sua conta corporativa
  ainda não foi associada a um colaborador. Contate o administrador." —
  substituindo o texto estático atual "Associação corporativa ainda não
  configurada" (que hoje aparece sempre, sem relação com o estado real, ver
  diagnóstico item 1) por um texto que só aparece **quando de fato não há
  vínculo**, condicionado ao estado real da consulta a `user_links`.
- Nunca oferecer fallback silencioso para um membro mock/demo em produção.

## 6. Usuário desativado

- `active == false` em `user_links` **não apaga** o documento — preserva
  `memberId`/`teamIds`/histórico para auditoria, só bloqueia o acesso.
  Mesmo padrão de soft-disable já usado em `system_admins.active` no
  Dashboard (`systemAdminsRepository.ts`).
- App trata `active == false` como equivalente a "sem vínculo" para fins de
  UI (`IDENTITY_NOT_LINKED`), mas o diagnóstico interno/log pode diferenciar
  "nunca vinculado" de "vínculo desativado" para o administrador.

## 7. Múltiplas equipes e mudança de setor

- `teamIds` é a lista de vínculos ativos no momento; mudança de setor é uma
  operação administrativa no Dashboard que **adiciona** o novo `teamId` e
  **remove** (ou marca historicamente) o antigo — nunca sobrescreve
  silenciosamente sem rastro.
- `primaryTeamId` (seção 3) é o **default administrativo**, definido por
  quem faz o vínculo em `user_links`. A **preferência do próprio usuário**
  (quando ele quer ver outra equipe por padrão, dentre as `teamIds`
  disponíveis) já tem campo definido na spec 35 do EscalaSOC:
  `app_user_preferences.preferredTeamId` — esta spec reaproveita esse nome
  exato, não inventa `primaryTeamId` como campo de preferência de usuário (só
  `user_links.primaryTeamId` é nome novo, e é administrativo, não de
  preferência).
- **Tensão conhecida, não resolvida por esta spec**: `app_user_preferences`
  (spec 35) é indexada por `userEmail`, enquanto esta spec (seção 2/3) exige
  `tenantId`+`objectId`/`firebaseUid` como chave estável, nunca e-mail. Antes
  de implementar a leitura/escrita de `preferredTeamId` na FASE 14c/14e, é
  preciso decidir explicitamente se `app_user_preferences` migra para ser
  indexada por `firebaseUid` (mudança na spec 35, fora do escopo desta fase)
  ou se mantém `userEmail` como excepção documentada para essa coleção
  específica. Esta spec não decide isso — apenas registra a inconsistência
  para que a FASE 14c não a resolva por acidente, de forma divergente entre
  Dashboard e app.

## 8. Processo de vínculo no Dashboard

- Uma tela administrativa (fora do escopo desta fase implementar) precisa
  permitir: buscar um membro existente (`members`), buscar/confirmar a
  identidade Microsoft correspondente (por e-mail, já que o Dashboard não
  tem `objectId` disponível antes do primeiro login dessa pessoa), e criar/
  atualizar o `user_links/{firebaseUid}` correspondente.
- Problema real a resolver nesta fase futura (documentado aqui, não
  implementado): o Dashboard não pode "criar" um `firebaseUid` antes da
  pessoa logar ao menos uma vez (ele é derivado de `objectId`, que só existe
  após o primeiro login MSAL bem-sucedido, spec 46). Solução recomendada:
  vínculo em duas etapas — (a) administrador pré-cadastra a intenção de
  vínculo por e-mail (`user_links_pending/{emailNormalizado}` ou campo
  equivalente), (b) no primeiro login bem-sucedido dessa pessoa, uma Cloud
  Function resolve o pré-cadastro por e-mail e materializa o
  `user_links/{firebaseUid}` definitivo, apagando o pendente. Este desenho
  fica registrado aqui como direção; a implementação é FASE 14c.

## 9. Auditoria

- Todo `user_links` grava `linkedAt`/`linkedBy` na criação e
  `deactivatedAt`/`deactivatedBy` na desativação — nunca update silencioso
  sem rastro de quem fez a mudança administrativa.
- Mudança de `teamIds`/`primaryTeamId`/`roles` deve gerar um evento em
  `schedule_change_events` (coleção já prevista na spec 35) ou equivalente,
  para histórico auditável — decisão de implementação exata cabe à FASE 14c.

## 10. Critérios de aceite

1. `user_links` é indexado por `firebaseUid` derivado de `tenantId`+`objectId`,
   nunca por e-mail como chave primária.
2. Usuário sem vínculo nunca cai em dado mock/demo — sempre estado explícito
   `IDENTITY_NOT_LINKED` com mensagem acionável.
3. Usuário com múltiplas equipes consegue trocar de equipe sem perder o
   vínculo administrativo original.
4. Desativar um vínculo preserva histórico, não apaga o documento.
5. Membro ou equipe removida após o vínculo já ter sido criado nunca causa
   crash — sempre estado tipado (`TEAM_NOT_FOUND`).
6. Todo vínculo criado/desativado tem `linkedBy`/`deactivatedBy` registrado.
