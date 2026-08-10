# Checkpoint FASE 16 — Pré-Staging

Auditoria do commit local `636f517` (branch `feature/fase-16-trocas-reais`). Nenhum código foi alterado nesta auditoria; nenhuma escrita foi feita em staging. Achados que sugerem mudança de comportamento estão marcados como **⚠️ decisão pendente**, não como correção já aplicada.

---

## 1. Foreground

**Arquivo:** `composeApp/src/commonMain/kotlin/br/com/leorvergani/escalaici/kmp/lab/ui/App.kt:176`

```kotlin
ObserveAppForeground { scope.launch { refreshTrocasBadgeQuietly() } }
```

`refreshTrocasBadgeQuietly()` (`App.kt:140-143`) chama **somente** `trocasSession.badge()` — duas queries leves (`buscarMinhasTrocas` + `buscarNotificacoes`), nunca `syncCoordinator.refresh()`.

- **Fluxo atual:** volta ao foreground → 2 queries Firestore (trocas + notificações do usuário) → atualiza só o contador do badge. A escala (Hoje/Escala) **não** é resincronizada automaticamente ao voltar do background.
- **Custo:** 2 requisições leves por volta ao foreground (mesmo padrão de `buscarMinhasTrocas`/`buscarNotificacoes` já usado ao abrir a aba Trocas).
- **Refresh duplicado:** não identificado. `LaunchedEffect(Unit)` (`App.kt:161-165`, roda uma vez na montagem do Composable) já chama `refreshTrocasBadgeQuietly()` na abertura do app; `ObserveAppForeground` só dispara em `ON_START` subsequentes (Android: `ProcessLifecycleOwner`, nunca na primeira composição) — sem sobreposição observada entre os dois.

**Conclusão:** já está no comportamento pedido (refresh leve, não o sync completo da escala). Nenhuma alteração necessária.

---

## 2. Notificação lida

**Existe:** `TrocasEscalaRepository.marcarNotificacaoComoLida` (`TrocasEscalaRepository.kt:256-264`) e `TrocasSession.marcarNotificacaoComoLida` (`TrocasSession.kt:71-73`).

**Patch envia só `lidaEm`:** confirmado. `TrocasRemoteMappers.encodeNotificacaoLidaPatch` (`TrocasRemoteMappers.kt:161-163`) retorna um mapa com uma única chave, e o `FirestoreWrite.Patch` correspondente (`TrocasEscalaRepository.kt:257-262`) usa `updateMaskFieldPaths = listOf("lidaEm")` — o `:commit` grava `updateMask.fieldPaths=["lidaEm"]`, nunca reenvia o documento inteiro.

**⚠️ Em qual interação isso é chamado: NENHUMA hoje.**

Busquei todos os call sites (`grep -rn "marcarNotificacaoComoLida"`) — o único lugar fora da própria camada de dados é o teste de integração (`TrocasEscalaIntegrationTest.kt:111`). `TrocasScreen.kt` nunca chama `trocasSession.marcarNotificacaoComoLida(...)` — a tela lê `notificacoes` só para calcular `naoLidas` no badge (`TrocasScreen.kt:104-121`), mas não marca nada como lida ao abrir a troca, ao responder, nem em nenhum outro ponto da UI.

**Efeito prático:** o contador `naoLidas` nunca some depois que o usuário vê/responde a solicitação — ele só some se a notificação virar lida por outro caminho (que não existe ainda). Isto é uma lacuna real de UX, não um bug de escrita.

---

## 3. Conflitos (409 / 412 / FAILED_PRECONDITION)

**Mapeamento tipado — confirmado.**
`FirestoreRestClient.kt:141-162` (`commit()`) + `isConflictResponse` (`FirestoreRestClient.kt:164-167`):

```kotlin
private fun isConflictResponse(status: HttpStatusCode, rawBody: String): Boolean =
    status == HttpStatusCode.Conflict ||
        status == HttpStatusCode.PreconditionFailed ||
        (status == HttpStatusCode.BadRequest && CONFLICT_STATUS_MARKERS.any { rawBody.contains(it) })
```
com `CONFLICT_STATUS_MARKERS = listOf("FAILED_PRECONDITION", "ALREADY_EXISTS", "ABORTED")` (linha 227). Qualquer um dos três → `FirestoreConflictException` (nunca sobrescreve nada — a exceção é lançada *antes* de qualquer parsing do corpo de sucesso).

