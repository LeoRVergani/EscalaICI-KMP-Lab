# Specs do Escala ICI

Este arquivo é um índice local resumido. O conteúdo completo das specs vive no
repositório irmão `EscalaSOC`, em `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/`.

O repositório `EscalaICI-KMP-Lab` é onde o app novo **Escala ICI** é
implementado em Kotlin Multiplatform + PWA. As specs 35-45 formam o plano
completo para levar esse app até o MVP oficial, sem desligar o `EscalaSOC`
abruptamente e sem transformar documentação em mudança real de produção.

## Specs originais

| Spec | Resumo | Caminho completo |
|---|---|---|
| 35 — Firestore universal | Define as 18 coleções universais, a hierarquia ICI/GEDSI/COSI/SOC e GST/CCS/SD/N1, regras de `ScheduleUiConfig`, cargos, códigos N1 e compatibilidade com coleções existentes. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/35-ESCALAICI-FIRESTORE-UNIVERSAL.md` |
| 36 — Dashboard universal | Planeja o dashboard para cadastrar organização, unidades, equipes, membros, vínculos, cargos, perfis, códigos, importações, preview, publicação, histórico e permissões. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/36-ESCALAICI-DASHBOARD-UNIVERSAL.md` |
| 37 — Importadores Excel | Define parsers por layout: SOC 6x1 por turnos, N1 matriz por códigos, Plantão COSI por intervalos, 12x36 e horário comercial futuro. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/37-ESCALAICI-IMPORTADORES-EXCEL.md` |
| 38 — App: leitura universal | Descreve como o app futuro deve ler login, membro, memberships, equipe, perfil, período, assignments e códigos com fallback local/importado. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/38-ESCALAICI-APP-LEITURA-UNIVERSAL.md` |
| 39 — MSAL, Entra e permissões | Registra login Microsoft/Entra, diferenças Android/Web, dados reais já documentados, `dashboard_permissions` e separação entre cargo/função e permissão. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/39-ESCALAICI-MSAL-ENTRA-PERMISSOES.md` |
| 40 — PWA, Web/Wasm e iOS | Separa o que já existe em Android/Web/PWA, os limites de SheetJS/POI/OAuth, e trata iOS como alvo futuro dependente de Mac. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/40-ESCALAICI-PWA-WEB-IOS-ESTRATEGIA.md` |
| 41 — Release, APK e update | Consolida versionamento, assinatura, APK, Dropbox manual, campos `kmp*` no `version.json`, keystore e regra de não mudar `applicationId` sem decisão explícita. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/41-ESCALAICI-RELEASE-UPDATE-APK.md` |
| 42 — Migração EscalaSOC -> Escala ICI | Define migração gradual, compatibilidade de dados, critérios para substituir por grupo, rollback e limites que exigem confirmação humana. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/42-ESCALAICI-MIGRACAO-ESCALASOC-PARA-ESCALAICI.md` |
| 43 — Cards UI/UX oficial | Documenta cards atuais e futuros, card **Atividade do Dia**, cargo/função, nomenclatura oficial e adaptação por `ScheduleUiConfig`. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/43-ESCALAICI-CARDS-UI-UX-OFICIAL.md` |
| 44 — Testes, qualidade e validação | Registra os 39 testes atuais, lacunas de parser/Firestore/dashboard/cache e checklists Android, Web/PWA e manuais. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/44-ESCALAICI-TESTES-QUALIDADE-VALIDACAO.md` |
| 45 — Roadmap MVP oficial | Organiza as fases 12c a 13f até o MVP oficial, com objetivo, escopo permitido, validações e pontos de parada humana. | `/home/lvergani/AndroidStudioProjects/EscalaSOC/docs/spec/45-ESCALAICI-ROADMAP-MVP-OFICIAL.md` |

## Specs locais — FASE 14a (autenticação, vínculo, sincronização, pausa, migração)

Diferente das specs 35-45 (que vivem no repositório `EscalaSOC`), as specs
46-50 abaixo vivem **neste repositório**, em `docs/spec/`, porque tratam
especificamente da implementação do Escala ICI KMP (Android + Web/PWA) e do
ponto de integração com o Dashboard/Firestore. Nenhuma delas implementa código
funcional — são o resultado da auditoria e do desenho arquitetural da FASE
14a.

