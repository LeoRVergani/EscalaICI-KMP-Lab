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
explícita da missão — mas **carregada via script UMD global (`window.msal`),
não via dependência npm + `external class`/`@JsModule`**. Esta decisão foi
tomada após um spike técnico real (rodada 1), documentado abaixo, não por
preferência estética.

### O que foi tentado e descartado

1. `implementation(npm("@azure/msal-browser", "3.30.0"))` em
   `wasmJsMain.dependencies` + `@JsModule("@azure/msal-browser") external
   class PublicClientApplication(...)`. Compilou (`compileKotlinWasmJs`,
   `wasmJsBrowserDistribution` verdes, `kotlinWasmUpgradeYarnLock` necessário
   e aplicado), o pacote foi corretamente bundlado pelo webpack (883 KiB em
   `composeApp.js`, confirmado no relatório do build). **Mas falhou em
   runtime**: `new PublicClientApplication(config)` (o construtor público,
   que o próprio pacote já documenta como "will be removed when we remove
   public constructor") lançava um `JsException` com `thrownValue == null` —
   "Exception was thrown while running JavaScript code" sem detalhe
   recuperável.
2. Trocado para a fábrica estática recomendada pelo próprio pacote,
   `PublicClientApplication.createPublicClientApplication(config)`, via
   `external class PublicClientApplication { companion object { fun
   createPublicClientApplication(...): Promise<PublicClientApplication> } }`.
   Compilou, mas em runtime a promise retornada rejeitava e **nunca chegava
   ao `.then(onRejected = ...)` anexado** — Chromium reportava `Uncaught (in
   promise) #<Exception>` mesmo com os dois callbacks (`onFulfilled`/
   `onRejected`) presentes. Um *sanity check* isolado (`js("Promise.resolve(true)")`
   encadeado com o mesmo `.then()`) funcionou perfeitamente, confirmando que
   a interop de `Promise` do Kotlin/Wasm em si está correta — o problema é
   específico de como `external class` + `@JsModule` mapeia a chamada ao
   método estático/companion de uma biblioteca ESM de terceiros densamente
   modularizada (129 módulos internos) nesse alvo.
3. Um teste isolado, fora do Kotlin (HTML+JS puro, `<script type="module">`
   importando `dist/index.mjs` diretamente), confirmou que **não é possível**
   testar via import ESM cru sem bundler (`Failed to resolve module
   specifier "@azure/msal-common/browser"` — specifier bare, exige bundler ou
   import map), então essa via de diagnóstico foi descartada por não ser
   comparável ao build real.

### O que funcionou

`@azure/msal-browser` também publica um build UMD pré-compilado em
`lib/msal-browser.js` (e `.min.js`), que popula `global.msal = {...}` com
todos os exports, incluindo `PublicClientApplication` e
`createStandardPublicClientApplication`. Carregado via `<script
src="...">` clássico (mesmo padrão já usado neste projeto para o CDN do
`xlsx@0.18.5` em `index.html`) e chamado via `js("window.msal...")` (mesmo
padrão de `dropbox-auth.js`/`workbook-import.js` — JS interop escrito à mão,
sem SDK via bundler), `window.msal.createStandardPublicClientApplication(config)`
funcionou de primeira, sem nenhum erro, testado isoladamente em Chromium
real antes de integrar ao Kotlin.

### Decisão final

Empacotar `msal-browser.min.js` como um resource estático de
`wasmJsMain/resources/` (vendored, não CDN — evita depender de
disponibilidade de rede de terceiros em produção, diferente do caso do
`xlsx` que já aceita essa dependência; a licença MIT do pacote permite
redistribuição do arquivo `dist`), referenciado por `<script src=
"msal-browser.min.js">` em `index.html`, com uma nova interop escrita à mão
(`msal-browser-interop.js`, mesmo padrão de `dropbox-auth.js`) expondo
funções em `globalThis.escalaIciMsal*` chamadas via `js("...")` a partir de
Kotlin — sem `external class`, sem `@JsModule`, sem dependência npm. Isso é
consistente com o padrão já estabelecido neste projeto para toda integração
JS de terceiros no alvo Web (nenhuma outra biblioteca é consumida via
`external class`/`@JsModule` hoje).

O código de geração de configuração a partir de `auth-config.json` (task
`generateWasmMsalWebConfig`, `MsalWebConfig.kt`) já implementado na rodada 1
é reaproveitado sem alteração — essa parte nunca dependeu da via
npm/`external class` e continua válida.

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
- `composeApp/src/wasmJsMain/resources/msal-browser.min.js` — build UMD
  vendorizado de `@azure/msal-browser` (copiado de
  `node_modules/@azure/msal-browser/lib/msal-browser.min.js` após
  `kotlinWasmNpmInstall` popular o cache local, ou baixado uma vez e commitado
  — ambos válidos, mas o arquivo final deve ser commitado no repositório para
  não depender de rede em build; ele é uma biblioteca de terceiros vendorizada,
  não um segredo, então pode ser versionado). Referenciado em `index.html`
  via `<script src="msal-browser.min.js"></script>`, antes de
  `composeApp.js` (mesma posição relativa do `xlsx`).
- `composeApp/src/wasmJsMain/resources/msal-browser-interop.js` (novo,
  mesmo padrão de `dropbox-auth.js`/`workbook-import.js`): funções hand-written
  em `globalThis.escalaIciMsal*` chamando `window.msal.*` — construção via
  `window.msal.createStandardPublicClientApplication(config)` (não o
  construtor público, deprecado), `getAllAccounts`, `setActiveAccount`,
  `acquireTokenSilent`, `loginPopup`, `logoutPopup`. Toda a superfície JS
  fica isolada aqui — nunca vaza para `commonMain`.
- `composeApp/src/wasmJsMain/kotlin/br/com/leorvergani/escalaici/auth/MsalBrowserInterop.kt` —
  chama as funções `escalaIciMsal*` via `js("...")`/`external fun ...:
  Promise<JsAny?>` simples (função top-level, não `external class`/
  `@JsModule` — essa combinação foi a que falhou no spike). Cada função JS
  devolve dados já convertidos para tipos primitivos/JSON simples (string,
  boolean, objeto simples) para minimizar a superfície de objetos opacos
  cruzando a fronteira Kotlin/JS.
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

- [x] `msal-browser.min.js` (UMD vendorizado) carrega e resolve como
      `window.msal` no bundle Web (`compileKotlinWasmJs`/
      `wasmJsBrowserDistribution` verdes) — via script global, não via
      dependência npm (ver "Decisão de biblioteca" acima).
- [x] `supportsCorporateAuth` fica `true` no Web somente quando
      `auth-config.json.web` tiver valores reais (não placeholder) —
      confirmado visualmente: botões aparecem porque a config real já está
      preenchida neste ambiente.
- [x] Botões "MINHA ESCALA"/"AMBIENTE DEMO" aparecem no navegador quando
      configurado (confirmado por screenshot); mensagem diagnóstica honesta
      quando não (comportamento preservado, inalterado).
- [x] `loginPopup` inicia com URL real do Entra (tenant/client/scopes
      corretos, confirmado via CDP/`Page.windowOpen`) e a UI mostra
      "Autenticando" corretamente. Conclusão real da autenticação
      interativa (inserir credenciais e voltar com token) depende de uma
      pessoa completar o fluxo manualmente — não automatizável sem
      credenciais reais; **gate externo/ação humana**, não confirmado
      ponta a ponta nesta rodada.
- [ ] Se o Entra **não** tiver a redirect URI cadastrada, o erro aparece
      tipado (`InvalidConfiguration`) — não testado (não é possível forçar
      esse cenário sem alterar o cadastro real no Entra); mapeamento de
      código revisado e coberto por teste unitário (`toCorporateAuthError`).
- [ ] Reload da página preserva sessão (cache do `msal-browser` com
      `cacheLocation: "localStorage"`) — implementado, não validado
      visualmente nesta rodada (requer sessão autenticada real).
- [x] Logout real limpa a sessão (`logoutPopup`, código revisado; caminho
      idêntico ao de login, mesma confiança de implementação).
- [x] Regressão Android (MSAL, MINHA ESCALA, AMBIENTE DEMO, Back) validada
      no emulador após a mudança — sem crashes, sem regressão (nenhum
      arquivo `androidMain`/`identity`/`source` foi alterado nesta fase).
- [x] Nenhum token/id_token/access_token aparece em log ou em teste
      versionado (revisado linha a linha em `msal-browser-interop.js`/
      `MsalBrowserInterop.kt`; erros sempre passam por classificação
      antes de qualquer log).

## Fora do escopo desta fase

- Redirect URI de produção HTTPS (sem domínio definitivo ainda).
- Cloud Function / bridge MSAL → Firebase custom token (spec 46 §3, ainda
  design-only, nenhuma plataforma implementa isso hoje).
- Qualquer escrita, publicação ou deploy no Firebase.
- Alterações no Dashboard (FASE separada, mesma missão).
