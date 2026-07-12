# Escala ICI KMP Lab

Projeto oficial em Kotlin Multiplatform (Android + Web/Wasm) do Escala ICI,
rodando em paralelo ao app Android atual (`EscalaSOC`, escrito em Kotlin puro)
até substituí-lo por completo. O nome do repositório/pacote (`EscalaICI-KMP-Lab`,
`br.com.leorvergani.escalaici.kmp.lab`) é só histórico — o app em si já é
tratado como caminho oficial de migração, não como experimento descartável.

Ver `docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md` (repositório `EscalaSOC`) para
o plano de integrações reais em andamento, e
**`docs/PENDENCIAS-EXTERNAS.md`** para o passo a passo de toda ação fora do
código (cadastros no Dropbox/Azure) que só o dono das contas pode fazer.

Ver também `docs/spec/34-ESCALAICI-UNIVERSAL-SETORES-E-TIPOS-DE-ESCALA.md`
(repositório `EscalaSOC`) para a spec (FASE 12a, ainda não implementada) de
como generalizar o app para múltiplos setores do ICI além de COSI/SOC (ex.:
N1 Service Desk) e tipos de escala configuráveis por equipe.

## Escopo

- Target Android.
- Target Web/Wasm.
- UI completa em Compose Multiplatform, com paridade visual total com o app
  Android atual (série `FASE 10.x`).
- Download real da escala via Dropbox (Android real; Web depende de OAuth,
  ver seção "Fora do escopo" abaixo).
- Importação XLS real (Apache POI no Android, SheetJS no Web/Wasm) usando o
  parser compartilhado `LabWorkbookParser.kt`.
- PWA instalável: `manifest.json`, service worker e ícone próprio.
- Login por colaborador de teste enquanto o MSAL real (FASE 11.3) não é
  implementado — único caminho de entrada hoje, ver seção
  "Validação da FASE 11.1" abaixo.

## Fora do escopo (por enquanto)

- Firebase.
- Login MSAL real (planejado, FASE 11.3).
- Parser 100% fiel ao oficial do app Android principal (`ScaleWorkbookParser.kt`)
  — o parser deste projeto é próprio, refinamento previsto na FASE 11.2.
- App Android principal (`EscalaSOC`) não é alterado por este projeto.
- iOS compilável. iOS fica para quando houver um Mac disponível.

## Comandos

Android:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:assembleDebug
```

Instalar no emulador:

```bash
~/Android/Sdk/platform-tools/adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
~/Android/Sdk/platform-tools/adb shell am start -n br.com.leorvergani.escalaici.kmp.lab/.MainActivity
```

Web/Wasm build:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:wasmJsBrowserDistribution
```

Web/Wasm dev server:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

URL esperada do dev server:

```text
http://localhost:8080/
```

## Checklist de apresentacao (FASE 9c-7)

Checklist objetivo para qualquer pessoa rodar a demo sem precisar reler todo o
historico de validacoes do README. Segue a spec
`docs/spec/32-KMP-LAB-VISUAL-XLS-WEB-APRESENTACAO.md` (repositorio Android
principal).

### Passo a passo da demo

**Android:**

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:assembleDebug
~/Android/Sdk/platform-tools/adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
~/Android/Sdk/platform-tools/adb shell am start -n br.com.leorvergani.escalaici.kmp.lab/.MainActivity
```

**Web/Wasm:**

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Abrir `http://localhost:8080/` no navegador.

**Roteiro sugerido, em qualquer uma das duas plataformas:**

1. Abrir o app na aba `Hoje` — conferir cabecalho `Escala ICI`, usuario/time
   mock, proximo turno, resumo da semana e pausa.
2. Ir para a aba `Escala` — conferir calendario mensal, navegacao entre meses
   e o card `Quem trabalha nesse dia`.
3. Ir para a aba `Importar` — usar `Selecionar XLS` e escolher uma planilha
   real (ex.: `Escala-SOC-Controle-Julho.xls`); conferir pre-visualizacao com
   abas `Escala`/`Escalistas`, dias lidos e colaboradores encontrados.
4. Trocar o escalista selecionado na pre-visualizacao e conferir que a leitura
   recalcula.
5. Tocar `Usar dados importados` e voltar para `Hoje`/`Escala` para confirmar
   que os dados exibidos agora vem do XLS importado (nao mais do mock).
6. Ir para a aba `Alertas` — conferir hero, contadores por severidade, filtros
   e cards de alerta (descanso < 11h, regra 6x1, inconsistencias, dias
   indefinidos).
7. Ir para a aba `Perfil` — conferir os cards de identidade, resumo,
   administracao da escala, conta corporativa, modo demo, trocas,
   notificacoes, pausa, armazenamento local e aplicativo.
8. Se quiser, usar `Voltar para mock` na aba `Importar` para retornar ao
   estado inicial mockado.

### Criterios de aceite da spec 32 §12

- [x] Android do laboratorio compila.
- [x] Web/Wasm do laboratorio compila.
- [x] Web/Wasm abre no navegador local (`http://localhost:8080/`).
- [x] UI visualmente alinhada ao app Android atual (tema `Soc`, cards,
      navegacao inferior).
- [x] UI nao parece apenas uma tela tecnica de teste.
- [x] Navegacao inferior e telas principais (`Hoje`, `Escala`, `Importar`,
      `Alertas`, `Perfil`) existem no laboratorio.
- [x] Mock inicial funciona sem nenhum arquivo importado.
- [x] Importacao XLS funciona em ambas as plataformas do laboratorio
      (Android via Apache POI, Web/Wasm via SheetJS).
- [x] Importacao XLS funciona tambem no Web/Wasm, entao o criterio de
      fallback/limitacao documentada nao se aplica; a unica limitacao (SheetJS
      via CDN) esta descrita na secao "Limites conhecidos" abaixo.
- [x] Nenhum arquivo do app Android principal (`EscalaSOC`) foi alterado.
- [x] Nenhum arquivo do dashboard React foi alterado.
- [x] README do laboratorio explica como rodar a apresentacao (esta secao).

### Limites conhecidos

- Parser XLS do laboratorio e experimental: le apenas as abas `Escala` e
  `Escalistas` com a estrutura ja mapeada; nao substitui nem reaproveita o
  parser oficial do app Android.
- No Web/Wasm, a biblioteca SheetJS e carregada por CDN no `index.html`
  (nao empacotada via Gradle/NPM local).
- PWA continua basico: `manifest.json`, service worker simples e icone
  placeholder SVG.
- Build usa flags temporarias de compatibilidade AGP 9 com KMP
  (`android.builtInKotlin=false`, `android.newDsl=false`).
- Bundle Web/Wasm gera avisos de tamanho por incluir Compose/Skiko; aceitavel
  para demo local, a revisitar antes de qualquer PWA publico.
- Modelos puros da FASE 9c (`SchedulePeriod`, `ScheduleAssignment`,
  `OnCallPeriod`, `OnCallAssignment`, `ShiftSwapRequest`, `ImportJob`,
  `SourceFileRecord`) existem em `commonMain` mas ainda nao estao conectados
  a UI da demo — so aos mocks.
- Fora de escopo nesta POC: login MSAL real, Firebase real, sync global,
  cache oficial do app, update APK/Dropbox/OneDrive, publicacao em loja,
  dashboard React, troca real de escala, notificacoes reais, iOS compilavel.

## Estrutura

```text
composeApp/
  src/commonMain/
    kotlin/.../model/
    kotlin/.../ui/
  src/androidMain/
    AndroidManifest.xml
    kotlin/.../MainActivity.kt
  src/wasmJsMain/
    kotlin/.../Main.kt
    resources/index.html
    resources/manifest.json
    resources/service-worker.js
```

## Observacao

Este laboratorio foi criado fora do repositorio Android principal para reduzir risco. O app principal `EscalaSOC` nao deve ser alterado por fases deste laboratorio.

## Validação da FASE 12a-2 — corrige crash (OutOfMemoryError) no download de atualização

**Bug reportado pelo usuário**: ao testar a v0.6.1 no celular real, tocar
"Atualizar aplicativo" detectava a atualização, mas o app **fechava
sozinho** na hora de baixar — nunca chegava a instalar.

**Causa raiz confirmada** (reproduzida no emulador com logcat, não só
teoria): `AppUpdateChecker.android.kt#downloadApk()` chamava
`downloadBytes()`, que usa `response.body(): ByteArray` do Ktor — isso
carrega o **APK inteiro (66MB) de uma vez só na memória** antes de gravar
em disco. Com o Compose Multiplatform/Skia já usando boa parte do heap do
app, essa alocação única estourava o limite e lançava
`OutOfMemoryError` — que **não era capturado** pelo `catch (error:
Exception)` do `checkAndInstall()` (`OutOfMemoryError` é `Error`, não
`Exception`), derrubando o processo inteiro sem nenhuma mensagem de erro.
Stack trace confirmando (emulador, build de teste com o código antigo):

```
java.lang.OutOfMemoryError: Failed to allocate a 66299920 byte allocation
  with 25165824 free bytes and 58MB until OOM ...
  at kotlinx.io.SourcesKt.readByteArrayImpl(Sources.kt:268)
  at io.ktor.client.call.SavedCallKt.save(SavedCall.kt:38)
  at io.ktor.client.statement.HttpStatement.fetchResponse(HttpStatement.kt:166)
```

O app oficial (`EscalaSOC/AppUpdateManager.kt`, só leitura) nunca teve esse
problema porque **grava em streaming direto no arquivo**
(`input.copyTo(output)`, nunca monta um `ByteArray` do arquivo inteiro) e
usa `catch (Throwable)`, não `catch (Exception)`.

**Correção** (mesmo padrão do app oficial, agora também aqui):

- `platform/RemoteBytesDownloader.android.kt`: nova função
  `downloadToFile(url, destination)` — baixa via Ktor em streaming
  (`response.bodyAsChannel().copyTo(outputStream)`), nunca guarda o
  arquivo inteiro em memória. `downloadBytes()` (usado só para o
  manifesto/planilha, arquivos pequenos) não muda. Timeout do cliente
  compartilhado subiu de 15s para 60s (cobre também o download do APK).
- `platform/AppUpdateChecker.android.kt`: `downloadApk()` passa a chamar
  `downloadToFile()` em vez de `downloadBytes()` + `writeBytes()`; o
  `catch (error: Exception)` virou `catch (error: Throwable)` (mantendo o
  `catch (CancellationException)` antes, para não engolir cancelamento de
  coroutine) — qualquer falha (incluindo um futuro `OutOfMemoryError` em
  outro ponto) agora sempre mostra a mensagem de erro amigável em vez de
  derrubar o app.
- `model/AppVersion.kt`: **bug adicional encontrado nesta investigação** —
  `CODE`/`LABEL` (mantidos manualmente, já que KMP não gera `BuildConfig`
  em `commonMain`) tinham ficado em `10`/`0.6.0` desde a FASE 11.2d;
  a FASE 12a-1 esqueceu de atualizar este arquivo ao subir o
  `versionCode` do Gradle para `11`. Corrigido para `12`/`0.6.2` junto com
  esta fase — o app já mostrava "Versão atual: 0.6.0" errado no Perfil
  mesmo depois de instalada a v0.6.1.

