# Spec 53 — MSAL Android e identidade corporativa (FASE 14b-1)

Auditoria e desenho da autenticação corporativa Microsoft (MSAL) real para o
Android do Escala ICI. Escopo estrito: login MSAL Android funcional, com
estados tipados e modo demonstração preservado. **Não** cobre Firebase
Custom Token, `user_links`, carregamento de time pelo usuário, MSAL Web ou
deploy — isso fica para as fases 14b-2 em diante (ver spec 46).

## 1. Estado atual (antes desta fase)

- `ui/LoginGateScreen.kt:58-66`: botão "Login" sempre falha com a mensagem
  fixa "Login corporativo Microsoft ainda não disponível...", sem nenhuma
  chamada MSAL.
- "Login de teste" (linhas 131-164) abre um diálogo com 3 membros fixos
  (`members.getOrNull(0/1/2)`), passados por `onSelectMember`.
- Sessão hoje é só `repository/MockRepositories.kt:81-108`:
  `MockAuthSessionRepository` (memberId fixo `"lvergani@ici.tec.br"`,
  não usado pelo App) e `InMemoryAuthSessionRepository` (mutável,
  `signIn(memberId)`/`signOut()`, usada de fato em `ui/App.kt:118,232,343`).
  Nenhuma das duas conhece MSAL/Firebase — são só um seletor de "qual
  colaborador mock estou vendo".
- `ui/ProfileTab.kt:117-120`: card "Identidade da escala" imprime
  incondicionalmente `ScheduleSyncCause.IDENTITY_NOT_LINKED.defaultMessage()`
  — texto estático, não reflete nenhum estado real. É o texto enganoso que
  esta fase deve substituir por um estado condicional de verdade.
- `gradle/libs.versions.toml:13,39`: catálogo já tem `msal = "4.9.0"` e a
  lib `com.microsoft.identity.client:msal`, preparados por uma fase
  anterior mas **ainda não usados** em `composeApp/build.gradle.kts`.
- `AndroidManifest.xml` não tem `BrowserTabActivity` nem intent-filter
  `msauth://`.

## 2. Decisões já tomadas nas specs 46/47/50/52 (não reabrir)

- Cloud Function-ponte MSAL→Firebase Custom Token fica para depois (spec 46,
  fora do escopo desta fase).
- `client_id`/`tenant_id` reaproveitados do App Registration existente do
  EscalaSOC; `AzureADMyOrg` (single-tenant); modo single-account; escopo
  único `User.Read`; redirect URI própria do Escala ICI (hash de assinatura
  diferente do EscalaSOC).
- Login silencioso primeiro, interativo só se falhar. Logout local (Firebase
  fica para a fase do Custom Token).
- Vínculo futuro por `tenantId + objectId` (spec 47), nunca por e-mail.
  `CorporateIdentity` desta fase já expõe esses dois campos para preparar o
  terreno, mas **não deriva `memberId`/`teamId`** — isso é FASE 14c
  (`user_links`).
- Identidade oficial já congelada (spec 52): `applicationId`
  `br.com.leorvergani.escalaici`, keystore `escalaici-kmp-lab.jks`.

## 3. Por que não copiar o padrão MSAL do EscalaSOC direto

`MicrosoftAuthManager.kt` do EscalaSOC (só consulta) trata todo erro MSAL
como um `MsalException` genérico (heurística textual `requiresInteractiveConsent()`
procurando "consent"/"interaction" na mensagem) e a UI mostra uma única
mensagem genérica de falha. Esta fase exige o oposto — estados tipados,
sem mensagem genérica — então o adapter Android daqui usa os subtipos reais
da exception (`MsalUiRequiredException`, `MsalClientException`,
`MsalServiceException`, `MsalDeclinedScopeException`) para decidir o estado,
em vez de inspecionar a mensagem de texto.

## 4. Arquitetura comum (`composeApp/src/commonMain/kotlin/.../auth/`)

Novo pacote `br.com.leorvergani.escalaici.auth`, independente dos
repositórios de escala/membro existentes.

