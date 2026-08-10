# CHECKPOINT FASE 17B — Contrato Firebase + Validação do Build Web/Wasm

Branch `feature/fase-17b-contrato-firebase-web`, base `e96903a`. Fonte de verdade consultada via clone read-only de `github.com/LeoRVergani/Escala-ICI` (branch `main`, HEAD `36587b3`) — **não** o diretório `/home/lvergani/Projetos/escala-ici` (confirmado na FASE 17A como um projeto não relacionado).

---

## 1. Achado principal desta fase — causa raiz do problema de login Web

**O uso de `HttpClient(CIO)` neste projeto/runtime Web/Wasm falhou em Chromium real ao tentar acessar o módulo `net` do Node.js.** Clicar em "Entrar" (com Playwright + Chromium real, servindo o build via HTTP local, não `file://`) produz:

```
IllegalArgumentException: Node.js net module is not available. Please verify that you are using Node.js
```

**Causa:** `CoordinatorFactory.kt` instancia `HttpClient(CIO)` em código **comum** (`commonMain`), usado por Android e Web igualmente. O engine **CIO** do Ktor implementa rede via sockets TCP brutos — disponíveis na JVM (Android) e em Node.js, mas **não existem em navegadores**, que só oferecem `fetch`/`XMLHttpRequest`. Ao rodar dentro de um browser real (não dentro do Node/Karma que orquestra `wasmJsBrowserTest`), o engine CIO tenta acessar o módulo `net` do Node e falha imediatamente.

**Por que isso nunca foi pego antes:**
- `wasmJsBrowserTest` roda o app compilado dentro de um browser controlado por Karma/Node, mas os testes atuais usam `libs.ktor.client.mock` (`build.gradle.kts:47`, dependência de `commonTest`) — nenhum teste automatizado exercita o `HttpClient(CIO)` real contra rede.
- `restoreSession()` no primeiro carregamento não encontra sessão salva (localStorage vazio) e retorna sem nunca chamar a rede — por isso a tela carrega normalmente e não mostra erro até o usuário de fato tentar logar.
- O erro é lançado como excecão JS não tratada visível só no console do navegador — a UI simplesmente permanece com a mensagem genérica "Entre para ver sua escala.", sem nunca mostrar o erro real.

**Evidência corroborante:** o warning do webpack `Critical dependency: the request of a dependency is an expression`, visto tanto no build de produção quanto no `wasmJsTest`, é o padrão clássico de um `require()` dinâmico sobrevivendo ao bundling — exatamente o que se espera do engine CIO tentando `require('net')` condicionalmente mesmo dentro do bundle de navegador.

**Reprodução:** 100% consistente em 2 execuções independentes (Playwright + Chromium real headless, servido via `python3 -m http.server`, sem `file://`).

**Não corrigido nesta fase** — trocar o engine HTTP para Web (o caminho correto é o engine `Js` do Ktor, que usa `fetch`, mantendo CIO só para Android/JVM via `expect/actual`) é uma mudança estrutural em código compartilhado (`CoordinatorFactory.kt`), com risco de afetar também o caminho Android se malfeita. A FASE 17B pediu explicitamente **"apenas provar"** o problema, não mudar o mecanismo de login (seção 22). Registrado como pendência de maior prioridade para uma fase dedicada — ver seção "Pendências".

---

## 2. Contrato oficial `tiposTurno`

`packages/contrato/src/tipos.ts` (Escala-ICI/main):

```ts
export type Categoria = 'TRABALHO' | 'PLANTAO' | 'EXTRA' | 'DESCANSO' | 'COMPENSACAO' | 'AUSENCIA';
export interface TipoTurno {
  codigo: string; descricao: string; categoria: Categoria;
  horaInicio?: string; horaFim?: string; duracaoMinutos: number;
  viraDia: boolean; contaComoPlantao: boolean; pesoPlantao: number;
  corHex: string; aliasesXLS: string[];
}
```

Leitura real (`lib/firebase/readRepository.ts:34-49`, `listarCatalogo`): consulta `tiposTurno` por `equipeId`; se vazio, usa `CATALOGO_SOC` (`packages/contrato/src/catalogo.ts`); por documento, `codigo = dados.codigo ?? snapshot.id.split('_').at(-1) ?? ''` — fallback para o ID do documento quando o campo `codigo` não existe no payload (documentos legados). Documentos são seedados com ID `{equipeId}_{codigo}` (`seed/seed.ts:65`, ex. `EQ_SOC_M`).

## 3. DTO/modelo KMP `tiposTurno`

`dto/RemoteDtos.kt:66-78` (`TipoTurnoRemoteDto`) — campos idênticos ao contrato oficial, na mesma ordem conceitual. `CatalogoPadrao.kt` (`CATALOGO_SOC`) é porte literal byte-a-byte do catálogo oficial (12 códigos, mesmas cores/horários/categorias/aliases, comparado item a item — sem divergência).

