# Escala ICI KMP Lab

Laboratorio paralelo da FASE 9b para validar Kotlin Multiplatform com Android e Web/Wasm.

Este projeto nao porta o app real e nao depende de Firebase, MSAL ou parser XLS.

## Escopo

- Target Android.
- Target Web/Wasm.
- UI minima em Compose Multiplatform.
- Modelos e dados mock em `commonMain`.
- PWA basico como POC: `manifest.json`, service worker e icone placeholder SVG.
- Importacao XLS experimental dentro do laboratorio, sem reaproveitar nem alterar o parser oficial Android.

## Fora do escopo

- Firebase.
- MSAL.
- Parser XLS oficial do app Android principal.
- App Android principal.
- iOS compilavel. iOS fica para estudo futuro com macOS/Xcode.

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