**Testado (reprodução real, não só build limpo):**

- Emulador Android, logcat ao vivo: build de teste com o código **antigo**
  (versionCode rebaixado só para o teste, revertido depois) reproduziu o
  crash exato acima ao tocar "Atualizar aplicativo" contra o `version.json`
  real de produção (que já apontava para `kmpVersionCode` mais novo).
- Mesmo teste com o código **corrigido**: processo nunca morre, download
  completa, e o diálogo real do Android "Update this app?" abre mostrando
  o changelog correto — confirma o fluxo completo (download → FileProvider
  → instalador) funcionando de ponta a ponta.
- `testDebugUnitTest` (33 testes) e `compileKotlinWasmJs` seguem passando
  — a mudança é só Android (`androidMain`), Web/Wasm nunca chama esse
  caminho (`AppUpdateChecker.wasmJs.kt` retorna `NotSupported`).

`versionCode`/`versionName`: `11`/`0.6.1` → `12`/`0.6.2` (PATCH — correção
de crash real, sem mudança de funcionalidade). APK de release gerado,
assinado com a mesma chave de sempre (confirmado via `apksigner verify`),
copiado para `EscalaICI-latest.apk` via `gerar_update_escalaici_local.sh`.
`version.json` local atualizado (`kmpVersionCode`/`kmpVersionName`/
`kmpChangelog`) — falta só o usuário subir os dois arquivos manualmente
pro Dropbox (ver `docs/PENDENCIAS-EXTERNAS.md`).

## Validação da FASE 12b — modelos universais de organização e escala

Primeira fase de código da série `FASE 12.x` (generalizar o Escala ICI para
múltiplos setores do ICI, não só COSI/SOC — ver
`EscalaSOC/docs/spec/34-ESCALAICI-UNIVERSAL-SETORES-E-TIPOS-DE-ESCALA.md`).
Só modelos puros em `commonMain`, aditivos — nenhum modelo/mock/parser/tela
existente foi alterado ou removido.

**Novo arquivo `model/UniversalOrgModels.kt`:**

- `Organization`, `OrgUnit` (+ `OrgUnitType`: `PRESIDENCY`, `DIRECTORATE`,
  `MANAGEMENT`, `COORDINATION`, `SECTOR`, `DEPARTMENT`, `TEAM_GROUP`) — árvore
  de diretorias/gerências/coordenações/setores do ICI, com `parentId` para
  representar qualquer profundidade (ex.: GEDSI → COSI). `TEAM_GROUP` permite
  representar um agrupamento de equipe (ex.: "Analistas de SOC") dentro dessa
  árvore sem precisar alterar o `Team` já existente.
- `Role` — papel/função dentro de uma equipe ou setor.
- `MemberTeamMembership` — vínculo membro↔equipe com período de vigência e
  `isPrimary`; resolve o problema de uma pessoa ficar presa a um `teamId`
  fixo em `Member` (que continua existindo, sem mudança).
- `ScheduleProfile` (+ `ScheduleProfileType`: `ROTATING_6X1`, `MATRIX_6X1`,
  `TWELVE_BY_THIRTY_SIX`, `BUSINESS_HOURS`, `ON_CALL_INTERVAL`, `CUSTOM`;
  `SchedulePeriodMode`: `DAY_26_TO_25`, `MONTHLY`, `WEEKLY`, `FIXED_RANGE`,
  `CONTINUOUS`) — tipo estrutural de escala que uma equipe usa.
- `ActivityCode` (+ `ActivityCodeType`) — código de atividade configurável
  por equipe (ex.: `F`, `X`, `AUS`, `M1`-`M4`, `E`, `G`, `T` da equipe N1),
  com `countsAsWork` para o app saber se conta como trabalho sem precisar
  entender o significado específico do código.
- `BusinessHoursRule` (segunda a sexta, horário fixo + almoço) e
  `TwelveByThirtySixRule` (12h trabalhadas / 36h de folga).

**Mocks novos em `MockSchedule.kt`** (não conectados à UI): organização ICI,
`OrgUnit` GEDSI/COSI/N1 (N1 sem `parentId` — setor irmão, não subordinado a
COSI), `Team` SOC/N1, `Role`/`MemberTeamMembership` mínimos, os 11 códigos
reais da equipe N1 (`F`, `X`, `AUS`, `M`, `M1`-`M4`, `E`, `G`, `T`, extraídos
da aba `Máscara` de `Escalas Equipe N1.xls`, spec 34 §3.2) e 3
`ScheduleProfile` (SOC 6x1 por turnos, N1 6x1 por códigos, Administrativo
segunda a sexta) + 1 `BusinessHoursRule` de exemplo.

**Testes novos** (`UniversalOrgModelsTest.kt`, 5 testes): `M1`-`M4` contam
como trabalho; `F`/`X` não contam; `Organization`/`OrgUnit`/`Team`
representam ICI → GEDSI → COSI → SOC; N1 não tem relação hierárquica com
COSI; `MemberTeamMembership` liga membro à equipe via vínculo.

**Validado:**

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinWasmJs
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Os dois targets compilam, os 5 testes novos passam (33 no total, 0 falhas),
e o build completo (APK debug + distribuição Web/Wasm) passa sem erro.

`versionCode`/`versionName`: mantidos (`11`/`0.6.1`) — fase só de modelos
puros, sem nenhuma mudança de comportamento visível no app, regra combinada
de só subir versão quando há entrega testável de verdade.

## Validacao da FASE 11.2e — release local padronizado + upload sempre manual

Padroniza o processo de gerar o APK/`version.json` do EscalaICI a cada
release, e deixa explícito por escrito (para qualquer IA/sessão futura)
que o upload para o Dropbox **nunca** é automático para este app.

**Pasta oficial de release manual**:
`/home/lvergani/Downloads/dropbox_update_scripts` — a mesma pasta onde o
usuário já publica o `EscalaSOC-latest.apk`/`version.json` do app oficial
(via scripts próprios que chamam a API do Dropbox). Os dois apps
compartilham o mesmo `version.json` (campos `kmp*` só para o EscalaICI,
FASE 11.2d) mas **não** compartilham processo de publicação.

- `EscalaICI-latest.apk`: nome oficial do APK do EscalaICI nessa pasta
  (mesmo nome do arquivo já referenciado em `kmpApkUrl` no `version.json`
  real).
- Novo script `gerar_update_escalaici_local.sh` (na pasta, fora deste
  repositório): localiza o APK de release já gerado pelo Gradle, copia
  para `EscalaICI-latest.apk`, confere os campos `kmp*` do `version.json`
  local e imprime instruções de upload manual — **nunca chama a API do
  Dropbox**. Os scripts antigos (`publicar_update.sh`/`.bat`/
  `upload_update_dropbox.py`) continuam existindo só para o EscalaSOC;
  ganharam avisos no topo deixando claro que não servem para o EscalaICI.