| Spec | Resumo |
|---|---|
| [46 — Autenticação corporativa MSAL + Firebase](spec/46-ESCALAICI-AUTENTICACAO-CORPORATIVA-MSAL-FIREBASE.md) | Arquitetura MSAL Android/Web, ponte segura (Cloud Function) entre id_token Microsoft e Firebase custom token, nunca tratando um como o outro diretamente. |
| [47 — Vínculo usuário, membro e time](spec/47-ESCALAICI-VINCULO-USUARIO-MEMBRO-E-TIME.md) | Define `CorporateIdentity` e a coleção `user_links/{firebaseUid}`, indexada por `tenantId`+`objectId`, nunca por e-mail. |
| [48 — Sincronização de escala e cache offline](spec/48-ESCALAICI-SINCRONIZACAO-ESCALA-CACHE-OFFLINE.md) | Substitui a leitura "busca-tudo-e-filtra" por queries reais, define 9 estados tipados de erro e formaliza a política de preservar cache em falha. |
| [49 — Pausa de 15 minutos e notificações](spec/49-ESCALAICI-PAUSA-15-MINUTOS-E-NOTIFICACOES.md) | Torna funcional o controle hoje decorativo, corrigindo as lacunas conhecidas do app legado (boot receiver, mudança de fuso, alarme inexato). |
| [50 — Migração final e paridade](spec/50-ESCALAICI-MIGRACAO-FINAL-E-PARIDADE.md) | Matriz completa EscalaSOC × Escala ICI por funcionalidade, com bloqueadores centrais ordenados por urgência. |
| [51 — Endurecimento transitório do Firestore](spec/51-ESCALAICI-FIRESTORE-HARDENING-TRANSITORIO.md) | Regra emergencial (FASE 14a.1) que elimina toda escrita anônima, testada com 22 casos no Emulator, mantendo a leitura anônima mínima do KMP no nível atual até a FASE 14d. |
| [52 — Identidade oficial, assinatura e release](spec/52-ESCALAICI-IDENTIDADE-OFICIAL-ASSINATURA-E-RELEASE.md) | Consolida `applicationId`, assinatura, versionamento e processo de release manual do Escala ICI. |
| [53 — MSAL Android e identidade corporativa](spec/53-ESCALAICI-MSAL-ANDROID-E-IDENTIDADE-CORPORATIVA.md) | Implementa a identidade corporativa Android com MSAL, mantendo fallback explícito quando não configurado. |
| [54 — Ícone oficial Android e Web](spec/54-ESCALAICI-ICONE-OFICIAL-ANDROID-WEB.md) | Registra fonte, transparência, área segura e aplicação do ícone oficial no Android, splash e Web/PWA. |
| [56 — Gestão de responsáveis e aprovações](spec/56-GESTAO-DE-RESPONSAVEIS-E-APROVACOES.md) | Contrato mestre (FASE 14c-0): responsáveis/aprovadores por equipe, papéis, permissões, substituição temporária e o ambiente de demonstração (`workspace`) oficial. |
| [57 — Modelo Firestore: organização e solicitações](spec/57-MODELO-FIRESTORE-ORGANIZACAO-E-SOLICITACOES.md) | Estende o Firestore universal (spec 35) sem substituir: `team_manager_assignments`, `schedule_change_requests`, `workspaces`, `publication_records`, campo aditivo `workspaceId`. |
| [58 — Roadmap: responsáveis, solicitações e área do gestor](spec/58-ROADMAP-RESPONSAVEIS-SOLICITACOES-E-AREA-GESTOR.md) | Sequência FASE 14c-1 a 14c-10, incluindo o cenário de aceitação ponta a ponta `DEMO-E2E-001`. |

**Achado que motivou a urgência desta fase**: as regras Firestore hoje em
produção (`firebase/firestore.production.snapshot.rules`) liberam leitura e
escrita totalmente livres até **4 de agosto de 2026** — nenhum cliente tem
ainda uma sessão Firebase Auth real (ver spec 46), então essa janela de ~3
semanas é o prazo prático para decidir e ao menos iniciar a implementação da
ponte de autenticação antes de qualquer regra autenticada poder ser publicada
com segurança.

## Relação com o código atual

O app neste repositório já implementou parte da direção do spec 35 como
modelos Kotlin puros, mas ainda não está conectado ao Firestore universal real.

Já existe na FASE 12b o arquivo `UniversalOrgModels.kt`, com modelos como
`Organization`, `OrgUnit`, `Role`, `MemberTeamMembership`, `ScheduleProfile`,
`ActivityCode`, `BusinessHoursRule` e `TwelveByThirtySixRule`.

Na FASE 12b-2 também já existem `ScheduleUiConfig`,
`ActivityCode.visibleInApp`, `ActivityCode.cardTitle`,
`ActivityCode.sortOrder`, `Role.roleShortName`,
`Role.roleDisplayName` e `formatMemberWithRole()`. Esses pontos cobrem parte
do schema e das regras de UI descritas no spec 35, especialmente card de
atividade por perfil/equipe e cargo/função visível no app.

Limite importante: esses modelos e helpers ainda são locais/mocks/testes. O
app **não** lê `organizations`, `org_units`, `schedule_profiles`,
`activity_codes` ou `member_team_memberships` de Firestore real nesta fase.

