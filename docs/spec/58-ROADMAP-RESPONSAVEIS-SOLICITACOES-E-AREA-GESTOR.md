# Spec 58 — Roadmap: responsáveis, solicitações, área do gestor e workspace Demo

**Status:** proposta; nenhuma linha de código funcional criada por esta spec
**Escopo:** ordena a implementação das specs 56 e 57
**Fase:** FASE 14c-0 (contrato mestre, documental)

## 1. Como ler este roadmap

Cada fase abaixo é **independente e sequencial** — nenhuma começa antes de a
anterior satisfazer sua "condição para avançar". Todas as fases de código
(14c-1 em diante) vivem em branches próprias, nascidas da branch documental
`feature/fase-14c-gestao-responsaveis-aprovacoes` (ou de `master` depois que
ela for revisada/mesclada — decisão humana, fora do escopo desta spec).

Dois repositórios estão envolvidos; não misturar alterações:

| Repositório | Uso nesta série |
|---|---|
| `EscalaICI-KMP-Lab` (este) | App Escala ICI (Android/Web/PWA), Firestore Rules, specs. |
| Dashboard (React, repositório irmão) | Cadastro de responsáveis, publicação, aprovação — branch sugerida `feature/responsaveis-times-aprovacoes`, só começa depois que este contrato mestre estiver aprovado. |

## 2. FASE 14c-1 — MSAL para member/team e identidade Demo

- **Repositório:** `EscalaICI-KMP-Lab`.
- **Branch sugerida:** `feature/fase-14c1-msal-member-team`.
- **Arquivos previstos:** novo repositório/adapter que resolve
  `CorporateIdentity` (spec 53) → `members` → `member_team_memberships` →
  `teamId`; extensão do seletor de personagem Demo existente
  (`LoginGateScreen`) para também resolver dentro do `workspace demo-v1`.
- **Dependências:** spec 46/47 (identidade, `user_links`), spec 53 (MSAL
  Android já validado).
- **Critérios de aceitação:** login corporativo autenticado resolve
  `memberId`/`teamId` reais quando existir vínculo; estado explícito quando
  não existir (`AUTHENTICATED_MEMBER_NOT_FOUND`, `TEAM_NOT_FOUND`, spec 57);
  modo Demo resolve o mesmo caminho dentro de `workspaceId: demo-v1`, sem
  nenhuma dependência de MSAL.
- **Testes:** unitário da resolução de identidade → membro → equipe (casos:
  encontrado, membro ausente, equipe ausente, múltiplas equipes); fake
  repository para o modo Demo.
- **Não fazer:** implementar cadastro de responsáveis; alterar
  `firestore.rules`; ligar Firebase Auth real (isso é FASE 14d, série
  paralela já documentada).
- **Condição para avançar:** os 5 estados da spec 57 seção "consultas do
  app" resolvem corretamente em teste, para produção e para Demo.

## 3. FASE 14c-2 — Fixtures oficiais do workspace `demo-v1`

- **Repositório:** `EscalaICI-KMP-Lab`.
- **Branch sugerida:** `feature/fase-14c2-fixtures-demo`.
- **Arquivos previstos:**
  ```text
  fixtures/demo/workspace-demo-v1.json
  fixtures/demo/members-demo-v1.json
  fixtures/demo/teams-demo-v1.json
  fixtures/demo/manager-assignments-demo-v1.json
  fixtures/demo/schedule-demo-v1.json
  fixtures/demo/requests-demo-v1.json
  ```
  Avaliar nesta fase se um único pacote agregado é mais coerente com o
  contrato JSON existente (`docs/contracts/organization-approval-v1.schema.json`)
  do que seis arquivos separados — decisão de implementação, não desta spec.
- **Dependências:** spec 56 seção 12 (cenário `schedule-approval-basic`),
  spec 57 (campos de cada coleção).
- **Critérios de aceitação:** fixtures validam contra o JSON Schema; IDs
  determinísticos batem exatamente com os listados na spec 56 seção 12.7;
  nenhum dado real, nenhum e-mail fora de `example.invalid`.
- **Testes:** validação de schema automatizada (mesmo comando da seção 15
  desta fase 14c-0, aplicado a cada fixture).
