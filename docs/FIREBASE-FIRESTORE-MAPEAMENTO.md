> **SUPERSEDED BY FASE 15** (`docs/spec/FASE-15-FIREBASE-UNIFICADO.md`).
> Este documento descreve o projeto Firebase antigo do laboratório
> (`escalaici`, leitura anônima) — substituído por completo pelo Firebase
> real do Escala-ICI, autenticado. Mantido só como registro histórico.

# Mapeamento Firebase/Firestore — KMP-MVP-1B

## Decisão da auditoria

A KMP-MVP-1B-SIMPLES autorizou uma leitura anônima e temporária da escala real,
sem escrita e sem Firebase Auth no KMP. As regras publicadas no projeto
`escalaici`, banco `(default)`, continuam em modo de teste: qualquer cliente
pode ler e escrever até 4 de agosto de 2026, sem Firebase Auth.

O KMP usa somente requisições REST `GET` às coleções existentes. Nenhum SDK
Firebase foi incluído, nenhum segredo foi adicionado e as regras locais ou
publicadas não foram alteradas. Essa integração não torna o banco seguro: ela
depende provisoriamente da leitura pública e deverá ser substituída ou protegida
antes da expiração das regras.

## Evidências auditadas

- Projeto Firebase configurado no Android legado: `escalaici`.
- O `google-services.json` existe somente no app Android irmão; ele não foi
  copiado para o KMP e nenhum valor de configuração foi exposto neste relatório.
- O Android legado usa diretamente o SDK `firebase-firestore`, sem Firebase
  Auth. A decisão técnica registrada no EscalaSOC confirma que suas requisições
  atuais chegam com `request.auth == null`.
- O Dashboard possui Firebase Auth/Microsoft e chega ao Firestore autenticado,
  mas as regras publicadas não usam essa identidade para autorizar operações.
- O arquivo `firestore.rules` encontrado no repositório do Dashboard cobre
  apenas `/escalas/{anoMes}` para usuários com claim `coordenador`; o restante
  é negado. O próprio arquivo avisa que precisaria ser mesclado às regras do
  Android. Portanto, ele não comprova as regras publicadas do projeto principal.
- O snapshot literal confirmado pelo Console está em
  `firebase/firestore.production.snapshot.rules`. Ele é evidência, não arquivo
  de deploy.

## Classificação das regras

### Publicadas e confirmadas

As regras reais permitem `read` e `write` recursivamente enquanto
`request.time < timestamp.date(2026, 8, 4)`. Não exigem autenticação, claims,
equipe ou papel. Após a expiração, todas as chamadas de clientes serão negadas.

### Encontradas no código

O Dashboard contém uma regra local para `/escalas/{anoMes}` e `dias`, exigindo
claim `coordenador`, seguida de negação global. O próprio comentário manda
mesclá-la às regras do Android. Ela não representa a publicação atual.

O arquivo `firebase/firestore.rules` deste repositório é uma nova proposta para
o Emulator: exige Firebase Auth para leitura e restringe escrita a
administradores de sistema/equipe. Também não representa produção.

### Desejadas para produção

As regras finais devem usar identidade Firebase verificável, permitir somente
as coleções e operações necessárias, restringir escrita por papel/equipe e
manter fallback negado. Só podem ser publicadas após testes conjuntos do
Dashboard e dos aplicativos, inventário de todas as operações e plano de
rollback. Não se deve trocar o modo de teste por `deny-all` sem essa validação.

## Coleções confirmadas pelo código legado e pela auditoria pública

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

Em 14 de julho de 2026 foi feita auditoria somente de nomes de campos, sem
registrar valores pessoais. Os dez documentos então existentes também
apresentavam `role`, `title` e `status` conforme o documento. Não foram
encontrados campos de telefone, endereço, documento pessoal, token, credencial
ou dado médico. O campo `email` existe, mas não é transportado para o modelo de
UI nem exibido pelo KMP.

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

