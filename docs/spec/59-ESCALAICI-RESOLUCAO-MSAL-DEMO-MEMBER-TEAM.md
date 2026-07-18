# Spec 59 — Resolução de identidade: MSAL corporativo e personagem Demo → member/team

**Status:** implementada (camada pura + integração de UI); validação final registrada nas seções 11-13
**Escopo:** Escala ICI KMP (Android + Web/PWA), pacote novo `br.com.leorvergani.escalaici.identity`
**Fase:** FASE 14c-1 — primeira fase de código da série 14c (specs 56-58)
**Depende de:** spec 46/47/53 (MSAL, `CorporateIdentity`, `CorporateAuthState`), spec 56/57 (contrato de responsáveis/workspace, ainda não implementado — só o vínculo membro↔equipe é resolvido aqui)
**Não implementa:** responsáveis por equipe (`team_manager_assignments`), solicitações de alteração, Dashboard, publicação, escrita no Firestore, Firebase Authentication, área do gestor — tudo isso continua na spec 58.

## 1. Escopo desta fase

```text
Identidade corporativa MSAL (CorporateIdentity, já validada)
        ↓
member (Member existente)
        ↓
member_team_memberships (MemberTeamMembership existente, agora consultado de verdade)
        ↓
team (Team existente)
```

e, em paralelo, isolado por workspace:

```text
Personagem do workspace Demo (novo catálogo fixo, 3 personas)
        ↓
member Demo / membership Demo / team Demo (dados fixos em memória, workspaceId = demo-v1)
```

Ao final desta fase o app sabe, quando configurado com um `OrganizationIdentityResolver`: workspace atual, `memberId`, nome do membro, equipe principal, outros vínculos ativos (quando existirem) e a origem da identidade (`CORPORATE_MSAL` ou `DEMO_PERSONA`) — sem nunca misturar os dois workspaces.

## 2. Diagnóstico do modelo real (antes de qualquer código, ver auditoria completa no relatório desta fase)

Confirmado por leitura direta do código antes de escrever qualquer linha nova:

- `Member`/`Team` (`model/ScheduleModels.kt`) já existiam e são os únicos modelos conectados à UI/estado real.
- `MemberTeamMembership` (`model/UniversalOrgModels.kt`, FASE 12b) já existia, mas como modelo Kotlin puro **sem nenhum repositório, gateway Firestore ou UI conectados** — esta fase é a primeira a consultá-lo de fato (ainda que só com dados locais/Demo, nunca Firestore real).
- Não existe hoje nenhuma leitura Firestore de `member_team_memberships` (fora das 6 coleções da spec 51) — o caminho corporativo desta fase é honesto sobre isso: sem uma fonte real, `MEMBER_NOT_FOUND`/`MEMBERSHIP_NOT_FOUND` são resultados válidos e esperados, não bugs.
- Não existia conceito de "time principal", vigência aplicada em runtime, nem `workspaceId` em nenhum modelo — tudo introduzido nesta fase de forma aditiva (campo novo com valor padrão `null`, nenhum modelo existente quebrado).
- **Achado colateral (bug pré-existente, não introduzido por esta fase)**: `model/AppVersion.kt` estava desatualizado (`18`/`0.7.4`) em relação a `composeApp/build.gradle.kts` (`19`/`0.7.5`) desde o hotfix da spec 55 — corrigido durante o bump de versão desta fase (seção 12).

## 3. Modelos novos (`br.com.leorvergani.escalaici.identity`, commonMain)

