# Escala ICI KMP Lab

Laboratorio paralelo da FASE 9b para validar Kotlin Multiplatform com Android e Web/Wasm.

Este projeto nao porta o app real e nao depende de Firebase, MSAL ou parser XLS.

## Escopo

- Target Android.
- Target Web/Wasm.
- UI minima em Compose Multiplatform.
- Modelos e dados mock em `commonMain`.
- PWA basico como POC: `manifest.json`, service worker e icone placeholder SVG.

## Fora do escopo

- Firebase.
- MSAL.
- Importacao XLS.
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
