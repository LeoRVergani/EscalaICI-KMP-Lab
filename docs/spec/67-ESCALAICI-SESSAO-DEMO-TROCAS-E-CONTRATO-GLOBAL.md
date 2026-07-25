# FASE 14J — Sessão persistente, acesso Demo autorizado, estado global de trocas e contrato unificado

**Status:** aprovado para implementação (Checkpoint A)
**Escopo:** `EscalaICI-KMP-Lab` (Android + Web/Wasm), `commonMain` + adaptadores de plataforma
**Base:** `feature/fase-14i-kmp-global-state-ui-consistency` @ `versionCode 29`/`versionName 0.7.15`
**Leitura prévia completa:** specs 64, 65, 66; `docs/ROADMAP.md`; `App.kt`, `LoginGateScreen.kt`,
`MsalCorporateAuthRepository.kt` (Android), `WasmMsalCorporateAuthRepository.kt` +
`msal-browser-interop.js` (Web), `OrganizationIdentityResolver.kt`/`OrganizationRepositories.kt`,
`DemoAuthorization.kt`, `DemoPublicationDtos.kt`/`DemoPublicationResolver.kt`, `TodayTab.kt`,
`ProfileTab.kt`, `ScheduleTab.kt`, `ShiftSwapScreen.kt`, `AndroidLocalDataCache.kt`/
`WebLocalDataCache.kt`/`FirebaseCache.kt`, `AndroidNotifications.kt`, `AppVersion.kt`/
`build.gradle.kts`; contrato Dashboard (`contracts/organization-approval-v1.schema.json`,
`src/lib/demoWorkspace/dto.ts`, `server/domain/*PublicationPlanner.mjs`,
`src/components/DemoChangeRequestsDialog.tsx`, `docs/spec/FASE-14C4-...md`,
`docs/spec/FASE-14D-...md`); fluxo de referência em `EscalaSOC`
(`feature/troca-escala-microsoft-firebase`, somente leitura).

## 0. Checkpoint 0 — o que a pesquisa confirmou (fatos, não hipóteses)

Toda a investigação abaixo foi feita por leitura direta de código (própria e via agentes de
pesquisa cujos achados eu revisei e, nos pontos críticos, reconferi pessoalmente lendo o arquivo).

### 0.1 Causa raiz do "retorno recorrente ao login"

`App.kt:165-170` já chama `corporateAuthRepository.restoreSession()` num `LaunchedEffect(Unit)` —
a restauração silenciosa **já funciona** (o MSAL cache local, no Android via
`getCurrentAccountAsync`, no Web via `localStorage` do MSAL.js, já é consultado e
`CorporateAuthState` já vira `Authenticated` corretamente). O bug nunca foi "a sessão não
restaura" — é que **nada reage a isso automaticamente**: `sessionMemberId` só é preenchido dentro
do `LaunchedEffect(corporateAuthState, requestedEntryContext)` (`App.kt:197+`), e esse efeito só
faz algo além de `Unit` quando `requestedEntryContext != null` — que só é setado por
`onLogin`/`onDemo` de `LoginGateScreen`, ou seja, **por toque explícito do usuário**. Resultado:
mesmo com `corporateAuthState == Authenticated` logo na abertura do app, a `LoginGateScreen`
aparece (linha 476, `if (sessionMemberId == null ...)`) e fica esperando um toque — daí o "flash"
e o "preciso logar de novo toda vez".

Segundo bug, específico do Web: `WasmMsalCorporateAuthRepository.restoreSession()` (linhas
26-38) só marca `Authenticated` se `acquireTokenSilent` (chamada de rede real, ao token endpoint)
tiver sucesso; qualquer falha aí — **incluindo falha de rede pura, com conta em cache válida** —
cai em `mutableState.value = identity?.let(...) ?: CorporateAuthState.SignedOut`, ou seja, **trata
indisponibilidade momentânea de rede como logout**, exatamente o comportamento proibido pelo
prompt desta fase. `msalWebGetActiveIdentityJson()` (leitura pura local, `getAllAccounts()[0]`,
sem rede) já devolve o mesmo formato JSON que `parseIdentityJson()` espera (confirmado lendo
`accountToIdentityJson()` em `msal-browser-interop.js:120-122` — é a mesma função usada por
`acquireTokenSilent`/`loginPopup`), então dá para restaurar `Authenticated` a partir só da conta
em cache, sem depender do sucesso de `acquireTokenSilent`.

`onLogout` (`App.kt:619-626`) hoje só chama `authRepository.signOut()` — o **mock**
`InMemoryAuthSessionRepository`, nunca `corporateAuthRepository.signOut()` (MSAL real). O botão
correspondente já se chama "Sair (login de teste)" (`ProfileTab.kt:178-180`), confirmando que é
um resquício de teste, não logout de produção.

### 0.2 Cache local — hoje global, não particionado por usuário