| Arquivo | Conteúdo |
|---|---|
| `IdentitySource.kt` | `enum class IdentitySource { CORPORATE_MSAL, DEMO_PERSONA }` |
| `OrganizationWorkspace.kt` | Constantes `CORPORATE_WORKSPACE_ID = "ici"`, `DEMO_WORKSPACE_ID = "demo-v1"` |
| `IdentityNormalizer.kt` | `normalizeIdentity(raw: String?): String?` — trim + lowercase + vazio→null. Nenhuma correspondência aproximada em nenhum lugar do fluxo (proibido `contains`/`startsWith`/Levenshtein/nome parcial — testado explicitamente). |
| `OrganizationResolutionModels.kt` | `ResolvedTeamMembership`, `ResolvedOrganizationContext` (workspaceId, identitySource, memberId, memberDisplayName, normalizedLogin, normalizedEmail, primaryTeamId, primaryTeamName, roleDisplayName, activeMemberships) — nunca carrega token, claim bruto, tenant completo ou objeto MSAL. |
| `OrganizationResolutionResult.kt` | `sealed interface` com os 9 estados pedidos (`Resolved`, `MemberFoundNoActiveTeam`, `MemberNotFound`, `MemberInactive`, `MemberIdentityAmbiguous`, `MembershipNotFound`, `TeamNotFound`, `MultipleActiveTeams`, `WorkspaceMismatch`, `DataSourceUnavailable`) + `OrganizationResolutionStatus` (enum grosso para UI) + `toStatus()`. |
| `DemoPersona.kt` | Catálogo fixo de 3 personas 100% fictícias (`example.invalid`): `member-demo-soc-01` (Analista SOC Demo 1), `member-demo-seguranca-01` (Analista de Segurança Demo 1), `member-demo-gestor-seguranca` (Gestor de Segurança Demo). |
| `DemoOrganizationData.kt` | Fonte fixa em memória (workspaceId `demo-v1`): 3 `Member`, 2 `Team` (`team-demo-soc`, `team-demo-seguranca`), 4 `MemberTeamMembership`. **Decisão deliberada**: o Gestor Demo tem dois vínculos ativos, nenhum `isPrimary` — resolve para `MultipleActiveTeams` de propósito, testando esse caso (um responsável para várias equipes, spec 56 seção 7, ainda sem `team_manager_assignments` — a UI mostra "múltiplos vínculos" sem quebrar). |
| `OrganizationRepositories.kt` | `MemberDirectoryRepository`/`MembershipRepository` (interfaces novas) + implementações em memória (corporativa e Demo). Corporativa não acessa Firestore nesta fase (ver seção 2). |
| `OrganizationIdentityResolver.kt` | `resolveCorporateIdentity`/`resolveDemoPersona`, isolamento total entre repositórios corporativos e Demo (nunca a mesma instância), verificação defensiva de `workspaceId` em `Member`/`Team`/`MemberTeamMembership` encontrados. |
| `OrganizationIdentityCache.kt` | Cache puro em memória, chave `workspaceId:identidade`, guarda só `workspaceId`/`identitySource`/`memberId`/`primaryTeamId`/`dataUpdatedAt` — nunca o contexto inteiro. |

### 3.1 Campos aditivos nos modelos existentes

`workspaceId: String? = null` adicionado ao final de `Member`, `Team` (`ScheduleModels.kt`) e `MemberTeamMembership` (`UniversalOrgModels.kt`) — aditivo, valor padrão, nenhum call site existente quebrado (confirmado pelos 148 testes já existentes continuando a passar). Ausência (`null`) é lida como workspace `ici`, conforme convenção da spec 57.

### 3.2 Regras de resolução (`DefaultOrganizationIdentityResolver`)

1. Normaliza e-mail/UPN da identidade (corporativa) ou login/e-mail fictício (Demo).
2. Busca membros ativos por correspondência **exata** (nunca aproximada) — zero resultados → `MemberNotFound`; mais de um → `MemberIdentityAmbiguous` (nunca escolhe um dos dois em silêncio).
3. Membro inativo → `MemberInactive`.
4. Sem nenhum vínculo → `MembershipNotFound`; com vínculos mas nenhum ativo *agora* (`startDate <= hoje <= endDate`, usando `TodayProvider` existente, nunca uma nova abstração de relógio) → `MemberFoundNoActiveTeam` (member existe, não bloqueia a tela).
5. Exatamente um vínculo ativo → principal. Mais de um: exatamente um `isPrimary=true` → principal; caso contrário → `MultipleActiveTeams` (nunca escolhe o primeiro documento arbitrariamente).
6. Equipe da vinculação principal não encontrada → `TeamNotFound`.
7. `workspaceId` explícito e divergente em `Member`/`Team`/`MemberTeamMembership` → `WorkspaceMismatch`, em vez de prosseguir.
8. Sucesso → `Resolved(ResolvedOrganizationContext)`.