`RemoteDtoMappers.tipoTurno()` (`RemoteDtoMappers.kt:39-56`) fazia `codec.string(f, "codigo") ?: return null` — **descartava o documento inteiro** quando faltava `codigo`, ao contrário do fallback do contrato oficial. **Corrigido nesta fase** (ver seção 15).

## 4. Matriz campo-a-campo `tiposTurno`

| CAMPO | OFICIAL | DTO KMP | MAPPER | USADO NA UI? | STATUS |
|---|---|---|---|---|---|
| `codigo` | `string`, fallback ID doc | `String` | ver seção 3 | Sim (chave do catálogo) | **DIVERGENTE → CORRIGIDO** |
| `descricao` | `string` | `String` | direto | Sim | OK |
| `categoria` | enum 6 valores | `CategoriaTurno` enum, default `TRABALHO` se ausente/inválido | `runCatching{valueOf}` | Sim (`categoriaTrabalha` em `JornadaDia.kt`) | OK |
| `horaInicio` | `string?` | `String?` | direto, pode ser null | Sim (`JornadaDia.horario`) | OK — nunca vira `00:00`/`--:--`, vira string vazia (`JornadaDia.kt:26`) |
| `horaFim` | `string?` | `String?` | idem | Sim | OK |
| `duracaoMinutos` | `number` | `Int` | direto | Sim (`JornadaDia.trabalha`) | OK |
| `viraDia` | `boolean` | `Boolean` | direto | Parseado, **não consumido em UI** (nenhum uso fora do DTO/mapper) | VALIDADO MAS NÃO USADO |
| `contaComoPlantao` | `boolean` | `Boolean` | direto | Não (Plantão não tem contrato Firebase ainda — FASE 17A seção 14) | IGNORADO INTENCIONALMENTE |
| `pesoPlantao` | `number` | `Int` | direto | Não | IGNORADO INTENCIONALMENTE |
| `corHex` | `string` | `String`, default `#9E9E9E` se ausente | direto | **Não usado em nenhum Composable** | `CORHEX_KMP_IGNORADO_INTENCIONALMENTE` |
| `aliasesXLS` | `string[]` | `List<String>` | direto | Só relevante para import XLS local, não para o caminho Firebase | OK (fora de escopo desta fase) |

## 5. Catálogo SOC

Comparação item a item entre `packages/contrato/src/catalogo.ts` e `CatalogoPadrao.kt`: **12/12 códigos idênticos** (`MD, M, T, N, X, DF, DU, BH, FOLGA, AN, HE, AFA`) — mesma descrição, categoria, horário, `viraDia`, `duracaoMinutos`, `corHex`, `aliasesXLS`. Sem divergência.

## 6. Contrato oficial `turnosMes`

`packages/contrato/src/tipos.ts:47-63`:

```ts
export interface TurnosMes {
  schemaVersion: number; usuarioUid: string; login: string; equipeId: string;
  competencia: string; periodoInicio: string; periodoFim: string; turnoPadrao: string;
  status: 'RASCUNHO' | 'PUBLICADA'; dias: Record<string, Dia>; totais: Totais;
  importacaoId?: string; publicadoPor?: string | null; publicadoEm?: string | null; atualizadoEm?: string;
}
export interface Dia { c: string; i?: string; f?: string; m?: number; vd?: boolean; seq?: number; }
export interface Totais { min, diasTrabalhados, df, du, x, he, bh, an, folga, afa: number; }
```

## 7. DTO/modelo KMP `turnosMes`

`dto/RemoteDtos.kt:43-60` (`TurnosMesRemoteDto`) — todos os campos presentes, mesmos tipos/nulabilidade. `DiaRemoteDto`/`TotaisRemoteDto` idênticos campo-a-campo aos `Dia`/`Totais` oficiais. Comentário no próprio DTO já documenta a pegadinha do `usuarioUid` (ver seção 9).

## 8. Matriz campo-a-campo `turnosMes`

| CAMPO | STATUS |
|---|---|
| `schemaVersion` | CONSUMIDO (default 1 se ausente, `RemoteDtoMappers.kt:65`) |
| `usuarioUid` | CONSUMIDO — mas contém o mesmo valor de `login`, nunca um UID real (ver seção 9) |
| `login` | CONSUMIDO — identidade funcional |
| `equipeId` | CONSUMIDO |
| `competencia` | CONSUMIDO |
| `periodoInicio` | CONSUMIDO — fonte de verdade do período (ver seção 10) |
| `periodoFim` | CONSUMIDO |
| `turnoPadrao` | CONSUMIDO (default `""`), **não usado na UI** hoje |
| `status` | CONSUMIDO — gate PUBLICADA/RASCUNHO (ver seção 11) |
| `dias` | CONSUMIDO — mapeado para `JornadaDia` (ver seção 13) |
| `totais` | **VALIDADO MAS NÃO USADO** (ver seção 12) |
| `importacaoId` | CONSUMIDO no DTO, não usado na UI |
| `publicadoPor` | idem |
| `publicadoEm` | idem |
| `atualizadoEm` | idem |

