# SPEC 52 — Identidade oficial, assinatura e release do Escala ICI

**Status:** auditoria concluída; implementação executada nesta mesma FASE 14b-0
**Escopo:** Escala ICI KMP (Android + Web/PWA) — identidade técnica, assinatura, versionamento e processo de release
**Fase:** FASE 14b-0 (congelamento de identidade), anterior a FASE 14b (MSAL) e FASE 14d (Firebase Auth/Rules)
**Não implementa:** login MSAL completo, Firebase Custom Token, deploy de qualquer tipo

## 1. Contexto

O projeto chegou a este ponto com uma identidade técnica de laboratório
(`applicationId`/`namespace` contendo `.kmp.lab`, package Kotlin espelhando o
mesmo sufixo em ~90 arquivos). Essa identidade foi documentada como pendência
explícita em `docs/setup/01-IDENTIDADE-OFICIAL-DO-APP.md` (sprint noturno de
2026-07-15): a decisão sobre "laboratório para sempre" vs. "promovido a app
oficial" ficou em aberto para decisão humana. Antes de implementar MSAL
(spec 46) — cujo redirect URI Android é derivado justamente do `applicationId`
e do hash de assinatura —, esta fase congela essa identidade para evitar
cadastrar uma integração externa (Entra ID) sobre um identificador que seria
descartado logo depois.

## 2. Evidência levantada e decisão sobre o identificador

Nenhuma release assinada deste projeto (`EscalaICI-KMP-Lab`) foi publicada com
um `applicationId` diferente de `br.com.leorvergani.escalaici.kmp.lab`. Os
APKs encontrados localmente em `/home/lvergani/Downloads/dropbox_update_scripts/`
(`EscalaICI-latest.apk`, `EscalaICI_v1_19.apk`, `EscalaICIv1201.apk`) foram
todos gerados com esse mesmo `applicationId` de laboratório — não há um
terceiro identificador "oficial" concorrente. `EscalaSOC-latest.apk`, também
presente na mesma pasta, pertence a um **aplicativo Android nativo diferente**
(`br.com.leorvergani.escalasoc`, repositório `EscalaSOC`), não a este projeto
KMP — não é evidência de um `applicationId` oficial alternativo para o
Escala ICI.