**Limitação conhecida, documentada**: `roleDisplayName` fica sempre `null` nesta fase — `MemberTeamMembership` só carrega `roleId`, e não existe ainda repositório/catálogo de `roles` conectado para resolver o rótulo de exibição. Resolver isso fica para uma fase futura (spec 57 seção 1, `roles` já existe como modelo de schema universal, ainda não lido pelo app).

## 4. Integração de UI (só as telas de login/perfil/identidade/demo, sem redesenhar navegação)

- `ui/App.kt`: novo parâmetro opcional `organizationIdentityResolver: OrganizationIdentityResolver? = null`. Dois `LaunchedEffect` independentes: um chaveado por `corporateAuthState` (dispara `resolveCorporateIdentity` só quando `Authenticated`; o próprio `LaunchedEffect` cancela a corrotina anterior quando o estado muda) e outro chaveado por `selectedDemoPersona` (dispara `resolveDemoPersona`). Nenhum dos dois fluxos toca `sessionMemberId`, `authRepository` (o seletor de "Login de teste" pré-existente) ou chama `signOut()`/`enterDemoMode()` — são estritamente ortogonais ao gate de navegação já existente.
- `ui/LoginGateScreen.kt` e `ui/ProfileTab.kt`: seletor "Testar como" (iterando `DemoPersonaCatalog.personas`, nunca hard-coded) e o bloco "AMBIENTE DE DEMONSTRAÇÃO / Personagem: X / Equipe: Y" no Perfil. O diálogo pré-existente "Login de teste" (`showDemoOptions`, 3 membros de escala mock) **não foi alterado** — mecanismo diferente (demonstração de dados de escala, não de identidade organizacional), continua existindo lado a lado, sempre visualmente distinguível pelo próprio rótulo. `ui/ProfileTab.kt`: o card "Identidade da escala" mostra, quando `Authenticated`, o resultado real da resolução corporativa (equipe/função quando `Resolved`; mensagem específica para cada um dos outros 8 estados, nunca genérica) em vez do texto estático anterior.
- `MainActivity.kt` (Android) **e** `Main.kt` (Web/Wasm) passam um `DefaultOrganizationIdentityResolver` real, com diretório/membership corporativos vazios (honesto — sem fonte real ainda, seção 2) e os repositórios Demo padrão (já embutidos na classe).

### 4.1 Duas correções feitas depois do teste manual e da revisão final (seção 12)

O desenho original prendia o seletor/bloco Demo ao branch `corporateAuthState == CorporateAuthState.Demo` — que só é alcançável quando o MSAL **não** está configurado (`enterDemoMode()` só é chamado nesse branch, um comportamento já existente desde a spec 53, não alterado por esta fase). Em qualquer dispositivo com MSAL configurado, isso deixava a seção Demo inatingível. Corrigido em duas partes:

1. `DemoPersonaSelector`/`DemoPersonaResolutionSection` passaram a renderizar sempre que há `selectedDemoPersona` (ou, no seletor, sempre visível), **independente** de `corporateAuthState` — workspace Demo é ortogonal à identidade corporativa (spec 56 seção 12), nunca deveria depender de um estado que só existe quando o MSAL está desconfigurado.
2. A revisão independente encontrou mais dois efeitos colaterais dessa mudança: (a) o `LaunchedEffect` ainda limpava `selectedDemoPersona` sempre que `corporateAuthState != Demo` — como isso agora é quase sempre verdadeiro, a persona escolhida podia sumir sozinha assim que a restauração silenciosa terminasse depois da seleção (corrigido: removida essa limpeza automática, a persona só muda por ação explícita do usuário); (b) o seletor, agora sempre visível, ficava sem `organizationIdentityResolver` no alvo Web (`Main.kt` não passava nenhum), então uma seleção lá nunca resolvia (corrigido: `Main.kt` passou a receber o mesmo resolver honesto/vazio do Android).