## 9. Identidade `usuarios/{login}`

**Confirmado alinhado, sem divergência.** `email → normalização (parte antes do "@") → login → usuarios/{login}` — mesma regra em `Identity.kt:11-12`, `AuthRepository.kt:49-51`, `UsuarioRepository.kt`. O campo `usuarioUid` dentro de `TurnosMesRemoteDto` é legado (contém o mesmo valor de `login`, não um UID Firebase real — comentário explícito no DTO, `RemoteDtos.kt:45`) e **não altera** a regra de identidade funcional. Nenhum uso de `usuarios/{uid}` encontrado em nenhum lugar do KMP.

## 10. `periodoInicio` / `periodoFim`

Confirmado: `CurrentScheduleResolver.kt` lê `periodoInicio`/`periodoFim` diretamente do documento `turnosMes` PUBLICADA retornado pelo Firestore — não recalcula a regra 26→25 localmente para esse propósito. `PARIDADE_CALENDARIO_26_25_PENDENTE` refere-se à **navegação do calendário** (grade mês-a-mês vs. grade única — FASE 17A seção 10), não à leitura do período em si, que já está correta. **Não implementado nesta fase**, conforme instruído.

## 11. PUBLICADA vs RASCUNHO

Confirmado corretamente diferenciado: `CurrentScheduleResolver.kt:70,81` consulta com `status=='PUBLICADA'` explícito e reconfirma via `it.status == TurnosMesStatus.PUBLICADA`; `TeamScheduleRepository.kt:81` idem para a escala da equipe. RASCUNHO nunca é selecionado como escala ativa do colaborador. Sem divergência.

## 12. Totais

**KMP recalcula localmente, não usa `totais` do documento.** `TurnosMesRemoteDto.totais` é parseado (`RemoteDtoMappers.kt:75`) mas nunca lido em `EscalaIciScheduleMapper.kt` nem em nenhum outro lugar de `commonMain` (grep confirma zero usos fora da própria declaração do DTO). Em vez disso, `ScheduleSummary` (`ScheduleModels.kt:110-117`) calcula `workedDays`/`restDays`/`totalHours` localmente, com **`totalHours = workedDays * 6`** — uma suposição fixa de 6h por dia trabalhado (coerente com o catálogo atual, onde todos os turnos `TRABALHO` têm `duracaoMinutos=360`, mas frágil se o catálogo mudar). Não alterado nesta fase (instrução explícita: "não alterar UI"; o recálculo alimenta a UI de Hoje/Escala). Registrado como divergência de fonte, não como bug.

## 13. Dias/mapping

`resolverJornadaDia()` (`JornadaDia.kt:33-58`) — porte de `resolverJornadaDia`/`categoriaTrabalha` (`packages/contrato/src/jornada.ts`). Tratamento seguro confirmado:
- Dia ausente no mapa (`dias[data]` inexistente) → `"Sem escala publicada"`, não crasha.
- Código de turno desconhecido no catálogo (`catalogo[dia.c]` null) → `descricao = dia.c` (código bruto como fallback), `inicio`/`fim` ficam `null`, `horario` fica string vazia — nunca `"00:00"`/`"--:--"`.
- `viraDia` da jornada (N: 19:00→01:00) é dado do catálogo, não recalculado — coerente.
- Competência atravessando ano: não testado especificamente nesta fase (fora do escopo — a lógica de `dias` é só um `Map<String, Dia>` por data ISO, sem dependência de ano civil).

## 14. Divergências encontradas

1. **`RemoteDtoMappers.tipoTurno()` descartava documentos `tiposTurno` sem campo `codigo`**, ao contrário do fallback por ID de documento do contrato oficial — **CORRIGIDO** (seção 15).
2. **`HttpClient(CIO)` falhou em Chromium real ao tentar acessar o módulo `net` do Node.js** — causa raiz do problema de login Web (seção 1) — **documentado, não corrigido nesta rodada** (fora do escopo permitido desta fase; risco de blast radius no Android; corrigido depois na FASE 17B.1, ver seção dedicada abaixo).
3. `corHex` parseado mas nunca consumido na UI — decisão de design existente, não uma divergência de contrato; registrado como `CORHEX_KMP_IGNORADO_INTENCIONALMENTE`, sem ação.
4. `totais` do documento oficial ignorado em favor de recálculo local com suposição de 6h/dia — registrado, sem ação (exigiria mudança de UI).
5. Gradle: `inputs.file(propertiesFile).optional()` na task `generateFirebaseConfig` ainda lança erro de validação Gradle quando `local.firebase.properties` está **totalmente ausente** (não apenas vazio) — bug pré-existente, não relacionado a staging/produção, não introduzido nem corrigido nesta fase (confirmado reproduzindo o mesmo erro com o `build.gradle.kts` original via `git stash`).

## 15. Correções realizadas