- **Não fazer:** conectar as fixtures a Firebase real ainda (isso é FASE
  14c-4); criar fixtures para o workspace `ici`.
- **Condição para avançar:** as fixtures existem, validam contra o schema, e
  descrevem exatamente o cenário da spec 56 seção 12.5 (um gestor, duas
  equipes, quatro colaboradores, um período, uma solicitação pendente).

## 4. FASE 14c-3 — Dashboard: cadastro de responsáveis + Demo local

- **Repositório:** Dashboard (branch `feature/responsaveis-times-aprovacoes`).
- **Arquivos previstos:** tela `Configurações → Responsáveis e aprovações`
  (spec 56 seção 10.1); seletor "Iniciar workspace" (Produção/local vs.
  Demonstração); ação local "carregar cenário Demo" lendo as fixtures da
  FASE 14c-2, sem Firebase ainda.
- **Dependências:** FASE 14c-2 (fixtures), spec 56 seções 10.1/11/12.6.
- **Critérios de aceitação:** cadastro de responsável funciona localmente
  (sem publicar); modo Demo carrega o cenário fixo em memória/local; aviso
  "modo local de teste" e aviso "ambiente de demonstração" aparecem conforme
  o modo ativo (podem aparecer juntos).
- **Testes:** testes de componente do Dashboard (fora do escopo deste
  repositório KMP — a cargo do time/sessão do Dashboard).
- **Não fazer:** publicar no Firebase (isso é FASE 14c-4); implementar MSAL
  no Dashboard (isso é FASE 14c-10); permitir campo de texto livre para
  login do responsável (deve ser sempre seleção, spec 56 seção 11).
- **Condição para avançar:** um responsável cadastrado localmente aparece
  corretamente na lista, com as seis permissões editáveis individualmente.

## 5. FASE 14c-4 — Publicação Demo (e produção) pelo Express local

- **Repositório:** Dashboard.
- **Arquivos previstos:** endpoint `POST /api/publish` (produção) e
  `POST /api/demo/reset` (spec 56 seção 12.9) no backend Express local;
  Firebase Admin SDK só no backend, credenciais fora do Git.
- **Dependências:** FASE 14c-3, spec 56 seção 13 (segurança transitória),
  spec 57 seção 6 (`publication_records`).
- **Critérios de aceitação:** botão "Publicar" mostra preview de diferenças
  (contagem de inclusões/alterações/exclusões) antes de confirmar; exclusão
  destrutiva desabilitada; toda publicação grava um `publication_records`;
  `workspaces/{id}.publicationRevision` incrementa corretamente; reset Demo
  nunca afeta `workspaceId: ici` (testado explicitamente).
- **Testes:** teste de integração contra o Firebase Emulator — publicar Demo,
  confirmar `publicationRevision` incrementado, confirmar zero documentos
  afetados em `ici`; publicar produção com dry-run antes de escrita real.
- **Não fazer:** expor Firebase Admin ao navegador; permitir escrita pública
  no Firestore; automatizar upload/deploy de regras.
- **Condição para avançar:** publicar o cenário Demo cria/atualiza
  exatamente os documentos esperados no Firestore (Emulator ou projeto de
  desenvolvimento), sem tocar em `ici`.

## 6. FASE 14c-5 — App sincroniza equipe, responsável e escala (produção e Demo)

- **Repositório:** `EscalaICI-KMP-Lab`.
- **Branch sugerida:** `feature/fase-14c5-app-equipe-responsavel`.
- **Arquivos previstos:** consulta real a `team_manager_assignments` (spec
  57 seção 3) a partir do `teamId` resolvido na FASE 14c-1; tela "Minha
  equipe" com responsável principal/substituto (spec 56 seção 10.2).
- **Dependências:** FASE 14c-1 (identidade→equipe), FASE 14c-4 (dados
  publicados para ler).
- **Critérios de aceitação:** app mostra nome/cargo do responsável (nunca
  login/e-mail); ausência de responsável não bloqueia a consulta da própria
  escala (spec 56/57, estado `MANAGER_NOT_ASSIGNED` continua permitindo ver
  a escala); modo Demo mostra o mesmo fluxo com o personagem selecionado.