`TrocasEscalaRepository.runFirestore` (`TrocasEscalaRepository.kt:324-326`) traduz isso para `EscalaIciException(EscalaIciError.TROCA_CONFLICT, ...)` — mensagem amigável: *"A solicitação foi alterada por outra operação..."*.

**⚠️ Reload automático: NÃO ocorre hoje.**

Em `TrocasScreen.kt`, os três handlers (`onAceitar`/`onRecusar`/`onCancelar`, linhas 203-222) e o `onEnviar` do wizard (linhas ~455-467) seguem o mesmo padrão:

```kotlin
runCatching { trocasSession.responder(...) }
    .onSuccess { trocaSelecionada = null; feedback = "..."; carregar() }   // reload só aqui
    .onFailure { errorMessage = it.mensagemAmigavel() }                    // sem carregar()
```

Ou seja: **erro tipado** ✅ e **mensagem amigável** ✅ acontecem; **reload da lista/documento** ❌ não acontece automaticamente em caso de falha (inclusive conflito) — o usuário só veria dados atualizados se puxar um refresh manual ou reabrir a tela. Nenhum estado antigo é sobrescrito (a escrita simplesmente falha e não é reenviada), mas a tela pode continuar mostrando o card com o status desatualizado até o próximo reload manual.

---

## 4. Snapshot de equipe

**Um snapshot lógico por `equipeId+competencia`, com mutex — confirmado.**
`TeamScheduleRepository.kt:44-66`:

```kotlin
class TeamScheduleRepository(...) {
    private val mutex = Mutex()
    private var cached: TeamScheduleSnapshot? = null

    suspend fun snapshot(idToken, equipeId, competencia, forceRefresh = false): TeamScheduleSnapshot = mutex.withLock {
        val current = cached
        if (!forceRefresh && current != null && current.equipeId == equipeId && current.competencia == competencia) {
            return@withLock current
        }
        // só busca (usuarios + turnosMes + catálogo) se não houver cache válido
    }
}
```

Conteúdo (`TeamScheduleRepository.kt:15-20`): `usuariosAtivos`, `turnosMesPublicadas`, `catalogo` — exatamente os três.

**Reuso confirmado (nenhum `runQuery` extra por clique):**
- Trocas (assistente): `TrocasSession.teamSnapshot()` (`TrocasSession.kt:35-38`) → `TeamScheduleRepository.snapshot(...)`.
- "Quem trabalha nesse dia": `App.kt` mantém `teamSnapshot` em `remember { mutableStateOf<TeamScheduleSnapshot?>(null) }` (linha ~137) carregado por `refreshTeamSnapshotQuietly()` (linhas 145-148) — chamado só no `LaunchedEffect(Unit)` inicial e em `performRefresh()`, nunca por recomposição de tela. Esse mesmo `teamSnapshot` é passado como parâmetro para `ScheduleTab`/`TodayTab` (`App.kt`, chamadas de `TodayTab(...)`/`ScheduleTab(...)`), que o consultam via `EscalaIciScheduleMapper.quemTrabalhaPorTurno(snapshot, dataIso)` (`EscalaIciScheduleMapper.kt`) — leitura pura em memória, sem I/O.

Ou seja: **Trocas e "quem trabalha nesse dia" usam a mesma instância de `TeamScheduleSnapshot`** quando ambos pedem a mesma `equipeId+competencia` (o cache é global ao `TeamScheduleRepository`, não por chamador). Nenhum dos dois dispara `runQuery` a cada clique/recomposição — só `invalidate()`/`forceRefresh=true` (chamados só pelo botão de refresh manual, `App.kt:156`) força uma nova busca.

---

## 5. Badge

**Fórmula exata** (`TrocasSession.kt:85-92`):