`AndroidLocalDataCache`/`WebLocalDataCache` (chaves fixas `escalaici.web.schedule.cache.v1`/
`...oncall.cache.v1`) e `FirebaseSourceCache` (`escalaici.firebase.schedule.cache.v1`/
`...oncall.cache.v1`, esta última só particionada por `groupId` de plantão, nunca por usuário) e
o agendador de alarmes (`AndroidNotificationScheduler`, `SharedPreferences
escalaici.notification.scheduler`) usam **slot único global**. Nenhuma dessas caches é limpa em
nenhum fluxo de logout hoje (porque não existe fluxo de logout real — ver 0.1). Existe um
`identity/OrganizationIdentityCache.kt` já desenhado para ser particionado por
`workspaceId:email/login/memberId`, mas **sem implementação Android/Web e sem uso em produção**
(só `InMemoryOrganizationIdentityCacheStorage`, usada em teste).

### 0.3 Demo — autorização já é por claim de backend, não e-mail (maioritariamente)

`isDemoAuthorizedForIdentity()` (`identity/DemoAuthorization.kt`) já resolve por
`allowedDeveloperObjectIds` do documento Firestore `workspaces/demo-v1` (claim real de backend,
recarregada a cada chamada — não há cache do booleano). Existe **um** fallback adicional,
explicitamente marcado `// Temporario: remover assim que...`: e-mail hardcoded
`lvergani@ici.tec.br`, mas só ativo quando `isDevelopmentBuild == true` (== `BuildConfig.DEBUG` no
Android; sempre `false` no Web) — ou seja, **nunca ativo em build release**, já teve 4 testes
cobrindo isso. Não vou remover esse fallback nesta fase (não foi pedido, é debug-only, já
documentado); o problema real não é "autorização por e-mail" — é que a **UI mostra o botão Demo
antes mesmo de autenticar**, sem checar autorização nenhuma (`LoginGateScreen.kt:114-120`, sempre
renderizado lado a lado com "MINHA ESCALA"). A checagem de autorização só acontece depois do
clique, dentro do `LaunchedEffect` (`App.kt:240`) — isso já protege contra bypass de navegação
direta (mesmo forçando `requestedEntryContext = DEMO` no estado, a checagem roda de novo), só
falta esconder o botão na UI antes da autenticação.

### 0.4 `scheduleChangeRequests` — já parcialmente lido, descartado antes da UI

O Dashboard escreve em `workspaces/{workspaceId}/revisions/{revision}/schedule_change_requests`
(snake_case; mesmo padrão de todas as outras coleções versionadas por revisão). Campos reais
(`contracts/organization-approval-v1.schema.json`, confirmado por leitura de agente + já bate com
o parser KMP): `id`, `workspaceId`, `requesterMemberId`, `requesterTeamId`,
`assignedManagerMemberId` (fotografia imutável do responsável), `schedulePeriodId`,
`assignmentId` (opcional), `requestType` (`SHIFT_CHANGE|DAY_OFF_CHANGE|SWAP_WITH_MEMBER|
SCHEDULE_CORRECTION|OTHER`), `status` (`DRAFT|PENDING|APPROVED|REJECTED|CANCELLED|EXPIRED`),
`reason`, `createdAt`/`resolvedAt` (string ISO, não Firestore Timestamp), `resolvedByMemberId`,
`resolutionNote` (obrigatório e não-vazio quando `status == REJECTED`), `schemaVersion`,
`publicationRevision` (atribuído pelo servidor, não vem do pacote do cliente).

**Não existe** campo de "turno pretendido"/"data pretendida" separado no contrato real — o
vínculo é só via `assignmentId` (aponta para UM `ScheduleAssignment` já existente) + `reason`
(texto livre). O card na tela Hoje vai mostrar só o que o contrato realmente tem — nada de
inventar "turno pretendido" que não existe (conferido: o prompt desta fase já antecipa isso —
"turno atual e turno pretendido **quando disponíveis**").

**Já correto no KMP** (verificado por mim, linha a linha): `toDemoChangeRequest()`
(`DemoPublicationDtos.kt:154-170`) já lê `requesterMemberId`/`requesterTeamId`/`schedulePeriodId`
(nomes reais do Firestore) com fallback para `memberId`/`teamId`/`periodId` (nomes curtos do
fixture local antigo), mapeando para os campos internos do modelo `ScheduleChangeRequest`
(`memberId`/`teamId`/`periodId` — **mesmo dado, nome interno mais curto**; não é um bug de
contrato, é um alias já correto na fronteira de parsing. Documentado aqui para não ser
"descoberto" de novo numa fase futura).

**Bug real, confirmado por mim lendo `OrganizationRepositories.kt:341-360`**: `DemoPublicationData`
(a classe que a UI de fato consome) **não tem** campo `scheduleChangeRequests` nem
`teamManagerAssignments` — `DemoPublicationSnapshot.toData()` e `DemoFixturePackage.toData()`
descartam os dois antes de chegar à UI, mesmo já os tendo parseado e validado. É só plumbing
incompleto, mesma categoria de bug do `TodayTab` sem `NotificationSettings` na FASE 14I — não é
preciso nova arquitetura, só terminar de conectar o que já existe.

`assignedManagerMemberId` é suficiente, por si só, para saber "esta solicitação está aguardando
a ação deste usuário como responsável" — não é preciso resolver o catálogo de papéis
(`TeamManagerRole`/`roleId`, hoje só modelo de dados sem repositório conectado, ver
`OrganizationIdentityResolver.kt:208-212`) para isso. Resolver esse catálogo genérico fica fora do
escopo desta fase (não é bloqueador para o card/tela de trocas).

