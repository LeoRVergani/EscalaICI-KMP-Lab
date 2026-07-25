# FASE 14L — Nova identidade visual: ícone e splash azul-marinho

**Status:** implementado
**Escopo:** `EscalaICI-KMP-Lab` (Android + Web/PWA), apenas assets visuais e cores de
inicialização — nenhum código de autenticação, Firestore ou estado global alterado.
**Base:** `feature/fase-14j1-admin-view-as-member` @ `versionCode 33`/`versionName 0.7.19`
**Branch:** `feature/fase-14l-icon-splash-dark-blue`

## 1. Identidade visual anterior

Ícone e splash usavam um fundo roxo sólido (`#2F145C`), com o mesmo símbolo de
calendário+relógio em violeta/lilás/branco sobre esse fundo. O restante do app (telas Compose)
já era azul-marinho escuro (`LabColors.background = #070B12`) desde a origem do projeto — só o
ícone/splash/PWA ficaram presos à paleta roxa antiga, criando uma inconsistência visual entre o
primeiro contato (launcher, splash) e o app em si.

## 2. Nova paleta

Nenhuma cor nova foi inventada: o fundo escolhido é exatamente `LabColors.background` (`#070B12`),
já usado em toda a UI do app (`composeApp/src/commonMain/kotlin/.../ui/theme/LabColors.kt`). Isso
garante consistência total entre launcher, splash e o app em execução — a mesma cor aparece do
primeiro frame ao último.

| Papel | Cor | Onde |
|---|---|---|
| Fundo principal (ícone/splash) | `#070B12` | `brand_icon_background`, `manifest.json`, `index.html`, status/nav bar |
| Destaque principal | violeta (já existente no símbolo) | calendário/relógio, inalterado |
| Destaque suave | lilás (já existente no símbolo) | grade do calendário, inalterado |
| Contraste | branco (já existente no símbolo) | ponteiros do relógio, texto do login |

O símbolo (calendário com relógio, `ic_launcher_foreground.png`/`ic_splash_icon.png`) **não foi
redesenhado** — já era arte transparente (violeta/lilás/branco, sem fundo próprio), preservada
byte a byte. Só o que estava atrás dela mudou.

## 3. Adaptive icon e safe zone

`mipmap-anydpi-v26/ic_launcher.xml`/`ic_launcher_round.xml` já separam fundo (cor) de frente
(drawable) — a troca de `brand_icon_background` em `values/colors.xml` (`#2F145C` → `#070B12`) por
si só corrige o adaptive icon real (API 26+) sem tocar em nenhum PNG.

Os PNGs flatten de fallback (`mipmap-{m,h,x,xx,xxx}hdpi/ic_launcher.png` e `ic_launcher_round.png`,
usados por launchers/API sem suporte a adaptive icon) foram **recompostos**, não redesenhados:
1. Confirmado empiricamente que a convenção de bake destes arquivos é recorte central de 288×288
   (72dp) do canvas adaptive de 432×432 (108dp), depois redimensionado para o tamanho de cada
   densidade — a mesma matemática usada pelo Android Studio ao gerar os assets originais.
2. Reaplicada essa mesma transformação com o novo fundo `#070B12` + o `ic_launcher_foreground.png`
   já existente, para as 5 densidades × {quadrado, redondo (com máscara circular
   anti-aliased)} = 10 arquivos.
3. Verificado que o conteúdo do símbolo permanece dentro da safe zone (círculo de 66dp
   centrado em 108dp) sem qualquer recorte — a arte já respeitava essa margem antes da troca.

`ic_launcher_background.png` (432×432, um gradiente azul/preto de uma iteração de design anterior
à FASE 11.0b) **não é referenciado por nenhum XML** — o adaptive icon usa `@color`, não esse
drawable. Foi deixado intocado por estar fora do escopo (arquivo órfão, sem efeito no app).

## 4. Splash Android

`drawable/splash_screen.xml` (fallback pré-API31) e `values-v31/styles.xml`
(`windowSplashScreenBackground`/`windowSplashScreenAnimatedIcon`, Android 12+) já apontavam para
`@color/brand_icon_background` e `@drawable/ic_splash_icon` — a mesma troca de cor em
`colors.xml` corrige os dois caminhos simultaneamente, sem duplicar a mudança. `ic_splash_icon.png`
(arte transparente, igual ao foreground) foi mantido sem alteração.

`statusBarColor`/`navigationBarColor` em `values/styles.xml` e `values-v31/styles.xml` (usados
durante a splash e antes do Compose assumir os insets) foram alinhados de `#101418` (aproximação
antiga) para `#070B12` exato — elimina qualquer diferença perceptível entre a barra de sistema e o
fundo da splash/app.

## 5. PWA e Web

- `manifest.json`: `background_color`/`theme_color` de `#101418` → `#070B12`.
- `index.html`: `<meta name="theme-color">` e o `background` do `<body>` (fallback antes do
  Compose para Wasm montar) também para `#070B12`.
- `icons/icon-192.png`/`icon-512.png`/`favicon-32.png`: já eram arte transparente (mesmo símbolo,
  sem fundo próprio) — preservados sem alteração.
