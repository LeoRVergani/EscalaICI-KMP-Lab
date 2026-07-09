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