```kotlin
suspend fun badge(): TrocasBadge {
    val trocas = buscarMinhasTrocas(equipeId, competencia, login)
    val notificacoes = buscarNotificacoes(login)
    val paraResponder = trocas.count { it.destinatarioLogin == login && it.status == PENDENTE_USUARIO }
    val naoLidas = notificacoes.count { it.lidaEm == null }
    return TrocasBadge(paraResponder, naoLidas)   // total = paraResponder + naoLidas
}
```

**⚠️ Pode contar a mesma situação duas vezes — confirmado, é soma simples sem deduplicação.**

Cenário concreto: A solicita troca com B. `criarSolicitacao` (`TrocasEscalaRepository.kt:142-151`) cria, no mesmo commit, a troca (`status=PENDENTE_USUARIO`, `destinatarioLogin=B`) **e** uma `NotificacaoTrocaDto` (`tipo=TROCA_SOLICITADA`, `destinatarioLogin=B`, `lidaEm=null`). Para B, `badge()` conta:
- +1 em `paraResponder` (a troca em si, `destinatarioLogin==B && PENDENTE_USUARIO`);
- +1 em `naoLidas` (a notificação da mesma troca, ainda não lida — e, pelo achado do item 2, **nunca fica lida pela UI hoje**).

Resultado: o mesmo evento de negócio ("B tem uma troca nova para responder") aparece como **2** no badge, não 1. Isso se agrava com o item 2 (notificação nunca marcada como lida): o `naoLidas` tende a crescer e nunca abaixar, mesmo depois de B responder.

Não alterei a fórmula — só documento o comportamento, conforme pedido.

---

## 6. REST commit — payloads (sem tokens/dados reais)

Formato base de cada escrita (`FirestoreRestClient.buildCommitBody`, `FirestoreRestClient.kt:169-189`): um array `writes`, cada item com `update.name` + `update.fields`, e `currentDocument`/`updateMask` conforme o tipo.

### a) CREATE da troca
`FirestoreWrite.Create("trocasEscala", trocaId, encodeSolicitacaoTroca(troca))` — payload completo (`TrocasRemoteMappers.kt:92-124`, campos: `trocaId, equipeId, competencia, solicitanteLogin, solicitanteNome, destinatarioLogin, destinatarioNome, data, turnoSolicitanteAntes, horarioSolicitanteAntes, turnoDestinatarioAntes, horarioDestinatarioAntes, status, mensagemSolicitante, motivoRecusa, criadoEm, atualizadoEm, respondidoEm, aprovadoEm, publicadoEm, gestorLogin, gestorNome, historico[1], snapshotValidacao{...}`):

```json
{
  "writes": [{
    "update": {
      "name": "projects/<p>/databases/(default)/documents/trocasEscala/<trocaId>",
      "fields": {
        "trocaId": {"stringValue": "<uuid>"},
        "solicitanteLogin": {"stringValue": "ana.silva"},
        "destinatarioLogin": {"stringValue": "carlos.souza"},
        "status": {"stringValue": "PENDENTE_USUARIO"},
        "historico": {"arrayValue": {"values": [{"mapValue": {"fields": {"tipo": {"stringValue": "SOLICITACAO_CRIADA"}, "porPerfil": {"stringValue": "SOLICITANTE"}, "...": "..."}}}]}},
        "...": "demais campos acima, cada um {tipoValue: valor}"
      }
    },
    "currentDocument": {"exists": false}
  }]
}
```

### b) PATCH da troca
Exemplo `cancelar` (`TrocasRemoteMappers.encodeTrocaPatch`, sem `respondidoEm`/`motivoRecusa` — só 3 chaves, `TrocasRemoteMappers.kt:150-158`):

```json
{
  "writes": [{
    "update": {
      "name": ".../trocasEscala/<trocaId>",
      "fields": {
        "status": {"stringValue": "CANCELADA_SOLICITANTE"},
        "atualizadoEm": {"stringValue": "2026-08-20T10:00:00.000Z"},
        "historico": {"arrayValue": {"values": ["... array inteiro reenviado, nunca truncado ..."]}}
      }
    },
    "updateMask": {"fieldPaths": ["status", "atualizadoEm", "historico"]},
    "currentDocument": {"exists": true}
  }]
}
```
No `responder` (aceitar/recusar), o mesmo patch ganha mais 2 chaves (`respondidoEm`, `motivoRecusa`) e o `updateMask` reflete isso (`TrocasEscalaRepository.kt:246-247`).

