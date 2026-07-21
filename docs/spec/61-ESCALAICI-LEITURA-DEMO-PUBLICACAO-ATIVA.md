# 61 — Escala ICI KMP: leitura somente-leitura da publicacao Demo ativa

## Contexto

Esta fase implementa no KMP a leitura segura da publicacao Demo revisionada que
o Dashboard grava no Firestore real. O app continua sem Firebase Admin, sem
service account e sem escrita remota.

O contrato consumido e:

1. Ler o ponteiro `workspaces/demo-v1`.
2. Obter `publicationRevision`.
3. Ler entidades somente em
   `workspaces/demo-v1/revisions/{publicationRevision}/{collection}`.
4. Nunca ler entidades Demo das colecoes raiz.
5. Validar `workspaceId == "demo-v1"` e `publicationRevision == R` em cada
   entidade antes de publicar qualquer snapshot no app.

As colecoes de snapshot lidas sao `teams`, `members`,
`member_team_memberships`, `team_manager_assignments`, `schedule_periods`,
`schedule_assignments` e `schedule_change_requests`.

## Modelos

Campos aditivos foram incluidos nos modelos puros existentes:

- `Team.publicationRevision: Int? = null`
- `Member.publicationRevision: Int? = null`
- `MemberTeamMembership.publicationRevision: Int? = null`
- `SchedulePeriod.publicationRevision: Int? = null`
- `ScheduleAssignment.publicationRevision: Int? = null`

Novos modelos puros:

- `WorkspacePublicationPointer`
- `TeamManagerAssignment`
- `TeamManagerPermissions`
- `TeamManagerRole`
- `ScheduleChangeRequest`

Nenhum modelo introduz dependencia de Android, navegador, Firebase SDK, MSAL ou
service account em `commonMain`.

## Algoritmo

`DemoPublicationResolver` executa o fluxo atomico:

1. Le `workspaces/demo-v1`.
2. Valida revisao positiva e status ativo, quando o status vem exposto.
3. Le as sete colecoes dentro da revisao ativa.
4. Valida workspace e revisao de todas as entidades.
5. Monta `DemoPublicationSnapshot` somente depois de todas as colecoes
   passarem.
6. Rele `workspaces/demo-v1`.
7. Se o ponteiro mudou, descarta o snapshot e tenta uma unica vez.
8. Se mudar de novo, retorna `DemoPublicationLoadResult.Failure` com causa
   tipada; nenhum estado parcial e publicado.

Remocoes sao estruturais: uma entidade ausente no pacote da revisao simplesmente
nao existe no snapshot. Nao ha tombstone nem soft delete a processar no KMP.

## Fallback

`DemoPublicationRepository` compoe o resolvedor remoto com a fixture local:

- sucesso remoto: origem `REMOTE_PUBLICATION`, com revisao ativa;
- erro de rede, permissao, parse ou validacao: origem `LOCAL_FIXTURE`, com a
  revisao da fixture;
- o ultimo snapshot consistente em memoria nao e apagado por uma leitura remota
  incompleta.

Os repositórios Demo antigos continuam existindo. Os entrypoints Android e Web
passam a usar repositórios `RemoteFirstDemo*Repository`, que tentam a
publicacao remota e caem para `DemoFixtureCache`.

O contexto resolvido da persona Demo expoe a origem exibida na UI:
`Fonte: Demo remoto rev. N` ou a mensagem de fallback para fixture local.

## Limitacao conhecida de regras

`firebase/firestore.rules` deste repo ainda nao libera `workspaces/**` e a
catch-all final nega tudo que nao foi listado explicitamente. Assim, uma leitura
real contra `workspaces/demo-v1/...` deve retornar `403`/`permission-denied` ate
que as regras sejam alteradas e publicadas fora desta fase.

Isso e esperado para a validacao manual atual. O KMP classifica esse erro como
`ScheduleSyncCause.PERMISSION_DENIED` e usa a fixture local. Esta fase nao altera
regras, nao faz deploy e nao contorna a falta de permissao.

## Fora do escopo / proxima fase

- Religar a aba Escala/Plantao, ainda baseada no hardcode legado `teamId =
  "soc"` para o fluxo Firebase existente, ao snapshot Demo remoto. Isso exige
  refatoracao maior da fonte de escala e fica para 14c-5B ou fase seguinte.
- Atualizar e publicar regras `workspaces/**`.
- Escrever no Firestore.
- Usar Firebase Auth, service account, Firebase Admin ou tokens fixos.
- Consumir revisoes informadas pelo cliente sem resolver antes o ponteiro ativo.

## Testes

`DemoPublicationResolverTest` cobre:

- carregamento de revisao ativa consistente;
- paths revisionados `workspaces/demo-v1/revisions/{R}/{collection}`;
- ausencia de leitura das colecoes raiz Demo;
- rejeicao por `workspaceId` divergente;
- rejeicao por `publicationRevision` divergente;
- membership resolvendo membro e equipe sem preencher `Member.teamId`;
- status `PREPARING` rejeitado defensivamente;
- retry unico quando o ponteiro muda;
- erro controlado quando o ponteiro muda de novo;
- falha de colecao descartando o snapshot remoto;
- `permission-denied` simulado com fallback para fixture local.

Os testes usam fakes em `commonTest`; nenhum teste acessa Firestore real.

## FASE 14c-5B — separacao de causas de erro, autorizacao real de `lvergani` e visao administrativa Demo

Esta secao documenta a rodada seguinte, motivada por teste manual real no
emulador (sessao MSAL ja restaurada, conta corporativa `lvergani@ici.tec.br`):
LOGIN mostrava "Publicação corporativa indisponível ou cadastro não
localizado." e DEMO mostrava "Esta conta não possui acesso ao modo Demo." —
ambos falhando **depois** da autenticacao MSAL, na etapa de
autorizacao/resolucao, nao no MSAL em si.

### Causa raiz confirmada (bloqueio externo, nao e bug)

Leitura somente-leitura direta (sem credenciais, replicando o que o app faz
com `FirebaseAuthTokenProvider = null`) contra
`https://firestore.googleapis.com/v1/projects/escala-ici-dev/databases/(default)/documents/workspaces/ici-dev`
e `.../workspaces/demo-v1` retornou, no inicio e no fim desta fase:

```
403 PERMISSION_DENIED — reason: SERVICE_DISABLED
"Cloud Firestore API has not been used in project escala-ici-dev before or
it is disabled."
```

Ou seja: o banco Firestore `(default)` do projeto `escala-ici-dev` ainda nao
foi criado/ativado no Console GCP. Isso bloqueia LOGIN e DEMO igualmente,
**antes** de qualquer regra do Firestore ser avaliada — nao e um problema de
`firebase/firestore.rules` (que ja permite leitura sem `request.auth` para
`workspaces/{demo-v1,ici-dev}`, ver secao "Limitacao conhecida de regras"
acima) nem de codigo do app. So a ativacao do banco (acao humana, escolhendo
regiao no Console) muda esse estado.

### Separacao de causas

`ScheduleSyncCause.FIRESTORE_DATABASE_DISABLED` passou a existir separado de
`AUTH_REQUIRED`. `classifySyncFailure` reconhece os marcadores reais do erro
acima antes do fallback generico de 403/"not configured". A UI (`App.kt`,
`corporatePublicationUnavailableMessage`) mostra, exclusivamente para essa
causa: "O banco de dados Firebase deste ambiente ainda não foi ativado." —
nunca combinada com as mensagens de cadastro ausente, escala ausente ou
acesso negado.

No LOGIN, `loginGateErrorMessage` tambem separa, dentro do que o modelo atual
de `OrganizationResolutionResult` permite distinguir: cadastro nao
localizado, membro inativo, identidade ambigua, membership sem equipe ativa,
equipe nao encontrada, multiplas equipes ativas, workspace divergente, e as
causas de `ScheduleSyncCause` (Firestore desativado, permissao negada, rede,
dados invalidos, escala/periodo/atribuicoes ausentes, publicacao nunca feita
neste ambiente).

### Autorizacao real de `lvergani` no DEMO

`identity/DemoAuthorization.kt` (`isDemoAuthorizedForIdentity`): autoriza por
`allowedDeveloperObjectIds` do ponteiro remoto (caminho preferencial) ou, uma
vez que esse ponteiro ainda nao esta configurado para a conta corporativa
real, por um fallback temporario **exclusivo de build de desenvolvimento**:
e-mail normalizado (trim + lowercase, comparacao exata, sem `contains` nem
dominio inteiro) igual a `lvergani@ici.tec.br`. Fora de build de
desenvolvimento (inclusive o alvo Web/Wasm, que nao expõe uma flag
debug/release equivalente a `BuildConfig.DEBUG` no codigo comum) esse
fallback fica desligado e so `allowedDeveloperObjectIds` autoriza.

### Fim do fallback mock silencioso