## 5. Mensagens ao usuário (nunca uma mensagem genérica cobrindo mais de um estado)

| Estado | Mensagem |
|---|---|
| `Resolved` | "Equipe: {nome}" + "Função: {cargo}" (se houver) |
| `MemberFoundNoActiveTeam` | "Seu cadastro foi encontrado, mas ainda não possui uma equipe ativa vinculada." |
| `MemberNotFound` | "Conta corporativa autenticada, mas seu cadastro ainda não foi localizado na organização." |
| `MemberInactive` | "Seu cadastro na organização está inativo no momento. Contate o administrador." |
| `MemberIdentityAmbiguous` | "Foram encontrados cadastros duplicados para esta identidade. O responsável pelo cadastro precisa revisar os dados." |
| `MembershipNotFound` | "Seu cadastro foi encontrado, mas ainda não possui nenhum vínculo de equipe registrado." |
| `TeamNotFound` | "Seu vínculo de equipe foi encontrado, mas a equipe correspondente não está mais disponível. Contate o administrador." |
| `MultipleActiveTeams` | "Foram encontrados múltiplos vínculos de equipe ativos. A seleção de equipe estará disponível em uma próxima fase." |
| `WorkspaceMismatch`/`DataSourceUnavailable` | "Não foi possível confirmar seu vínculo organizacional no momento. Tente novamente mais tarde." |

Nenhum desses estados desloga a conta, apaga a sessão ou redireciona automaticamente para o Demo — o usuário sempre continua podendo navegar o app normalmente.

## 6. Cache

`OrganizationIdentityCacheStorage` (interface) + `InMemoryOrganizationIdentityCacheStorage` (implementação de teste), chave `workspaceId:identidade`, guardando apenas `workspaceId`/`identitySource`/`memberId`/`primaryTeamId`/`dataUpdatedAt`. **Não integrado à UI nesta fase** (só a estrutura pura, testada) — a persistência real (Android/Web) e a leitura no `App.kt` ficam para a fase que também conectar Firestore de verdade, para não duplicar trabalho de invalidação antes de haver dado real para invalidar.

## 7. Isolamento de workspace (testado)

- `resolveCorporateIdentity` nunca consulta os repositórios Demo, nem vice-versa (testado com repositórios que contam chamadas — zero chamadas cruzadas em todos os testes).
- `MemberNotFound` no corporativo nunca cai em fallback para Demo.
- Um `Member`/`Team`/`MemberTeamMembership` com `workspaceId` explícito divergente do esperado interrompe a resolução (`WorkspaceMismatch`) em vez de prosseguir silenciosamente.
- IDs Demo (`member-demo-*`, `team-demo-*`) nunca colidem com nenhum ID real usado em produção.

## 8. Testes (commonTest, `br.com.leorvergani.escalaici.identity`)

29 testes novos, cobrindo: normalização (maiúscula/espaços/vazio/sem correspondência parcial), resolução de membro (e-mail, login, ausente, inativo, ambíguo, workspace incorreto, sem fallback para Demo), membership (ativa/inativa/futura/expirada/ausente/múltiplas com e sem principal), time (encontrado/ausente/workspace divergente — inatividade de `Team` documentada como não testável nesta fase por ausência do campo `active` no modelo), as 3 personas Demo + isolamento `ici`/`demo-v1`, cache (salvar/carregar/limpar por chave, chave de outro workspace não interfere), e o mapeamento `toStatus()`.

