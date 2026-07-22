# FASE 14f — MSAL Web/Wasm: autenticação corporativa real no navegador

## Contexto

O Android já autentica via MSAL (spec 53/55). O alvo Web/Wasm nunca implementou o
adaptador real: `rememberCorporateAuthHost()` sempre retorna `null`
(`CorporateAuthHostProvider.wasmJs.kt`), nenhum `CorporateAuthRepository` é
instanciado em `Main.kt`, e `PlatformCapabilities.supportsCorporateAuth` fica no
padrão `false`. Por isso a tela de entrada mostra apenas "Login corporativo
indisponível neste ambiente" e nenhum botão (spec 62, gap documentado como
pré-existente).

`auth-config.json` (raiz, fora do Git) já tem um bloco `web` desde a spec 46,
mas nenhum código o lê até esta fase:

```json
"web": {
  "redirect_uri_local": "http://localhost:8080/",
  "redirect_uri_production": "<WEB_REDIRECT_URI_PRODUCTION_CLOUDFLARE>",
  "scope": ["openid", "profile", "User.Read"]
}
```

`tenant_id`/`client_id` são reaproveitados do mesmo App Registration usado pelo
Android (spec 46 §4: Web é cadastrado como uma segunda plataforma "Single-page
application" no mesmo registro, não um app novo). `redirect_uri_local` já tem um
valor real preenchido (`http://localhost:8080/`, a porta do
`wasmJsBrowserDevelopmentRun`); `redirect_uri_production` continua placeholder —
não há domínio HTTPS definitivo ainda, então essa via permanece um gate externo.

## Decisão de biblioteca

Uso de `@azure/msal-browser` (biblioteca oficial), conforme preferência
explícita da missão. Esta é a primeira dependência npm de terceiros deste
projeto — todo JS interop existente (`dropbox-auth.js`,
`workbook-import.js`, `remote-download.js`) é feito à mão, sem SDK. Antes de
investir na implementação completa, a primeira rodada de execução faz um
*spike* mínimo: declarar a dependência `npm("@azure/msal-browser", ...)`
apenas em `wasmJsMain.dependencies`, escrever uma `external class`/`external
interface` mínima e confirmar que `compileKotlinWasmJs` e
`wasmJsBrowserDistribution` compilam e que o bundle carrega no Chromium sem
erro de console antes de prosseguir. Se a interop com `external class` para uma
lib JS de terceiros nesse alvo (`wasmJs`, não `js` clássico) se mostrar
inviável ou instável, a rodada documenta o obstáculo concreto (erro de
compilação/link) e reavalia — mas não se assume essa alternativa antes de
tentar a via preferida.

## Contratos preservados (não mudam)

```kotlin
data class CorporateIdentity(tenantId, objectId, username, displayName, email: String?, accountId)
enum class CorporateAuthConfigurationState { CONFIGURED, NOT_CONFIGURED }
sealed interface CorporateAuthError { Cancelled, TenantNotAllowed, AccountNotFound, InteractionRequired, NetworkError, InvalidConfiguration, Unknown(detail) }
sealed interface CorporateAuthState { NotConfigured, SignedOut, Authenticating, Authenticated(identity), Failed(error), Demo }
interface CorporateAuthRepository { configurationState; state: StateFlow<CorporateAuthState>; restoreSession(); signInInteractive(host); signOut(); enterDemoMode() }
```

Nenhum desses tipos muda de forma. O adaptador Web implementa exatamente a
mesma interface que o Android já implementa — `App.kt`, `LoginGateScreen.kt`,
`OrganizationIdentityResolver` e `DemoAuthorization.kt` não precisam saber que
a plataforma mudou.

Estados adicionais de erro necessários para os cenários específicos de
navegador (popup bloqueado, redirect inválido) são cobertos reaproveitando
`CorporateAuthError.InvalidConfiguration`/`Unknown(detail)` — não se introduz
um novo tipo de erro na interface `commonMain`; o adaptador Web apenas escolhe
qual variante existente melhor descreve cada falha do `msal-browser`,
documentada em tabela abaixo, para não duplicar a taxonomia por plataforma.

| Cenário navegador | `CorporateAuthError` |
|---|---|
| Popup bloqueado pelo navegador | `Unknown("Pop-up de login bloqueado pelo navegador. Permita pop-ups para este site e tente novamente.")` |
| Redirect URI não cadastrada no Entra (`AADSTS9002326`/`50011`) | `InvalidConfiguration` |
| Usuário fechou o popup / cancelou | `Cancelled` |
| Conta não encontrada após redirect | `AccountNotFound` |
| `interaction_required`/`InteractionRequiredAuthError` | `InteractionRequired` |
| Tenant não permitido (`AADSTS50020`/similar) | `TenantNotAllowed` |
| Falha de rede (`fetch` rejeitado, offline) | `NetworkError` |
| Qualquer outro erro do MSAL Browser | `Unknown(detail = error.errorCode ?: error.message)` — nunca inclui token/id_token/access_token no `detail` |

## Novo código

- `composeApp/src/wasmJsMain/kotlin/br/com/leorvergani/escalaici/auth/MsalWebConfig.kt` —
  `data class MsalWebConfig(tenantId, clientId, redirectUri, scopes: List<String>)`
  com `val isConfigured: Boolean` (nenhum campo em branco ou começando com `<`).
  Só existe em `wasmJsMain` (Android não precisa; não é `expect/actual`).
- Gradle: task `generateWasmMsalWebConfig` (mesmo padrão de
  `generateWasmFirebaseConfig`, `composeApp/build.gradle.kts`) lendo
  `auth-config.json` (`tenant_id`, `client_id`, `web.redirect_uri_local`,
  `web.scope`) e emitindo
  `br/com/leorvergani/escalaici/auth/MsalWebConfig.wasmJs.generated.kt` com
  `val platformMsalWebConfig: MsalWebConfig = MsalWebConfig(...)`. Usa
  `redirect_uri_local` (ambiente local é o único suportado agora;
  `redirect_uri_production` fica documentado mas não consumido até existir
  domínio HTTPS real — gate externo).
- `composeApp/src/wasmJsMain/kotlin/br/com/leorvergani/escalaici/auth/MsalBrowserInterop.kt` —
  `external`/`@JsModule("@azure/msal-browser")` mínimo necessário:
  `PublicClientApplication`, `Configuration`, `AccountInfo`,
  `AuthenticationResult`, métodos `initialize`, `getAllAccounts`,
  `setActiveAccount`, `acquireTokenSilent`, `loginPopup`, `logoutPopup`. Toda
  a superfície JS fica isolada aqui — nunca vaza para `commonMain`.
- `composeApp/src/wasmJsMain/kotlin/br/com/leorvergani/escalaici/auth/WasmMsalCorporateAuthRepository.kt` —
  implementa `CorporateAuthRepository`:
  - `configurationState` = `CONFIGURED` apenas se `platformMsalWebConfig.isConfigured`;
  - `restoreSession()`: `getAllAccounts()` → se houver conta, `setActiveAccount` +
    `acquireTokenSilent` (renovação silenciosa); falha silenciosa (sem popup) →
    `SignedOut`, nunca dispara interação sozinha;
  - `signInInteractive(host)`: `loginPopup(scopes)`; host não é usado no Web
    (parâmetro ignorado — a interface exige `CorporateAuthHost` mas o browser
    não precisa de uma Activity/contexto);
  - `signOut()`: `logoutPopup()` + limpa estado local para `SignedOut`;
  - `enterDemoMode()`: mesmo comportamento do Android — define `Demo`
    diretamente, sem chamar o MSAL;
  - previne login concorrente: um `Mutex`/flag local rejeita uma segunda
    chamada de `signInInteractive` enquanto `Authenticating` já está em
    andamento (retorna sem reabrir popup, evita duplo popup);
  - mapeamento de claims do `idTokenClaims` do `AccountInfo`:
    `oid` → `objectId`, `tid`/`AccountInfo.tenantId` → `tenantId`,
    `name` → `displayName` (fallback `username`), `preferred_username` →
    `email` (fallback `username`), `homeAccountId` → `accountId` — mesmo
    fallback do Android (`oid ?: id`, `name ?: username`, etc.), para manter
    paridade de comportamento entre plataformas.
- `composeApp/src/wasmJsMain/kotlin/br/com/leorvergani/escalaici/auth/CorporateAuthHostProvider.wasmJs.kt` —
  sem mudança de assinatura; continua podendo retornar um host marcador vazio
  (o Web não precisa de Activity), só para satisfazer o parâmetro não-nulo se
  necessário pela UI — decisão de implementação fica com a rodada de execução,
  desde que não quebre `signInInteractive`.
- `Main.kt` (wasmJs): troca `corporateAuthRepository = null` (implícito, hoje
  nenhum é passado) por `WasmMsalCorporateAuthRepository()` e
  `platformCapabilities.supportsCorporateAuth =
  WasmMsalCorporateAuthRepository.configurationState == CONFIGURED`
  computado uma vez na criação (mesma regra do Android:
  `supportsCorporateAuth` não fica incondicionalmente `true`).

## Persistência de sessão

`@azure/msal-browser` já persiste em `sessionStorage`/`localStorage` conforme
`cache.cacheLocation` da config (`localStorage`, para sobreviver a reload de
página, já que o requisito pede "recarregar preserva sessão quando
permitido"). Não se grava nada adicional manualmente — não duplicar o cache
do MSAL. Nenhum `console.log`/log da aplicação imprime `idToken`,
`accessToken` ou o conteúdo bruto de `idTokenClaims`; diagnósticos (se
necessários) logam só o nome do estado/erro, nunca o payload do token —
mesma regra do `CorporateAuthDiagnostics` Android.

## Segurança

- SPA (`Authorization Code + PKCE`, gerado internamente pelo `msal-browser` —
  não há client secret em nenhum lugar do código Web, MSAL Browser já não
  aceita `clientSecret` na config de SPA);
- `@azure/msal-browser` fica declarado **apenas** em
  `wasmJsMain.dependencies` (nunca em `commonMain` ou `androidMain`);
- nenhuma credencial nova versionada — `auth-config.json` continua
  gitignored; o Gradle apenas lê os campos já existentes.

## Redirect URIs a cadastrar no Microsoft Entra (ação humana / gate externo)

No mesmo App Registration do Android, adicionar uma plataforma **Single-page
application** (se ainda não existir) com:

1. `http://localhost:8080/` — ambiente local de desenvolvimento (valor já
   presente em `auth-config.json.web.redirect_uri_local`; o time de execução
   não pode confirmar via API se isso já foi cadastrado no Entra — só um
   login real revela isso).
2. `<domínio HTTPS definitivo>` — placeholder explícito, pendente de decisão
   de hospedagem (Cloudflare Pages ou outro) — gate externo, sem ETA nesta
   fase.

Tipo de plataforma: **SPA**, não "Web" (que exigiria client secret) e não
"Public client/mobile" (usado pelo Android). Sem client secret em nenhum dos
dois.

## Testes obrigatórios

Como `WasmMsalCorporateAuthRepository` depende de `external`/JS do
navegador, ele não é diretamente testável em `commonTest`. A rodada de
execução deve extrair toda a lógica pura e testável para funções/classes
sem dependência de `external` (mapeamento de claims, classificação de
erro por `errorCode`, decisão de configurado/não configurado, prevenção de
login concorrente como uma máquina de estados pura), testadas em
`wasmJsTest` ou `commonTest` conforme onde o código puro acabar residindo,
usando fakes/dados sintéticos — nunca uma conta corporativa real. Cobrir:

- `MsalWebConfig.isConfigured` verdadeiro/falso (campos em branco, com `<`,
  válidos);
- classificação de erro por `errorCode`/nome da exceção do msal-browser →
  `CorporateAuthError` (tabela acima), incluindo o caso "outro erro" caindo
  em `Unknown` sem incluir token na mensagem;
- mapeamento de claims → `CorporateIdentity` (com e sem `preferred_username`,
  com e sem `oid`, fallback para `username`/`homeAccountId`);
- prevenção de segunda chamada concorrente de login;
- regressão: `FakeCorporateAuthRepository`/testes Android existentes
  continuam passando sem alteração (a interface não muda).

## Critérios de aceite

- [ ] `@azure/msal-browser` compila e resolve como dependência de
      `wasmJsMain` (`compileKotlinWasmJs` verde).
- [ ] `supportsCorporateAuth` fica `true` no Web somente quando
      `auth-config.json.web` tiver valores reais (não placeholder).
- [ ] Botões "MINHA ESCALA"/"AMBIENTE DEMO" aparecem no navegador quando
      configurado; mensagem diagnóstica honesta quando não.
- [ ] `loginPopup` inicia e, se o Entra já tiver a redirect URI cadastrada,
      completa a autenticação real (validado com Chromium real, sem
      registrar senha/token em log).
- [ ] Se o Entra **não** tiver a redirect URI cadastrada, o erro aparece
      tipado (`InvalidConfiguration`) e isso é registrado como gate externo,
      não como falha de código.
- [ ] Reload da página preserva sessão (cache do `msal-browser`).
- [ ] Logout real limpa a sessão.
- [ ] Regressão Android (MSAL, MINHA ESCALA, AMBIENTE DEMO, Back) validada no
      emulador após a mudança.
- [ ] Nenhum token/id_token/access_token aparece em log ou em teste
      versionado.

## Fora do escopo desta fase

- Redirect URI de produção HTTPS (sem domínio definitivo ainda).
- Cloud Function / bridge MSAL → Firebase custom token (spec 46 §3, ainda
  design-only, nenhuma plataforma implementa isso hoje).
- Qualquer escrita, publicação ou deploy no Firebase.
- Alterações no Dashboard (FASE separada, mesma missão).