Além disso, a plataforma Android para o `applicationId` de laboratório **nunca
foi cadastrada** no App Registration do Entra ID (confirmado em
`docs/setup/00-CHECKLIST-CONFIGURACAO-AMANHA.md`, item 1: "BLOQUEADO
EXTERNAMENTE" — cadastro pendente). Ou seja, não existe nenhum cadastro externo
já feito que a mudança de identificador invalidaria.

**Decisão aplicada, seguindo o critério definido para esta fase:** o
identificador oficial passa a ser `br.com.leorvergani.escalaici` (namespace,
applicationId e raiz dos packages Kotlin), eliminando `.kmp.lab`. Não é uma
terceira identidade inventada — é a remoção do sufixo de laboratório do
identificador já existente, recomendada explicitamente pelo enunciado desta
fase e sem evidência em contrário.

**Impacto da mudança (documentado, não bloqueador):**
- Qualquer instalação existente do APK de laboratório
  (`br.com.leorvergani.escalaici.kmp.lab`) não recebe mais atualização
  automática por `AppUpdateChecker` — Android não permite mudar
  `applicationId` nem assinatura de um app já instalado sem desinstalar antes.
  Como a distribuição até aqui foi só manual/teste (nunca publicada em loja),
  o universo afetado é o(s) aparelho(s) de teste do próprio Leonardo.
- A chave de assinatura muda (seção 5) — hash de assinatura novo, então
  qualquer cadastro futuro no Entra (MSAL) deve usar os valores novos, nunca
  os antigos já registrados em `docs/PENDENCIAS-EXTERNAS.md`/spec 46 (que
  ficam desatualizados e são corrigidos nesta fase).
- `msauth://` redirect URI, `FileProvider` authority (`${applicationId}.fileprovider`,
  já dinâmico) e qualquer referência textual ao `applicationId` antigo em
  documentação precisam mudar juntas — ver matriz seção 3.

## 3. Matriz de auditoria

| Campo | Valor atual (antes desta fase) | Package EscalaSOC (referência, não copiado) | Valor recomendado/aplicado | Arquivo(s) onde está definido | Impacto da mudança |
|---|---|---|---|---|---|
| Nome visível (Android) | `Escala ICI` | `Escala SOC` | `Escala ICI` (sem mudança) | `composeApp/src/androidMain/AndroidManifest.xml` (`android:label`) | Nenhum — já correto |
| Nome visível (Web/PWA) | `Escala ICI` | n/a | `Escala ICI` (sem mudança) | `composeApp/src/wasmJsMain/resources/manifest.json` (`name`, `short_name`) | Nenhum — já correto |
| `applicationId` | `br.com.leorvergani.escalaici.kmp.lab` | `br.com.leorvergani.escalasoc` | `br.com.leorvergani.escalaici` | `composeApp/build.gradle.kts` (`defaultConfig.applicationId`) | Reinstalação obrigatória em qualquer aparelho de teste; hash de assinatura MSAL muda junto |
| `namespace` | `br.com.leorvergani.escalaici.kmp.lab` | `br.com.leorvergani.escalasoc` | `br.com.leorvergani.escalaici` | `composeApp/build.gradle.kts` (`android { namespace }`) | Precisa bater com o novo package raiz Kotlin |
| Package Kotlin raiz | `br.com.leorvergani.escalaici.kmp.lab` (9 subpacotes: `.model`, `.platform`, `.repository`, `.source`, `.ui`, `.ui.components`, `.ui.theme`, `.ui.util`) | `br.com.leorvergani.escalasoc.*` | `br.com.leorvergani.escalaici` (mesmos 9 subpacotes) | ~86 arquivos `.kt` em `composeApp/src/{commonMain,androidMain,wasmJsMain,commonTest}` (ver lista completa via `grep -rl "kmp.lab"`) | Rename mecânico de `package`/imports; sem mudança de lógica |
| `AndroidManifest.xml` | sem referência textual ao package (usa `${applicationId}` implícito para o pacote base da activity `.MainActivity`) | — | sem mudança de texto necessária | `composeApp/src/androidMain/AndroidManifest.xml` | Nenhum — resolve automaticamente pelo novo `namespace` |
| `FileProvider` authority | `${applicationId}.fileprovider` (já dinâmico) | `${applicationId}.fileprovider` (padrão AndroidX) | sem mudança de texto — segue `applicationId` automaticamente | `AndroidManifest.xml` + `AndroidAppUpdateChecker.android.kt` (usa `context.packageName` para montar a URI, também dinâmico) | Nenhum — já correto, nenhuma referência hardcoded ao package antigo |
| `file_paths.xml` (paths do FileProvider) | `<cache-path name="updates" path="updates/" />`, sem referência a package | — | sem mudança | `composeApp/src/androidMain/res/xml/file_paths.xml` | Nenhum |
| Deep links | Nenhum declarado (só `intent-filter` de `MAIN`/`LAUNCHER`) | MSAL usa `msauth://` scheme (ver linha MSAL abaixo), não é deep link de conteúdo | Nenhum a criar nesta fase | `AndroidManifest.xml` | Nenhum |
| Redirect URI MSAL Android | Documentado (não implementado) como `msauth://br.com.leorvergani.escalaici.kmp.lab/CNEvyhyc8lYTPFcNPDJzzJe1XyI%3D` — **nunca cadastrado no Entra** | `msauth://br.com.leorvergani.escalasoc/<hash-do-EscalaSOC>` (app registration próprio) | `msauth://br.com.leorvergani.escalaici/<ANDROID_SIGNATURE_HASH_DEBUG_OU_RELEASE>` — hash a recalcular após troca de `applicationId` (seção 5) | `docs/PENDENCIAS-EXTERNAS.md`, `docs/spec/46-...md` (atualizados nesta fase); futuramente `auth_config_single_account.json` (ainda não existe) | Bloqueado externamente até cadastro humano no Entra — nenhuma mudança de código pendente além dos docs |
| Firebase App (projeto) | `escalaici` (compartilhado com o Dashboard), sem `google-services.json`, acesso via `FirestoreRestGateway` (REST, sem SDK nativo) | Não usa Firestore (usa OneDrive/Dropbox) | sem mudança — projeto Firebase é identidade de infraestrutura, não de app cliente | `composeApp/src/commonMain/.../source/FirestoreRestGateway.kt`, `docs/ADR-FIREBASE-AUTH.md` | Nenhum — `applicationId` do cliente não precisa ser cadastrado no Firebase quando o acesso é via REST puro (sem SDK/`google-services.json`) |
| MSAL (client_id/tenant_id) | `client_id e5b5154d-e65e-4605-b221-73d7ee570580`, `tenant_id d2d23346-e737-4cac-96ec-fb25e7889f01` (mesmo app registration do EscalaSOC, sem plataforma Android própria do lab cadastrada) | mesmo `client_id`/`tenant_id`, plataforma Android já cadastrada | sem mudança — reaproveita o mesmo app registration; só a plataforma Android (package+hash) muda | `docs/PENDENCIAS-EXTERNAS.md`, spec 46 | Nenhum cadastro humano ainda feito para o lab — nada a desfazer |
| Assinatura debug/release | Única config (`signingConfigs.lab`), keystore `escalaici-kmp-lab.jks`, alias `escalaici-kmp-lab`, mesma chave para os dois build types | Keystore próprio do EscalaSOC, não auditado a fundo (fora do escopo, não copiar) | Ver seção 5 — decisão: manter uma única keystore própria do Escala ICI, renomeada/gerada de acordo com a nova identidade | `keystore.properties` (fora do Git), `composeApp/build.gradle.kts` (`signingConfigs`) | Ver seção 5 |
| Atualização de APK (`version.json`) | Campos próprios `kmp*` (`kmpVersionCode`, `kmpVersionName`, `kmpApkUrl`, `kmpChangelog`) convivendo no mesmo `version.json` do EscalaSOC, nomes escolhidos para nunca colidir com os campos legados (`versionCode`/`versionName`/`apkUrl`/`changelog`) | Campos legados (`versionCode`, `versionName`, `apkUrl`, `changelog`) | sem mudança de esquema — só o valor de `kmpApkUrl`/`kmpVersionCode`/`kmpVersionName`/`kmpChangelog` avança | `AppUpdateModels.kt` (`AppUpdateConfig.MANIFEST_URL`), `/home/lvergani/Downloads/dropbox_update_scripts/version.json` (fora do Git) | Nenhum — o parser usa `optString`/`optInt`, ignora chaves desconhecidas |
| Nome do APK gerado localmente | `EscalaICI-latest.apk` (fixo, sobrescrito a cada release, para não mudar o link do Dropbox) | `EscalaSOC-latest.apk` | sem mudança | `/home/lvergani/Downloads/dropbox_update_scripts/gerar_update_escalaici_local.sh` (fora do Git, não editado nesta fase — só documentado como pendência textual, ver seção 8) | Baixo — nome do arquivo não deriva do `applicationId` |
| `versionCode` / `versionName` | `13` / `0.6.3` | `31` / (não auditado) | `14` / `0.7.0` (ver seção 6) | `composeApp/build.gradle.kts`, `AppVersion.kt` (duplicado manualmente, sem BuildConfig cross-target) | Nenhum risco — incremento simples, sem redução |
| Web/PWA — identidade | `name`/`short_name` "Escala ICI", `start_url: "."`, `scope: "."`, sem referência a `.lab`/`KMP`/`mock` no manifest | n/a | sem mudança necessária no manifest — já correto | `composeApp/src/wasmJsMain/resources/manifest.json` | Nenhum |
| Textos visíveis com `lab`/`mock`/`KMP` | Nenhum encontrado na UI (`ui/*.kt`) além de nomes de classe internos (`EscalaIciLabApp`, `LabColors`, `LabTheme`, `LabCard`, `LabAlerts`, `LabWorkbookParser` — identificadores de código, não texto visível ao usuário) | — | Renomear identificadores de código junto com o package (consistência), sem prometer que "Lab" no nome de classe é visível ao usuário — nunca foi | arquivos citados na matriz do rename (seção 3 acima) | Nenhum funcional — só cosmético/consistência de nome de classe |
| Dropbox `REDIRECT_URI` (Web, leitura de escala) | `http://localhost:8080/dropbox-callback.html`, App Key `5by0pkzt2bgx95g`, escopo `sharing.read` — já cadastrado e confirmado funcionando (2026-07-11) | Fluxo ADM do EscalaSOC usa outro escopo (`files.content.*`), mesmo App Key | sem mudança — não depende de `applicationId` Android (é fluxo Web) | `DropboxAuthConfig.kt` | Nenhum |

## 4. O que NÃO foi alterado nesta fase

- Nome do repositório Git (`EscalaICI-KMP-Lab`) — fora de escopo desta fase.
- Nome visível `Escala ICI` — já correto em Android e Web, sem mudança.
- Login MSAL (continua mock — `LoginGateScreen.kt` sem alteração de lógica).
- Firebase Auth / Custom Token (não implementado).
- Regras do Firestore (`firebase/firestore.rules`, prazo 2026-08-04 — fora de escopo).
- `client_id`/`tenant_id` do Entra (reaproveitados, sem mudança).
- Qualquer arquivo do repositório `EscalaSOC` ou `Dashboard` (somente consulta).

## 5. Assinatura — decisão

O keystore existente (`escalaici-kmp-lab.jks`, alias `escalaici-kmp-lab`) foi
gerado especificamente para a identidade de laboratório (nome do alias e do
arquivo espelham `.kmp.lab`) e **nunca foi usado para registrar nada em um
provedor externo** (nenhuma plataforma Android cadastrada no Entra até
agora — seção 2). Não há keystore "específico do Escala ICI oficial"
preexistente em nenhum lugar consultado (nem no repositório, nem em
`/home/lvergani/Downloads/dropbox_update_scripts/keystore/`, que só contém
`escalasoc-update.jks`, do outro app).

**Decisão:** manter o keystore físico atual como a chave de assinatura
(reduz risco — trocar de chave sem necessidade adicionaria uma segunda causa
de descontinuidade, além da troca de `applicationId`), mas isso é uma decisão
técnica reversível que cabe a Leonardo confirmar antes do primeiro cadastro
externo (Entra). Documentado como decisão humana pendente de confirmação em
`docs/setup/00-CHECKLIST-CONFIGURACAO-AMANHA.md`, não travada em código: o
`keystore.properties` real (fora do Git) continua apontando para o arquivo
`.jks` físico já existente; só o `applicationId` referenciado nos comandos de
hash muda.

Nenhuma senha, alias sensível ou caminho de keystore real foi impresso nesta
auditoria ou em qualquer arquivo versionado.

### 5.1 Redirect URI Android (MSAL) — formato com placeholders

Comandos para gerar os hashes reais: `scripts/print-signing-info.sh` (ver
seção 5.2). Nenhum client ID/tenant ID fictício é usado abaixo — só
placeholders explícitos.

| Placeholder | Preenchido com |
|---|---|
| `<ANDROID_PACKAGE_NAME>` | `br.com.leorvergani.escalaici` (fixo, já aplicado) |
| `<ANDROID_SIGNATURE_HASH_DEBUG>` | saída "Signature hash Base64" de `scripts/print-signing-info.sh` rodado contra a keystore de **debug** (`~/.android/debug.keystore`, alias `androiddebugkey`) |
| `<ANDROID_SIGNATURE_HASH_RELEASE>` | saída "Signature hash Base64" de `scripts/print-signing-info.sh` rodado contra `escalaici-kmp-lab.jks` (keystore de release/debug compartilhada deste projeto — ver seção 5), usando `keystore.properties` real (fora do Git) |
| `<ANDROID_REDIRECT_URI_DEBUG>` | `msauth://<ANDROID_PACKAGE_NAME>/<ANDROID_SIGNATURE_HASH_DEBUG>` (URL-encode o `=` final como `%3D` se o portal do Entra pedir o valor já pronto em vez de gerar automaticamente) |
| `<ANDROID_REDIRECT_URI_RELEASE>` | `msauth://<ANDROID_PACKAGE_NAME>/<ANDROID_SIGNATURE_HASH_RELEASE>` |

Como este projeto usa a **mesma keystore para debug e release**
(`signingConfigs["lab"]` aplicado aos dois `buildTypes` — ver seção 5),
`<ANDROID_SIGNATURE_HASH_DEBUG>` (calculado a partir do `debug.keystore`
padrão do Android SDK) e `<ANDROID_SIGNATURE_HASH_RELEASE>` (calculado a
partir de `escalaici-kmp-lab.jks`) **serão valores diferentes** — o
`debug.keystore` é uma chave separada, gerada automaticamente pelo
Android Studio/SDK, nunca a mesma da keystore de release deste projeto.
Cadastre as duas plataformas Android no Entra (uma redirect URI para cada
hash) para que o app funcione tanto rodando localmente via `adb install`
quanto instalado a partir do APK de release.

### 5.2 Comandos para obter os hashes (sem expor senha)

```bash
# Debug (usa debug.keystore padrão, senha "android" conhecida publicamente):
./scripts/print-signing-info.sh

# Release (keystore.properties real, fora do Git, tem os valores):
KEYSTORE_PATH=/caminho/para/escalaici-kmp-lab.jks \
KEYSTORE_ALIAS=escalaici-kmp-lab \
KEYSTORE_PASSWORD='<preencha na hora, não versione>' \
  ./scripts/print-signing-info.sh
```

Equivalente manual com `keytool`/`openssl`, caso prefira não usar o
script (mesma lógica, sem imprimir a senha em nenhum log persistente):

```bash
# SHA-1 / SHA-256:
keytool -list -v -keystore <arquivo.jks> -alias <alias> -storepass "$KEYSTORE_PASSWORD"

# Signature hash Base64 (MSAL):
keytool -exportcert -alias <alias> -keystore <arquivo.jks> -storepass "$KEYSTORE_PASSWORD" \
  | openssl sha1 -binary | openssl base64
```

## 6. Versão

- Anterior: `versionCode 13` / `versionName 0.6.3`.
- Nova: `versionCode 14` / `versionName 0.7.0` — incremento minor (não
  patch), porque esta fase muda `applicationId`/`namespace`/todos os packages
  Kotlin, o que é uma mudança estrutural maior que os incrementos patch
  anteriores (FASE 12b/12b-2/12b-3, refletidos no `0.6.3` atual).
- Nome de release: `EscalaICI` (mantido).
- **Aplicado** em `composeApp/build.gradle.kts` (`versionCode`/`versionName`)
  e `AppVersion.kt` (`CODE`/`LABEL`), mantendo as duas fontes em sincronia
  manual como já era o padrão do projeto.

## 7. Critérios de aceite desta spec

1. Matriz cobre todos os campos pedidos (nome visível, `applicationId`,
   `namespace`, packages Kotlin, assinatura debug/release, redirect URI
   Android, identidade Web/PWA, versão, localização do APK, formato do
   `version.json`, procedimento de atualização futura, arquivos externos a
   preencher depois).
2. Decisão do identificador documentada com evidência, sem inventar terceiro
   identificador.
3. Nenhuma senha ou segredo aparece neste documento.
4. Nenhuma recomendação desliga/altera `EscalaSOC` ou `Dashboard`.