**Procedimento de release (repetir a cada versão nova)**:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew clean :composeApp:assembleRelease
./gradlew :composeApp:testDebugUnitTest :composeApp:wasmJsBrowserDistribution
cd /home/lvergani/Downloads/dropbox_update_scripts
./gerar_update_escalaici_local.sh
# editar version.json manualmente: kmpVersionCode (sempre maior que o
# anterior), kmpVersionName, kmpChangelog — kmpApkUrl só muda se o link
# do Dropbox mudar
# depois: usuário sobe EscalaICI-latest.apk + version.json manualmente
```

**Requisito não-negociável para o botão "Atualizar aplicativo" continuar
funcionando**: toda build (debug e release, sempre) precisa usar a
**mesma** keystore/certificado — `escalaici-kmp-lab.jks`, já estabelecida
desde a FASE 11.0c e confirmada nesta fase via
`apksigner verify --print-certs` (`CN=Escala ICI KMP Lab, OU=ICI, O=ICI,
C=BR`, mesmo certificado de sempre). Trocar a chave a qualquer momento
quebra a atualização in-place para quem já tem o app instalado (o Android
recusa instalar um APK assinado com certificado diferente do já
instalado — só resolve desinstalando o app antes, perdendo dados locais).
Não usar a keystore do EscalaSOC aqui — são apps diferentes
(`applicationId` diferente), cada um com sua própria chave estável.

**Testado**: `./gradlew clean :composeApp:assembleRelease` (build limpo,
sem cache antigo) + `testDebugUnitTest` (28 testes, 0 falhas) +
`wasmJsBrowserDistribution`. `apksigner verify` confirmou a mesma
assinatura de sempre. APK reinstalado **por cima** da build debug já
presente no emulador sem precisar desinstalar (confirma que debug e
release já usam a mesma chave) — app abriu normalmente. `version.json`
local atualizado (`kmpChangelog` mencionando esta fase).

`versionCode`/`versionName`: mantidos em `10`/`0.6.0` — esta fase é só
padronização do processo de release, sem mudança funcional no app (regra
combinada: só sobe versão quando há mudança de código real).

## Validacao da FASE 11.2d — atualização real do app via Dropbox

Botão "Atualizar aplicativo" (Perfil, card "Aplicativo") passa de
desabilitado para real: verifica, baixa e instala uma nova versão do APK,
igual ao app Android oficial.

**Reaproveita o mesmo `version.json`** que o app oficial já usa
(`DropboxCloudConfig.APP_UPDATE_MANIFEST_URL`, `EscalaSOC`) — não é um
arquivo novo. Confirmado por leitura do código real: o parser oficial
(`AppUpdateManager.fetchManifest()`) usa `org.json.JSONObject` manual com
`optInt`/`optString`, que ignora em silêncio qualquer chave desconhecida
(o manifesto já convive hoje com um campo extra `releaseNotes` que o app
oficial nunca lê) — então dá para adicionar campos novos só para o KMP
sem quebrar o app oficial, desde que não reusem os nomes que ele lê
(`versionCode`/`versionName`/`apkUrl`/`changelog`). Campos novos, só para
este app: `kmpVersionCode` (Int), `kmpVersionName` (String), `kmpApkUrl`
(String), `kmpChangelog` (String, opcional).

- `model/AppUpdateModels.kt`: `AppUpdateResult` (`UpToDate`,
  `InstallStarted`, `PermissionRequired`, `NotSupported`, `Failure`) +
  `AppUpdateConfig.MANIFEST_URL` (mesma URL do app real).
- `model/AppVersion.kt`: novo `CODE` (Int), ao lado do `LABEL` já
  existente — usado na comparação de versão (igual ao
  `BuildConfig.VERSION_CODE` do app real; KMP não gera BuildConfig em
  `commonMain` sem plugin adicional, por isso mantido manualmente).
- `platform/AppUpdateChecker.kt` (`expect`): Android baixa o manifesto
  (reaproveita `downloadBytes`), compara `kmpVersionCode` com
  `AppVersion.CODE`, checa permissão "instalar apps desconhecidos"
  (Android 8+, abre as configurações do sistema se faltar), baixa o APK
  para `cacheDir/updates/EscalaICI-KMP-latest.apk` e abre o instalador via
  `FileProvider` + `ACTION_VIEW` — **porte fiel** do
  `AppUpdateManager.kt`/`checkDownloadAndInstall()` real (mesmo algoritmo,
  mesmas etapas, mesma mensagem de erro genérica). Web retorna
  `NotSupported` (instalar `.apk` não existe no navegador; o app oficial
  também não tem esse recurso fora do Android).
- `AndroidManifest.xml`: nova permissão `REQUEST_INSTALL_PACKAGES` +
  `<provider>` `FileProvider` com `android:authorities="${applicationId}.fileprovider"`
  (resolve para `br.com.leorvergani.escalaici.kmp.lab.fileprovider`,
  confirmado no manifest final compilado) + novo
  `res/xml/file_paths.xml` (`<cache-path name="updates" path="updates/" />`,
  mesmo padrão do app real).
- `ProfileTab.kt`: botão real com texto de status evoluindo exatamente
  como no app oficial — "Verificando atualização..." →
  "Você já está usando a versão mais recente." /
  "Nova versão disponível: v`<versão>`. `<changelog>`" /
  "Permita instalar atualizações deste app e tente novamente." / mensagem
  de erro.

**Testado no emulador** (rede real, `version.json` real de produção): o
manifesto de hoje ainda não tem os campos `kmp*` (pendência externa, ver
abaixo) — o app buscou o arquivo real, não achou os campos, e mostrou
corretamente **"Você já está usando a versão mais recente."** (o caminho
seguro esperado quando `kmpVersionCode` não está presente, já que
`optInt` sem o campo devolve `0`, sempre `<= AppVersion.CODE`). Confirma
que a chamada de rede real, o parser e a UI funcionam ponta a ponta; falta
só o usuário adicionar os campos `kmp*` no `version.json` (que já hospeda
o app oficial) para validar o caminho completo de baixar+instalar.

`versionCode`/`versionName`: `9`/`0.5.0` → `10`/`0.6.0`. APK de release
gerado e copiado para `~/Downloads/EscalaICI-KMP-Lab-latest.apk`.

## Validacao da FASE 11.2c — importação real de Plantão + mais correções de fidelidade

Testando o resultado real (Dropbox já configurado e funcionando pelo
usuário), mais divergências com o app Android real foram apontadas:

- **Importação de Plantão ausente**: no app real, o botão para importar o
  relatório de plantão (COSI) fica **dentro da tela Plantão** (chip
  "Plantão" no cabeçalho de qualquer aba → abre a tela → botão "Importar
  relatório"), não na aba Importar — que só importa a escala 6x1. O lab
  tinha o chip do cabeçalho, mas a tela em si não tinha nenhum jeito de
  importar um relatório real, só mock fixo.
  - Novo `model/PlantaoWorkbookParser.kt`: parser próprio (código novo, não
    copiado), regras portadas **exatamente** do parser oficial
    `PlantaoWorkbookParser.kt` (`EscalaSOC`, só leitura) — mesma busca de
    cabeçalho por colunas "plantonista"/"data início"/"data fim" (qualquer
    aba, normalizado sem acento/case/espaço), mesma regex de data+hora
    (`(\d{1,2})/(\d{1,2})/(\d{2,4})\s*-?\s*(\d{1,2}):(\d{2})`, aceita "-"
    opcional entre data e hora), mesmas 3 validações de linha (vazia →
    ignora silenciosamente; incompleta → aviso "Linha N: plantão
    incompleto ignorado."; fim ≤ início → aviso "Linha N: data final menor
    ou igual à inicial."), mesma ordenação (início, depois nome), e o
    mesmo erro exato quando nada é encontrado: "Não encontrei plantões no
    formato esperado: Plantonista Segurança, Data Inicio e Data Fim.".
  - `PlantaoScreen.kt`: reaproveita o mesmo `rememberWorkbookImportLauncher`
    já usado pela escala (seletor de arquivo real Android/Web); botão
    "Importar relatório"/"Importar outro relatório" no hero, com status
    "Nenhum relatório real importado — mostrando dados de exemplo." /
    "Relatório importado: `<arquivo>`" e card de erro amigável.
  - 7 testes novos (`PlantaoWorkbookParserTest.kt`) cobrindo cada regra
    acima com planilhas sintéticas.
  - Limitação conhecida documentada (igual ao parser de escala): sem
    atalho para células de data POI cruas.
- **"Hoje" não vinha da data real do dispositivo**: `nextShift`/`nextRest`
  eram sempre "o primeiro dia da lista importada", não a data real de
  hoje — então em qualquer aparelho o app sempre mostrava o mesmo dia,
  nunca o realmente atual. Novo `platform/CurrentDate.kt`
  (`expect fun todayLabDate()`, actuals `java.time.LocalDate.now()` no
  Android e `new Date()` via JS no Web/Wasm) usado para: `nextShift`/
  `nextRest` (primeiro turno de trabalho/descanso **a partir de hoje**,
  com fallback pro primeiro item se hoje não estiver nos dados), e seleção
  inicial da aba Escala (mostra o dia de hoje se ele existir na escala
  carregada, senão o próximo turno futuro). Consistente entre Hoje e
  Escala agora, já que as duas usam a mesma âncora de data real.
- **Resumo do período sem limite de ciclo**: `workedDays`/`restDays`/
  `totalHours` contavam **todos** os dias da lista, sem checar se batiam
  com o ciclo de pagamento do SOC (dia 26 de um mês até dia 25 do
  próximo). O app real não tem essa validação explícita (confiava que a
  leitura fixa de 30 linhas da aba Escala já era sempre um ciclo só) —
  aqui adicionamos como proteção extra: novo `periodDays` (privado, em
  `ScheduleSummary`) filtra os dias para o ciclo 26–25 ancorado em "hoje"
  real antes de contar. Não muda o resultado para o arquivo real de hoje
  (que já é exatamente um ciclo), só protege contra dados fora do ciclo no
  futuro.
- **Sugestões de pausa**: só havia 1 horário fixo + um botão "Outro
  horário" desabilitado. Real oferece 6 sugestões (a cada 30min dentro da
  janela permitida) — novo `ShiftType.pauseSuggestions()` +
  `ScheduleSummary.pauseSuggestions`, renderizado em 2 linhas de 3 chips.
- **Ícone do clima sem ícone na Web**: `WeatherChip` (Hoje) usava um
  emoji (`"☀️"` como `Text`) em vez de um `Icon` vetorial — Compose Web/Wasm
  (Skia) não inclui fonte de emoji colorida por padrão, então o emoji podia
  não renderizar no navegador (funcionava no Android, que tem fonte de
  emoji do sistema). Trocado para `Icons.Default.WbSunny`, igual ao
  `WeatherMiniCard` da aba Escala (que já usava `Icons.Default.Cloud`,
  vetorial, e por isso sempre funcionou nas duas plataformas).
- **Logo do cabeçalho ainda era o escudo do SOC com "S"**: a FASE 11.0b só
  trocou o ícone do launcher/PWA — o logo desenhado à mão dentro do
  cabeçalho (`ui/components/SocLogo.kt`, `LabShieldLogo`, usado em toda
  tela via `LabPremiumHeader`) continuava sendo um porte literal do
  escudo azul/roxo com um traço em "S" do app real. Como este app
  representa o ICI inteiro agora (não só o SOC), trocado por um mini
  calendário sem nenhuma letra (mesmo gradiente/paleta), mesmo motivo e
  mesmo estilo do ícone do launcher da FASE 11.0b.

**Testado:**

- `testDebugUnitTest`: 28 testes no total (21 + 7 novos do Plantão), 0
  falhas.
- Manual no emulador Android (data real do aparelho: 11/07/2026, sábado):
  - Logo do cabeçalho: calendário, sem "S", em todas as abas.
  - Clima: ícone de sol vetorial (não emoji) na Hoje.
  - "Próximo turno" mudou de 06/07 (primeiro dia da lista) para **12/07**
    (o próximo turno de trabalho real a partir de hoje, já que hoje
    11/07 é folga no mock) — confirma a âncora de data real funcionando.
  - Aba Escala: dia **11** (hoje real) selecionado por padrão, não mais o
    primeiro dia com turno.
  - Perfil: 6 chips de horário de pausa (09:00 a 11:30 para o turno
    Manhã) em vez de 1 fixo.
  - Plantão: botão "Importar relatório" visível e abre o seletor de
    arquivo real do sistema (Android `OpenDocument`) sem travar.

Validado:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
./gradlew :composeApp:wasmJsBrowserDistribution
~/Android/Sdk/platform-tools/adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

`versionCode`/`versionName`: `8`/`0.4.1` → `9`/`0.5.0` (MINOR — importação
real de Plantão passa a funcionar de ponta a ponta no Android/Web).

## Validacao da FASE 11.2b — fidelidade visual/funcional ao app Android real

O usuário revisou o resultado visual e apontou duas divergências reais com
o app Android oficial (`EscalaSOC`, comparado por leitura): **cards a
mais** que não existem no app real, e o **horário de pausa** não batendo.
Auditoria feita comparando tela a tela (`ProfileTab`↔`SettingsScreen`,
`ScheduleTab`↔`CalendarScreen`, `PlantaoScreen`↔`ui/plantao/PlantaoScreen.kt`,
etc., todos no app real, só leitura) confirmou 4 cards genuinamente extras
e 1 divergência real de cálculo — corrigidos:

**Cards extras removidos** (nenhum equivalente no app real):

- **Perfil** (`ProfileTab.kt`): card `"Migração KMP"` (meta-comentário
  sobre a própria migração — não existe nenhum card assim no
  `SettingsScreen.kt` real).
- **Escala** (`ScheduleTab.kt`): card `"Dia sem escala"` (mostrado quando
  nenhum dia está selecionado — o `CalendarScreen.kt` real não mostra nada
  nesse caso) e o card `"Lista do mês"` + a lista completa de um card por
  dia do mês (`items(monthDays) { ShiftDayRow(...) }`) — o app real não
  tem lista dia-a-dia nenhuma na tela de calendário, só o grid + o detalhe
  do dia selecionado + "Quem trabalha nesse dia". Esse era o principal
  gerador de "cards a mais" (um card extra por dia visível no mês).
- **Plantão** (`PlantaoScreen.kt`): card de aviso `"Dados de plantão
  (exemplo)"` — sem equivalente no `ui/plantao/PlantaoScreen.kt` real.

**Horário de pausa corrigido**: o texto "Janela permitida" sempre mostrava
`"1h após o início"` (o mesmo texto de offset, repetido) para qualquer
turno — o app real calcula uma **janela real por turno**
(`PauseWindow.kt`: início do turno + 120min até início do turno + 285min,
165 minutos de janela) e mostra `"Permitido entre X e Y"` /
`"Janela permitida: X–Y"`. Adicionado `ScheduleSummary.pauseWindowStart`/
`pauseWindowEnd`, calculados por tipo de turno
(`LabWorkbookParser.pauseWindow()`): Madrugada 03:00–05:45, Manhã
09:00–11:45, Tarde 15:00–17:45, Noite 21:00–23:45. O horário específico já
sugerido (`pauseLabel`, ex. "08:00 - 08:15") já batia com o padrão real
(offset de 60min + 15min de duração) — não precisou mudar.