**A) `RemoteDtoMappers.kt` — fallback de `codigo` para documentos legados de `tiposTurno`:**

```kotlin
val codigoFallback = documentId(document)?.substringAfterLast('_')?.takeIf { it.isNotBlank() }
// ...
codigo = codec.string(f, "codigo") ?: codigoFallback ?: return null,
```
Espelha exatamente `listarCatalogo()` (`lib/firebase/readRepository.ts:48`). Testes novos: `tipoTurno_fallsBackToDocumentIdSuffix_whenCodigoFieldMissing`, `tipoTurno_returnsNull_whenCodigoAndDocumentIdBothMissing` (`RemoteDtoMappersTest.kt`).

**B) `build.gradle.kts` — fail-fast de config staging vazia/incorreta** (ver seção 18).

## 16. Pipeline Web atual

`.github/workflows/web-ci.yml` e `publish-cloudflare-branch.yml` (ambos do commit `fadd086`): passo "Configure Firebase" grava `secrets.FIREBASE_PROPERTIES` em `local.firebase.properties`, com `exit 1` se o secret estiver vazio — proteção de presença já existe no CI. Em seguida chamam `scripts/build-web.sh`, que roda `wasmJsTest` + `composeCompatibilityBrowserDistribution`, copia o resultado para `cloudflare-dist/`, e valida apenas a **existência** de `index.html`/`composeApp.js`/`*.wasm` — nenhuma validação de **conteúdo/target** do Firebase antes desta fase.

## 17. Causa provável do antigo problema Web

Ver seção 1 — `HttpClient(CIO)` falhou em Chromium real ao tentar acessar o módulo `net` do Node.js (ausente em qualquer navegador). Este é o achado central desta fase e provavelmente explica por que builds Web anteriores compilavam e publicavam normalmente, mas o login nunca funcionava de fato quando alguém tentava usar o site publicado. Resolvido na FASE 17B.1 (arquitetura: Android → engine CIO, Web/Wasm → engine Ktor `Js`/Fetch API — ver seção dedicada).

## 18. Fail-fast Firebase

Implementado em `composeApp/build.gradle.kts`, dentro da task `generateFirebaseConfig` (afeta Android e Web igualmente, pois ambos dependem da mesma task): se `firebase.environment=STAGING`, exige `projectId`/`apiKey`/`authDomain`/`appId`/`messagingSenderId` não vazios E `projectId == "escala-ici-staging"` — falha com `GradleException` e mensagem clara (sem imprimir `apiKey`) se qualquer condição não for atendida. `LOCAL_EMULATOR` permanece livre de qualquer exigência (build local de desenvolvimento continua funcionando com properties mínimas ou ausentes-porém-com-arquivo-presente).

**Validado empiricamente** (arquivo real de staging salvo em backup antes de cada teste, restaurado depois — confirmado por `md5sum` idêntico ao original):
- STAGING + campos vazios → `BUILD FAILED`, mensagem lista os campos faltantes.
- STAGING + `projectId` errado (`demo-escalaici-kmp`) → `BUILD FAILED`, mensagem indica o projectId esperado.
- LOCAL_EMULATOR + arquivo com só `firebase.environment=LOCAL_EMULATOR` → `BUILD SUCCESSFUL` (comportamento de dev preservado).
- STAGING + config real correta (arquivo original da máquina) → `BUILD SUCCESSFUL`.

Optei por embutir a validação na própria task Gradle (em vez de um script `verify-web-firebase-target.sh` separado) porque é o único ponto por onde toda config passa, para Android e Web — mais difícil de contornar por esquecimento do que um script que precisa ser lembrado/chamado manualmente, e evita duplicar a mesma lógica em dois lugares.

## 19. Target Firebase do artefato Android

`assembleDebug`/`assembleRelease` usam a mesma `GeneratedFirebaseConfig.kt` — confirmado `environment=STAGING`, `projectId=escala-ici-staging` (mesma config lida na seção 20/21).

## 20. Target Firebase do artefato Web

Confirmado via `GeneratedFirebaseConfig.kt` gerado imediatamente antes do build: `environment = FirebaseEnvironment.STAGING`, `projectId = "escala-ici-staging"`.

## 21. Evidência do build Wasm

Strings extraídas do `.wasm` compilado (`f917e0cc882ce4eef6a3.wasm`, decodificação UTF-16LE — Kotlin/Wasm armazena strings assim, por isso uma busca ASCII simples retorna 0 falsamente) confirmam, embutidos no artefato final: `escala-ici-staging`, `escala-ici-staging.firebaseapp.com`, `escala-ici-staging.firebasestorage.app`, `messagingSenderId` numérico coerente com o projeto. Um `apiKey` também apareceu na mesma extração — **seu valor não é reproduzido aqui** (instrução explícita da fase), apenas confirmo que está presente e não vazio. As strings `demo-escalaici-kmp`/`127.0.0.1` também aparecem no binário, mas são os **valores-padrão inertes dos campos `emulatorProjectId`/`emulatorAuthHost`** da classe `EscalaIciFirebaseConfig` (sempre presentes na definição do tipo, independente do ambiente ativo) — não indicam que o Emulator está em uso; o campo `environment` selecionado, confirmado separadamente, é `STAGING`.