```kotlin
data class CorporateIdentity(
    val tenantId: String,
    val objectId: String,
    val username: String,       // UPN devolvido pela MSAL (accountId lógico)
    val displayName: String,
    val email: String?,         // pode não vir preenchido, nunca é a chave
    val accountId: String       // IAccount.getId() da MSAL — cache/lookup local
)

enum class CorporateAuthConfigurationState { CONFIGURED, NOT_CONFIGURED }

sealed interface CorporateAuthError {
    data object Cancelled : CorporateAuthError
    data object TenantNotAllowed : CorporateAuthError
    data object AccountNotFound : CorporateAuthError
    data object InteractionRequired : CorporateAuthError
    data object NetworkError : CorporateAuthError
    data object InvalidConfiguration : CorporateAuthError
    data class Unknown(val detail: String? = null) : CorporateAuthError
}
// defaultMessage() dá uma frase própria por variante — nunca uma mensagem única.

sealed interface CorporateAuthState {
    data object NotConfigured : CorporateAuthState
    data object SignedOut : CorporateAuthState
    data object Authenticating : CorporateAuthState
    data class Authenticated(val identity: CorporateIdentity) : CorporateAuthState
    data class Failed(val error: CorporateAuthError) : CorporateAuthState
    data object Demo : CorporateAuthState
}
```

Isso cobre os 12 estados pedidos: `NOT_CONFIGURED`/`SIGNED_OUT`/
`AUTHENTICATING`/`AUTHENTICATED`/`DEMO_MODE` como variantes diretas de
`CorporateAuthState`; `CANCELLED`/`TENANT_NOT_ALLOWED`/`ACCOUNT_NOT_FOUND`/
`INTERACTION_REQUIRED`/`NETWORK_ERROR`/`INVALID_CONFIGURATION`/
`UNKNOWN_ERROR` como variantes de `CorporateAuthError`, carregadas dentro de
`CorporateAuthState.Failed`.

```kotlin
interface CorporateAuthHost   // marcador vazio; actual carrega a Activity no Android

interface CorporateAuthRepository {
    val configurationState: CorporateAuthConfigurationState
    val state: StateFlow<CorporateAuthState>
    suspend fun restoreSession()                       // recuperação silenciosa
    suspend fun signInInteractive(host: CorporateAuthHost?)
    suspend fun signOut()
    fun enterDemoMode()
}

@Composable
expect fun rememberCorporateAuthHost(): CorporateAuthHost?
```

`signInInteractive` recebe um `host` opaco (não `Activity` diretamente) para
o contrato ficar 100% comum — a implementação androidMain casta
internamente; a fake de teste e o stub Wasm ignoram o parâmetro.

### Por que não gatear a entrada no app pelo login corporativo