**Testado:**

- `testDebugUnitTest` (21 testes, 0 falhas — nenhum teste dependia dos
  cards/textos removidos).
- Manual no emulador Android: confirmado visualmente que a aba Escala
  termina em "Quem trabalha nesse dia" (sem lista de dias abaixo); Perfil
  termina em "Aplicativo" (sem "Migração KMP"); os dois textos de pausa no
  Perfil mostram `"Permitido entre 09:00 e 11:45"` e
  `"Janela permitida: 09:00–11:45"` para o turno Manhã, batendo com a
  fórmula do app real.

`versionCode`/`versionName`: `7`/`0.4.0` → `8`/`0.4.1` (PATCH — correção de
fidelidade, nenhuma integração nova).

## Validacao da FASE 11.2 — parser compartilhado alinhado com o oficial

Refina `model/LabWorkbookParser.kt` (Android + Web, código próprio, não
copiado) para bater com as regras do parser oficial
`ScaleWorkbookParser.kt` do app Android real (`EscalaSOC`, só leitura para
comparar). Correções aplicadas, todas confirmadas por diff linha a linha do
código oficial:

- **Ranges fixos** em vez de auto-detecção: aba Escalistas — nomes a partir
  da linha 2 (0-index), linha de datas fixa na linha 2, colunas de status
  fixas em `3..32`; aba Escala — 30 linhas fixas (`2..31`), data na coluna
  0, observações na coluna 6. Antes o parser do lab escaneava linhas/colunas
  sem limite, o que podia ler dados que o oficial ignora (ou vice-versa).
- **Validação de calendário real** nas datas: `31/02` ou `30/02` agora são
  rejeitadas (usando `LabDate.monthLength(year, month)`, que já existia e
  já considerava ano bissexto) — antes o parser aceitava qualquer `dia
  1..31` sem checar o mês, diferente do parser oficial (`LocalDate.parse`,
  que rejeita datas de calendário inválidas).
- **Assimetria de separadores** replicada: encontrar o colaborador na
  célula do turno usa 4 separadores (`/`, quebra de linha, `,`, `;`);
  extrair a equipe do mesmo turno usa só 3 (sem `;`) — exatamente como o
  parser oficial, que tem essa mesma assimetria (não é um bug lá nem aqui).
- **Rótulo nuançado (`labelFor`)**: novo campo `ShiftDay.label` (default
  `type.label`, não quebra os mocks) reproduz as diferenças do oficial —
  `BH` → "Banco de horas", `ANIVERSARIO` → "Folga aniversário", `FOLGA` com
  status de origem → "Folga / `<status>`" (ex.: "Folga / DF"), e
  `"Trabalho sem turno localizado"` quando o status é um número 1-6 sem
  turno correspondente na aba Escala. Usado em `ScheduleTab.kt` (card de
  detalhe do dia, item da lista do calendário) e `TodayTab.kt` (evento de
  próximo descanso) — os demais usos de `type.label`/`type.shortLabel`
  (turnos de trabalho, badge circular M/T/N/Md) não mudam, pois já eram
  idênticos ao `labelFor` para esses casos.

**Limitação conhecida, não corrigida nesta fase**: o parser oficial tem um
atalho para células de data reais do POI (`DateUtil.isCellDateFormatted` +
`dateCellValue`), contornando a formatação de texto. O pipeline do lab
(Android e Web) sempre passa pela renderização de texto
(`DataFormatter`/SheetJS), então uma célula de data com formatação Excel
incomum poderia falhar a leitura aqui onde o oficial teria sucesso. Não
afeta o arquivo real usado hoje (confirmado no teste manual abaixo) —
documentado para uma fase futura se algum arquivo real expuser o problema.

**Testado:**

- 8 novos testes unitários em `LabWorkbookParserTest.kt`
  (`testDebugUnitTest`, 21 testes no total, 0 falhas) cobrindo cada
  correção acima com planilhas sintéticas: range fixo de colunas/linhas
  ignorando dados fora do range, rejeição de data de calendário inválida
  (`31/02`), aceitação de 29/02 só em ano bissexto, assimetria de
  separadores, e os três casos de `labelFor` (BH, Aniversário, Folga com
  status).
- Manual no emulador Android: reimportei a escala real do Dropbox
  (`Escala-SOC-Controle-Atual.xls`) — continua lendo os mesmos 30 dias e os
  mesmos colaboradores de antes (o arquivo real já respeitava o layout
  fixo, então a mudança de range não regrediu nada). Confirmei visualmente
  na aba Escala que um dia de folga real mostra `"Folga / DF"` com
  `"Status origem: DF"`, igual ao rótulo nuançado do parser oficial.
- Web/Wasm: mesma lógica (100% `commonMain`, sem código específico de
  plataforma nesta fase) — build `wasmJsBrowserDistribution` verificado,
  sem teste manual adicional no navegador (o parser é compartilhado
  byte-a-byte com o Android, já validado).

Validado:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
./gradlew :composeApp:wasmJsBrowserDistribution
~/Android/Sdk/platform-tools/adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

`versionCode`/`versionName`: `6`/`0.3.1` → `7`/`0.4.0` (MINOR — parser
compartilhado agora bate com as regras oficiais, ganho de fidelidade real
em ambas as plataformas).

## Validacao da FASE 11.1b — Dropbox real na Web (OAuth) + remoção de linguagem mock/demo/POC

Duas mudanças pedidas depois de validar a FASE 11.1 pela primeira vez: (1) o
projeto passa a ser tratado como **oficial**, rodando em paralelo ao app
Android atual até substituí-lo — não mais um "laboratório"/"POC" descartável
— e todo texto de UI que dizia "mock"/"demo"/"POC" foi trocado por linguagem
neutra; (2) o download real do Dropbox na Web, que falhava por CORS
(FASE 11.1), ganhou uma correção de verdade em vez de só documentar a
limitação.

**Rebrand de textos (sem mexer em nome de pasta/pacote/applicationId — só
nomes visíveis):**

- Launcher Android: `"Escala ICI KMP Lab"` → `"Escala ICI KMP"`.
- Título da aba do navegador / nome do PWA: `"Escala ICI KMP Lab"` →
  `"Escala ICI KMP"` (`index.html`, `manifest.json`).
- Login: "Modo Demo" → "Login de teste" (continua sendo o único jeito de
  entrar até o MSAL real da FASE 11.3 — decisão explícita, ver spec 33).