- **Testes:** teste de UI/unitário para os 5 estados (`AUTHENTICATED_MEMBER_FOUND`,
  `AUTHENTICATED_MEMBER_NOT_FOUND`, `TEAM_NOT_FOUND`, `MANAGER_NOT_ASSIGNED`,
  `MANAGER_FOUND`).
- **Não fazer:** implementar criação de solicitação ainda (FASE 14c-6);
  expor `tenantId`/`objectId`/login do responsável na tela comum.
- **Condição para avançar:** o cenário Demo, publicado na FASE 14c-4,
  aparece corretamente no app (duas equipes, um responsável comum às duas).

## 7. FASE 14c-6 — App cria solicitação de alteração

- **Repositório:** `EscalaICI-KMP-Lab`.
- **Branch sugerida:** `feature/fase-14c6-app-solicitacoes`.
- **Arquivos previstos:** tela "Solicitar alteração" (a partir de "Minhas
  solicitações", spec 56 seção 10.2); escrita em `schedule_change_requests`
  via backend (nunca escrita direta do app no Firestore sem regra validada).
- **Dependências:** FASE 14c-5, spec 57 seção 4.
- **Critérios de aceitação:** solicitação criada grava
  `assignedManagerMemberId` resolvido no momento (fotografia, spec 56 seção
  9.3); os 5 tipos (`SHIFT_CHANGE`/`DAY_OFF_CHANGE`/`SWAP_WITH_MEMBER`/
  `SCHEDULE_CORRECTION`/`OTHER`) estão disponíveis na UI.
- **Testes:** unitário da fotografia do responsável (trocar o responsável
  depois de criar a solicitação e confirmar que ela não migra).
- **Não fazer:** aprovar/recusar no app ainda (fica só no Dashboard até a
  FASE 14c-9); permitir solicitação sem `reason` preenchido.
- **Condição para avançar:** criar uma solicitação pelo personagem "Analista
  SOC Demo 1" aparece na fila do Dashboard, endereçada ao "Gestor de
  Segurança Demo".

## 8. FASE 14c-7 — Dashboard aprova ou recusa

- **Repositório:** Dashboard.
- **Arquivos previstos:** aba "Solicitações" (spec 56 seção 10.1); ações
  Aprovar/Recusar (recusa exige `resolutionNote`, spec 56 seção 9.4).
- **Dependências:** FASE 14c-6.
- **Critérios de aceitação:** filtros (pendentes/aprovadas/recusadas/times/
  colaborador) funcionam; recusar sem justificativa é bloqueado pela UI;
  aprovar/recusar grava `resolvedAt`/`resolvedByMemberId`.
- **Testes:** teste de componente cobrindo aprovação, recusa sem nota
  (bloqueada) e recusa com nota (permitida).
- **Não fazer:** aplicar a alteração na escala automaticamente ainda (FASE
  14c-8); permitir decisão por alguém sem `approveScheduleChanges` na
  equipe da solicitação.
- **Condição para avançar:** aprovar/recusar a solicitação Demo inicial
  (spec 56 seção 12.5) funciona ponta a ponta, incluindo a recusa exigindo
  nota.

## 9. FASE 14c-8 — Aplicação e republicação da alteração

- **Repositório:** Dashboard.
- **Arquivos previstos:** ação "Aplicar na escala" (spec 56 seção 11,
  fluxo em duas etapas: aprovar → confirmar aplicação); nova
  `publicationRevision` após aplicar.
- **Dependências:** FASE 14c-7, FASE 14c-4 (publicação/revisão).
- **Critérios de aceitação:** aplicar uma solicitação aprovada gera um
  `schedule_change_events` (coleção já existente, spec 57 seção 1) referenciando
  a solicitação; o app, ao sincronizar de novo, mostra a escala já alterada
  e a nova `publicationRevision`.
- **Testes:** cenário `DEMO-E2E-001` completo (seção 11 desta spec).
- **Não fazer:** tornar a aplicação automática/transacional nesta fase — o
  gestor sempre confirma explicitamente antes de aplicar (decisão
  arquitetural fixada na spec 56 seção 11; automatizar por transação fica
  para uma fase posterior, fora deste roadmap).
- **Condição para avançar:** o cenário `DEMO-E2E-001` passa integralmente,
  sem nenhum documento do workspace `ici` alterado.

## 10. FASE 14c-9 — Área do gestor no mesmo aplicativo

- **Repositório:** `EscalaICI-KMP-Lab`.
- **Branch sugerida:** `feature/fase-14c9-area-gestor`.
- **Arquivos previstos:** nova seção condicional no app, liberada quando o
  `memberId` autenticado (corporativo ou personagem Demo) tem ao menos um
  `team_manager_assignments` ativo.
- **Dependências:** FASE 14c-1, FASE 14c-8 (para a etapa "aplicar
  alteração", que só chega na segunda etapa desta fase).
- **Critérios de aceitação:** primeira etapa somente leitura (equipes
  geridas, colaboradores, escalas, solicitações); segunda etapa (mesma fase,
  entrega incremental) adiciona aprovar/recusar/justificar/solicitar
  informação direto pelo app, espelhando o Dashboard.
- **Testes:** teste de navegação condicional (usuário sem vínculo de gestão
  nunca vê a área do gestor); teste do fluxo de aprovação pelo app.
- **Não fazer:** criar um segundo aplicativo (proibido explicitamente, spec
  56 seção 10.3); liberar a área do gestor a partir só do papel, ignorando
  as permissões concretas do vínculo.
- **Condição para avançar:** o "Gestor de Segurança Demo" consegue ver e
  (na segunda etapa) decidir sobre solicitações das duas equipes que
  administra, dentro do próprio Escala ICI.

## 11. FASE 14c-10 — MSAL no Dashboard e segurança de produção

- **Repositório:** Dashboard.
- **Arquivos previstos:** integração MSAL.js (`@azure/msal-browser`, mesmo
  app registration, mesmo tenant — spec 46 seção 4); backend validando o
  id_token Microsoft antes de qualquer publicação real.
- **Dependências:** todas as fases anteriores desta spec funcionando com o
  modo local de teste.
- **Critérios de aceitação:** login manual/seletor de teste é **removido de
  produção** (pode continuar existindo só no workspace Demo, com o aviso da
  spec 56 seção 12.6); toda publicação de produção passa a exigir identidade
  corporativa real; Firestore Rules definitivas (spec 51 seção 13, FASE 14d)
  reconciliadas com `team_manager_assignments` (spec 57 seção 8).
- **Testes:** os mesmos da spec 46 seção 12, aplicados ao Dashboard.
- **Não fazer:** remover o modo Demo (ele continua existindo como ambiente
  de homologação oficial, spec 56 seção 12.1); misturar esta fase com
  qualquer alteração de escala/parser.
- **Condição para avançar:** nenhuma — é a última fase desta série; a saída
  natural é o MVP oficial (spec 45 do EscalaSOC, FASE 13f do roadmap local).

## 12. Cenário de aceitação ponta a ponta — `DEMO-E2E-001`

Critério de aceite agregado, verificável a partir da FASE 14c-8:

```text
1. restaurar demo-v1
2. abrir Dashboard em modo Demo
3. alterar um turno da escala Demo
4. publicar nova revisão
5. abrir Escala ICI em modo Demo
6. sincronizar
7. confirmar que a alteração chegou (nova publicationRevision)
8. criar uma solicitação (personagem Analista SOC Demo 1)
9. abrir Dashboard como Gestor de Segurança Demo
10. confirmar que a solicitação aparece pendente
11. aprovar ou recusar (recusar exige nota)
12. sincronizar o app
13. confirmar o novo status da solicitação
14. aplicar a alteração na escala (se aprovada)
15. publicar nova revisão
16. sincronizar novamente o app
17. confirmar a escala final
18. restaurar o cenário Demo
```

**Critério de sucesso:** nenhum documento do workspace `ici` foi alterado em
nenhum passo.

## 13. O que esta série de fases nunca faz

- Criar um segundo aplicativo para gestores.
- Tratar o login manual do Dashboard como autenticação real, em qualquer
  fase anterior à FASE 14c-10.
- Aplicar uma solicitação aprovada na escala sem confirmação humana
  explícita, antes da FASE 14c-8 decidir automatizar (e mesmo essa fase
  mantém confirmação, não transação automática).
- Misturar dados/IDs do workspace Demo com o workspace de produção.
- Publicar `firestore.rules` novas (fica com a série FASE 14d, já
  documentada nas specs 46/48/51).