## 22. Teste manual Web

Servido via `python3 -m http.server 8099 --bind 127.0.0.1` a partir de `composeApp/build/dist/wasmJs/productionExecutable` (não `file://`). Testado com Playwright (Chromium real, headless, binário já em cache local — `playwright-core` instalado no scratchpad, sem afetar o repositório) via `chromium.launch`.

**Resultados:**
- Página carrega sem erros de console (`[]` no load inicial). Título "Escala ICI".
- Tela de login renderiza corretamente (screenshot capturado): título, campo E-mail, campo Senha, botão "Entrar", mensagem "Entre para ver sua escala." (estado `AUTH_REQUIRED` esperado), link "Modo demonstração" claramente separado e não pré-selecionado.
- Campos aceitam entrada de teclado corretamente (confirmado por screenshot com texto/máscara de senha visíveis).
- **Ao clicar em "Entrar" com credenciais fake** (`teste.nao.existe.fase17b@example.invalid` / senha fake, **nenhuma senha real usada**): nenhuma requisição chega a `identitytoolkit.googleapis.com` — em vez disso, `IllegalArgumentException: Node.js net module is not available` no console (ver seção 1). Não cai em Demo, não mostra "offline" como autenticação — mas também não consegue de fato tentar autenticar.

## 23. Integração staging

**Não executada.** A seção 1 já demonstra que o artefato Web atual não consegue emitir nenhuma requisição real de rede — uma tentativa de integração real contra staging pelo Web seria só uma repetição do mesmo erro, sem valor adicional. Pelo lado Android, as credenciais de teste seguras não foram acionadas nesta fase (fora do escopo: a fase pediu integração "somente se claramente preparada" e o achado da seção 1 tornou isso secundário). `INTEGRACAO_STAGING_REAL_NAO_EXECUTADA` **mantida**.

## 24. Testes

`./gradlew :composeApp:testDebugUnitTest :composeApp:wasmJsTest --rerun-tasks` → **BUILD SUCCESSFUL**. Agregado: **368 testes, 8 skipped, 0 failures, 0 errors** (+4 em relação à FASE 17A: 2 testes novos × JVM + wasmJsBrowserTest).

## 25. Builds

- `./gradlew :composeApp:assembleDebug` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:assembleRelease` → BUILD SUCCESSFUL (executado por a correção em `RemoteDtoMappers.kt` ser código Android compartilhado).
- `./gradlew :composeApp:wasmJsBrowserDistribution` → BUILD SUCCESSFUL, artefato inspecionado (seções 20-22).
- `./gradlew :composeApp:validateSigningRelease` — não solicitado para este repositório nesta fase (tarefa não existe neste projeto; o pedido de `validateSigningRelease` no prompt-base é do outro app, EscalaSOC).

## 26. Arquivos novos

Nenhum arquivo de código novo — só o checkpoint (`CHECKPOINT-FASE-17B-CONTRATO-FIREBASE-WEB.md`). Não foi necessário criar `scripts/verify-web-firebase-target.sh` separado (ver justificativa na seção 18).

## 27. Arquivos modificados

- `composeApp/build.gradle.kts` — fail-fast de config staging (seção 18).
- `composeApp/src/commonMain/kotlin/.../firebase/RemoteDtoMappers.kt` — fallback de `codigo` (seção 15A).
- `composeApp/src/commonTest/kotlin/.../firebase/RemoteDtoMappersTest.kt` — 2 testes novos.

## 28. CHECKPOINT

Este arquivo.

## 29. `git status --short`

```
 M composeApp/build.gradle.kts
 M composeApp/src/commonMain/kotlin/br/com/leorvergani/escalaici/kmp/lab/firebase/RemoteDtoMappers.kt
 M composeApp/src/commonTest/kotlin/br/com/leorvergani/escalaici/kmp/lab/firebase/RemoteDtoMappersTest.kt