### 0.5 `ShiftSwapScreen`/`ShiftSwapRequest`/`SwapStatus` — mock peer-to-peer, será substituído

Existe hoje uma tela completa (`ShiftSwapScreen.kt`) alcançável em produção
(`ProfileTab` → `onOpenSwap` → `StackedScreen.SWAP`), mas 100% sobre `MockShiftSwapRepository`
(lista em memória, nunca Firestore) e um modelo (`ShiftSwapRequest`/`SwapStatus`) conceitualmente
diferente do contrato real (tem `targetMemberId`/turno-alvo — um pedido peer-to-peer de troca
direta —, que **não existe** no contrato real do Dashboard, que é sempre requerente→aprovador).
Decisão desta spec: a tela real de trocas (Checkpoint C) substitui o conteúdo de
`StackedScreen.SWAP` por uma versão que lê `ScheduleChangeRequest` real; o modelo/repositório mock
(`ShiftSwapRequest`, `SwapStatus`, `ShiftSwapRepository`, `MockShiftSwapRepository`,
`mockShiftSwapRequests()`) e seu teste dedicado (`MockRepositoriesTest.shiftSwapRepository_...`)
são removidos como consequência direta — não sobra código morto competindo com o fluxo real, e o
rótulo do card/rota ("Trocas de escala") não muda.

### 0.6 Versão — `AppVersion.kt` mantido à mão, sem plugin de codegen no projeto

`buildFeatures.buildConfig = true` já ativo, mas só gera `BuildConfig` acessível de `androidMain`
(padrão AGP), nunca de `commonMain`. Não há plugin de codegen KMP (tipo `buildkonfig`) no
projeto. Já existe, porém, o padrão exato necessário: duas tasks Gradle customizadas
(`generateWasmFirebaseConfig`/`generateWasmMsalWebConfig`, `build.gradle.kts:223-283`) que geram
um arquivo `.kt` em `build/generated/...` a partir de valores do Gradle, registram esse diretório
como source set extra, e usam `dependsOn` explícito nas tasks de compilação — só que hoje só para
`wasmJsMain`. Decisão: replicar o mesmo padrão, mas gerando para `commonMain` (herdado
automaticamente por `androidMain` e `wasmJsMain` via KMP, sem precisar `expect`/`actual`, já que
é um valor puro sem lógica por-plataforma) — ver Checkpoint F.

## 1. Decisão arquitetural — continuidade da FASE 14I, não big-bang

A spec 66 já decidiu não introduzir um `EscalaIciAppState`/`StateFlow` novo, porque `App.kt` já é,
na prática, a única fonte de verdade (estado único em `remember`/`mutableStateOf`, consumido por
todos os tabs). Nada nesta fase muda essa decisão — os problemas reais (sessão não navega
automaticamente, cache global vaza entre contas, Demo visível antes da hora, `scheduleChangeRequests`
descartado antes da UI) são, de novo, **plumbing incompleto e dado descartado cedo demais**, não
"estado espalhado". A única adição estrutural nova é um enum de fase de bootstrap
(`SessionBootstrapPhase`) e um campo de lista (`scheduleChangeRequests`) no `DemoPublicationData`
já existente — nenhuma classe de estado global nova, nenhum `StateFlow` novo (mantém o padrão
100% suspend-function + `mutableStateOf` já usado por todo o resto do arquivo).

## 2. Checkpoint B — sessão sem flash, sem logout por rede, com logout real

### 2.1 `SessionBootstrapPhase` (novo, `commonMain`, arquivo `ui/SessionBootstrap.kt` ou dentro de `App.kt`)

```kotlin
private enum class SessionBootstrapPhase { INITIALIZING, RESTORING, READY }
```

Computado, não um `var` solto sujeito a corrida entre os dois `LaunchedEffect`s existentes:

```kotlin
val sessionBootstrapPhase = when {
    corporateAuthRepository == null ||
        corporateAuthRepository.configurationState != CorporateAuthConfigurationState.CONFIGURED -> SessionBootstrapPhase.READY
    corporateAuthState == null -> SessionBootstrapPhase.INITIALIZING
    corporateAuthState == CorporateAuthState.SignedOut ||
        corporateAuthState is CorporateAuthState.Failed ||
        corporateAuthState == CorporateAuthState.Demo -> SessionBootstrapPhase.READY
    corporateAuthState is CorporateAuthState.Authenticated ->
        if (sessionMemberId != null || gateErrorMessage != null || requestedEntryContext == EntryContext.DEMO) {
            SessionBootstrapPhase.READY
        } else {
            SessionBootstrapPhase.RESTORING
        }
    else -> SessionBootstrapPhase.READY // Authenticating (login manual) - tela de login já trata via isAuthenticating
}
```
Nota: `requestedEntryContext == EntryContext.DEMO` entra na condição de "pronto" porque, nesse
ramo, quem decide exibir algo é o fluxo Demo (overview/persona), não `sessionMemberId` — sem essa
cláusula, tocar em "Ambiente Demo" ficaria preso em `RESTORING` para sempre.

