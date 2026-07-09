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