```
(este checkpoint aparecerá como `??` depois de salvo)

## 30. `git diff --check`

Limpo (sem saída, exit 0).

## 31. Pendências

- `PARIDADE_CALENDARIO_26_25_PENDENTE` — mantida.
- `PARIDADE_REGRAS_TROCA_GLOBAL_PENDENTE` — mantida.
- `INTEGRACAO_STAGING_REAL_NAO_EXECUTADA` — mantida (motivo: seção 23).
- **NOVA — `WEB_HTTP_ENGINE_INCOMPATIVEL_COM_BROWSER`** — `HttpClient(CIO)` falhou em Chromium real ao tentar acessar o módulo `net` do Node.js; login e qualquer chamada de rede no Web publicado estavam quebrados até essa troca de engine ser feita (recomendado: engine `Js` do Ktor para `wasmJsMain`, via `expect/actual`, mantendo CIO em `androidMain`). Esta é a pendência de maior prioridade prática para o objetivo "Web funcional" do projeto — resolvida na FASE 17B.1 (ver seção dedicada abaixo).
- **NOVA — `GRADLE_LOCAL_FIREBASE_PROPERTIES_AUSENTE_QUEBRA_BUILD`** — bug pré-existente (não introduzido nesta fase): se `local.firebase.properties` estiver totalmente ausente (não apenas vazio), a validação de input do Gradle falha antes até de chegar à lógica de fallback do `doLast`. Afeta qualquer clone novo do repo sem esse arquivo configurado, mesmo para build local de Emulator.

## 32. Git (estado ao final da FASE 17B, antes da 17B.1)

commit: NÃO
push: NÃO
merge: NÃO

---

# FASE 17B.1 — HTTP ENGINE WEB/WASM

## Causa raiz

Confirmada exatamente como diagnosticado na FASE 17B: `createEscalaIciSession()` (`CoordinatorFactory.kt`) instanciava `HttpClient(CIO)` em código comum, compartilhado por Android e Web. **O uso de `HttpClient(CIO)` neste projeto/runtime Web/Wasm falhou em Chromium real tentando acessar o módulo `net` do Node.js** — evidência preservada, reproduzida de forma 100% consistente:

```
IllegalArgumentException: Node.js net module is not available. Please verify that you are using Node.js
```

A aplicação passou a usar o engine **`Js`** do Ktor no target `wasmJs`, que utiliza a **Fetch API** do navegador em vez de sockets. Arquitetura resultante:

```
Android  → engine CIO   (sockets reais, JVM)
Web/Wasm → engine Js    (fetch() nativo do navegador)
```

## Arquitetura anterior

Um único `HttpClient(CIO)`, criado em `commonMain`, sem `expect/actual` — "engine único, válido em Android e Wasm" (comentário agora corrigido, era falso para Wasm em navegador real).

## Arquitetura nova

`expect fun createPlatformHttpClient(): HttpClient` em `commonMain` (`firebase/PlatformHttpClient.kt`), seguindo exatamente o mesmo padrão de `createSessionTokenStore()`/`createRawKeyValueStore()` já existente no projeto. `CoordinatorFactory.kt` passou a chamar `createPlatformHttpClient()` em vez de `HttpClient(CIO)` diretamente — nenhum outro código mudou: `IdentityToolkitAuthClient`, `FirestoreRestClient`, `AuthRepository`, `TrocasEscalaRepository` e todos os demais repositórios continuam 100% `commonMain`, sem duplicação por plataforma.

## Engine Android

`firebase/PlatformHttpClient.android.kt`: `actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO)` — inalterado em comportamento, só movido para um arquivo `actual`. `ktor-client-cio` migrado de `commonMain.dependencies` para `androidMain.dependencies` em `build.gradle.kts`.

## Engine Wasm

`firebase/PlatformHttpClient.wasmJs.kt`: `actual fun createPlatformHttpClient(): HttpClient = HttpClient(Js)` — engine `Js` do Ktor, que usa `fetch()` nativo do navegador. Dependência nova `ktor-client-js` (adicionada a `libs.versions.toml` e a um novo bloco `wasmJsMain.dependencies` em `build.gradle.kts`) — confirmado que o artefato `io.ktor:ktor-client-js` publica uma variante real `-wasm-js` (já presente no cache local do Gradle antes mesmo da mudança, `~/.gradle/caches/modules-2/files-2.1/io.ktor/ktor-client-js-wasm-js`), não foi necessário nenhum artefato extra além do já resolvido pelo Ktor 3.3.0.

Não foi necessário nenhum bloco de configuração compartilhada (`HttpClientConfig<*>.configureEscalaIciHttpClient()`) — `HttpClient(CIO)` original não tinha nenhuma configuração (timeouts/headers/plugins) para preservar; os dois `actual` continuam com a assinatura mínima.

## Dependências Gradle

```
commonMain:  ktor-client-core                (inalterado)
androidMain: ktor-client-cio                 (movido de commonMain)
wasmJsMain:  ktor-client-js                  (novo bloco de dependências)
commonTest:  ktor-client-mock                (inalterado)
```

## Identity Toolkit / Secure Token / Firestore REST

Nenhuma mudança de código nesses três — continuam 100% comuns, recebendo o `HttpClient` já construído pelo engine certo via injeção de construtor (`IdentityToolkitAuthClient(httpClient, config)`, `FirestoreRestClient(httpClient, config)`). Confirmado funcionando via teste real (ver "Chromium real" e "Staging real" abaixo) para os três: login (Identity Toolkit), leitura de `usuarios/{login}` e `turnosMes` (Firestore REST).

## Testes unitários

Nenhum teste novo pela mudança em si (é troca de engine, não lógica testável por `MockEngine` — os testes de `FirestoreRestClientTest.kt` já usam `HttpClient(engine)` com `MockEngine` passado por parâmetro, não `CIO` hardcoded, então não precisaram de ajuste). **368 testes, 8 skipped, 0 failures, 0 errors** — idêntico à FASE 17B, confirmando que a troca de engine não regrediu nada testável por mock.

## wasmJsTest

`./gradlew :composeApp:wasmJsTest --rerun-tasks` → BUILD SUCCESSFUL (56 tasks). O warning de webpack `Critical dependency: the request of a dependency is an expression` ainda aparece (é normal do próprio bundling dos klibs Ktor, não indica mais o bug — confirmado pelo teste em Chromium real abaixo).

## Browser build

`./gradlew :composeApp:wasmJsBrowserDistribution` → BUILD SUCCESSFUL (5m46s). Servido via `python3 -m http.server 8099 --bind 127.0.0.1` a partir de `composeApp/build/dist/wasmJs/productionExecutable` (não `file://`).