Novo gate de render (substitui a condição simples de `App.kt:476`):
```kotlin
when {
    sessionBootstrapPhase != SessionBootstrapPhase.READY -> SessionBootstrapScreen()
    sessionMemberId == null && activeDemoWorkspaceSession == null -> LoginGateScreen(...)
    else -> <app autenticado, como hoje>
}
```
`SessionBootstrapScreen` — tela nova, mínima (spinner + "Escala ICI" + texto curto tipo
"Verificando sessão..."), sem nenhum botão, inspirada no `AuthLoadingScreen` do EscalaSOC
(`ui/auth/AuthGate.kt`, referência de comportamento, não de código — não copiar UI, só o
conceito de um terceiro estado explícito antes de decidir logado/deslogado).

### 2.2 Disparo automático da restauração (elimina o toque manual)

Novo `LaunchedEffect(corporateAuthState)`:
```kotlin
LaunchedEffect(corporateAuthState) {
    if (corporateAuthState is CorporateAuthState.Authenticated && requestedEntryContext == null) {
        requestedEntryContext = EntryContext.LOGIN
    }
}
```
Não mexe no fluxo de toque manual (que já seta `requestedEntryContext` antes de `signInInteractive`
terminar) — só cobre o caso em que `Authenticated` aparece **sem** toque (restauração silenciosa).
O `LaunchedEffect(Unit)` original (`App.kt:165-170`) permanece, só chamando `restoreSession()` —
a decisão de navegar é sempre feita pelo `LaunchedEffect(corporateAuthState,
requestedEntryContext)` já existente (`App.kt:197+`), sem duplicar a lógica de resolução de
identidade em dois lugares.

### 2.3 Corrigir `WasmMsalCorporateAuthRepository.restoreSession()` — não tratar falha de rede como logout

```kotlin
override suspend fun restoreSession() {
    if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) return

    msalWebInit(config)
    val cachedIdentity = runCatching { parseIdentityJson(msalWebGetActiveIdentityJson()) }.getOrNull()
    if (cachedIdentity == null) {
        mutableState.value = CorporateAuthState.SignedOut
        return
    }
    mutableState.value = CorporateAuthState.Authenticated(cachedIdentity)

    // Best-effort: renova o token silenciosamente, mas falha de rede/desconhecida NUNCA
    // derruba a sessão já restaurada acima (estado offline honesto, não logout). Só uma
    // falha que prove que a sessão de fato não é mais válida (conta removida/precisa de
    // interação) derruba para SignedOut.
    runCatching { parseIdentityJson(msalWebAcquireTokenSilent(config.scopesCsv())) }
        .onSuccess { refreshed -> if (refreshed != null) mutableState.value = CorporateAuthState.Authenticated(refreshed) }
        .onFailure { rejection ->
            val error = (rejection as? MsalWebRejection)?.toCorporateAuthError()
            if (error == CorporateAuthError.AccountNotFound || error == CorporateAuthError.InteractionRequired) {
                mutableState.value = CorporateAuthState.SignedOut
            }
            // NetworkError/Unknown: mantém o Authenticated já setado.
        }
}
```
Teste novo (`WasmMsalCorporateAuthRepositoryTest` ou onde já existir teste desse repositório):
conta em cache + `acquireTokenSilent` rejeitando com erro classificado como rede →
`state.value` permanece `Authenticated`, nunca `SignedOut`. Mesma conta + rejeição
`interaction_required` → `SignedOut` (sessão de fato inválida).

O lado Android (`MsalCorporateAuthRepository.restoreSession()`) já não depende de rede
(`getCurrentAccountAsync` é leitura local do broker) — nenhuma mudança necessária lá, só
confirmar com teste que existe (ou criar) cobrindo "conta em cache → Authenticated diretamente".

### 2.4 Logout real

`onLogout` em `App.kt` passa a:
1. Chamar `corporateAuthRepository?.signOut()` (MSAL real — Android e Web) além do
   `authRepository.signOut()` mock (mantém compatibilidade com o que já existe).
2. Zerar todo o estado de sessão em memória: `sessionMemberId`, `organizationResolutionResult`,
   `corporateDataSourceState`, `demoWorkspaceSession`, `selectedDemoPersona`,
   `demoPersonaResolutionResult`, `requestedEntryContext`, `gateErrorMessage`, `summary` (volta
   para `mockScheduleSummary()` ou estado vazio equivalente), `firebaseOnCall`/
   `firebaseOnCallGroups`, `scheduleChangeRequests` (novo, ver Checkpoint C).
3. Chamar uma função nova `clearAllUserScopedCaches()` (nome sugerido, `commonMain`, chamando os
   `clear`/`clearSchedule`/`clearOnCall` já existentes de `LocalDataCache`/`FirebaseSourceCache`
   injetados em `EscalaIciLabApp`) — como essas caches são slot único global (seção 0.2), **limpar
   tudo no logout** é a correção proporcional (não é preciso reescrever para multi-slot
   particionado por usuário; um slot só nunca fica com dado de duas contas ao mesmo tempo se for
   sempre limpo na troca).
4. Cancelar os alarmes agendados (Android): `AndroidNotificationScheduler.reconcile(emptyList())`
   (ou método equivalente que já exista para desagendar tudo) — chamado a partir do adaptador
   Android (`MainActivity`), já que `App.kt` (`commonMain`) não conhece `AlarmManager`
   diretamente; seguir o padrão `expect`/callback já usado para `localNotificationRuntime`.