Firebase Auth não é obrigatório pelas regras publicadas. O Dashboard, contudo,
usa Firebase Auth com o provedor Microsoft/Entra e verifica permissões em
`system_admins`, `teams` e `members` antes de mostrar a aplicação. Essa barreira
é apenas de UI/aplicação enquanto o banco permanece aberto: outro cliente pode
ignorar o Dashboard e acessar diretamente todas as coleções.

O Android legado e o KMP não possuem Firebase Auth. A retirada segura do modo
de teste precisa considerar essa diferença para não interromper o Android.

Cloudflare Access protege a entrada do site, mas não produz uma identidade
Firebase para o Web/Wasm. Copiar a configuração pública do cliente também não
resolve autorização. Não serão usados service account, client secret, token ou
credencial corporativa no cliente.

## Estratégia técnica implementada

DTOs, mapeadores, validações, gateway abstrato e fontes vivem em `commonMain`.
Web/Wasm e Android compartilham um cliente REST Firestore somente leitura; os
`actual` de plataforma fornecem cache isolado (`localStorage` na Web e
`SharedPreferences` no Android). O contrato não expõe `add`, `create`, `set`,
`update`, `delete`, `batch` ou `transaction`.

O leitor busca o período ativo, assignments e membros da equipe, valida o lote
completo e só então troca o cache. Escala e plantão têm chaves independentes.
`assignmentType` foi limitado aos valores confirmados `WORK_SHIFT`, `OFF` e
`VACATION`; os turnos confirmados são `Madrugada`, `Manhã`, `Tarde` e `Noite`.
Documento incompatível preserva o cache anterior.

## Campos e fatos ainda desconhecidos

- índices compostos efetivamente implantados;
- existência e conteúdo real das coleções universais propostas;
- `organizationId` dos dados publicados;
- contrato remoto de `schemaVersion`;
- garantia de unicidade de um período ativo por equipe;
- convenções reais de `assignmentType` e sua correspondência completa com
  `ShiftType` no KMP;
- tratamento oficial de assignment sem `memberId`;
- ambiente Firebase de desenvolvimento autorizado para Web e Android.

## Riscos e condição para retirada segura do modo de teste

O banco já está exposto a leitura, criação, alteração e exclusão anônimas até a
data limite. Conectar mais um cliente agora consolidaria essa dependência. Uma
troca apressada por regras fechadas, por outro lado, pode interromper Dashboard
e Android. Também permanecem os riscos de documento incompatível, identidade de
membro inventada e mistura entre schema proposto e produção legada.

A leitura implementada é uma ponte operacional, não a solução de autorização.
Antes de 4 de agosto de 2026 ainda são necessários:

1. decisão de autenticação Firebase para Android e Web/Wasm, validada em
   ambiente não produtivo;
2. amostra sanitizada do schema efetivamente publicado no Firestore Emulator;
3. definição explícita de `schemaVersion`, tipos de assignment e política para
   documentos legados sem `memberId`;
4. testes de regras cobrindo Dashboard e leitores antes da retirada do modo de
   teste;
5. plano de migração e rollback anterior a 4 de agosto de 2026.

Arquivo local, cache local e demonstração permanecem como fallbacks. O modo
LOCAL não é substituído silenciosamente pelo Firebase.

## Emulator isolado e validação

A estrutura em `firebase/` usa o project ID reservado
`demo-escalaici-kmp`, Firestore em `127.0.0.1:8085` e UI em
`127.0.0.1:4005`. O prefixo `demo-` faz o Firebase Emulator rejeitar tentativas
de alcançar serviços não emulados.

Comando executado em 14 de julho de 2026:

```text
cd firebase
npm run test:rules
```

Resultado: 7 testes aprovados, nenhum reprovado. Foram cobertos acesso anônimo,
leitura autenticada, leitor sem escrita, administrador de equipe, proteção de
`teamId`/`adminEmails`, administrador do sistema e coleção desconhecida. Esses
testes não validam ainda todos os fluxos do Dashboard nem autorizam deploy.