- `icons/icon-maskable-192.png`/`icon-maskable-512.png`: tinham fundo roxo sólido (`#2E1065`)
  cravado nos próprios pixels (obrigatório para o formato "maskable", que não aceita
  transparência). Recompostos colando o mesmo `icon-192.png`/`icon-512.png` transparente sobre um
  novo fundo `#070B12`, preservando a mesma proporção/enquadramento do símbolo.
- `service-worker.js`: `CACHE_NAME` incrementado (`escala-ici-web-v7` → `v8`) para forçar a
  invalidação do cache antigo — sem esse bump, navegadores com a PWA já instalada continuariam
  servindo os ícones roxos antigos do cache offline indefinidamente.
- `icon.svg`/`icon-maskable.svg`: encontrados durante a auditoria, mas **não referenciados por
  `manifest.json`** (que aponta só para os `.png`) — são rascunhos órfãos de uma iteração de
  design anterior (mesma leva do `ic_launcher_background.png` do Android, FASE 11.0b). Deixados
  intocados por estarem fora do fluxo real de renderização.

## 6. Fora de escopo (confirmado, nada alterado)

Autenticação/MSAL, Firestore, estado global da tela Hoje, cache de identidade da spec 70, contrato
`teamId` da spec 69, "Visualizar como colaborador" (spec 68) — nenhum desses arquivos foi tocado
nesta fase. `git diff --stat` desta branch contém apenas assets de ícone/splash, arquivos de
cor/tema, `manifest.json`, `index.html`, `service-worker.js` e a própria fonte de versão.

## 7. Validação

**Testes automatizados**: 298 testes JVM + 291 testes Wasm/Chromium, 0 falhas (mesma contagem da
FASE 14J.1 — nenhum teste foi removido ou alterado, pois nenhuma lógica de domínio mudou).
`compileDebugKotlinAndroid`, `compileKotlinWasmJs`, `assembleDebug`, `assembleRelease` e
`wasmJsBrowserDistribution` — todos com sucesso.

**Android (emulador, API 37, sessão MSAL real via SSO)**:
1. Ícone no launcher (dock) renderiza com fundo azul-marinho, símbolo intacto, máscara circular
   sem qualquer franja branca ou resquício do roxo antigo.
2. Cold start: splash mostra fundo `#070B12` + símbolo centrado desde o primeiro frame — sem
   flash roxo, sem flash branco.
3. Transição splash → tela de login: mesma cor de fundo, sem salto perceptível.
4. Login real (SSO silencioso) → Hoje carrega dado real (revisão vigente, card "Folga" no dia
   correto, tira semanal destacando "hoje" corretamente — confirma que os dois bugfixes da FASE
   14J.1 continuam válidos).
5. Warm start (home → reabrir pelos recentes): retorno instantâneo ao estado em execução.
6. Logout → tela de login limpa, no mesmo tema.
7. Novo login → Custom Tab abre corretamente em `login.microsoftonline.com`; cancelamento do
   usuário tratado com mensagem de erro clara ("Login corporativo cancelado pelo usuário."), sem
   crash, mesmo padrão de tratamento de erro já existente.

**Web/PWA (Chromium headless real via CDP, build `wasmJsBrowserDistribution` servido localmente)**:
- `manifest.json` servido com `background_color`/`theme_color` = `#070B12`.
- `<meta name="theme-color">` e `getComputedStyle(document.body).backgroundColor` = `#070B12`.
- Todos os ícones (`icon-192`, `icon-512`, `icon-maskable-192`, `icon-maskable-512`,
  `favicon-32`) respondem HTTP 200 — nenhum 404.
- Service worker registra e ativa (`state: activated`) sob o novo `CACHE_NAME` v8.
- Console sem erros/exceções (só o trace de diagnóstico já esperado,
  `[Web] resolutionEffect start ...`).
- Login MSAL interativo Web não foi exercitado nesta rodada (exigiria OAuth real) — mesma
  limitação já registrada nos checkpoints anteriores.

## 8. Critérios de aceite

1. Nenhum fundo roxo remanescente em ícone, splash ou PWA — confirmado por auditoria de todas as
   ocorrências de `#2F145C`/`#101418`/`#2E1065` no código-fonte (não em `build/`).
2. Símbolo de calendário/relógio preservado pixel a pixel onde já era transparente
   (`ic_launcher_foreground.png`, `ic_splash_icon.png`, `icon-192/512.png`, `favicon-32.png`).
3. Ícones recompostos (mipmap launcher + PWA maskable) sem franjas brancas, sem perda de
   qualidade, respeitando a safe zone do adaptive icon.
4. Splash sem flash roxo/branco em cold start; warm start sem regressão.
5. Login, logout e novo login continuam funcionando (nenhuma mudança em MSAL/auth).
6. 298 testes JVM / 291 Wasm continuam passando sem alteração de contagem.
7. Nenhum upload automático ao Dropbox; nenhuma escrita no Firebase; specs 69/70 preservadas como
   documentação (69: mecanismo confirmado, nenhuma migração necessária; 70: proposta, não
   implementada).