`Não derivar memberId/teamId nesta fase` e `Não selecionar automaticamente
um membro mock como se fosse usuário corporativo` (requisitos explícitos)
significam que autenticar com sucesso na MSAL **não** decide sozinho qual
`Member`/escala aparece — isso só existirá com `user_links` (FASE 14c). Por
isso o gate atual do `App.kt` (`sessionMemberId == null` → `LoginGateScreen`,
resolvido só pelo seletor de membro demo) **não muda nesta fase**. O que
muda: `LoginGateScreen` ganha uma seção real de login corporativo
(independente, mostra progresso/erro/estado), e uma vez autenticado, essa
identidade fica visível no Perfil (via o mesmo `CorporateAuthRepository`,
reconsultado a cada tela) com o aviso de que o vínculo com membro/time ainda
não existe. O usuário ainda escolhe um perfil de demonstração
separadamente para ver dados de escala — os dois fluxos coexistem sem se
confundir, exatamente como pedido ("modo demonstração pode continuar
existindo, mas deve estar visualmente identificado").

## 5. Adapter Android — problema do arquivo de configuração e solução

MSAL Android só aceita configuração via **resource `raw` compilado** (`R.raw.x`,
precisa existir em tempo de compilação) ou via **`File` de sistema de
arquivos** (documentado na doc de migração ADAL→MSAL: *"you either provide
as a file or store as a resource"*). Nenhum dos dois aceita "arquivo pode
não existir e o app continua compilando" diretamente — resource ausente
quebra a compilação; `File` em disco do dispositivo não tem acesso ao
arquivo de configuração do repositório (que só existe na máquina de
desenvolvimento).

Solução adotada (mesmo padrão já usado para `keystore.properties` em
`composeApp/build.gradle.kts:53-58`, só que aplicado à configuração MSAL):

1. Em **tempo de build** (Gradle, `composeApp/build.gradle.kts`), ler
   `auth-config.json` da raiz do projeto (arquivo real, gitignorado desde a
   FASE 14b-0) se existir, via `groovy.json.JsonSlurper` (já disponível no
   classpath do Gradle, sem dependência nova).
2. Gerar `buildConfigField` (precisa `buildFeatures { buildConfig = true }`):
   `MSAL_CONFIGURED: Boolean`, `MSAL_TENANT_ID`, `MSAL_CLIENT_ID`,
   `MSAL_REDIRECT_URI_DEBUG`, `MSAL_REDIRECT_URI_RELEASE`. `MSAL_CONFIGURED`
   só é `true` se o arquivo existir **e** os campos não estiverem com
   placeholder (`<...>`) nem vazios.
3. Em **tempo de execução**, se `BuildConfig.MSAL_CONFIGURED == false`, o
   repositório nunca toca a API da MSAL — retorna
   `CorporateAuthConfigurationState.NOT_CONFIGURED` /
   `CorporateAuthState.NotConfigured` direto. Isso garante "compila e abre
   sem configuração real" de forma trivial (o campo sempre existe, só muda
   de valor).
4. Se configurado, o repositório monta o JSON exigido pela MSAL
   (`client_id`, `redirect_uri` — escolhido entre debug/release via
   `BuildConfig.DEBUG` — , `account_mode: SINGLE`, `authorities` com
   `tenant_id`) em memória e grava em
   `File(context.filesDir, "msal_runtime_config.json")` (dado não sensível
   além do que já está no `BuildConfig`/APK; não é token, não é senha), e
   inicializa via `PublicClientApplication.createSingleAccountPublicClientApplication(context, file, listener)`.
   Nenhum valor de configuração real chega ao Git — o arquivo gerado fica só
   no armazenamento privado do app instalado.
5. `AndroidManifest.xml` ganha a `BrowserTabActivity` com
   `manifestPlaceholders` (`msalRedirectHost`/`msalRedirectPath`) definidos
   por `buildType` a partir dos mesmos campos do `auth-config.json` — se
   ausente, o placeholder vira um valor inócuo (intent-filter nunca
   corresponde a nada real, sem crash).

Isso evita duas armadilhas: (a) resource raw comitado com valor placeholder
que arrisca ser editado in-place e commitado com segredo real por engano; e
(b) qualquer necessidade de o app ler arquivos fora do seu sandbox.

## 6. Mapeamento de exceções MSAL → `CorporateAuthError`

| Exceção/situação MSAL | `CorporateAuthError` |
|---|---|
| `onCancel()` do callback interativo | `Cancelled` |
| `MsalServiceException` com `errorCode` indicando tenant/audience inválido | `TenantNotAllowed` |
| `getCurrentAccountAsync` retorna conta nula ao tentar silent | `AccountNotFound` |
| `MsalUiRequiredException` (silent falhou, precisa interação) | `InteractionRequired` |
| `MsalClientException` com causa de rede (`NO_NETWORK_CONNECTION` etc.) ou `IOException` | `NetworkError` |
| `MsalClientException`/`MsalArgumentException` de config inválida, ou `MSAL_CONFIGURED == false` | `InvalidConfiguration` |
| Qualquer outro `MsalException` | `Unknown(detail = exception.message)` |

## 7. Interface (sem recriar as telas)

`LoginGateScreen`: adiciona um bloco condicionado a
`rememberCorporateAuthHost()` + o estado do repositório — botão "Entrar com
conta corporativa", spinner durante `Authenticating`, mensagem específica
por `CorporateAuthError` (nunca genérica) com "Tentar novamente", e quando
`NotConfigured`: texto "Autenticação corporativa ainda não configurada
neste ambiente." + "Ver instruções de configuração" (abre um `AlertDialog`
com o resumo do checklist 00, sem link externo) + "Entrar no modo
demonstração" (reaproveita o diálogo de membros já existente). O seletor de
membro demo continua igual, só ganha o rótulo visual "(demonstração)".

`ProfileTab`: substitui a linha fixa
`ScheduleSyncCause.IDENTITY_NOT_LINKED.defaultMessage()` por um bloco
condicional lendo o mesmo `CorporateAuthRepository`: mostra nome/login,
confirma apenas que a organização corporativa foi identificada (sem expor o
`tenantId` completo) e mostra o status quando `Authenticated`, além do aviso "vínculo com membro/time
ainda será configurado em uma próxima fase" sempre que autenticado (não é
mais texto incondicional — só aparece quando há identidade real para
qualificar). Quando não autenticado corporativamente, mostra o estado
correspondente (`NotConfigured`/`SignedOut`/`Demo`) sem tokens nem IDs
completos (nenhum `accountId`/`objectId` bruto na tela comum).