## JSONs documentais locais

Foram adicionados dois arquivos apenas para documentação:

- `docs/firebase/firestore-universal-schema.json`: estrutura resumida das 18
  coleções do spec 35, com campos e tipos esperados.
- `docs/firebase/firestore-universal-seed-example.json`: exemplos de
  documentos baseados no spec 35.

Esses JSONs têm `_documentOnly: true`, não são seed de produção, não são lidos
pelo app e não autorizam deploy, importação, Rules ou escrita em Firestore.

## Roadmap resumido

| Fase | Objetivo | Status |
|---|---|---|
| 12c | Fechar documentação do schema universal, contratos de migração e pacote de specs 35-45. | em andamento — specs concluídas, schema/dashboard/app real ainda não implementados |
| 12d | Dashboard universal para organização, unidades, siglas, equipes, membros, cargos e memberships. | não iniciada |
| 12e | Dashboard universal para perfis de escala, `ScheduleUiConfig`, turnos e códigos de atividade. | não iniciada |
| 12f | Importador/preview real da planilha N1 Service Desk por matriz mensal de códigos. | não iniciada |
| 12g | Firestore universal em modo seguro/dev ou emulador, com seeds e testes de integração. | não iniciada |
| 12h | App Escala ICI lendo Firestore universal, mantendo fallback local/importado. | não iniciada |
| 12i | Cards configuráveis reais no app: Atividade do Dia e cargo/função por `ScheduleUiConfig`. | não iniciada |
| 12j | Plantão COSI universalizado por `oncall_periods` e `oncall_assignments`. | não iniciada |
| 12k | Perfis reais de 12x36 e horário comercial, com regras e validações próprias. | não iniciada |
| 12l | Suporte a múltiplas equipes por usuário, seleção de equipe e preferência/cache por perfil. | não iniciada |
| 13a | Decisão explícita de identidade oficial final: pacote, ícone, loja e estratégia de migração. | não iniciada |
| 13b | MSAL/Entra real no Escala ICI, começando pelo Android e planejando Web/PWA. | não iniciada |
| 13c | Permissões reais no dashboard com `dashboard_permissions` por escopo. | não iniciada |
| 13d | Release beta interno para grupo pequeno, com versionamento, APK assinado e rollback. | não iniciada |
| 13e | Testes com usuários reais, feedback, correções e validação operacional. | não iniciada |
| 13f | MVP oficial com Firestore, dashboard, MSAL, permissões, beta estável e rollback. | não iniciada |
| 14a | Auditoria e specs finais de autenticação, sincronização, pausa e migração (specs 46-50, este pacote). | concluída — documentação apenas |
| 14a.1 | Endurecimento emergencial das Firestore Rules: elimina escrita anônima, mantém leitura mínima atual do KMP (spec 51). | concluída — regras/testes prontos, deploy pendente de ação humana |
| 14b | MSAL e identidade real no Escala ICI (Android primeiro, specs 46 e 53). | em andamento — 14b-1 e 14b-1a concluídas |
| 14c | Vínculo de usuário: coleção `user_links`, fluxo de vínculo administrativo (spec 47). Detalhado em 10 subfases (14c-1 a 14c-10) pelo contrato mestre de responsáveis/aprovações e workspace de demonstração (specs 56-58). | 14c-0 concluída — documentação apenas; 14c-1 em diante não iniciadas |
| 14c-0 | Contrato mestre de responsáveis, aprovadores, solicitações de alteração de escala e workspace de demonstração (specs 56-58). | concluída — documentação apenas |
| 14d | Firebase Auth real + substituição das Firestore Rules abertas (spec 46/48, antes de 2026-08-04). | não iniciada |
| 14e | Sincronização real de escala/plantão por query, cache e estados tipados (spec 48). | não iniciada |
| 14f | Pausa de 15 minutos e notificações reais, corrigindo lacunas do legado (spec 49). | não iniciada |
| 14g | Paridade funcional completa EscalaSOC × Escala ICI (spec 50). | não iniciada |
| 14h | Release candidato para o MVP oficial. | não iniciada |

## Identidade técnica e release

Identidade técnica atual:

- `applicationId`: `br.com.leorvergani.escalaici`;
- keystore de assinatura do KMP lab: `escalaici-kmp-lab.jks`;
- nome técnico do repositório: `EscalaICI-KMP-Lab`;
- processo de upload do Escala ICI: manual, conforme spec 41.

O nome visível do produto é **Escala ICI**, mas a identidade técnica instalada
passou a usar o identificador oficial `br.com.leorvergani.escalaici`.
Qualquer nova mudança de `applicationId`, keystore, pacote, ícone final,
loja ou estratégia de migração exige decisão explícita do usuário em fase
própria.
