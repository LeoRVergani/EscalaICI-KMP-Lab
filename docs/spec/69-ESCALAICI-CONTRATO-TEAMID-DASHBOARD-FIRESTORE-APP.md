# Spec 69 — Contrato de `teamId` entre Dashboard, Firestore e app (KMP/Web)

**Status:** investigação concluída — mecanismo já existe e já funciona; nenhuma implementação nova
necessária hoje. Documento de referência para não reinventar o que já está pronto.

## 1. Pergunta original

> "Essa equipe que está mostrando aí são do SOC, tem que ver na hora de cadastrar no dashboard
> para escolher tipo o equipe SOC é ID 1 ao escolher ela e fazer a publicação na escala do time
> do SOC todas essas pessoas que foram carregadas no ID 1 por exemplo o firebase sabe que
> pertencem ao SOC (...) Implantar isso no dashboard e o app e o dashboard reconhecer essa
> regra."

## 2. O que já existe (lido direto do código, `escala-dashboard`)

O mecanismo pedido **já está implementado de ponta a ponta** desde a FASE 14c/14f (auditoria de
contrato oficial, spec 64). Resumo do fluxo real:

1. **Cadastro do time** (`src/lib/teamsRepository.ts`, `saveTeam()`): só `system_admin` cadastra um
   time. O `id` é derivado do nome/identificador escolhido (slug normalizado - minúsculas, sem
   acento, `[^a-z0-9-]` viram `-`), gravado como o **ID do documento** em `teams/{id}` E como o
   campo `teamId` dentro do próprio doc:
   ```
   await setDoc(doc(services.db, 'teams', id), { ...team, id, teamId: id, ... })
   ```
2. **Permissão por equipe** (`canManageTeamWithRole`): usuários do Dashboard têm
   `user.teamIds: string[]` - um `SCHEDULE_ADMIN` só gerencia/publica para times cujo `teamId`
   esteja na própria lista. Já é, na prática, "o Firebase sabe que fulano pertence ao SOC".
3. **Publicação/importação** (`src/lib/publicationPreview.ts`): ao montar o preview/contagem da
   publicação, as atribuições (`schedule_assignments`) são consultadas por
   `where('teamId', '==', team.id)` - todo membro/atribuição importado nessa publicação já nasce
   marcado com o `teamId` do time selecionado no cadastro.
4. **Leitura oficial** (`src/lib/officialWorkspace/officialScheduleGateway.ts`,
   `loadOfficialSchedule(teamId, revision)`): a leitura do lado do Dashboard também é sempre
   parametrizada por `teamId`.
5. **App KMP/Web** (já auditado nesta sessão, FASE 14J/14J.1): `Member.teamId`, `Team.teamId`,
   `ScheduleAssignment` e `MemberTeamMembership.teamId` são exatamente os mesmos campos lidos da
   publicação (`DemoPublicationRepository`/`RemoteFirstDemoMemberRepository.getMembersByTeam(teamId)`)
   - é o que já faz "Visualizar como colaborador" (spec 68) mostrar só a equipe certa, e o mesmo
   campo usado para resolver a identidade do usuário logado desde a FASE 14c.

Ou seja: **cadastra o time no Dashboard → publica a escala para esse time → todo mundo carregado
naquela publicação já vem com o `teamId` certo no Firestore → o app já lê e respeita esse campo**.
Nenhuma peça nova precisa ser construída para o comportamento descrito.

## 3. Onde a expectativa do usuário diverge do que existe

O exemplo citado foi "SOC é ID 1" (um número sequencial). O sistema real usa um **slug
legível** (ex.: `soc`, `noc`) em vez de um contador numérico. Isso é intencional e melhor:

- Um slug é auto-descritivo em qualquer log/URL/tela de admin (`teams/soc` vs. `teams/1`).
- Não depende de um contador central (risco de corrida ao cadastrar dois times ao mesmo tempo).
- Já é o valor usado em `applicationId`-like referências no app (mensagens de erro, filtros).

**Recomendação:** manter o slug como está. Trocar para um ID numérico sequencial seria retrabalho
sem ganho real, e quebraria referências já publicadas (times existentes teriam que ser
re-cadastrados com novo ID, e toda escala/membro/atribuição já publicada teria que ser migrada).

## 4. Lacunas reais encontradas (candidatas a ajuste futuro, não implementadas aqui)

Nenhuma delas bloqueia o fluxo já funcional; são melhorias de robustez, não bugs:

1. **Validação de colisão de slug**: `saveTeam()` normaliza o `id` mas não parece checar
   explicitamente se o slug já existe antes de sobrescrever (`setDoc(..., { merge: true })`) - um
   admin poderia digitar um nome que gera o mesmo slug de um time existente e mesclar dados sem
   aviso. Vale um `getDoc` prévio + confirmação explícita se já existir.
2. **Edição do slug após publicações existentes**: não há, pelo que foi lido, um fluxo de
   "renomear o ID de um time" que migre `schedule_assignments`/`members` já publicados - o slug é
   efetivamente imutável na prática (mudar o nome de exibição é seguro, mudar o `id` não).
3. **Documentação explícita do contrato**: existia em specs anteriores (64, 66, 67) de forma
   espalhada; este documento consolida numa referência única.

## 5. Ação recomendada

Nenhuma implementação nova pedida foi encontrada como necessária - o comportamento já existe.
Se quiser, os itens da seção 4 podem virar tarefas pequenas e isoladas (ex.: "FASE X: valida
colisão de slug ao cadastrar time no Dashboard"), mas são independentes uma da outra e não afetam
o app KMP/Web.