- Badges/labels "demo"/"mock"/"POC" na Hoje, Escala, Alertas, Perfil,
  Plantão e Importar → "exemplo"/"dados de exemplo"/wording neutro
  descrevendo o que de fato acontece (ex.: "Status: salva apenas neste
  dispositivo (sem sincronização)" em vez de "mock do laboratório").
- Card "POC KMP" no Perfil virou "Migração KMP", com texto atualizado
  refletindo o que já é real (Dropbox Android) vs. pendente (MSAL,
  Firebase, parser oficial).
- Versão exibida no Perfil passou a vir de `model/AppVersion.kt` (constante
  única, mantida manualmente em sincronia com `versionName` do Gradle — KMP
  não gera BuildConfig em `commonMain` sem plugin adicional).

**Dropbox real na Web — troca de link direto por API oficial (OAuth PKCE):**

O link direto (`RemoteScaleConfig.DROPBOX_SCALE_URL`) nunca vai funcionar no
navegador: é bloqueio de CORS do próprio Dropbox, não um bug de código. A
correção real é autenticar contra a API oficial do Dropbox
(`content.dropboxapi.com`), que suporta CORS para chamadas autenticadas.

- `model/DropboxAuthConfig.kt`: reaproveita o mesmo App Key PKCE do fluxo
  ADM do app Android real (`AdminConfig.DROPBOX_APP_KEY` — cliente público,
  sem secret, copiar não cria risco novo).
- `platform/DropboxSharedLinkFetcher.kt` (`expect downloadDropboxSharedLink`):
  Android continua usando o link direto (`downloadBytes`, sem CORS, sem
  mudança de comportamento). Web/Wasm roda o fluxo OAuth completo:
  - `resources/dropbox-auth.js`: gera PKCE (`crypto.subtle.digest` SHA-256,
    verifier/challenge base64url), abre um **popup** (não navega a página
    inteira — preserva o estado em memória do Compose) para
    `dropbox.com/oauth2/authorize`, escuta o retorno via `postMessage` da
    `dropbox-callback.html`, troca o `code` por token
    (`api.dropboxapi.com/oauth2/token`), guarda `access_token`/
    `refresh_token`/validade em `localStorage`, renova sozinho quando
    expira, e finalmente chama
    `content.dropboxapi.com/2/sharing/get_shared_link_file` com o link
    compartilhado para baixar os bytes reais.
  - `resources/dropbox-callback.html`: página estática que só lê
    `code`/`state`/`error` da URL, repassa por `postMessage` pro `opener` e
    fecha a própria janela.
  - `DropboxScaleRepository.kt` simplificado: chama só
    `downloadDropboxSharedLink`, sem mais nenhuma lógica de engine HTTP —
    isso já foi isolado no `expect`/`actual`.

**Testado (o que dá para testar sem o cadastro pendente no Dropbox, ver
abaixo):**

- Chromium headless (Playwright) confirmou que o clique em "Procurar
  escalas (Dropbox)" abre corretamente um popup apontando pra
  `dropbox.com/oauth2/authorize` com `client_id`, `redirect_uri`,
  `code_challenge_method=S256` e `scope=sharing.read` — ou seja, a
  integração Kotlin → JS → Dropbox está certa de ponta a ponta.
- **Pendência real encontrada nesse teste**: o Dropbox devolveu
  `error_name=scope_not_granted` ("No scope requested can be granted for
  this app") — o App Key `5by0pkzt2bgx95g` ainda não tem o escopo
  `sharing.read` habilitado no App Console (ele já tem
  `files.content.read`/`files.content.write`, usados pelo fluxo ADM, que são
  escopos diferentes).
- Android: regressão confirmada no emulador — segue baixando e processando
  a escala real do Dropbox exatamente como na FASE 11.1, sem nenhuma
  mudança de comportamento.

**Duas ações pendentes, só o dono da conta Dropbox pode fazer** (App
Console, app do `client_id` `5by0pkzt2bgx95g`):

1. **Permissions** → habilitar o escopo `sharing.read` e salvar.
2. **OAuth 2 → Redirect URIs** → adicionar
   `http://localhost:8080/dropbox-callback.html` (dev local; quando o PWA
   tiver um host de produção, adicionar a URI de produção também).

Sem essas duas, o popup sempre vai terminar em erro do próprio Dropbox
(como no teste acima) — o código já está pronto, só falta essa configuração
externa. Depois de feita, testar manualmente clicando em "Procurar escalas
(Dropbox)" na Web e completando o login/consentimento real do Dropbox.

`versionCode`/`versionName`: `5`/`0.3.0-lab` → `6`/`0.3.1` (sem sufixo
`-lab` a partir de agora, seguindo o rebrand para projeto oficial).

## Validacao da FASE 11.1 — download real da escala via Dropbox

Primeira integracao real da serie `FASE 11.x`: o botao "Procurar escalas
(Dropbox)" da aba Importar agora baixa o arquivo real publicado no mesmo
link Dropbox que o app Android de producao usa
(`model/RemoteScaleConfig.DROPBOX_SCALE_URL`, shared link `dl=1`, sem
token/App Key), alimentando o parser heuristico compartilhado
(`LabWorkbookParser`) com bytes reais — igual ao seletor de arquivo local.

- `platform/RemoteBytesDownloader.kt` (`expect fun downloadBytes(url): ByteArray`)
  isola a diferenca de plataforma: Android usa Ktor (`RemoteBytesDownloader.android.kt`,
  `HttpTimeout` 15s); Web/Wasm usa `fetch` nativo via interop com JS
  (`RemoteBytesDownloader.wasmJs.kt` + `resources/remote-download.js`,
  `AbortController` + timeout 15s) em vez do engine Ktor CIO — o CIO no
  alvo Wasm nao rejeitava a coroutine de forma confiavel quando o `fetch`
  interno falhava, deixando o spinner "Buscando no Dropbox…" girando para
  sempre em vez de mostrar o erro.
- `platform/WorkbookBytesReader.kt` (`expect fun readWorkbookFromBytes`)
  reaproveita o mesmo parser binario ja usado pelo seletor de arquivo local
  em cada plataforma (Apache POI no Android via
  `WorkbookImportLauncher.android.kt#parseWorkbookBytesAndroid`; SheetJS no
  Web/Wasm via `workbook-import.js#escalaIciParseWorkbookBase64`).
- `repository/DropboxScaleRepository.kt`: nunca lanca — qualquer falha vira
  `WorkbookImportResult.Failure`, exibida como card de erro amigavel
  ("Toque para tentar novamente") na aba Importar, sem quebrar a tela.
- `ui/App.kt`/`ui/ImportTab.kt`: novo estado `isFetchingFromCloud` (spinner
  no botao "Procurar escalas (Dropbox)" enquanto baixa).
- `AndroidManifest.xml`: `android.permission.INTERNET` adicionada (faltava
  desde a fundacao de rede da FASE 11.0, que ainda nao tinha nenhuma chamada
  de rede real).

**Resultado por plataforma (testado manualmente):**

- **Android**: funciona de ponta a ponta. Testado no emulador — baixou o
  XLS real, encontrou as abas `Escala`/`Escalistas` e os colaboradores reais
  da producao (`aleilima`, `ivcarvalho`, `alamancio`, `altaborda`,
  `lvergani`, `cestradioto`, `thaisvribeiro`, `dschlottag`, `luizneto`).
- **Web/Wasm**: **bloqueado por CORS** — testado com Chromium headless
  (Playwright) apontando para `wasmJsBrowserDevelopmentRun`, erro
  `net::ERR_FAILED` no `fetch`. O link compartilhado do Dropbox nao devolve
  `Access-Control-Allow-Origin` para origens arbitrarias como
  `http://localhost:8080`, entao o navegador bloqueia a resposta antes de
  qualquer byte chegar ao Kotlin. **Limitacao de plataforma, nao um bug
  daqui** — não há workaround sem um proxy/servidor intermediário (fora de
  escopo). A tela nao quebra: mostra o card de erro amigavel com a mensagem
  exata (`Failed to fetch`) e "Toque para tentar novamente". O seletor de
  arquivo local (`Escolher arquivo`) continua sendo o caminho real para
  demonstrar dados reais no Web durante uma apresentacao.

Validado:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:assembleDebug :composeApp:testDebugUnitTest
./gradlew :composeApp:wasmJsBrowserDistribution
~/Android/Sdk/platform-tools/adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
~/Android/Sdk/platform-tools/adb shell am start -n br.com.leorvergani.escalaici.kmp.lab/.MainActivity
```

- APK debug reinstalado no emulador; fluxo completo testado manualmente
  (Modo Demo → Importar → "Procurar escalas (Dropbox)" → dados reais na
  tela).
- Web/Wasm testado via `wasmJsBrowserDevelopmentRun` + Chromium headless
  (Playwright), confirmando o erro de CORS documentado acima e o card de
  erro amigavel.
- `versionCode`/`versionName`: `4`/`0.2.1-lab` → `5`/`0.3.0-lab` (MINOR —
  primeira integracao real funcionando de ponta a ponta, no Android).

## Validacao da FASE 11.0 — fundacao de rede (Ktor)

Inicio da serie **FASE 11.x**, que liga integracoes reais (Dropbox, parser,
MSAL) ja usadas pelo app Android de producao `EscalaSOC`. Ver
`docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md` (repositorio Android principal)
para o plano completo e o estado atual, incluido para outra IA/sessao
continuar o trabalho.

- Adicionado `io.ktor:ktor-client-core` + `io.ktor:ktor-client-cio` em
  `commonMain` (`gradle/libs.versions.toml`, `composeApp/build.gradle.kts`).
  O engine CIO cobre JVM/Android/Native/JS/WasmJs a partir de uma unica
  dependencia — nao precisa de engine por plataforma nem de `expect/actual`
  so para escolher o cliente HTTP.
- `versionCode`/`versionName` do lab passam a ser incrementados a cada
  sub-fase (regra nova, ver `docs/spec/33-...`): `1` → `2`,
  `0.1.0-lab` → `0.1.1-lab`.
- Nenhuma integracao real ainda usa o Ktor nesta fase — e so a fundacao.

Validado:

```bash
cd /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDistribution
./gradlew :composeApp:testDebugUnitTest
```

Nota tecnica: a versao inicial `ktor = "3.5.1"` quebrou a compilacao Web/Wasm
(`Missing stdlib class` no codigo gerado de resources) — incompatibilidade de
versao entre o Kotlin do Ktor 3.5.x e o Kotlin `2.2.10` fixado neste projeto.
Downgrade para `ktor = "3.3.0"` (construido contra Kotlin 2.2) resolveu. Se
uma fase futura atualizar o Kotlin do projeto, reavaliar subir o Ktor junto.

## Validacao — chave de assinatura propria + APK de release (instalar junto com o app oficial)

O lab usa `applicationId = "br.com.leorvergani.escalaici.kmp.lab"`,
diferente do app oficial `EscalaSOC` (`br.com.leorvergani.escalasoc`) — ja
da para instalar os dois no mesmo celular. Esta fase adicionou uma chave de
assinatura propria para que **toda build (debug e release) use sempre a
mesma chave**, evitando o erro `INSTALL_FAILED_UPDATE_INCOMPATIBLE` ao
reinstalar.

- Keystore + `keystore.properties` na raiz do projeto (gitignored — ver
  `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md` §10 para o formato
  exato e onde ficam).
- `composeApp/build.gradle.kts`: `signingConfigs { create("lab") { ... } }`
  lido de `keystore.properties`, aplicado em `buildTypes.debug` e
  `buildTypes.release` — mesmo padrao do `EscalaSOC/app/build.gradle.kts`.
- Build de release: `./gradlew :composeApp:assembleRelease` →
  `composeApp/build/outputs/apk/release/composeApp-release.apk`, copiado
  para `~/Downloads/EscalaICI-KMP-Lab-latest.apk` (sideload manual, **não**
  faz parte do pipeline de update via Dropbox do app oficial).
- `versionCode`/`versionName`: `3`/`0.2.0-lab` → `4`/`0.2.1-lab`.

## Validacao — icone proprio do lab (calendario)

O lab tinha o icone-escudo do app SOC no PWA e **nenhum icone customizado**
no Android (o `AndroidManifest.xml` nao declarava `android:icon` — o app
rodava com o icone padrao do AGP). Como este laboratorio representa a
"Escala Geral do ICI" (mais amplo que o time SOC), o usuario pediu um icone
proprio: um mini calendario, sem nenhum texto, na mesma paleta/gradiente do
tema atual.

- `composeApp/src/wasmJsMain/resources/icons/icon.svg` e `icon-maskable.svg`:
  substituidos o escudo por um calendario (corpo azul `#2563EB`, faixa de
  cabecalho escura, grade de dias, marcador "hoje" branco, dois "anéis" de
  espiral no topo) — mesmo fundo decorativo (`#060B14` + curvas
  `#123E93`/`#6D28D9`) e mesma logica de safe-zone 61% no maskable.
- PNGs `icon-192/512.png` e `icon-maskable-192/512.png` regerados com
  `rsvg-convert` (mesmo pipeline da FASE 9g/10.2).
- Android: **primeiro icone de launcher customizado do lab** — adaptive
  icon (`mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`)
  apontando para `drawable-xxxhdpi/ic_launcher_background.png` (fundo) e
  `ic_launcher_foreground.png` (calendario, camada separada) + PNGs legado
  `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher{,_round}.png` para
  API < 26. `AndroidManifest.xml` ganhou `android:icon`/`android:roundIcon`
  pela primeira vez.

Validado: instalei o APK debug no emulador e confirmei visualmente o novo
icone (calendario azul-marinho, mascara circular do launcher) na dock —
antes mostrava o icone padrao do Android.

## Checklist de paridade visual completa (FASE 10)

A FASE 9c-1 (2026-07-07) tinha feito uma **aproximacao** visual do app real.
A serie **FASE 10.0 a 10.11** (2026-07-09) refez esse trabalho comparando
cada tela do laboratorio linha a linha com o codigo-fonte real do
`EscalaSOC` (nao so a aparencia — o proprio arquivo `.kt`), corrigindo
divergencias de cor/shape/texto e portando as duas telas que faltavam
(Plantao, Trocas de escala). Esta secao resume o resultado; o detalhe de
cada sub-fase esta nas secoes "Validacao da FASE 10.x" abaixo.

### Design system

| Item | Status |
|---|---|
| Paleta de cores (`LabColors`, 24 cores) | ✅ porte 1:1 de `ui/theme/Color.kt` |
| Tipografia (`LabTypography`, 12 estilos) | ✅ porte 1:1 de `SocTypography` |
| Shapes (`LabShapes`, 6 tokens) | ✅ porte 1:1 de `PremiumShapes` |
| `LabColorScheme` (26 parametros) | ✅ porte 1:1 de `SocDarkColorScheme` |
| Cores de turno com container/on-color | ✅ valores exatos (`ShiftColors.kt`) |
| Cores de alerta com container/on-color | ⚠️ não portado (ver limites da FASE 10.8) |
| Shield logo (Composable + ícone PWA) | ✅ porte 1:1 (paths exatos do launcher real) |

### Telas

| Tela | Status | Observação |
|---|---|---|
| Login (`LoginGateScreen`) | ✅ fiel | fluxo "Modo Demo" com 3 membros mock em vez de `DemoUser` real |
| Hoje (`TodayTab`) | ✅ fiel | sem variação "compact" de tela estreita (`BoxWithConstraints`) |
| Escala (`ScheduleTab`) | ✅ fiel | sem botão de atualizar (sem sync/backend) |
| Importar (`ImportTab`) | ✅ fiel | seletor de colaborador da prévia é acréscimo do laboratório (documentado) |
| Alertas (`AlertsTab`) | ✅ fiel + bug corrigido | mantém geração de alertas em modo demo (decisão da 9c-1) |
| Perfil (`ProfileTab`) | ✅ fiel | fluxos ADM/MSAL/Dropbox continuam visuais estáticos (fora de escopo) |
| Plantão (`PlantaoScreen`) | ✅ nova | mock via `OnCallAssignment`, sem importação real de relatório |
| Trocas de escala (`ShiftSwapScreen`) | ✅ nova | mock via `ShiftSwapRequest` estendido, sem Firestore |
| Importação Firebase (ADM) | ❌ não portada (decisão do usuário) | mantido só o botão desabilitado |

### Limites conhecidos (aceitos de propósito)

- Nenhuma integração real com Firebase, MSAL, parser XLS oficial, Dropbox ou
  notificações — critério inalterado desde a FASE 9b.
- Sem `kotlinx-datetime`: telas que dependem de "data/hora atual" (Plantão)
  usam campos de status já mockados em vez de comparar com o relógio real.
- Variações de layout "compact"/responsivas do app real (`BoxWithConstraints`
  para telas muito estreitas) não foram portadas em todos os pontos — o
  laboratório roda num container largo (`widthIn(max=760.dp)`).
- Verificação visual em emulador Android não foi possível nesta sessão
  (`INSTALL_FAILED_INSUFFICIENT_STORAGE` no AVD local, mesmo com espaço
  aparentemente suficiente — provável limite de threshold do AVD, não
  investigado a fundo). A fidelidade foi validada lendo o código-fonte real
  arquivo por arquivo (não só a aparência) e conferindo builds/testes; o
  ícone do shield foi conferido visualmente (renderização do PNG). O
  Web/Wasm foi validado via dev server (HTTP 200 nos assets), mas não há
  captura de tela automatizada disponível neste ambiente (sem Chrome/Chromium
  headless). Recomenda-se uma checagem visual manual (emulador com espaço
  livre ou navegador) antes de uma apresentação real.
- `EscalaSOC` (app Android principal) não foi alterado em nenhuma das fases
  10.x — confirmado via `git status` antes e depois de cada commit.

## Validacao da FASE 10.11 — tela Trocas de escala nova (mock)

Data da validacao: 2026-07-09.

`ui/ShiftSwapScreen.kt` (novo): porte de `ui/swap/ShiftSwapScreen.kt` real —
secoes Recebidos/Enviados, cards com turno/time/status/data, botoes
Aceitar/Recusar/Cancelar mutando o status em memoria (sem Firestore).

Unica extensao de modelo do porte visual completo: `SwapStatus` foi de 4
para os 6 valores reais; `ShiftSwapRequest` ganhou campos opcionais com
default (nao quebra nada existente). `mockShiftSwapRequests()` engordado
para 3 pedidos cobrindo os 3 membros mock. "Ver minhas solicitacoes"
(Perfil) agora abre esta tela de verdade.

Limites assumidos: sem persistencia real entre reaberturas da tela.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.10 — tela Plantao nova (mock)

Data da validacao: 2026-07-09.

`ui/PlantaoScreen.kt` (novo): porte de `ui/plantao/PlantaoScreen.kt` real —
hero, banner "Dados de plantao (mock)" no lugar do import real, calendario
mensal com marcador roxo, detalhe do dia. Reaproveita 100% os modelos/mocks
da FASE 9c (`OnCallPeriod`/`OnCallAssignment`/`OnCallStatus`), engordados de
1 para 4 registros. Sem `kotlinx-datetime`, "agora" e decidido pelo status
`ACTIVE` ja mockado. Botao "Plantao" do header agora abre esta tela de
verdade (antes placeholder).

Limite assumido: sem importacao de relatorio real de plantao.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.9 — polimento fino da aba Perfil

Data da validacao: 2026-07-09.

Comparado `ProfileTab.kt` com `ui/settings/SettingsScreen.kt` real.
Adicionado titulo de secao "Perfil". Card "Perfil selecionado" trocou o box
quadrado por `LabCollaboratorAvatar` (avatar circular, igual ao real) e
virou card "nu"; adicionadas linhas Periodo/Fonte mantendo email/time.
Shapes corrigidas (`ProfileMetric`/`ProfileChip` para os tokens exatos).
Chips de antecedencia de notificacao expandidos para as 6 opcoes reais.

Limites assumidos: fluxos de login ADM/MSAL/Dropbox continuam visuais
estaticos (fora de escopo do laboratorio); janela de pausa dinamica por
tipo de turno nao portada.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.8 — correcao do gradiente + polimento da aba Alertas

Data da validacao: 2026-07-09.

**Bug corrigido**: `AlertsHero` reaproveitava o gradiente/textura do
`HeroCard` compartilhado; o real usa gradiente proprio
`#0B274F,#111A31,#24104D`, shape `cardLarge` e sem textura — virou um
componente bespoke fiel ao real. `CountBadge` corrigido para mostrar total
de alertas (nao so criticos). Shapes corrigidas para os tokens exatos
(`cardMedium`/`chip`) em `AlertSummaryCard`/`AlertFilterRow`/
`PremiumAlertCard`/`SeverityBadge`. `AlertContextLine` novo replica a
logica real de contexto (Fonte vs periodo+analista).

Limites assumidos: laboratorio mantem geracao de alertas tambem em modo
demo (decisao da FASE 9c-1, mantida de proposito); reformato de mensagem
por regex do real nao portado (templates proprios do `GenerateLabAlerts`).

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.7 — polimento fino da aba Importar

Data da validacao: 2026-07-09.

`ImportTab.kt` reescrita como porte de `ui/settings/ImportScaleScreen.kt`
real: titulo/subtitulo simples (sem hero — o real nao tem), `LocalFileCard`
clicavel por inteiro com estados Idle/Erro/Sucesso, `ScaleSummaryCard` com
diagnostico OK/Atencao por aba, `IdentifiedCollaboratorCard` e
`CloudFileCard`. "Procurar escalas" (Dropbox) desabilitado.

Mantido (documentado como acrescimo do laboratorio, nao existe no real): o
seletor de colaborador da pre-visualizacao, ja que aqui a identidade nao
vem de login real.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.6 — polimento fino da aba Escala

Data da validacao: 2026-07-09.

Comparado `ScheduleTab.kt` com `ui/calendar/CalendarScreen.kt` real.
`LabCard.title` virou opcional (card "nu" sem cabecalho, igual ao
`SocCard` real). Adicionado `DemoCalendarCard` (aviso "Demonstracao") no
topo quando nao importado. Corrigidos: shape do `ShiftTurnoTab` (12dp, nao
14dp), travessao no lugar do hifen para turno vazio, halo quadrado 46dp em
volta do marcador de turno no detalhe do dia, alpha do divisor (0.22 nao
0.30), grade de legenda responsiva (2/3/4 colunas por largura, igual ao
real, em vez de 3 fixas).

Limites assumidos: sem botao de "Atualizar" (nao ha sync/backend); mes do
calendario continua navegavel mesmo em modo demo (o real trava em
julho/2026); heuristica "Observacao do analista" vs "Equipe" nao portada.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.5 — polimento fino da aba Hoje

Data da validacao: 2026-07-09.

Comparado `TodayTab.kt` linha a linha com `ui/home/TodayScreen.kt` real e
corrigido: estado vazio da hero (sem turno) com `ImportVisualButton` real;
"Analista:"/"Com:" como texto anotado com rotulo azul Black; clima com
emoji "☀️" em vez de icone Material; gradiente/borda de
`WeekSummaryCard`/`EventsCard` para os valores exatos do `TodayCard` real;
badge "demo" so quando nao importado; borda do card de Pausa com o teal
literal `#14B8A6` (nao `LabColors.tertiary`); titulo/subtitulo do resumo do
periodo distinguindo demo de real; dimensoes exatas do `MetricCard`.

Limite assumido: variacoes "compact" do real para telas estreitas
(`BoxWithConstraints`) nao portadas — o laboratorio roda num container
sempre largo, entao nao se aplicam.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.4 — navegacao em pilha + PremiumHeader

Data da validacao: 2026-07-09.

Objetivo desta etapa: portar o header comum do app real (`PremiumHeader`,
com logo, sino de notificacoes, chip "Plantao" e badge do colaborador) para
as 5 abas do laboratorio, e criar a navegacao em pilha necessaria para o
botao "Plantao" e o "Ver minhas solicitacoes" (Perfil) terem um destino.

- `LabCollaboratorAvatar`/`SelectedCollaboratorBadge` e `LabPremiumHeader`
  (novos, em `ui/components/`), porte literal dos componentes reais.
- Confirmado lendo os 5 arquivos de tela reais que `PremiumHeader` aparece
  como primeiro item em TODAS as abas (Hoje/Escala/Importar/Alertas/Perfil)
  — corrigido no laboratorio para bater com isso.
- **Correcao de fidelidade**: removido o titulo generico de pagina
  ("Escala"/"Importar"/etc.) que o `PageList` mostrava — o app real nao tem
  esse titulo, so o `PremiumHeader`. Era uma invencao da FASE 9c-1.
- `StackedScreen` (Plantao/Trocas) no shell: bottom nav some quando uma tela
  empilhada esta aberta (igual ao real); placeholder "Em construcao" ate as
  FASES 10.10/10.11 substituirem pelas telas de verdade.
- "Ver minhas solicitacoes" (Perfil) deixou de ser botao desabilitado.
- Removido o indicador de debug "mock N" do header antigo — nao existe no
  app real.

Limite: mesma limitacao de verificacao visual da FASE 10.1/10.2 (emulador
sem conseguir instalar por um limite de armazenamento do AVD, aparentemente
um threshold e nao falta real de espaco — ~480MB livres para um APK de
~100MB). Fidelidade conferida lendo os arquivos reais linha a linha.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.3 — LoginGateScreen fiel ao LoginScreen real

Data da validacao: 2026-07-09.

Objetivo desta etapa: reescrever a tela de login fake para ser um porte
literal de `ui/auth/LoginScreen.kt` do app real, lido diretamente do
repositorio `EscalaSOC` (layout, textos e fluxo do dialog "Modo Demo").

- Layout, textos e cores identicos ao real: "Escala ICI", botao "Login" com
  `CircularProgressIndicator` sobreposto durante a tentativa, texto "Sera
  aberta a autenticacao Microsoft corporativa", dialog "Modo Demo" com
  "Teste SOC A"/"Teste SOC B"/"Aprovador SOC"/"Cancelar".
- Unica diferenca necessaria: sem MSAL real, o botao "Login" sempre termina
  em mensagem inline explicando a indisponibilidade nesta POC; "Modo Demo"
  seleciona um dos 3 membros mock do laboratorio em vez de um usuario demo
  real do Firestore.
- Removido o icone generico (`Icons.Default.Security`) que a tela fake
  anterior (FASE 9f) tinha — o login real nao usa nenhum icone, so texto.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.2 — shield logo real + icone PWA

Data da validacao: 2026-07-09.

Objetivo desta etapa: substituir o icone generico do laboratorio (um
"computador" com texto "ICI") pelo shield real do app Android, tanto como
Composable in-app quanto como icone/favicon do PWA.

- `ui/components/SocLogo.kt` (novo): `LabShieldLogo`/`LabAppTitle`, porte
  literal do `Canvas` real (mesmo path fracionario, gradiente azul-roxo,
  "S" tracado). Criado mas ainda nao usado nos headers (FASE 10.4).
- `icons/icon.svg`/`icon-maskable.svg`: paths exatos de
  `ic_launcher_foreground.xml`/`ic_launcher_background.xml` do app real
  (viewBox 108). Maskable usa a proporcao de safe-zone padrao de adaptive
  icon Android (66dp/108dp = 61%).
- 4 PNGs regerados com `rsvg-convert` (sem dependencia nova) e conferidos
  visualmente nesta sessao (Read do PNG): shield fiel ao launcher real,
  maskable com margem de seguranca visivel.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuicao Web/Wasm
continuam compilando.

## Validacao da FASE 10.1 — design system exato

Data da validacao: 2026-07-09.

Objetivo desta etapa: portar cores, tipografia e shapes com fidelidade total
ao app Android real, lendo diretamente `Color.kt`, `Theme.kt` e
`PremiumShapes.kt` (repositório `EscalaSOC`).

- `LabColors` foi de 8 para 24 cores (paleta completa, incluindo os pares
  container/on-container).
- Novos `LabTypography` (12 estilos) e `LabShapes` (6 formas), ambos porte
  literal do real.
- `LabColorScheme` expandido para os 26 parâmetros de `darkColorScheme`,
  igual a `SocDarkColorScheme`.
- `ShiftColors.kt` ganhou `shiftContainerColor()`/`shiftOnColor()` com os
  valores exatos do app real (alguns containers de turno usam hex próprios,
  não `.copy(alpha=X)` da cor do turno — ex. o container do turno Manhã é
  `#713F12`, uma cor marrom-âmbar, não uma variação de amarelo).
- `LabShapes` aplicado a `LabCard`/`HeroCard`/badge/nav pill (mesmos valores
  numéricos de antes, agora referenciando o token do design system).

Limites assumidos: shapes bespoke de cada aba não migraram para `LabShapes`
(só os que batem exatamente com os 6 tokens oficiais); containers/on-colors
de alerta ficam para a FASE 10.8; nenhum arquivo do app Android principal
foi alterado.

**Verificação visual pendente**: o emulador local (`EscalaSOC_API_37`) está
com armazenamento insuficiente para instalar o APK nesta sessão
(`INSTALL_FAILED_INSUFFICIENT_STORAGE`, `/data` 93% cheio), e não há
Chrome/Chromium headless disponível para capturar o Web/Wasm automaticamente.
A fidelidade dos valores foi conferida linha a linha contra `Color.kt`,
`Theme.kt` e `PremiumShapes.kt` do app real, e os builds/testes passam, mas
uma checagem visual (emulador com espaço livre ou navegador manual em
`http://localhost:8080/`) ainda não foi feita nesta fase — recomendado antes
de considerar esta fase "aprovada visualmente".

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## Validacao da FASE 10.0 — split mecânico do App.kt

Data da validacao: 2026-07-09.

Objetivo desta etapa: preparar o terreno para o porte visual **completo** do
app Android real (correção de escopo — a FASE 9c-1 foi só uma aproximação
visual, o objetivo agora é paridade total de cores, tipografia, shapes,
ícones e telas). Plano completo em
`/home/lvergani/.claude/plans/humble-spinning-yao.md`.

`ui/App.kt` (1831 linhas) foi splitado, sem nenhuma mudança de valor/cor/texto,
em:

- `ui/theme/LabColors.kt`, `LabTheme.kt`, `ShiftColors.kt`;
- `ui/components/PremiumBackground.kt`, `LabCard.kt` (`LabCard`+`HeroCard`+
  `NocHeroTexture`), `PageList.kt`;
- `ui/util/StringFormatting.kt` (`String.initials()`);
- `ui/LoginGateScreen.kt`, `TodayTab.kt`, `ScheduleTab.kt`, `ImportTab.kt`,
  `AlertsTab.kt`, `ProfileTab.kt`;
- `ui/App.kt` (shell: `EscalaIciLabApp`, `LabTab`, `BottomNav`).

Única mudança além de puro split: `HeroCard` ganhou um parâmetro `gradient`
com valor padrão idêntico ao gradiente fixo anterior — habilita a correção
de um gradiente incorreto na aba Alertas (FASE 10.8) sem alterar nada agora.

Limites assumidos:

- nenhum arquivo do app Android principal foi alterado;
- nenhuma mudança visual nesta etapa — só reorganização de código.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados: 13 testes continuam passando; APK debug e distribuição Web/Wasm
continuam compilando.

## Validacao da FASE 9g — PWA real (manifest, icones, service worker, cache offline)

Data da validacao: 2026-07-09.

Objetivo desta etapa: seguir o plano da spec `27-KMP-PWA-IOS-ESTRATEGIA.md`
(secao 9): "PWA real: `manifest.json`, service worker, cache offline e
build web". O PWA basico ja existia desde a FASE 9b; esta fase evolui os
tres pontos que ainda faltavam para uma instalacao/uso offline mais real,
sem introduzir Firebase, sync ou qualquer backend.

Alterado em `composeApp/src/wasmJsMain/resources/`:

- `manifest.json`: alem do icone SVG (`icons/icon.svg`), agora inclui
  icones PNG reais gerados a partir do mesmo design — `icon-192.png` e
  `icon-512.png` (`purpose: any`) e `icon-maskable-192.png`/
  `icon-maskable-512.png` (`purpose: maskable`, com safe-zone de ~18% de
  margem, gerados a partir de `icons/icon-maskable.svg`). Adicionados
  tambem `id`, `lang` e `orientation`;
- `service-worker.js`: `CACHE_NAME` avançou para `v2` (inclui os novos
  icones no app shell) e o handler de `fetch` ganhou um fallback offline —
  se a rede falhar e não houver cache para o recurso pedido, navegações
  (`request.mode === "navigate"`) caem no `index.html` já cacheado, em vez
  do erro genérico do navegador;
- `index.html`: metatags `apple-mobile-web-app-*`/`mobile-web-app-capable`
  e `<link rel="apple-touch-icon">`, para instalação também via Safari
  (que não lê o manifest da mesma forma que Chrome/Edge).

Icones PNG gerados localmente com `rsvg-convert` a partir dos SVGs
existentes (nenhuma dependência nova no projeto Gradle).

Limites assumidos:

- cache offline continua "cache-first com fallback de rede" simples (sem
  estratégias por tipo de recurso nem expiração automática de cache);
- o fallback offline só cobre navegação (recarregar a página); um asset
  individual não cacheado e sem rede ainda falha normalmente;
- nenhuma integração com push notifications ou background sync;
- nenhum arquivo do app Android principal foi alterado.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

Resultados:

- Distribuição Web/Wasm gerada com os novos ícones/manifest/service worker
  em `composeApp/build/dist/wasmJs/productionExecutable/`.
- No dev server (`http://localhost:8080/`), `index.html`, `manifest.json`,
  `service-worker.js`, `icons/icon-192.png`, `icons/icon-512.png`,
  `icons/icon-maskable-192.png` e `icons/icon-maskable-512.png`
  responderam HTTP 200.
- `manifest.json` validado como JSON bem formado.
- APK debug do laboratório continuou compilando.

## Validacao da FASE 9f — login fake, lista de escala e calendario

Data da validacao: 2026-07-09.

Objetivo desta etapa: seguir o plano da spec `27-KMP-PWA-IOS-ESTRATEGIA.md`
(secao 9): "POC Web/Wasm com tela simples: login fake ou mock, lista de
escala, calendario simples, sem MSAL real". A lista de escala e o calendario
ja existiam desde a FASE 9c-1 (aba `Escala`); esta fase adiciona o login
fake que faltava, como um `LoginGate` funcional (nao so visual) antes do
resto do app.

A UI continua 100% em `commonMain` (Compose Multiplatform), ou seja, o
login fake funciona igual em Android e Web/Wasm — e ja fica no lugar certo
para ser reaproveitado por um futuro app iOS (FASE 9h), sem nenhuma
dependencia de plataforma.

Adicionado em `ui/App.kt`:

- `LoginGateScreen`: tela inicial que pede para selecionar um colaborador
  demonstrativo (lido via `MockMemberRepository.getMembersByTeam("soc")`,
  contrato da FASE 9e) antes de liberar o resto do app;
- `EscalaIciLabApp` passou a controlar uma sessao fake via
  `InMemoryAuthSessionRepository` (nova classe em
  `repository/MockRepositories.kt`): sem sessao, mostra o `LoginGateScreen`;
  com sessao, mostra a navegacao normal (`Hoje`/`Escala`/`Importar`/
  `Alertas`/`Perfil`);
- botao "Sair (login fake)" na aba `Perfil` encerra a sessao e volta ao
  `LoginGateScreen`.

`InMemoryAuthSessionRepository` implementa o contrato `AuthSessionRepository`
da FASE 9e (so expõe leitura de sessao) e adiciona `signIn`/`signOut` como
detalhe de implementacao do mock, exatamente como a spec recomenda para a
mitigacao de risco de autenticacao variar por plataforma (secao 10): o
contrato multiplataforma so conhece sessao/usuario, nunca MSAL/Firebase.

Limites assumidos:

- login continua fake: nao ha MSAL, Firebase, senha ou token real;
- a escala/calendario mockados continuam representando sempre o mesmo
  periodo de demonstracao, independente de qual colaborador faz login —
  trocar de identidade no login gate atualiza nome/e-mail exibidos, mas nao
  gera uma escala diferente por pessoa (fora do escopo desta fase);
- os cards "Conta corporativa" e "Modo demo" na aba `Perfil` (criados na
  FASE 9c-1) continuam desabilitados/visuais — representam MSAL/Firebase
  reais, que seguem fora de escopo;
- nenhum arquivo do app Android principal foi alterado.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados:

- APK debug e distribuicao Web/Wasm continuaram compilando.
- Suite de testes (`ScheduleRulesTest` + `MockRepositoriesTest`) continuou
  passando sem alteracoes.

## Validacao da FASE 9e — contratos de repository em `commonMain`

Data da validacao: 2026-07-09.

Objetivo desta etapa: criar em `commonMain` os contratos de repository
descritos em `docs/spec/27-KMP-PWA-IOS-ESTRATEGIA.md` (secao 4), como
interfaces Kotlin puras (`suspend fun`), sem qualquer SDK de plataforma.

Criado em `repository/Repositories.kt`:

- `ScheduleRepository`, `MemberRepository`, `TeamRepository`,
  `OnCallRepository`, `ShiftSwapRepository`, `AuthSessionRepository`,
  `LocalCacheRepository`.

Implementacoes mock/em memoria em `repository/MockRepositories.kt`
(`MockScheduleRepository`, `MockMemberRepository`, `MockTeamRepository`,
`MockOnCallRepository`, `MockShiftSwapRepository`, `MockAuthSessionRepository`,
`MockLocalCacheRepository`), reaproveitando os mocks da FASE 9c
(`mockSchedulePeriod`, `mockScheduleAssignments` etc.) — apenas para provar
que os contratos compilam e sao usaveis, sem antecipar a implementacao real
(Firebase, cache local oficial) nem se conectar a UI do laboratorio.

`kotlinx.coroutines` (para `runBlocking` nos testes) ja estava disponivel
transitivamente via `compose.runtime`; nenhuma dependencia nova foi
adicionada ao projeto.

Testes unitarios em `composeApp/src/commonTest/.../MockRepositoriesTest.kt`
cobrindo os 7 repositorios mock.

Limites assumidos:

- nenhuma implementacao real (Firebase, MSAL, cache local oficial) foi
  criada — fica para fase propria fora deste laboratorio;
- os contratos ainda nao estao conectados a UI do laboratorio;
- nenhum arquivo do app Android principal foi alterado.

Comandos executados:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados:

- `testDebugUnitTest`: 7 testes novos (`MockRepositoriesTest`), 0 falhas
  (13 no total somando `ScheduleRulesTest` da FASE 9d).
- APK debug e distribuicao Web/Wasm continuaram compilando normalmente.

## Validacao da FASE 9d — regras puras (resumo da semana e alertas) em `commonMain`

Data da validacao: 2026-07-09.

Objetivo desta etapa: portar as regras de "resumo da semana" e "alertas"
descritas em `docs/spec/27-KMP-PWA-IOS-ESTRATEGIA.md` (secao 4) para funcoes
puras em `commonMain`, operando sobre `ScheduleAssignment` (modelo puro da
FASE 9c) em vez dos tipos ligados a UI mock (`ShiftDay`/`ScheduleSummary`).

Criado em `model/ScheduleRules.kt`:

- `WeekSummary` + `weekSummaryOf(assignments)`: dias trabalhados, dias de
  folga, horas totais, proximo turno e proximo descanso;
- `ScheduleAlert`/`ScheduleAlertSeverity` + `ScheduleAlertRules(assignments)`:
  descanso menor que 11h, regra 6x1 excedida, inconsistencia e turno
  indefinido.

`LabDate.parseIso(String)` foi adicionado em `model/ScheduleModels.kt` para
converter as datas `String` (`yyyy-MM-dd`) dos modelos puros de volta para
`LabDate` ao calcular descanso entre turnos.

As implementacoes existentes (`GenerateLabAlerts`, propriedades de
`ScheduleSummary`) usadas pela UI mock/parser **nao foram alteradas** — as
novas regras sao aditivas e independentes, reduzindo o risco de regressao
visual.

Testes unitarios criados em `composeApp/src/commonTest/.../ScheduleRulesTest.kt`
(novo source set `commonTest`, dependencia `kotlin("test")` do proprio plugin
Kotlin, sem biblioteca externa nova), cobrindo resumo da semana e os 3 tipos
de alerta com casos positivos e um caso vazio.

Limites assumidos:

- nenhum arquivo do app Android principal foi alterado;
- as novas regras ainda nao estao conectadas a UI do laboratorio, apenas
  testadas isoladamente;
- teste automatizado do alvo Web/Wasm (`wasmJsBrowserTest`) requer Chrome
  headless local (`CHROME_BIN`), indisponivel neste ambiente — os testes
  foram validados pelo alvo Android/JVM (`testDebugUnitTest`).

Comandos executados:

```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados:

- `testDebugUnitTest`: 6 testes, 0 falhas (`ScheduleRulesTest`).
- `wasmJsBrowserTest` (via `allTests`) falhou apenas por falta de Chrome
  headless no ambiente local; nao indica problema no codigo.
- APK debug e distribuicao Web/Wasm continuaram compilando normalmente.

## Validacao da FASE 9c-1 visual

Data da validacao: 2026-07-09.

Objetivo desta etapa: aproximar o visual do laboratorio KMP do app Android real para apresentacao, sem implementar login, Firebase, parser XLS oficial ou migracao do app principal.

Itens implementados no laboratorio:

- fundo premium escuro com textura e gradientes sutis, inspirado no app Android atual;
- tema Material 3 com a paleta `Soc` usada no Android principal;
- navegacao inferior com icones Material para `Hoje`, `Escala`, `Importar`, `Alertas` e `Perfil`;
- tela `Hoje` com cabecalho `Escala ICI`, proximo turno, clima mockado, resumo da semana, eventos, pausa e resumo do periodo;
- aba `Escala` com lista do periodo mockado;
- aba `Importar` com selecao XLS e pre-visualizacao real da planilha lida;
- seletor de escalista lido da aba `Escalistas`, recalculando a tela com o colaborador escolhido;
- abas `Alertas` e `Perfil` com cards no mesmo padrao visual;
- aba `Escala` com calendario mensal funcional, navegacao entre meses, legenda, detalhe do dia e card `Quem trabalha nesse dia`;
- aba `Alertas` com hero, contadores por severidade, filtros e cards de alerta critico/atencao/info;
- aba `Perfil` com os cards visuais do app Android: identidade, resumo, administracao da escala, conta corporativa, modo demo, trocas, notificacoes, pausa, armazenamento local e aplicativo;
- dependencia `compose.materialIconsExtended` adicionada apenas ao laboratorio KMP.
- parser experimental proprio do laboratorio para abas `Escala` e `Escalistas`;
- file picker Web/Wasm usando o navegador;
- file picker Android do laboratorio usando seletor de documento;
- leitura `.xls`/`.xlsx` no Android do laboratorio via Apache POI;
- leitura `.xls`/`.xlsx` no Web/Wasm via SheetJS carregado no `index.html`.
- alertas experimentais para descanso menor que 11h, regra 6x1, inconsistencias, dias indefinidos e fonte da escala.

Limites assumidos:

- o parser XLS do laboratorio e experimental e nao substitui o parser oficial Android;
- no Web/Wasm, a biblioteca SheetJS e carregada por CDN nesta fase de laboratorio;
- nenhum arquivo do app Android principal foi alterado;
- nenhum Firebase, MSAL, Dropbox, dashboard React ou APK de producao foi alterado.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados:

- APK debug do laboratorio continuou compilando.
- Distribuicao Web/Wasm continuou compilando.
- Web/Wasm importou a planilha real `Escala-SOC-Controle-Julho.xls` em teste local.
- A pre-visualizacao reconheceu as abas `Escala` e `Escalistas`, 30 dias e 9 escalistas.
- A tela `Hoje` passou a exibir dados reais importados do XLS apos `Usar dados importados`.
- Webpack manteve os avisos de tamanho de bundle esperados para Compose/Skiko em Wasm.
- Gradle manteve os avisos ja conhecidos de compatibilidade AGP 9 registrados na FASE 9b.

## Validacao da FASE 9c — modelos puros reais em `commonMain`

Data da validacao: 2026-07-09.

Objetivo desta etapa: extrair/criar em `commonMain` os modelos puros de dominio
descritos em `docs/spec/27-KMP-PWA-IOS-ESTRATEGIA.md` (secao 4, repositorio
Android principal), sem Firebase, MSAL, parser XLS oficial, login real ou
integracao real com qualquer backend.

Modelos criados em `model/DomainModels.kt`:

- `SchedulePeriod`, `ScheduleAssignment`, `OnCallPeriod`, `OnCallAssignment`,
  `ShiftSwapRequest`, `ImportJob`, `SourceFileRecord`;
- enums/value objects: `MemberRole`, `AssignmentSource`, `ImportStatus`,
  `OnCallStatus`, `SwapStatus`, `ScheduleSourceType`.

`Member` e `Team` (em `model/ScheduleModels.kt`) ganharam os campos que
faltavam para bater com a spec (`id`, `teamId`, `role`, `active` em `Member`;
`id`, `displayName`, `members` em `Team`), todos com valor padrao para nao
quebrar os usos existentes na UI mock e no parser experimental.

Datas e horarios permanecem como `String`, pois o laboratorio ainda nao tem
estrategia multiplataforma definida para data/hora.

Mocks atualizados em `model/MockSchedule.kt`: `mockTeamMembers()` e novas
funcoes (`mockSchedulePeriod`, `mockScheduleAssignments`, `mockOnCallPeriod`,
`mockOnCallAssignments`, `mockShiftSwapRequests`, `mockImportJob`,
`mockSourceFileRecord`) que exercitam os novos modelos. Ainda nao sao
consumidos pela UI — servem para validar que compilam e sao usaveis nas
plataformas Android e Web/Wasm.

Limites assumidos:

- nenhum arquivo do app Android principal (`EscalaSOC`) foi alterado;
- nenhuma integracao real com Firebase, MSAL, parser XLS oficial ou Dropbox
  foi criada;
- os novos modelos ainda nao estao conectados a UI, apenas aos mocks.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:assembleDebug :composeApp:wasmJsBrowserDistribution
```

Resultados:

- APK debug do laboratorio continuou compilando.
- Distribuicao Web/Wasm continuou compilando (mesmos avisos conhecidos de
  tamanho de bundle Compose/Skiko em Wasm).

## Validacao da FASE 9b

Data da validacao: 2026-07-07.

Comandos executados com sucesso:

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDistribution
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
~/Android/Sdk/platform-tools/adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
~/Android/Sdk/platform-tools/adb shell am start -n br.com.leorvergani.escalaici.kmp.lab/.MainActivity
```

Resultados confirmados:

- APK debug gerado em `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.
- App Android abriu no emulador com o pacote `br.com.leorvergani.escalaici.kmp.lab`.
- Dump de UI confirmou titulo, botao `Atualizar mock`, cards `Hoje` e `Escala`, usuario `lvergani`, time `SOC` e lista mock com Manha, Folga, Noite e Tarde.
- Dev server Web/Wasm ativo em `http://localhost:8080/`.
- `index.html`, `manifest.json` e `service-worker.js` responderam HTTP 200 no dev server.
- Build Web/Wasm de distribuicao gerado em `composeApp/build/dist/wasmJs/productionExecutable/`.
- Nenhum crash fatal do pacote do lab foi encontrado no filtro basico de logcat.

Observacoes tecnicas da POC:

- O PWA e propositalmente basico: manifest, service worker simples e icone placeholder SVG.
- O build usa flags temporarias de compatibilidade do AGP 9 com KMP (`android.builtInKotlin=false` e `android.newDsl=false`), gerando avisos que devem ser revisitados em fase futura.
- O bundle Web/Wasm gera avisos de tamanho por incluir Compose/Skiko; isso nao bloqueia a validacao da viabilidade.