5. Rótulo do botão passa de "Sair (login de teste)" para "Sair" (deixa de ser rotulado como
   remanescente de teste, já que agora desconecta de verdade).

### 2.5 Sessão Firebase — não existe hoje, nada a restaurar

Confirmado por busca: não há `FirebaseAuth` real implementado em lugar nenhum do projeto
(`FirebaseAuthTokenProvider` é só uma interface, sempre `null` em produção — `FirestoreRestGateway`
lê Firestore sem token). "Restaurar sessão Firebase" não se aplica a este código hoje; o prompt
falou em "MSAL/Firebase" genericamente, mas aqui a sessão que existe (e precisa ser restaurada) é
só a MSAL. Isto é documentado explicitamente para não ser tratado como pendência perdida — é
inexistente, não esquecido. `AUTH_REQUIRED`/`IDENTITY_NOT_LINKED` em `ScheduleSyncCause` já
prevêem esse futuro, sem implementação ainda (fora de escopo, spec anterior já registrava isso).

## 3. Checkpoint C1 — tela de login com um único botão

`LoginGateScreen.kt`, ramo `else` (linhas 101-125): remove o segundo `EntryButton`("AMBIENTE
DEMO") incondicionalmente. Renomeia o botão principal de "MINHA ESCALA" para "Entrar com a conta
corporativa". Mantém: estado de carregamento (`isAuthenticating`/spinner já existentes), mensagem
de erro (`CorporateAuthState.Failed`/`errorMessage` já existentes), texto de sessão já autenticada
("Conta corporativa autenticada"/nome/login) quando aplicável — mas na prática, com o Checkpoint B
implementado, essa tela só aparece quando `sessionBootstrapPhase == READY` **e**
`sessionMemberId == null`, ou seja: sem sessão nenhuma, ou sessão restaurada mas identidade não
resolvida (`gateErrorMessage` presente) — nunca mais como um passo manual obrigatório para quem já
tem sessão válida.

Acessibilidade/UX exigidas pelo prompt, aplicadas sem redesenhar a tela: `heightIn(min = 48.dp)`
já existe no botão (alvo de toque adequado); adicionar `contentDescription`/texto de estado ao
spinner para leitores de tela; manter contraste já definido em `LabColors`; garantir que o botão
único tenha foco inicial no Web (`Modifier.focusRequester`/similar, checar se já existe algo
equivalente no projeto antes de adicionar mecanismo novo).

## 4. Checkpoint C2 — Ambiente Demo como ação secundária pós-autenticação

Novo estado derivado (não persistido, recalculado): `demoAccessGranted: Boolean`, resultado de
`isDemoAuthorized(state.identity)` chamado a partir do momento em que `corporateAuthState is
Authenticated` — reaproveita o `isDemoAuthorized` já passado para `EscalaIciLabApp` (mesmo
parâmetro usado hoje só depois do clique em "AMBIENTE DEMO"). Recalculado (não cacheado
indefinidamente) a cada nova sessão autenticada, e antes de qualquer entrada real no Demo (a
checagem que já existe em `App.kt:240` continua sendo a autoridade final — este novo estado é só
para decidir **visibilidade** da ação na UI, nunca para decidir acesso sozinho).

UI: novo pequeno card/ação em `ProfileTab.kt`, logo após o card "Identidade da escala" (linha
~182), visível só quando `demoAccessGranted == true`: título "Acesso administrativo" (ou
reaproveitar ícone/estilo de `LabCard`), texto curto, botão "Ambiente Demo" chamando o mesmo
`onDemo` que hoje vem de `LoginGateScreen` (troca de dono do callback, mesma lógica). Removido de
`LoginGateScreen`. Usuários sem `demoAccessGranted` não veem nenhum card, nenhum espaço reservado,
nenhum menu vazio — o item some inteiro (não fica desabilitado visível).

## 5. Checkpoint D — `scheduleChangeRequests` no domínio e seletores

### 5.1 Modelo tipado

`model/DomainModels.kt`: `ScheduleChangeRequest.status`/`.requestType` continuam `String` (fonte
preservada, mesmo espírito de `sourceStatus` — nunca perder um valor desconhecido/futuro). Dois
enums novos com fallback seguro (nunca lançam):
```kotlin
enum class ChangeRequestStatus { DRAFT, PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED, UNKNOWN }
enum class ChangeRequestType { SHIFT_CHANGE, DAY_OFF_CHANGE, SWAP_WITH_MEMBER, SCHEDULE_CORRECTION, OTHER, UNKNOWN }
fun ChangeRequestStatus.Companion.parse(raw: String): ChangeRequestStatus
fun ChangeRequestType.Companion.parse(raw: String): ChangeRequestType
```
Propriedades de extensão em `ScheduleChangeRequest`: `val statusTyped get() = ChangeRequestStatus.parse(status)`,
`val requestTypeTyped get() = ChangeRequestType.parse(requestType)`. Labels em pt-BR **idênticos**
aos do Dashboard (`DemoChangeRequestsDialog.tsx`, seção 0.4) — mesmo contrato semântico visível
nos dois lados: "Rascunho"/"Pendente"/"Aprovada"/"Recusada"/"Cancelada"/"Expirada",
"Troca de turno"/"Troca de folga"/"Troca com colega"/"Correção de escala"/"Outro".

### 5.2 Plumbing — `DemoPublicationData` ganha o campo que já existe rio acima

`OrganizationRepositories.kt`:
```kotlin
data class DemoPublicationData(
    ...,
    val scheduleChangeRequests: List<ScheduleChangeRequest> = emptyList()
)
```
`DemoPublicationSnapshot.toData()` (linha 341): adiciona `scheduleChangeRequests =
scheduleChangeRequests` (já existe no snapshot, só não era repassado). `DemoFixturePackage.toData()`
(linha 362): nova função `DemoFixturePackage.toScheduleChangeRequests(): List<ScheduleChangeRequest>`
(mesmo padrão de `toSchedulePeriods()`/`toScheduleAssignments()` já existentes), convertendo
`DemoFixtureScheduleChangeRequest` → `ScheduleChangeRequest` campo a campo (nomes já idênticos,
conforme seção 0.4). O construtor de fallback vazio em `DemoPublicationRepository.data()`
(`OrganizationRepositories.kt:192-205`) ganha `scheduleChangeRequests = emptyList()` explícito.

### 5.3 Seletor puro (`ui/CardSelectors.kt`, ao lado de `effectivePause`/`colleaguesForShift` da FASE 14I)

```kotlin
fun changeRequestsRelevantTo(memberId: String, all: List<ScheduleChangeRequest>): List<ScheduleChangeRequest> =
    all.filter { it.memberId == memberId || it.assignedManagerMemberId == memberId }

fun pendingChangeRequestsRelevantTo(memberId: String, all: List<ScheduleChangeRequest>): List<ScheduleChangeRequest> =
    changeRequestsRelevantTo(memberId, all).filter { it.statusTyped == ChangeRequestStatus.PENDING }
```
Particionamento Demo × oficial: `scheduleChangeRequests` já vem de `DemoPublicationData`, que já é
uma instância **por workspace** (`ici-dev` vs `demo-v1` resolvidos por repositórios/resolvers
distintos, confirmado na pesquisa) — nenhuma mudança adicional necessária para não misturar, só
confirmar com um teste dedicado (Checkpoint F) que a lista nunca combina as duas origens.

## 6. Checkpoint E — card "Solicitações de troca" (Hoje) + tela real de trocas

### 6.1 Card em `TodayTab.kt`

Inserido entre `EventsCard` e `PauseCard` (`TodayTab.kt`, entre as linhas 88 e 90 atuais — ver
pesquisa, ponto exato confirmado). Usa `LabCard` (mesmo padrão visual dos outros cards).
Novo parâmetro em `TodayTab`: `changeRequests: List<ScheduleChangeRequest>`, `currentMemberId:
String`, `onOpenSwap: () -> Unit` (reaproveita o mesmo callback que `ProfileTab` já usa para
`StackedScreen.SWAP`).

Estados (computados de `changeRequests` via `changeRequestsRelevantTo`/`pendingChangeRequestsRelevantTo`,
mais um estado de carregamento/erro vindo do mesmo lugar que já carrega `DemoPublicationData` —
reaproveita `firebaseLoading`/`firebaseError`/`firebaseSyncCause` já existentes em `App.kt`, não
cria um segundo canal de loading/erro paralelo):
- **Carregando**: mesmo sinal que já existe para o resto da tela Hoje.
- **Vazio**: `pendingChangeRequestsRelevantTo` vazio → card **oculto** (decisão explícita desta
  spec: reduz ruído visual, igual à recomendação do prompt; sem espaço reservado). Se
  `changeRequestsRelevantTo` (todas, não só pendentes) não-vazio mas nenhuma pendente, o card
  também fica oculto em Hoje (não é urgente) — a lista completa continua acessível via Perfil →
  "Ver minhas solicitações", que sempre mostra tudo (pendente ou não).
- **Com pendências**: título "Solicitações de troca", badge com a contagem
  (`pendingChangeRequestsRelevantTo(...).size`), singular/plural correto ("1 solicitação
  pendente"/"N solicitações pendentes"), resumo compacto da mais recente
  (`maxByOrNull { it.createdAt }`): nome resolvido do solicitante (via `membersById`, mesmo padrão
  já usado em outros cards), `reason` como texto secundário, status por extenso
  (`statusTyped`/label), botão "Ver solicitações" → `onOpenSwap`.
- **Erro temporário/offline**: reaproveita a mensagem/estado já exibido pela `FirebaseStatusBar`
  para o resto da tela — não inventa um segundo texto de erro; se os dados gerais da escala não
  carregaram, o card nem tenta mostrar uma contagem (evita "0 solicitações" mentiroso quando na
  verdade é falha de leitura — regra explícita do prompt).

### 6.2 Tela completa (`StackedScreen.SWAP`, substitui o conteúdo hoje mockado)

Renomear/reescrever `ShiftSwapScreen.kt` (ou criar substituto e apagar o antigo — decisão de
implementação, mesma rota/título "Trocas de escala") para consumir
`changeRequestsRelevantTo(currentMemberId, data.scheduleChangeRequests)` em vez de
`mockShiftSwapRequests()`. Seções: "Enviadas por você" (`memberId == currentMemberId`) e
"Aguardando sua ação" (`assignedManagerMemberId == currentMemberId`) — nomeação alinhada ao
contrato real (requerente→aprovador), não ao antigo modelo peer-to-peer. Cada item mostra:
tipo (`requestTypeTyped` label), status (`statusTyped` label), `reason`, `createdAt` formatado,
e — só quando resolvido (`assignmentId` presente e encontrável em `scheduleAssignments`) — a
data/turno do plantão referenciado. **Sem botões de ação funcionais** (aprovar/recusar/cancelar) —
mesma decisão honesta já tomada pelo próprio Dashboard (`DemoChangeRequestsDialog`, botões
desabilitados com nota "Aprovação será habilitada em uma próxima etapa") — esta fase não
autoriza escrita real no Firestore, então nenhum botão deve fingir que persiste algo.

Remoção como consequência direta (código morto após a troca de fonte de dados): `ShiftSwapRequest`,
`SwapStatus`, `ShiftSwapRepository`, `MockShiftSwapRepository`, `mockShiftSwapRequests()`, e o
teste `MockRepositoriesTest.shiftSwapRepository_persistsNewRequests`.

## 7. Checkpoint F — golden contract + testes de isolamento

Fixture sintética nova (`commonTest`, sem dados reais, domínios `example.invalid` como já é
convenção no projeto): 2+ membros com login corporativo fictício, 4 turnos (Md/M/T/N), 1 folga, 1
pausa customizada, 3 `ScheduleChangeRequest` (uma `memberId == usuárioA`/`PENDING`, uma
`memberId == usuárioB` que **não pode aparecer** para o usuário A, uma
`assignedManagerMemberId == usuárioA`/`PENDING` — "aguardando coordenador"), workspace oficial e
workspace Demo com dados distintos. Teste único de ponta a ponta confirmando: identidade certa
resolvida; só a escala daquele usuário aparece; colegas separados por turno
(`colleaguesForShift`, já existente); pausa efetiva consistente (`effectivePause`, já existente);
`changeRequestsRelevantTo` nunca inclui a solicitação do usuário B; contagem do card bate com a
tela completa (mesma função, não dois cálculos); Demo não vaza para o workspace oficial.

Testes de isolamento (16 itens do prompt, mapeados para testes concretos, `commonTest` sempre que
a lógica for pura/testável sem Compose; quando exigir estado de UI real, documentar explicitamente
como "requer validação manual" em vez de simular Compose de forma frágil):
1–9 (sessão/Demo): `SessionBootstrapPhase` computado a partir de combinações de
`corporateAuthState`/`sessionMemberId`/`gateErrorMessage` (função pura, extraível para teste sem
Compose); `WasmMsalCorporateAuthRepository` (2.3); `demoAccessGranted` visível só quando
`isDemoAuthorized` retorna true (reaproveita os 4 testes já existentes de
`DemoAuthorizationTest`, mais um teste novo de "UI não mostra a ação quando false").
10–13 (logout/troca de usuário/cache): teste de `clearAllUserScopedCaches()` confirmando que,
após popular as três caches com dados do usuário A e chamar a função, `loadSchedule()`/
`loadOnCall()` voltam `null` (nenhum dado do usuário A sobra) — usando as implementações in-memory
de teste já existentes para essas interfaces.
14–16 (card): contagem/singular-plural (`pendingChangeRequestsRelevantTo` + função de label,
testada com 0/1/2+ diretamente); "erro de leitura não aparece como zero" (teste do estado
combinado card+`firebaseError`, não só da lista vazia).

## 8. Checkpoint G — fonte única de versão

Task Gradle nova `generateAppVersion` (`composeApp/build.gradle.kts`), replicando o padrão de
`generateWasmFirebaseConfig`: extrai `versionCode`/`versionName` para `val`s no topo do script
(usadas tanto por `defaultConfig` quanto pela task), gera `AppVersion.kt` (mesmo `object
AppVersion { const val CODE; const val LABEL }`) em `build/generated/source/appVersion/commonMain`,
registrado via `kotlin.sourceSets.named("commonMain") { kotlin.srcDir(...) }` — herdado
automaticamente por `androidMain`/`wasmJsMain`, sem `expect`/`actual` (valor puro). `dependsOn`
amplo (`tasks.matching { it.name.contains("compileKotlin", ignoreCase = true) }.configureEach {
dependsOn(generateAppVersion) }`) para cobrir Android, Wasm, metadata e testes. Arquivo manual
`composeApp/src/commonMain/kotlin/.../model/AppVersion.kt` removido do controle de versão
(git-ignorado por já cair em `build/`). Gate estrutural: como as duas únicas leituras de versão
(`ProfileTab.kt:419`, `AppUpdateChecker.android.kt:42`) importam `AppVersion` normalmente, se a
geração não rodar antes da compilação o build **falha por referência não resolvida** — divergência
silenciosa deixa de ser possível (documentado como o "gate automatizado" pedido pelo prompt: é
mais forte que um teste de runtime, é um erro de compilação).

## 8b. Checkpoint H — bugfix: identidade resolvida sempre concede entrada

Bug relatado pelo usuário em produção (2026-07-24), recorrente a cada versão nova: MSAL
autenticava (`corporateAuthState` virava `Authenticated`), a identidade corporativa resolvia
(membro/equipe reais encontrados via `OrganizationIdentityResolver`), mas o usuário ficava preso
na `LoginGateScreen` vendo "A escala oficial ainda não foi publicada neste ambiente" em vermelho
— mesmo quando, segundo o relato, a escala já tinha sido publicada. Nunca entrava no app; a única
coisa que deveria acontecer era a escala ficar desatualizada.

Causa raiz exata (`App.kt`, branch `EntryContext.LOGIN`, caso `OrganizationResolutionResult.Resolved`):
identidade resolvida e existência de escala publicada nunca foram separadas — ambas dependiam do
mesmo resultado (`decideResolvedScheduleSummary(...).summary != null`) para decidir se
`sessionMemberId` era setado. Quando não havia publicação carregável (`ActiveScheduleMissingMessage`),
o código zerava `sessionMemberId` E mostrava esse erro como `gateErrorMessage` — travando o gate
por um motivo que deveria ser, no máximo, um estado vazio dentro do app (mesmo espírito de
`ScheduleSyncCause.isEmptyState()`, seção "Estados tipados de erro" do spec 48).

Regra permanente fixada em código (`ResolvedScheduleSummaryDecision.kt`, `decideLoginEntry()`):
identidade corporativa resolvida **sempre** concede `sessionMemberId`; ausência de publicação
oficial **nunca** zera a sessão nem aparece como erro bloqueante no gate — só deixa a escala em si
desatualizada (identidade real sobreposta via `withResolvedIdentity()`, extraído do fallback que já
existia para o caso "sem carregador configurado"). `gateErrorMessage` deixa de ser setado neste
branch.

Testes de regressão (`ResolvedScheduleSummaryDecisionTest.kt`):
`decideLoginEntryGrantsSessionEvenWithoutPublishedSummary` prova que `sessionMemberId` nunca fica
nulo quando a identidade resolve, mesmo com `decision.summary == null`;
`decideLoginEntryUsesPublishedSummaryWhenAvailable` confirma que a publicação real continua sendo
usada quando existe. 276 testes JVM / 269 Wasm/Chromium (era 274/267), 0 falhas.

Achado relacionado, não corrigido neste checkpoint: reproduzindo em emulador, a MESMA tela
apareceu mesmo com o fix ativo (confirmado por bytecode do APK instalado) — porque, ali,
`result` nunca chegou a `Resolved` (a identidade em si não resolveu, um problema diferente).
`DemoPublicationResolver.loadOneAttempt()` engole qualquer `Throwable` silenciosamente, sem
log — investigação de causa raiz desse segundo problema é item separado, não coberto por este
checkpoint.

## 9. Compatibilidade e não-regressão

- Nenhuma mudança de schema de cache serializado (Android `SharedPreferences`/Web `localStorage`)
  além de novos `clear()` chamados no logout — nenhum formato de dado persistido muda.
  `NotificationSettings`/`ScheduleSummary`/`ShiftDay`/`PausePresentation` (FASE 14I) inalterados.
- `pauseFor`/`effectivePause`/`colleaguesForShift` (FASE 14I) continuam intocados e testados —
  esta fase só adiciona ao redor, nunca reabre essa lógica.
- Fixture local sem `scheduleChangeRequests` (lista vazia): `changeRequestsRelevantTo` retorna
  lista vazia, card fica oculto — nenhuma exceção, mesmo comportamento de "vazio" documentado.
- Nenhuma publicação real, nenhuma escrita no Firestore, nenhuma notificação real enviada,
  nenhuma aprovação/recusa persistida nesta fase.

## 10. Critérios de aceite desta spec

1. Sessão MSAL restaurada (Android e Web) leva direto a Hoje, sem toque manual, sem tela de login
   piscando antes.
2. Falha de rede durante restauração Web preserva `Authenticated` (teste dedicado, seção 2.3).
3. Tela desautenticada mostra só "Entrar com a conta corporativa".
4. "Ambiente Demo" nunca aparece antes da autenticação; aparece só para identidade autorizada,
   dentro da área autenticada; acesso direto sem autorização continua recusado no domínio.
5. Logout desconecta MSAL de verdade, limpa estado privado e as três caches globais, cancela
   alarmes; segundo login não mostra dado do usuário anterior.
6. `scheduleChangeRequests` chega à UI (deixou de ser descartado), tipado, com fallback seguro.
7. Card "Solicitações de troca" em Hoje consome a mesma fonte da tela completa, nunca um contador
   paralelo; singular/plural correto; oculto quando vazio; nunca mostra zero por erro de leitura.
8. Golden contract prova a cadeia completa e a não-contaminação Demo/oficial/usuário-B.
9. `AppVersion.CODE`/`LABEL` gerados de uma única fonte (`build.gradle.kts`); nenhum arquivo
   manual concorrente sobra no controle de versão.
10. Nenhum dos 247 testes JVM / 240 Wasm/Chromium pré-existentes quebra.