**Correção pós-revisão independente**: `InMemoryMemberDirectoryRepository.findActiveMemberIds()` filtrava `active` no próprio diretório, antes do resolver conseguir classificar — isso tornava `MemberInactive` inalcançável na implementação de produção (um membro inativo batendo exatamente na identidade caía em `MemberNotFound`). Corrigido removendo o filtro do diretório (a checagem de `active` já existia no resolver, seção 3.2, e passou a ser a única). O teste `corporateIdentity_inactiveMemberReturnsMemberInactive` foi ajustado para usar o diretório real (não um stub) e comprova o comportamento correto.

## 9. Firestore

Nenhuma escrita. Nenhuma coleção nova criada em produção. Nenhuma regra alterada. O caminho corporativo desta fase é 100% local (repositórios em memória) — conectar a um Firestore real (`member_team_memberships`) fica para uma fase futura, fora do escopo de FASE 14c-1.

## 10. Segurança

Nenhum token, claim bruto, `tenantId` completo ou objeto MSAL persistido em `ResolvedOrganizationContext` ou no cache. Nenhum dado real usado nos personagens Demo (domínio `example.invalid`). `MainActivity.kt` não introduz nenhuma credencial nova.

## 11. Validação automatizada

```text
./gradlew :composeApp:clean :composeApp:testDebugUnitTest :composeApp:assembleDebug \
  :composeApp:assembleRelease :composeApp:wasmJsBrowserDistribution :composeApp:wasmJsTest \
  --rerun-tasks --stacktrace
```

`BUILD SUCCESSFUL`. **148 testes Android (JVM), 0 falhas**; **144 testes
Wasm/Chromium, 0 falhas** (`wasmJsTest`, viável nesta sessão após o usuário
instalar o Chromium via Flatpak — `CHROME_BIN=/var/lib/flatpak/exports/bin/org.chromium.Chromium`,
fechando o gap registrado na spec 55). `assembleDebug`/`assembleRelease`
`BUILD SUCCESSFUL`; release verificado com `apksigner verify --print-certs`
— mesma keystore/identidade da spec 52 (`CN=Escala ICI KMP Lab, OU=ICI,
O=ICI, C=BR`).

## 12. Validação manual no emulador — executada com sucesso

Quatro tentativas headless (`-no-window`) iniciais de subir `EscalaSOC_API_37`
terminaram todas no mesmo crash reprodutível do `qemu-system-x86`
(segmentation fault, confirmado por `systemd-coredump`, sempre no mesmo
ponto de inicialização). A causa raiz era o backend gráfico
(`gfxstream`/swiftshader) neste host — **não** a ausência de janela. Rodando
sem `-no-window` (aproveitando o `DISPLAY`/Wayland real da sessão) e com
`-gpu host` (GPU AMD real do host, em vez de renderização por software), o
emulador subiu normalmente (`Boot completed in 20337 ms`).

Validado no dispositivo real (`emulator-5554`, Android 17), com uma sessão
MSAL corporativa já restaurada de uma fase anterior
(`lvergani@ici.tec.br`/Leonardo Rodrigo Vergani):

- **Fluxo corporativo**: card "Identidade da escala" mostrou corretamente
  "Conta corporativa autenticada, mas seu cadastro ainda não foi localizado
  na organização." — resultado `MemberNotFound` esperado e válido (diretório
  corporativo honesto/vazio nesta fase, seção 2). Force-stop + relançamento
  do processo confirmou restauração silenciosa (conta corporativa reaparece
  sem novo prompt de login), sem crash, sem logout, sem fallback para Demo.
  `signOut()` não foi exercitado propositalmente nesta sessão — não há como
  refazer o login MSAL interativo real (credenciais/consentimento) sem um
  humano presente, e destruir a única sessão corporativa restaurada seria
  irreversível para testes futuros.