## Chromium real

Playwright + Chromium real (mesmo binário em cache já usado na FASE 17B), headless, `--no-sandbox`. Sequência: app abre (título "Escala ICI", 0 erros de console), tela de login renderiza, campo e-mail aceita input (confirmado por screenshot com o texto digitado visível), campo senha aceita input (máscara visível), clique no botão "Entrar" (mouse down/up explícito no centro do botão).

**Resultado: `IllegalArgumentException: Node.js net module is not available` — 0 ocorrências** (antes: 100% reprodutível, toda tentativa). Requisição real capturada: `POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword` → resposta `400` (esperado — credencial de teste intencionalmente inválida, nunca usei senha real no navegador).

## Credencial inválida

E-mail `teste.nao.existe.fase17b1@example.invalid` + senha fake (não registrada em nenhum lugar) → UI mostrou corretamente **"E-mail ou senha inválidos."** com um link "Voltar para o login" (screenshot capturado) — prova a cadeia completa: Browser → `fetch` (engine `Js`) → Firebase real (`escala-ici-staging`) → resposta `400` → `AuthErrorMapper` → mensagem amigável na UI. Nenhuma senha usada foi real ou registrada neste documento.

## Staging real

Mecanismo pré-existente e seguro reaproveitado (`FirebaseIntegrationTest.kt`, gate `ESCALAICI_FIREBASE_STAGING=true`, credenciais lidas de `local.firebase.test.properties`, nunca impressas). Executado via JVM (Android/CIO), **não** via navegador (para não digitar senha real em um script de automação de browser):

```
ESCALAICI_FIREBASE_STAGING=true ./gradlew :composeApp:testDebugUnitTest --tests "*FirebaseIntegrationTest*"
```

- `staging_fullStack_manager_marinaAzevedo_identidadeEquipe` → **PASSOU**: login real + leitura de `usuarios/{login}` com sucesso.
- `staging_fullStack_employee_caioMonteiro` → **FALHOU**, mas por uma condição de dados em staging, não de código: login e leitura de `usuarios/{login}` funcionaram (o teste avançou até a etapa de escala); falhou só ao tentar resolver uma `turnosMes` PUBLICADA para `caio.monteiro/EQ_SOC` em `2026-08-10` (`NO_PUBLISHED_SCHEDULE` — "Nenhuma escala publicada foi encontrada para o seu login."), i.e., não há competência publicada vigente para esse time/data em staging agora. Nenhuma senha impressa em nenhum momento.
- `emulator_fullStack_login_usuario_escala_catalogo_mapper` → skipped (esperado, `ESCALAICI_FIREBASE_EMULATOR` não definido).

Isso confirma, com dados reais e sem mock, que `IdentityToolkitAuthClient`/`FirestoreRestClient`/`UsuarioRepository` funcionam corretamente contra `escala-ici-staging` — o mesmo código-fonte que agora também roda no navegador via o engine `Js`.

## Logout/restauração

**Não testado especificamente com credencial real via navegador** (decisão deliberada: evitar digitar uma senha real em um script de automação, por segurança, conforme instrução da própria fase de não usar senha real nas seções de browser). A troca de engine desta fase **não tocou** `FirebaseAuthRepository`, `SessionTokenStore` nem `LoggedScheduleSyncCoordinator` — a lógica de logout/refresh/isolamento de cache por login é a mesma de antes, já coberta pelos testes existentes (`EscalaIciScheduleCacheTest.kt`, `AuthErrorMapperTest.kt`) e pela suíte de integração Android (FASE 15/16), que não muda com a troca de transporte HTTP. Risco residual considerado baixo, mas não comprovado via browser real nesta fase — registrado como lacuna, não como pendência crítica.

## CORS

Nenhum erro de CORS observado na única chamada de rede exercitada via browser (`identitytoolkit.googleapis.com/v1/accounts:signInWithPassword` — resposta `400` processada normalmente pela UI, não um erro opaco de CORS). Chamadas Firestore REST via navegador com credencial real não foram testadas nesta fase (mesma razão da seção "Logout/restauração" — evitar senha real no browser); o mesmo `FirestoreRestClient` já foi validado com sucesso contra staging real via JVM (seção "Staging real"), usando o mesmo formato de requisição REST que o engine `Js` replica no navegador. Sem uso de `mode: no-cors` em nenhum ponto.

