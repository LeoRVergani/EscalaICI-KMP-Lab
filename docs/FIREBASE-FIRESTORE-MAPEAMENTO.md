# Mapeamento Firebase/Firestore — KMP-MVP-1B

## Decisão da auditoria

A integração real está **bloqueada antes da inclusão de SDK**. O schema legado
usado pelo EscalaSOC pode ser identificado no código, mas as regras atualmente
publicadas no projeto Firebase não estão versionadas nos repositórios
inspecionados. Além disso, o Escala ICI KMP não possui uma sessão Firebase Auth
compatível com as regras restritivas documentadas para o estado futuro.

Consequentemente, esta fase documental não adiciona dependências, não consulta
o Firestore, não cria gateways concretos, não altera regras e não escreve dados.
Prosseguir sem confirmar regras e autenticação violaria os critérios de parada
da KMP-MVP-1B.

## Evidências auditadas

- Projeto Firebase configurado no Android legado: `escalaici`.
- O `google-services.json` existe somente no app Android irmão; ele não foi
  copiado para o KMP e nenhum valor de configuração foi exposto neste relatório.
- O Android legado usa diretamente o SDK `firebase-firestore`, sem Firebase
  Auth. A decisão técnica registrada no EscalaSOC confirma que suas requisições
  atuais chegam com `request.auth == null`.
- O Dashboard possui autenticação Firebase/Microsoft, mas isso não cria sessão
  no KMP nem no Android legado.
- O arquivo `firestore.rules` encontrado no repositório do Dashboard cobre
  apenas `/escalas/{anoMes}` para usuários com claim `coordenador`; o restante
  é negado. O próprio arquivo avisa que precisaria ser mesclado às regras do
  Android. Portanto, ele não comprova as regras publicadas do projeto principal.
- As specs de segurança mantêm as regras restritivas pausadas até existir
  `request.auth` funcional no cliente leitor.

## Coleções confirmadas pelo código legado

As coleções abaixo são nomes efetivamente usados pelos repositórios Android e
Dashboard, não nomes inferidos:

| Coleção | Chave lógica | Filtro de leitura observado | Papel na KMP-MVP-1B |
|---|---|---|---|
| `teams` | `teamId` | `active == true` ou documento por ID | resolver equipe e rótulo |
| `members` | `memberId` | `teamId`, `email` ou documento por ID | mapear pessoa |
| `work_patterns` | `patternId` | `teamId` | referência legada |
| `shift_definitions` | `shiftId` | `teamId` | mapear turno |
| `schedule_periods` | `periodId` | `teamId`; ativo escolhido no cliente | localizar escala ativa |
| `schedule_assignments` | `assignmentId` | `teamId`; `periodId` filtrado no cliente | montar escala |
| `oncall_periods` | `periodId` | `teamId`; ativo escolhido no cliente | localizar plantão ativo |
| `oncall_assignments` | `onCallId` | `teamId`; `periodId` filtrado no cliente | montar plantão |
| `source_files` | `sourceFileId` | não necessária para leitura inicial | rastreabilidade |
| `import_jobs` | `jobId` | não necessária para leitura inicial | rastreabilidade |

Não foi encontrado documento central com `activeSchedulePeriodId` ou
`activeOnCallPeriodId`. No código atual, o período ativo é o primeiro documento
do time com `active == true`; esses dois nomes existem no cache de sincronização
do Android, não como contrato Firestore confirmado.

## Campos confirmados

### `schedule_periods`

Obrigatórios no mapeador existente: `periodId`, `teamId`, `name`, `startDate`,
`endDate`, `sourceType`, `active`, `createdAt`, `updatedAt`.

Opcionais: `sourceFileId`, `publishedAt`, `publishedBy`.

### `schedule_assignments`

Obrigatórios no modelo legado: `assignmentId`, `teamId`, `periodId`,
`scaleName`, `date`, `assignmentType`, `createdAt`, `updatedAt`.

Opcionais: `memberId`, `memberEmail`, `shiftId`, `shiftName`,
`startDateTime`, `endDateTime`, `breakStartDateTime`, `breakEndDateTime`,
`locationMode`, `dailyHours`, `reason`, `note`, `sourceFileId`.

### `oncall_periods`

Obrigatórios: `periodId`, `teamId`, `name`, `startDate`, `endDate`,
`sourceType`, `active`, `createdAt`, `updatedAt`.

Opcionais: `sourceFileId`, `publishedAt`, `publishedBy`.

### `oncall_assignments`

Obrigatórios: `onCallId`, `teamId`, `scaleName`, `startDateTime`,
`endDateTime`, `label`, `active`, `createdAt`, `updatedAt`.

Opcionais: `periodId`, `memberId`, `memberEmail`, `sourceFileId`, `notes`.

### `members`

Obrigatórios: `memberId`, `teamId`, `displayName`, `scaleName`, `active`,
`createdAt`, `updatedAt`.

Opcionais: `email`, `roles`, `defaultWorkPatternId`.

## Identificadores e relações

O schema legado é global por coleção. As relações são feitas pelos campos
`teamId`, `periodId`, `memberId`, `shiftId` e `sourceFileId`. O ID do documento
normalmente coincide com o identificador lógico correspondente.