- **Fluxo Demo**: os 3 personagens resolvidos corretamente — "Analista SOC
  Demo 1" → equipe "SOC Demonstração"; "Analista de Segurança Demo 1" →
  equipe "Segurança da Informação Demonstração"; "Gestor de Segurança Demo"
  → "Múltiplos vínculos de equipe (ver Dashboard)" (o `MultipleActiveTeams`
  desenhado de propósito, seção 3). Troca de personagem não afetou a sessão
  corporativa (nome/login corporativos permaneceram visíveis o tempo todo).
  Diálogo pré-existente "Login de teste" (3 membros mock de escala)
  confirmado intacto e funcional, coexistindo sem se confundir com o novo
  seletor.

**Dois bugs reais de alcançabilidade encontrados e corrigidos por este teste
manual** (não capturados pelos 148 testes automatizados, que testam a lógica
isoladamente da árvore de UI):

1. O seletor "Testar como" (`LoginGateScreen`) só era renderizado dentro do
   branch `corporateAuthState == CorporateAuthState.Demo`, que só é
   alcançável quando o MSAL **não** está configurado (`enterDemoMode()` só é
   chamado nesse branch) — em qualquer dispositivo com MSAL configurado
   (como este), o seletor era inatingível. Corrigido: `DemoPersonaSelector`
   passou a ser renderizado sempre, perto do botão pré-existente "Login de
   teste (demonstração)", independente do estado/configuração do MSAL —
   workspace Demo é um conceito ortogonal à identidade corporativa (spec 56
   seção 12).
2. O mesmo problema existia em `ProfileTab` (`DemoPersonaResolutionSection`
   só aparecia dentro do mesmo branch `Demo`) — corrigido da mesma forma,
   passou a aparecer sempre que há um `selectedDemoPersona`, junto com (não
   em vez de) a resolução corporativa.

Screenshots capturados só em `/tmp` (não versionados); nenhum nome, login ou
e-mail real foi incluído nesta spec além do necessário para descrever o
resultado do teste (o login `lvergani@ici.tec.br` já é dado pré-existente e
amplamente usado como membro de demonstração em specs anteriores deste
repositório, não uma credencial).

`wasmJsTest` também passou a funcionar nesta sessão, após o usuário instalar
o Chromium (Flatpak, `org.chromium.Chromium`) — apontando
`CHROME_BIN=/var/lib/flatpak/exports/bin/org.chromium.Chromium`, o Karma
encontrou o binário headless normalmente. Isso fecha o gap de infraestrutura
registrado na spec 55 (ausência de Chrome/Chromium).

## 13. Versionamento

`versionCode`/`versionName` e o `version.json` local (`kmp*`) atualizados após toda a bateria de testes/build passar — detalhes no relatório final. Corrigido também o desalinhamento pré-existente de `AppVersion.kt` (seção 2).

## 14. Dependência futura

Esta fase não implementa fixtures oficiais do workspace Demo (isso é a FASE 14c-2, spec 58) — `DemoOrganizationData.kt` é deliberadamente um provedor mínimo e isolado, pronto para ser substituído sem reescrever o resolver (`OrganizationIdentityResolver` não muda; só a fonte de dados injetada muda).

## 15. Critérios de aceite

1. Identidade MSAL corporativa resolve `member`/`team` reais quando existir vínculo, ou retorna um dos 9 estados tipados sem crashar.
2. Personagem Demo resolve dentro do workspace `demo-v1`, sempre isolado do workspace `ici`, nunca por fallback automático.
3. Correspondência de identidade é sempre exata (e-mail/UPN/login), nunca aproximada.
4. Ausência de vínculo/equipe nunca bloqueia o acesso às telas já permitidas nem desloga a conta.
5. Nenhum dado real aparece no catálogo Demo; nenhum segredo/token é persistido no contexto resolvido ou no cache.
6. Fluxo MSAL Android (login, logout, novo login, restauração silenciosa) continua funcionando sem nenhuma alteração de comportamento.
7. Testes automatizados, build debug/release e distribuição Web/Wasm passam; validação manual no emulador registrada.