### c) CREATE da notificação
`FirestoreWrite.Create("notificacoesTroca", notificacao.id, encodeNotificacaoTroca(...))` (`TrocasRemoteMappers.kt:127-140`, campos `id, destinatarioLogin, equipeId, tipo, titulo, mensagem, trocaId, criadoPorLogin, criadoEm, lidaEm, acao`):

```json
{
  "update": {
    "name": ".../notificacoesTroca/<notifId>",
    "fields": {
      "destinatarioLogin": {"stringValue": "carlos.souza"},
      "tipo": {"stringValue": "TROCA_SOLICITADA"},
      "criadoPorLogin": {"stringValue": "ana.silva"},
      "lidaEm": {"nullValue": null},
      "...": "..."
    }
  },
  "currentDocument": {"exists": false}
}
```
Sempre no **mesmo commit** da troca (create) ou do patch (cancelar/responder) — `writes` com 2 itens, atômico.

### d) PATCH só de `lidaEm`
(`TrocasRemoteMappers.encodeNotificacaoLidaPatch`, `TrocasRemoteMappers.kt:161-163`):

```json
{
  "writes": [{
    "update": {
      "name": ".../notificacoesTroca/<notifId>",
      "fields": { "lidaEm": {"stringValue": "2026-08-20T10:05:00.000Z"} }
    },
    "updateMask": {"fieldPaths": ["lidaEm"]},
    "currentDocument": {"exists": true}
  }]
}
```

**`currentDocument.exists=false` na criação:** confirmado em todos os `FirestoreWrite.Create` (troca e notificação) — `FirestoreRestClient.kt:178`.

---

## 7. Histórico

Confirmado em `TrocasEscalaRepository.kt`:
- **Criação** (linhas 124-133): `historico = listOf(EventoHistoricoTrocaDto(tipo="SOLICITACAO_CRIADA", ...))` — exatamente 1 evento.
- **Cancelar** (linhas 170-177): `val historico = troca.historico + EventoHistoricoTrocaDto(tipo="CANCELADA_SOLICITANTE", ...)` — concatena, nunca reatribui/apaga o array antigo.
- **Aceitar** (linhas 214-225, `aceitar=true`): `tipo="ACEITE_DESTINATARIO"`, mesma concatenação.
- **Recusar** (linhas 214-225, `aceitar=false`): `tipo="RECUSA_DESTINATARIO"`, mesma concatenação.

Em nenhum dos três casos o array anterior é removido ou substituído — sempre `anterior + novoEvento`, e o `:commit` sempre reenvia o array completo (Firestore REST não suporta "append" parcial de array via `updateMask`, então o array inteiro faz parte do payload, mas o conteúdo anterior está intacto dentro dele).

---

## 8. Dados sensíveis / Git

```
$ git status --short
?? .vscode/
?? auth-config.json
?? composeApp/google-services.json
```

Confirmado: nenhum dos três está em `git ls-files` (não rastreados, não commitados). Nada foi apagado nem adicionado nesta auditoria.

**Push:** não existe branch remota `origin/feature/fase-16-trocas-reais` — nenhum push foi feito, nesta sessão ou antes.

---

## 9. Testes

Nenhum defeito foi **corrigido** nesta auditoria (achados dos itens 2, 3 e 5 são lacunas documentadas, não bugs de escrita/segurança — o dado nunca é gravado errado, só a UI não reage a eles ainda). Por instrução explícita, não alterei código, então não há necessidade de repetir `testDebugUnitTest`/`wasmJsTest`/`assembleRelease` — os resultados do relatório anterior (commit `636f517`) permanecem válidos: 60+ testes unitários + 5 testes de integração real contra o Emulator, todos verdes; `wasmJsTest` com Chrome real, verde; `assembleDebug`/`assembleRelease`, verdes.

---

## 10. Git

- Sem push, sem merge, sem escrita em staging — nenhum desses comandos foi executado.
- Versão não alterada (`versionCode 15` / `versionName 0.7.1`, já commitados em `636f517`) — não há código novo nesta auditoria que justificasse mudar.

---