`organizationId` não está presente nos modelos legados de escala e plantão.
Ele pertence ao schema universal proposto, junto com `org_units`,
`member_team_memberships`, `schedule_profiles` e `activity_codes`. Essas
extensões não podem ser tratadas como publicadas ou obrigatórias sem inspeção
do ambiente real.

## Publicação, versão e timestamps

- `active == true` é o único marcador de período ativo confirmado no leitor
  legado.
- `publishedAt` e `publishedBy` existem, mas são opcionais; não há enum de
  preview/publicação confirmado nos documentos de período.
- `updatedAt` é usado para detectar mudanças e deve ser um timestamp Firestore
  ou uma representação ISO compatível no Dashboard.
- Não há `schemaVersion` confirmado nos modelos Firestore legados. Os valores
  `ScheduleCacheSchemaVersion = 1` e `OnCallCacheSchemaVersion = 1` pertencem ao
  cache local do KMP e não provam uma versão do documento remoto.
- A spec universal propõe campos adicionais, mas está marcada como proposta e
  não comprova deploy.

## Diferenças para `DomainModels`

| Remoto legado | `DomainModels` KMP | Conversão necessária |
|---|---|---|
| `periodId` | `SchedulePeriod.id` / `OnCallPeriod.id` | renomear |
| `assignmentId` | `ScheduleAssignment.id` | renomear |
| `onCallId` | `OnCallAssignment.id` | renomear |
| `scaleName` + `memberId?` | `memberName` + `memberId` obrigatórios | resolver membro sem inventar ID |
| `assignmentType` + `shiftId/shiftName` | `ShiftType` | tabela de mapeamento validada por dados reais |
| `startDateTime`/`endDateTime` | datas e horários separados | separar preservando virada de dia/mês/ano |
| timestamp Firestore | `String` | normalizar para ISO-8601 |
| `publishedAt` opcional | sem campo equivalente | manter em DTO/metadados, não perder silenciosamente |
| ausência de `schemaVersion` remoto | validação exigida pela 1B | contrato precisa ser definido antes da conexão |

O campo `memberId` remoto é opcional, mas o domínio KMP o exige. Usar
`scaleName`, e-mail ou valor sintético como identificador seria invenção de
dado e não está autorizado. O mesmo cuidado vale para converter
`assignmentType` em `ShiftType`.

## Autenticação e regras

Não é possível afirmar quais regras estão publicadas hoje. Há dois estados
documentados, ambos inadequados para ligar o KMP agora:

1. o Android legado funciona sem Firebase Auth, o que implica regras atuais
   permissivas ou exceções ainda não versionadas; não se deve reproduzir nem
   ampliar esse acesso;
2. as regras-alvo do Dashboard exigem `request.auth` e claims, sessão que o KMP
   não possui.

Cloudflare Access protege a entrada do site, mas não produz uma identidade
Firebase para o Web/Wasm. Copiar a configuração pública do cliente também não
resolve autorização. Não serão usados service account, client secret, token ou
credencial corporativa no cliente.

## Estratégia técnica avaliada

A arquitetura adequada, depois de liberados os pré-requisitos, é manter DTOs,
mapeadores, validações, gateway abstrato e fontes em `commonMain`, com
implementações específicas por plataforma. Android poderia usar o SDK Android;
Web/Wasm precisaria de integração JavaScript ou biblioteca comprovadamente
compatível. Nenhuma dependência foi escolhida porque isso seria prematuro sem:

- confirmar as regras publicadas e a identidade exigida;
- definir `schemaVersion` remoto compatível;
- resolver `memberId` ausente e o mapeamento de turnos sem inferência;
- disponibilizar ambiente de desenvolvimento/emulador com dados sanitizados.

## Campos e fatos ainda desconhecidos

- regras efetivamente publicadas no projeto `escalaici`;
- índices compostos efetivamente implantados;
- existência e conteúdo real das coleções universais propostas;
- `organizationId` dos dados publicados;
- contrato remoto de `schemaVersion`;
- garantia de unicidade de um período ativo por equipe;
- convenções reais de `assignmentType` e sua correspondência completa com
  `ShiftType` no KMP;
- tratamento oficial de assignment sem `memberId`;
- ambiente Firebase de desenvolvimento autorizado para Web e Android.

## Riscos e condição para retomada

Conectar agora poderia expor dados por acesso anônimo, bloquear todos os reads
sob regras autenticadas, aceitar documento incompatível, inventar identidade de
membro ou misturar schema proposto com produção legada.

A KMP-MVP-1B pode ser retomada quando houver, simultaneamente:

1. exportação ou confirmação das regras realmente publicadas;
2. decisão de autenticação Firebase para Android e Web/Wasm, validada em
   ambiente não produtivo;
3. amostra sanitizada do schema efetivamente publicado ou Firestore Emulator;
4. definição explícita de `schemaVersion`, tipos de assignment e política para
   documentos legados sem `memberId`;
5. autorização do ambiente de desenvolvimento somente leitura.

Até lá, arquivo local, cache local e demonstração permanecem inalterados e são
as únicas fontes operacionais do KMP.
