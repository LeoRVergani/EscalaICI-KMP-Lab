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

## Identidade técnica e release

Nesta fase não muda:

- `applicationId`: `br.com.leorvergani.escalaici.kmp.lab`;
- keystore de assinatura do KMP lab: `escalaici-kmp-lab.jks`;
- nome técnico do repositório: `EscalaICI-KMP-Lab`;
- processo de upload do Escala ICI: manual, conforme spec 41.

O nome visível do produto é **Escala ICI**, mas a identidade técnica instalada
continua histórica. Qualquer mudança de `applicationId`, keystore, pacote,
ícone final, loja ou estratégia de migração exige decisão explícita do usuário
em fase própria.