## 8. Plataforma Web

`rememberCorporateAuthHost()` actual em `wasmJsMain` retorna sempre `null`;
não há `CorporateAuthRepository` real no Wasm nesta fase — a UI usa
`PlatformCapabilities.supportsCorporateAuth = false` (novo campo, mesmo
padrão de `supportsAppUpdate`/`supportsWebNotifications`) para mostrar
direto "Login corporativo Web ainda não configurado." sem tentar nenhuma
chamada MSAL. Build Wasm não ganha a dependência MSAL (é `androidMain`
only).

## 9. Testes (repositório fake, sem Entra)

`FakeCorporateAuthRepository` (commonTest) implementa
`CorporateAuthRepository` inteiro, com hooks para script de cenário
(`scriptedState`, contadores de chamada) — cobre as 17 situações pedidas:
ausência de configuração, desconectado, autenticando, autenticado,
cancelado, erro de rede, tenant incorreto, conta ausente, interação
necessária, restauração silenciosa, logout, preservação de `tenantId`/
`objectId`, ausência de token nos modelos (checado por reflexão de campos
do `CorporateIdentity`, que não tem nenhum campo de token), modo
demonstração separado do autenticado, login mock (`InMemoryAuthSessionRepository`)
não é tratado como `CorporateAuthState.Authenticated`, Web permanece
`NotConfigured`, e ausência de configuração não lança exceção.

## 10. Escopo explicitamente fora desta fase

Firebase Custom Token, `user_links`, resolução de `memberId`/`teamId` a
partir da identidade corporativa, MSAL Web/PWA, Cloud Functions, deploy —
todos ficam para as fases seguintes (ver spec 46 seção de fases).

## 11. Encerramento e validação final (2026-07-17)

A validação final foi executada sequencialmente em Windows com JDK 21 e o
Gradle Wrapper, usando os equivalentes locais dos comandos prescritos. O WSL
estava instalado sem distribuição Linux, portanto o caminho literal
`/usr/lib/jvm/java-21-openjdk` não era utilizável. O SDK Android e o keystore
locais permaneceram fora do Git.

Resultados:

- `:composeApp:testDebugUnitTest`: `BUILD SUCCESSFUL` (executado novamente
  depois da última correção de segurança);
- `:composeApp:assembleDebug`: `BUILD SUCCESSFUL`;
- `:composeApp:assembleRelease`: `BUILD SUCCESSFUL`;
- `:composeApp:wasmJsBrowserDistribution`: `BUILD SUCCESSFUL`;
- `:composeApp:wasmJsTest`: `BUILD SUCCESSFUL` com Google Chrome local;
- revisão da interface: no estado `Authenticated`, o botão normal de entrada
  foi substituído por identidade autenticada, nome/login, aviso do vínculo
  futuro, logout corporativo e opção demo separada; o Perfil não exibe mais o
  `tenantId` completo;
- revisão independente desde `5416a3f`: nenhum achado crítico; um achado
  importante (fixture de teste reutilizando um tenant real) foi corrigido por
  UUID inequivocamente fictício; nenhum achado importante ficou aberto;
- segurança: nenhum token persistido manualmente ou emitido em log, nenhum
  client secret/credencial/registry privado, nenhum arquivo de configuração
  real, keystore ou APK versionado e nenhum arquivo versionado indevido acima
  de 1 MiB;
- Web continua sem MSAL e informa que o login corporativo Web ainda não está
  configurado; modo demonstração permanece distinto da identidade real;
- nenhum emulador foi iniciado e nenhum teste visual foi declarado. O teste em
  dispositivo/emulador permanece como pendência humana;
- nenhum merge, deploy, upload, rebase, force push ou alteração no EscalaSOC e
  Dashboard foi realizado.

Configuração externa pendente: concluir o App Registration no Entra, cadastrar
redirect URIs/hashes debug e release e preencher `auth-config.json` local
gitignorado. O vínculo `tenantId + objectId` para `memberId`/`teamId`, Firebase
Custom Token e MSAL Web continuam nas fases futuras previstas.
