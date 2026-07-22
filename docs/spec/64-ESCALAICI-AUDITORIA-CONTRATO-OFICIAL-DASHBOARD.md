# FASE 14f-3 — Auditoria de contrato: publicação oficial (Dashboard) x leitura (KMP)

## Contexto

Com o Dashboard capaz de publicar um pacote oficial genuíno em `ici-dev`
(FASE 14f, `escala-dashboard`) e o KMP já lendo essa mesma revisão via
`DemoPublicationResolver`/`OrganizationRepositories.kt`, era necessário
confirmar que os dois lados concordam sobre o formato real dos documentos —
não por suposição, mas comparando código com código dos dois repositórios.

Esta auditoria comparou, campo a campo, o que
`server/domain/officialPublicationPlanner.mjs` +
`src/lib/demoWorkspace/dto.ts` (Dashboard) escrevem em cada coleção de
`workspaces/ici-dev/revisions/{n}/{colecao}` contra o que
`composeApp/.../source/DemoPublicationDtos.kt` (KMP) lê.

## Achado corrigido nesta fase

**`corporateLogin` era escrito pelo Dashboard em todo membro publicado, mas
nunca lido pelo KMP no caminho remoto (Firestore real).** `toDemoMember()`
não extraía esse campo, e `DemoPublicationSnapshot.toData()` (usado
exclusivamente pelo caminho remoto — `RemoteFirstDemoMemberDirectoryRepository`
e todas as leituras de `ici-dev`/`demo-v1` via REST) tinha
`loginByMemberId = emptyMap()` hardcoded. Na prática, a resolução de
identidade por login (`findActiveMemberIds`, `OrganizationRepositories.kt`)
sempre caía para comparar o `username`/login do MSAL contra
`member.scaleName` (nome de exibição) em vez do login corporativo real —
silenciosamente, sem erro, apenas nunca dando match quando o nome de exibição
diverge do login (o caso comum).

Curiosamente, o caminho de **fixture local** (`DemoFixturePackage.kt`,
usado no modo Demo/offline) já fazia isso corretamente
(`loginByMemberId()` populado a partir de `DemoFixtureMember.corporateLogin`)
— só o caminho de dados **remotos reais** tinha a lacuna, o que explica por
que isso não apareceu em nenhuma validação anterior baseada em fixture.

**Corrigido:**
- `Member` (`model/ScheduleModels.kt`) ganhou `corporateLogin: String? = null`.
- `toDemoMember()` (`source/DemoPublicationDtos.kt`) agora lê
  `string("corporateLogin")`.
- `DemoPublicationSnapshot.toData()` (`identity/OrganizationRepositories.kt`)
  constrói `loginByMemberId` a partir de `member.corporateLogin` (com
  fallback já existente para `scaleName` continuando a valer quando o
  membro não tiver `corporateLogin` — nenhum comportamento anterior é
  removido, só passa a ter uma fonte melhor quando disponível).

## Outras diferenças encontradas (não corrigidas nesta fase — não afetam a resolução de identidade)

Documentadas aqui para não se perderem, mas deliberadamente fora do escopo
desta correção pontual (nenhuma delas quebra `MINHA ESCALA`/resolução de
identidade hoje):

- `teams.acronym`/`teams.active`: escritos pelo Dashboard, mas o modelo
  `Team` do KMP não tem esses campos — estruturalmente inacessíveis, sem
  efeito funcional conhecido hoje.
- `team_manager_assignments.role`: 4 dos 6 valores possíveis
  (`BACKUP_APPROVER`, `SCHEDULE_EDITOR`, `PUBLISHER`, `VIEW_ONLY`) colapsam
  para `TeamManagerRole.OTHER` no KMP — perde granularidade de exibição,
  mas não é usado em nenhuma decisão de autorização do lado KMP hoje.
- `schedule_periods.updatedAt`: o KMP procura `updatedAt`/`publishedAt`,
  nenhum dos dois é escrito pelo Dashboard — sempre fica `""`, só afeta
  exibição de "última atualização", não a resolução de dados.
- `schedule_assignments.memberName`/`notes`: não escritos pelo Dashboard;
  o KMP já resolve o nome do colega por outra via
  (`scheduleSummaryForMember`, lookup separado por `membersById`), então
  isso não aparece como bug visível na tela "colegas do dia" — é uma
  inconsistência latente no modelo, não um bug de UI confirmado.
- `OfficialCorporateLink.entraTenantId`/`entraObjectId` (coletados na etapa
  "Vínculo corporativo" do wizard) são validados só quanto a
  `memberId`/`teamId` e **nunca persistidos** em nenhum documento
  Firestore pelo Dashboard — o caminho "rápido" de resolução por Entra do
  KMP (`entraTenantId`/`entraObjectId` no membro) nunca vai encontrar
  match para dados publicados pelo Dashboard hoje; a resolução sempre cai
  para email/login. Registrado como possível próximo passo (o Dashboard
  precisaria persistir esses campos no documento do membro, não só
  validá-los), não implementado nesta fase por não ser necessário — o
  fallback de email/login (agora corrigido) já resolve o caso real.

## Testes adicionados

- `DemoPublicationResolverTest.parsesCorporateLoginWrittenByDashboardOfficialPublication`:
  confirma que `corporateLogin` é extraído do JSON remoto para `Member.corporateLogin`.
- `RemoteFirstDemoMemberDirectoryRepositoryTest.remoteFirstDirectoryResolvesByCorporateLoginWrittenByDashboard`:
  teste de contrato ponta a ponta — monta um documento de membro exatamente
  como o Dashboard publicaria (`corporateLogin` presente, `displayName`
  diferente do login) e confirma que a resolução por login encontra o
  membro certo através do pipeline real (JSON → `toDemoMember` →
  `toData()` → `loginByMemberId` → `InMemoryMemberDirectoryRepository`).
  Este teste falha sem a correção (prova de regressão).

## Fora do escopo

- Persistir `entraTenantId`/`entraObjectId` reais nos documentos de membro
  publicados pelo Dashboard (exigiria mudança no lado Dashboard, hoje
  desnecessária dado que login/email já resolvem).
- Qualquer mudança em `team_manager_assignments.role`/`teams.acronym`/`active`.
- Qualquer escrita, publicação ou deploy no Firebase.
