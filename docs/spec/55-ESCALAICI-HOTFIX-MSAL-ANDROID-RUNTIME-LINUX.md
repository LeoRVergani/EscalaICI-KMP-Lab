# Spec 55 — Hotfix MSAL Android: hash de assinatura real em runtime (FASE 14b-1c)

Hotfix sobre a FASE 14b-1 (spec 53). Fecha a pendência humana deixada em
aberto na spec 53 seção 11 ("nenhum emulador foi iniciado e nenhum teste
visual foi declarado") e corrige um erro de configuração que impedia a
abertura do fluxo Microsoft no ambiente Linux de desenvolvimento.

## 1. Sintoma inicial

Ao tocar em "Entrar com conta corporativa" no emulador (`EscalaSOC_API_37`,
Linux), a MSAL falhava a inicialização antes de abrir o navegador, e a UI
caía direto no estado `CorporateAuthState.Failed(InvalidConfiguration)` —
mensagem genérica de configuração inválida, sem o fluxo Microsoft chegar a
abrir.

## 2. Causa raiz

Antes deste hotfix, `composeApp/build.gradle.kts` lia
`signature_hash_debug`/`signature_hash_release` e
`redirect_uri_debug`/`redirect_uri_release` como **valores manuais** dentro
de `auth-config.json` (arquivo local, fora do Git). Esses valores precisam
ser exatamente o hash SHA-1/Base64 do certificado de assinatura
efetivamente usado para gerar o APK instalado no dispositivo. Bastava o
certificado local (`~/.android/debug.keystore` ou o keystore de laboratório
apontado por `keystore.properties`) divergir do que foi digitado à mão em
`auth-config.json` — troca de máquina, keystore de debug recriado,
copy/paste desatualizado — para o runtime MSAL (`msal_runtime_config.json`,
escrito por `MsalCorporateAuthRepository.writeRuntimeConfigFile()`) declarar
uma `redirect_uri` que não correspondia a nenhuma assinatura real instalada.
A MSAL detecta esse descasamento na inicialização do
`PublicClientApplication` e falha com um erro de validação de manifesto
(`app_manifest_validation_error`/`MsalClientException` com código
relacionado a `REDIRECT`/`SIGNATURE`), sem chegar a abrir o navegador.

## 3. Correção aplicada

`composeApp/build.gradle.kts` (função `signatureHashFromKeystore`, linha 94)
agora **deriva o hash real em tempo de build**, direto do certificado que
vai assinar aquele artefato específico:

- lê o certificado direto via `java.security.KeyStore.getInstance(File,
  char[])` do `storeFile`/`alias`/senha efetivos daquele `buildType`
  (keystore de laboratório se `keystore.properties` existir; senão o
  `debug.keystore` padrão do usuário, alias `androiddebugkey`, senha
  `android` — o padrão do Android SDK) — ver seção 12 sobre por que isso
  não passa mais por um processo `keytool` externo;
- aplica SHA-1 sobre os bytes do certificado exportado e codifica o
  resultado em Base64;
- constrói o `redirect_uri` (`msauth://<applicationId>/<hash>`) e os
  `buildConfigField` (`MSAL_REDIRECT_URI_DEBUG`/`_RELEASE`) a partir desse
  hash real, nunca mais de um valor digitado à mão.

Isso elimina a classe de erro inteira: o hash usado no runtime MSAL passa a
ser sempre consistente com a assinatura que efetivamente instalou o APK,
em qualquer máquina, sem depender de um valor sincronizado manualmente em
`auth-config.json`. Os campos `signature_hash_*`/`redirect_uri_*` de
`android` em `auth-config.example.json` ficam órfãos após este hotfix (não
são mais lidos) — permanecem no arquivo de exemplo só como referência
histórica do formato antigo; nenhum código os consome.

## 4. Hash bruto vs. hash codificado (RFC 3986) — o segundo bug

Existe uma segunda armadilha, independente da primeira, na própria MSAL
Android: o mesmo hash de assinatura precisa aparecer em **dois lugares com
duas representações diferentes**:

- no `redirect_uri` (usado no JSON de configuração MSAL e no registro do
  App Registration no Entra) o hash Base64 precisa estar **URL-encoded**
  (`+` → `%2B`, `/` → `%2F`, `=` → `%3D`) — é assim que a MSAL/AAD tratam a
  string como parte de uma URI;
- no `AndroidManifest.xml`, no `<data android:path="...">` do
  `BrowserTabActivity`, o valor precisa ser o hash Base64 **bruto, sem
  URL-encoding** — o matching de `intent-filter` do Android decodifica o
  path da URI recebida antes de comparar, então declarar a versão já
  codificada no manifesto nunca casa com o path decodificado do redirect
  real, e a MSAL rejeita a validação do manifesto.

Este segundo ponto é uma particularidade documentada, mas pouco intuitiva,
da integração MSAL Android — e era a causa mais provável do
`app_manifest_validation_error` mesmo com o hash certo. A correção
(`build.gradle.kts`, dentro de `buildTypes { getByName("debug"/"release") }`)
agora usa `realSignatureHashDebug`/`realSignatureHashRelease` brutos (sem
`urlEncodeSignatureHash`) diretamente no
`manifestPlaceholders["msalSignatureHashPath"]`, reservando
`urlEncodeSignatureHash()` só para a construção do `redirect_uri` (função
`redirectUriForSignatureHash`, linha 106).

Validação independente feita nesta sessão: o path declarado no
`AndroidManifest.xml` embutido no APK de release final foi extraído com
`aapt2 dump xmltree` e comparado byte a byte com o Base64 do SHA-1 reportado
pelo `apksigner verify --print-certs` sobre o mesmo APK — os dois valores
coincidem exatamente, confirmando que o manifesto compilado usa o hash real
da assinatura efetiva, na representação (bruta) correta.

## 5. Diagnóstico sanitizado adicionado

`CorporateAuthDiagnostics.kt` (novo arquivo) classifica qualquer
`MsalException` em um `AuthDiagnosticCode` interno (`AUTH_REDIRECT_SIGNATURE_MISMATCH`,
`AUTH_MSAL_INITIALIZATION_FAILED`, `AUTH_MSAL_RESOURCE_INVALID`, entre
outros) a partir do **tipo da exceção e do `errorCode`** — nunca da
mensagem livre, que pode conter detalhes sensíveis do tenant. Em
`MsalCorporateAuthRepository.getOrCreateMsalApplication()`, esse código é
logado (`Log.w`) só quando `BuildConfig.DEBUG` é verdadeiro, e o log nunca
inclui client secret, token, tenant real ou path de arquivo — só o enum e o
nome da classe da exceção. O comportamento observável ao usuário
(`CorporateAuthState.Failed`) também deixou de ser sempre
`InvalidConfiguration` fixo: `onError` agora mapeia a exceção real via
`exception.toCorporateAuthError()`, então falhas de rede, consentimento
recusado etc. já tinham essa distinção (código pré-existente); o que este
hotfix adicionou foi o log de diagnóstico e o `AuthDiagnosticCode` interno
usado nele.

## 6. Testes automatizados

`androidUnitTest/.../auth/CorporateAuthDiagnosticsTest.kt` (novo) cobre as 4
transições de `toDiagnosticCode()`: `MsalClientException` com `errorCode`
contendo `REDIRECT`/`SIGNATURE` → `AUTH_REDIRECT_SIGNATURE_MISMATCH`;
`MsalClientException` genérica → `AUTH_MSAL_INITIALIZATION_FAILED`;
`MsalArgumentException` → `AUTH_MSAL_RESOURCE_INVALID`.

Execução nesta sessão (`:composeApp:clean :composeApp:testDebugUnitTest
:composeApp:assembleDebug --rerun-tasks`):

```
119 testes, 0 falhas
BUILD SUCCESSFUL
```

(O número de testes é maior que o `94` de referência da spec anterior por
incluir os 4 testes novos deste hotfix mais outras suítes já existentes no
branch — usado aqui como fonte de verdade atual, não uma regressão.)

## 7. Validação manual no emulador (Linux, `EscalaSOC_API_37`)

Executada nesta sessão, com o app já instalado e autenticado:

- **Restauração silenciosa**: `am force-stop` + relançamento via `monkey`
  → app reabriu autenticado, nome e login corporativos reapareceram sem
  novo prompt de login, sem mensagem de configuração inválida, sem crash
  (`logcat` sem `FATAL EXCEPTION`/`AndroidRuntime` do processo do app).
- **Logout**: "Sair da conta corporativa" → retorno correto à tela de
  entrada (`Entrar com conta corporativa` visível, sessão local encerrada).
- **Novo login**: "Entrar com conta corporativa" → abriu o Custom Tab do
  Chrome em `login.microsoftonline.com`, exibindo a tela de seleção de
  conta e a tela de consentimento reais da Microsoft (nenhuma senha/MFA foi
  automatizada — a conta já estava autenticada no navegador do emulador) →
  retorno ao app já autenticado, nome e login corporativos exibidos
  novamente. Um crash nativo (`SIGILL`) foi observado no `logcat` durante
  esse fluxo, mas pertence ao processo `com.android.chrome`
  (`onTrimMemory`), não ao processo do app (`br.com.leorvergani.escalaici`)
  — não é uma regressão desta correção.

## 8. Modo demonstração

Confirmado que "Continuar em modo demonstração" (visível só quando já
autenticado) e "Login de teste (demonstração)" abrem o mesmo diálogo de
seleção de colaborador fictício (`LoginGateScreen.kt`, `showDemoOptions`) e
que `onSelectMember` **não chama `signOut()`** — não altera
`CorporateAuthState` nem o `ISingleAccountPublicClientApplication`
subjacente. Selecionar um colaborador de teste navega para o dashboard
mock; relançar o app depois disso mostra a conta corporativa ainda
autenticada, comprovando que o modo demonstração não substitui nem apaga a
sessão corporativa real. A autenticação corporativa continua sendo o fluxo
principal exibido primeiro na tela.

## 9. Build release e assinatura

```
:composeApp:assembleRelease → BUILD SUCCESSFUL
apksigner verify --print-certs → Verifies: true (v2 scheme)
```

Package `br.com.leorvergani.escalaici`, `versionCode`/`versionName`
confirmados no APK gerado (ver seção 11). Nenhuma senha de keystore foi
impressa nesta sessão — `apksigner verify` não a requer.

## 10. Web/Wasm

`:composeApp:wasmJsBrowserDistribution` → `BUILD SUCCESSFUL` (bundle Webpack
gerado normalmente). `:composeApp:wasmJsTest` falhou neste ambiente por
ausência de um binário Chrome/Chromium instalado na máquina Linux usada
(`ChromeHeadless` não encontrado pelo Karma) — falha de infraestrutura do
ambiente local, não do código: a compilação Kotlin/Wasm de produção e de
testes (`compileKotlinWasmJs`, `compileTestKotlinWasmJs`) terminou sem erro
antes da tentativa de lançar o navegador de teste. A alteração deste hotfix
é restrita a `androidMain`/lógica Gradle específica de Android; não há
nenhuma mudança em `commonMain`/`wasmJsMain` que pudesse quebrar o Web.

## 11. Versionamento

`versionCode` `18`/`versionName` `"0.7.4"` → `versionCode` `19`/`versionName`
`"0.7.5"`, aplicado após a bateria de testes descrita nas seções 6-10 (e a
suíte foi re-executada com a versão nova para confirmar que o bump não
introduziu regressão).

## 12. Revisão independente (Codex) e correções aplicadas

A revisão do Codex (`codex review`, sem alterar arquivos) sobre o diff desta
sessão encontrou dois achados, ambos corrigidos antes do commit:

- **Crítico** — a primeira versão de `signatureHashFromKeystore()` chamava
  `keytool -exportcert ... -storepass <senha>` via `ProcessBuilder`, o que
  expõe a senha do keystore em texto puro para qualquer usuário local capaz
  de ler `/proc/<pid>/cmdline` (ou `ps`) enquanto o processo roda — verificado
  nesta sessão reproduzindo a chamada e lendo `/proc/<pid>/cmdline` durante a
  execução. Corrigido eliminando o processo externo: a função agora lê o
  certificado direto via `java.security.KeyStore.getInstance(File, char[])`
  (API padrão da JVM desde o Java 9, detecta o tipo do keystore
  automaticamente), sem nunca colocar a senha em um argumento de linha de
  comando. Confirmado que o hash resultante é byte a byte idêntico ao
  produzido pela versão anterior (mesmo SHA-1/Base64 do certificado, mesmo
  `android:path` no manifesto do APK release compilado).
- **Importante** — com `keystore.properties` ausente, `realSignatureHashRelease`
  ficava sempre `null`, e o único booleano `msalConfigured` (usado em ambas
  as variantes) exigia o redirect de release também — isso deixava o
  **debug** como `NOT_CONFIGURED` mesmo com hash e redirect debug corretos,
  bloqueando o login corporativo em qualquer máquina de desenvolvimento sem
  o keystore de laboratório. Corrigido: `MSAL_CONFIGURED` passou a ser
  calculado por `buildType` (`msalConfiguredDebug`/`msalConfiguredRelease`,
  cada um exigindo só o próprio redirect) e é declarado dentro de
  `buildTypes { getByName("debug"/"release") { buildConfigField(...) } }` em
  vez de uma única vez em `defaultConfig`. Confirmado nesta sessão movendo
  `keystore.properties` temporariamente (restaurado por `trap` ao final do
  script) e inspecionando o `BuildConfig.java` gerado: `MSAL_CONFIGURED`
  ficou `true` para o debug (hash do `debug.keystore` padrão) e
  `MSAL_REDIRECT_URI_RELEASE` ficou vazio, como esperado — sem afetar o
  debug.

Após as duas correções, toda a bateria das seções 6-10 foi re-executada
(testes, `assembleDebug`, `assembleRelease`, verificação de assinatura,
`wasmJsBrowserDistribution`) com os mesmos resultados (`119` testes, `0`
falhas; `BUILD SUCCESSFUL`; mesmo hash/assinatura release).

## 13. Segurança

Nenhum arquivo real de segredo (`auth-config.json`, `keystore.properties`,
`.jks`, APK) está rastreado pelo Git — confirmado via `git ls-files` e
`git status`. O diff revisado nesta sessão não contém client ID, tenant ID,
senha, token ou hash/redirect reais — só nomes de campo, lógica de
derivação e placeholders. Os arquivos novos (`CorporateAuthDiagnostics.kt`,
`CorporateAuthDiagnosticsTest.kt`) não fazem log de PII nem de segredos. A
correção do achado crítico da seção 12 remove a única superfície nova de
exposição de segredo introduzida por este hotfix (senha de keystore em
argumento de processo).

## 14. Próxima fase

Sem alteração de escopo em relação à spec 53 seção 10: vínculo
`tenantId + objectId` → `memberId`/`teamId` (`user_links`), Firebase Custom
Token e MSAL Web continuam fora deste hotfix e ficam para a FASE 14c.
