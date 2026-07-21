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