Antes desta fase, quando a resolucao de LOGIN/DEMO nao encontrava publicacao
real, o app reaproveitava `mockScheduleSummary()` (dados fixos de
06–12/07) preenchendo so nome/equipe reais por cima — ou seja, uma escala
falsa era exibida como se fosse dado carregado. `ui/ResolvedScheduleSummaryDecision.kt`
centraliza essa decisao para os dois entrypoints: sem publicacao real
disponivel, o app mostra o erro tipado ("Escala ativa não encontrada para
esta conta."), nunca inventa uma escala.

### Visao administrativa do DEMO (mudanca de produto)

Antes: tocar "AMBIENTE DEMO" resolvia direto para
`DemoPersonaCatalog.personas[2]` ("Gestor de Segurança Demo"), que na fixture
nunca tem `scheduleAssignments` (so aparece em `team_manager_assignments`) —
por isso o DEMO sempre terminava em "Escala ativa não encontrada", tecnicamente
correto apos a remocao do mock, mas errado de produto: a identidade
administrativa estava sendo tratada como se fosse a persona operacional.

Agora, apos autorizado, "AMBIENTE DEMO" abre `DemoWorkspaceOverviewScreen`
(`identity.DemoWorkspaceOverview` + `ui.DemoWorkspaceOverviewDecision`):

- a identidade autenticada (`lvergani`) recebe o papel `DEMO_DEVELOPER`,
  exibido separado de qualquer persona ("A identidade autenticada não foi
  convertida em persona operacional.");
- mostra origem da publicacao (remoto/fixture local/remoto indisponivel),
  revisao, causa de fallback quando houver, contagem de membros ativos,
  periodo ativo e as equipes do workspace `demo-v1`;
- lista, para "ver como", somente as personas com `scheduleAssignments` reais
  na fixture/publicacao (hoje: Analista SOC Demo 1, Analista de Segurança
  Demo 1) — a persona gestora, sem atribuicoes, nao aparece;
- a selecao de persona acontece dentro dessa tela, nao na tela inicial do
  app; a sessao MSAL nao e afetada.

Escopo desta rodada e minimo deliberadamente: sem escrita, sem importacao
XLS, sem edicao — so leitura/visualizacao administrativa.

### UX dos botoes

`LoginGateScreen`: "LOGIN" → "MINHA ESCALA", "DEMO" → "AMBIENTE DEMO" — a
tela ja restaura o MSAL e mostra a conta autenticada antes dos botoes, entao
eles sao seletores de modo, nao um novo login.

### Testes novos desta rodada

- `ScheduleSyncCauseTest`: causa dedicada para o erro real observado +
  regressao confirmando que "Firebase not configured" generico continua
  `AUTH_REQUIRED` (nao vaza para `FIRESTORE_DATABASE_DISABLED`).
- `DemoAuthorizationTest`: e-mail exato normalizado (maiusculas/espacos)
  autorizado em build de desenvolvimento; outro e-mail rejeitado; objectId
  autorizado fora de build de desenvolvimento; fallback de e-mail desligado
  fora de build de desenvolvimento mesmo para `lvergani@ici.tec.br`.
- `RemoteFirstDemoMemberDirectoryRepositoryTest`: parametros
  `entraTenantId`/`entraObjectId` propagados ate o diretorio in-memory.
- `DemoPublicationResolverTest`: autorizacao com ponteiro remoto
  indisponivel resulta em fallback para fixture local, nao em
  `MemberNotFound`/acesso negado.
- `ResolvedScheduleSummaryDecisionTest` e `DemoWorkspaceOverviewDecisionTest`:
  DEMO autorizado nunca termina automaticamente em mock nem no erro de
  escala pessoal da persona gestora; escolher uma persona operacional
  resolve a escala publicada real (sem mock); a identidade
  `lvergani`/papel `DEMO_DEVELOPER` nunca e tratada como
  `member-demo-gestor-seguranca` nem qualquer outra persona.

Todos os testes usam fakes/fixtures em `commonTest`; nenhum acessa Firestore
real.

### Validado, mas ainda bloqueado externamente

- Codigo, testes (`testDebugUnitTest`, `compileKotlinWasmJs`, `wasmJsTest`,
  `wasmJsBrowserDistribution`, `assembleDebug`, `assembleRelease`, regras
  Firestore) e validacao manual no emulador: **concluidos**.
- Leitura remota real de `workspaces/ici-dev`/`workspaces/demo-v1`:
  **bloqueada** ate o banco Firestore `(default)` do projeto
  `escala-ici-dev` ser criado/ativado no Console GCP (acao humana). Depois
  disso, o proximo bloqueio esperado (documentado na secao "Limitacao
  conhecida de regras") e a publicacao real de `ici-dev` pelo Dashboard.
- Esta fase nao fez deploy de regras, nao publicou dados e nao escreveu no
  Firestore.