## `local.firebase.properties` ausente

**Causa confirmada e corrigida.** `inputs.file(propertiesFile).withPropertyName(...).optional()` ainda acionava a validação "Property specifies file which doesn't exist" do Gradle 9.4.1 quando o arquivo está totalmente ausente (não apenas vazio) — `.optional()` no DSL fluente não suprime essa validação específica para uma property `InputFile` única. Troquei para `inputs.files(propertiesFile).withPropertyName(...)` (uma `FileCollection`, que não exige existência) — o `doLast` já tratava a ausência via `propertiesFile.exists()`.

**Validado empiricamente** (arquivo real de staging salvo em `/tmp` e restaurado depois, `md5sum` confirmado idêntico ao original em cada teste):
- Arquivo totalmente ausente (`mv` para fora do diretório) → `BUILD SUCCESSFUL`, gera config `LOCAL_EMULATOR` com `projectId=""` (default seguro).
- Arquivo restaurado (STAGING real) → `BUILD SUCCESSFUL`, config correta (`STAGING`/`escala-ici-staging`).
- Reconfirmado que o fail-fast da FASE 17B continua intacto: STAGING + campos vazios → `BUILD FAILED` com a mesma mensagem de antes.

## Testes totais

368 testes, 8 skipped, 0 failures, 0 errors (idêntico à FASE 17B — a troca de engine não é testável por mock e não alterou nenhuma lógica coberta por teste existente).

## Builds

- `./gradlew :composeApp:testDebugUnitTest :composeApp:wasmJsTest --rerun-tasks` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:assembleDebug` → BUILD SUCCESSFUL (1m55s).
- `./gradlew :composeApp:assembleRelease` → BUILD SUCCESSFUL (1m39s).
- `./gradlew :composeApp:wasmJsBrowserDistribution` → BUILD SUCCESSFUL (5m46s), testado em Chromium real (ver acima).
- `git diff --check` → limpo.

## Arquivos novos

- `composeApp/src/commonMain/kotlin/.../firebase/PlatformHttpClient.kt` (`expect`)
- `composeApp/src/androidMain/kotlin/.../firebase/PlatformHttpClient.android.kt` (`actual`, CIO)
- `composeApp/src/wasmJsMain/kotlin/.../firebase/PlatformHttpClient.wasmJs.kt` (`actual`, Js)

## Arquivos modificados

- `composeApp/build.gradle.kts` — dependências `ktor-client-cio`/`ktor-client-js` reorganizadas por source set; fail-fast de `local.firebase.properties` ausente corrigido.
- `gradle/libs.versions.toml` — nova entrada `ktor-client-js`.
- `composeApp/src/commonMain/kotlin/.../firebase/CoordinatorFactory.kt` — usa `createPlatformHttpClient()` em vez de `HttpClient(CIO)` direto.

## Marcadores finais

- `WEB_HTTP_ENGINE_INCOMPATIVEL_COM_BROWSER` = **RESOLVIDO** (comprovado via Chromium real: requisição HTTP real chega a `identitytoolkit.googleapis.com`, resposta processada, erro antigo com 0 ocorrências).
- `GRADLE_LOCAL_FIREBASE_PROPERTIES_AUSENTE_QUEBRA_BUILD` = **RESOLVIDO** (comprovado empiricamente com o arquivo real removido e restaurado).
- `INTEGRACAO_STAGING_JVM_REAL` = **EXECUTADA** — autenticação real (Identity Toolkit) + leitura de `usuarios/{login}` confirmadas com sucesso via JVM/CIO contra `escala-ici-staging` (`staging_fullStack_manager_marinaAzevedo_identidadeEquipe`, seção "Staging real"). O caso do colaborador (`staging_fullStack_employee_caioMonteiro`) chegou até a etapa de escala e falhou só por não haver `turnosMes` PUBLICADA vigente para aquele time/data em staging hoje — condição de dados, não falha de transporte/autenticação.
- `INTEGRACAO_STAGING_BROWSER_REAL` = **PENDENTE** — no navegador (Chromium real, engine `Js`), o Firebase real foi atingido com sucesso usando uma credencial **inválida** (POST chegou a `identitytoolkit.googleapis.com`, resposta 400 processada corretamente pela UI). Um ciclo completo de **login válido + leitura Firestore** especificamente através do navegador ainda não foi executado (decisão deliberada de não digitar senha real em script de automação de browser nesta fase).
- `PARIDADE_CALENDARIO_26_25_PENDENTE` — mantida, inalterada.
- `PARIDADE_REGRAS_TROCA_GLOBAL_PENDENTE` — mantida, inalterada.

## Git

commit: NÃO
push: NÃO
merge: NÃO
