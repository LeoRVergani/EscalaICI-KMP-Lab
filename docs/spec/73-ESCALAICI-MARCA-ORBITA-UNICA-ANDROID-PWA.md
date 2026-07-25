# FASE 14M — Marca "órbita" única do projeto (Android + PWA + dentro do app)

**Status:** implementado
**Escopo:** `EscalaICI-KMP-Lab` (Android + Web/Wasm), `commonMain`/`androidMain`/`wasmJsMain`
**Base:** `feature/fase-14l-icon-splash-dark-blue` @ `versionCode 34`/`versionName 0.7.20`
**Branch:** `feature/fase-14m-icon-orbita-unico`

## 1. Objetivo

Substituir por completo o símbolo de calendário+relógio (fechado na FASE 14L) pela marca "órbita"
já adotada no dashboard (`escala-dashboard`, FASE 14K) — um único símbolo para todo o projeto
Escala ICI, em vez de dois símbolos diferentes por repositório.

## 2. Fonte única

Origem recebida: `/home/lvergani/Downloads/escala-ici-mark_f99b5596.webp` (1920×1920, RGBA
transparente). Comparação com `escala-dashboard/public/brand/escala-ici-mark.webp`:

- **Não são byte-idênticos** (hashes SHA-256 diferentes, tamanhos de arquivo diferentes — 103.870
  vs. 109.926 bytes) — mas são a **mesma arte**: mesma resolução (1920×1920), diff de pixel
  (`ImageChops`/`numpy`) com média de 1,27/255 (0,5%) e máximo de 39/255 (~15%) concentrado em
  poucos pixels de borda/gradiente, exatamente o padrão esperado de uma recompressão WebP com
  perdas (o dashboard reexportou/otimizou o arquivo em algum momento da FASE 14K). Nenhuma
  divergência de desenho, cor ou proporção.
- Cópia aprovada versionada em `design/source/escala-ici-orbit-mark.webp` (a partir do arquivo
  original recebido, não da cópia já reprocessada do dashboard) — fonte definitiva do pipeline
  deste repositório, nunca mais dependente de `/home/lvergani/Downloads`.

## 3. Safe zone e escala

A marca órbita é bem mais larga/diagonal que o calendário anterior: a distância radial do centro
até o pixel de conteúdo mais distante é 84,7% do raio do canvas (contra os ~33% exigidos pela
safe zone circular do adaptive icon, 66dp/108dp). Em escala 1:1 ela seria cortada por qualquer
máscara circular (selo verde e as duas pontas do anel visivelmente fora do círculo).

Testados via preview automatizado (composição sobre `#070B12` + anel de referência da safe zone +
máscara circular real): 100%, 70%, 55% e 42% do canvas. **Escolhida: 55%** — cabe com folga
(~24% de margem) dentro da safe zone circular, mantendo o símbolo grande e legível. Confirmado
visualmente na máscara circular real (launcher redondo) e no ícone maskable da PWA (que usa a
mesma escala; a safe zone do maskable é mais generosa, 80%, então a mesma escala serve para os
dois sem ajuste adicional).

## 4. Assets substituídos

Mesmo pipeline de composição da FASE 14L (fundo `#070B12` inalterado, recorte central 288×288 de
432×432 + escala para os ícones legados, máscara circular anti-aliased para as versões redondas):

- `ic_launcher_foreground.png` / `ic_splash_icon.png` — arte transparente, marca órbita a 55% do
  canvas de 432×432.
- `mipmap-{m,h,x,xx,xxx}hdpi/ic_launcher.png` + `ic_launcher_round.png` (10 arquivos).
- `icons/icon-{192,512}.png`, `favicon-32.png` — arte transparente (mesma proporção).
- `icons/icon-maskable-{192,512}.png` — composta sobre `#070B12` (mesma técnica da FASE 14L).
- `service-worker.js`: `CACHE_NAME` incrementado (v8 → v9) para não servir os ícones antigos do
  cache offline da PWA já instalada.

Nenhuma cor mudou (`colors.xml`/`styles.xml`/`manifest.json`/`index.html` já eram `#070B12` desde
a FASE 14L) — só os arquivos de imagem.

## 5. Achado fora do escopo original: marca também vivia dentro do app

Durante a validação, o usuário identificou que o cabeçalho interno do app (topo de Hoje/Escala/
Importar/Alertas/Perfil) ainda mostrava o símbolo antigo — não é um asset de imagem, é
`LabShieldLogo` (`ui/components/SocLogo.kt`), um `Canvas` que **desenhava o calendário
proceduralmente** (retângulos arredondados + círculos + gradiente roxo/azul), código totalmente
independente dos ícones de launcher/PWA. Substituído: `LabShieldLogo` agora renderiza a mesma
arte da órbita via `Image(painterResource(...))`, usando um novo recurso Compose Multiplatform
(`composeResources/drawable/escala_ici_orbit_mark.png`, 256×256, marca a 85% do canvas — sem a
restrição de safe zone do adaptive icon, já que não passa por máscara de launcher). Único ponto
de consumo (`LabAppTitle`), usado pelos 5 cabeçalhos das abas — corrigido uma vez, propaga para
o app inteiro. `LoginGateScreen`/tela de sessão não têm ícone próprio (só texto), nada a mudar
ali.

## 6. Validação

- `testDebugUnitTest`, `wasmJsTest`, `compileDebugKotlinAndroid`, `compileKotlinWasmJs`,
  `assembleDebug`, `assembleRelease`, `wasmJsBrowserDistribution`: todos verdes. 298 testes JVM /
  291 Wasm, 0 falhas (mesma contagem da FASE 14L — mudança de asset/composable visual, nenhuma
  lógica de domínio tocada).
- **Android (emulador, sessão MSAL real)**: launcher (dock) com a nova marca, máscara circular
  sem corte nem franja; splash cold start com a marca centrada sobre `#070B12`, sem flash roxo/
  branco; login real (SSO) → Hoje com dado real intacto (Folga no dia certo, semana destacando
  "hoje" corretamente — bugfixes da FASE 14J.1 preservados); cabeçalho da aba Hoje e da aba
  Perfil confirmados com a nova marca; warm start; logout limpo.
- **Web/PWA (Chromium headless real via CDP)**: manifest/favicon/ícones (192/512/maskable)
  respondem HTTP 200, zero 404; `theme-color` = `#070B12`; console sem erros; tela de entrada
  renderiza igual ao Android. Login MSAL interativo Web não foi exercitado (mesma limitação já
  registrada em checkpoints anteriores — exigiria OAuth real).

## 7. Critérios de aceite

1. Nenhuma ocorrência do símbolo calendário+relógio restante em nenhum lugar do app (launcher,
   splash, PWA, favicon, cabeçalho interno).
2. Símbolo novo idêntico ao usado pelo Dashboard (mesma arte-fonte, mesma cor de fundo do
   projeto).
3. Safe zone respeitada em todas as máscaras testadas (círculo, redondo Android, maskable PWA).
4. Login, logout, novo login e os bugfixes de "Hoje"/semana da FASE 14J.1 continuam funcionando.
5. 298 testes JVM / 291 Wasm mantidos, 0 falhas.
6. Nenhum upload automático ao Dropbox; nenhuma escrita no Firebase.