## Resumo dos achados que exigem decisão sua antes de qualquer ajuste

| # | Achado | Risco | Onde |
|---|---|---|---|
| 2 | `marcarNotificacaoComoLida` nunca é chamada pela UI | Badge de não-lidas nunca abaixa sozinho | `TrocasScreen.kt` (nenhuma chamada) |
| 3 | Conflito/erro não recarrega a lista automaticamente | Usuário pode ver status desatualizado até um refresh manual (dado no servidor está correto, só a tela fica velha) | `TrocasScreen.kt:203-222`, `~455-467` |
| 5 | Badge soma `paraResponder + naoLidas` sem deduplicar por `trocaId` | Uma troca nova para responder conta como 2 no total do badge | `TrocasSession.kt:85-92` |

Nenhuma alteração foi feita para nenhum dos três — aguardando sua orientação sobre qual (se algum) deve ser corrigido antes de ir para staging.

---

## ADENDO — 2026-08-09 — FASE 16 — correções pós-auditoria concluídas

O texto acima ("nenhuma alteração foi feita") era verdadeiro no momento da auditoria original. Deixou de ser verdadeiro: os três achados foram corrigidos localmente antes deste adendo. O histórico acima é preservado sem edição — este adendo só registra o que mudou desde então.

**Achado #2 — notificação nunca marcada como lida:** CORRIGIDO. `TrocasScreen.kt` agora chama `marcarNotificacoesDaTrocaComoLidas(trocaId)` ao abrir uma troca (`onClick` do `TrocaCard`), que marca como lidas apenas as notificações não lidas daquela troca específica (`notificacoesNaoLidasDaTroca`, `TrocasEscala.kt`) e recarrega o badge em seguida.

**Achado #3 — conflito/erro não recarregava a lista:** CORRIGIDO. `TrocasScreen.kt` centraliza aceitar/recusar/cancelar em `executarAcaoTroca(mensagemSucesso, acao)`, que em qualquer falha recarrega a lista em segundo plano (best-effort, via `recarregarSilenciosamente()`) e trata especificamente `EscalaIciError.TROCA_CONFLICT` com a mensagem "Esta solicitação foi atualizada. Recarregamos o estado mais recente." O mesmo tratamento (incluindo `TROCA_DUPLICATE`) foi replicado no fluxo de criação de nova solicitação (`NovaSolicitacaoTrocaWizard`).

**Achado #5 — badge contava a mesma troca duas vezes:** CORRIGIDO. `TrocasEscala.kt` introduz `TrocasBadge(paraResponder, notificacoesDistintas)` e `calcularTrocasBadge(loginAtual, trocas, notificacoes)`, que deduplica por `trocaId` — uma troca acionável e sua notificação correspondente contam como **um** item, não dois. `TrocasSession.badge()` agora delega para essa função em vez de somar as duas contagens de forma independente.

**Testes novos:** `TrocasBadgeTest.kt` (`composeApp/src/commonTest`) — 8 testes cobrindo dedup por `trocaId`, notificação lida não soma no badge, notificação órfã (sem troca acionável correspondente) conta como item distinto, e `notificacoesNaoLidasDaTroca` filtrando por `trocaId`+não-lida.

**Validação completa executada (FASE 17A.1):**
- `./gradlew :composeApp:testDebugUnitTest :composeApp:wasmJsTest --rerun-tasks` → BUILD SUCCESSFUL. Agregado: **364 testes, 8 skipped, 0 failures, 0 errors** (inclui os 8 testes novos de `TrocasBadgeTest` verdes em JVM e em `wasmJsBrowserTest`).
- `./gradlew :composeApp:assembleDebug` → BUILD SUCCESSFUL.
- `./gradlew :composeApp:wasmJsBrowserDistribution` → BUILD SUCCESSFUL.
- `git diff --check` → limpo.

**INTEGRACAO_STAGING_REAL_NAO_EXECUTADA** — nenhuma das validações acima tocou o ambiente `escala-ici-staging` real; toda a verificação foi local (JVM unit tests + Chrome headless via `wasmJsBrowserTest` + builds locais). Uma validação de integração real contra staging permanece pendente antes de considerar esta fase pronta para produção.
