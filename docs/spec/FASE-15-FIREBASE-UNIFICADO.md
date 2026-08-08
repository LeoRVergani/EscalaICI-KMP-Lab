# FASE 15 — Firebase unificado (Escala-ICI como fonte de verdade)

**Status:** implementada e validada automaticamente (código, testes
unitários, Firestore Rules no Emulator, e integração Kotlin real contra
Emulator **e** staging — ver seção 13). Dois bugs reais só apareceram
testando contra a API real e foram corrigidos. Validação **manual**
(dispositivo/navegador reais) e **release assinado** permanecem como
pendência externa (seção 13) — a FASE 15 não está encerrada até esses
itens serem fechados numa máquina/ambiente com os recursos necessários.

**Escopo:** Escala ICI KMP (Android + Web/Wasm), leitura autenticada e
read-only do mesmo Firebase/Firestore do `Escala-ICI`
(github.com/LeoRVergani/Escala-ICI, staging). Substitui por completo o
schema Firebase antigo (`teams`, `members`, `schedule_periods`,
`schedule_assignments`, `oncall_periods`, `oncall_assignments`) e o login
inteiramente mockado das fases 9-14.

## 1. Por que esta fase existe

Antes da FASE 15, o KMP-Lab lia um Firebase que não era o do Escala-ICI:
`FirestoreRestGateway` fazia `GET` anônimo em coleções que só existiam
neste laboratório, com `teamId = "soc"` hardcoded em toda a aplicação e
login inteiramente simulado (`InMemoryAuthSessionRepository`, 3
colaboradores fixos). O objetivo desta fase é fazer o KMP falar com o
Firebase **real** do Escala-ICI — o mesmo que o Dashboard e o PWA
(`apps/app/src/EmployeeApp.tsx`) já usam — sem manter dois schemas e sem
pedir ao usuário para escolher equipe, mês ou competência.

## 2. Divergência de auditoria (importante para quem revisitar este código)

A auditoria inicial desta fase leu um checkout local do `Escala-ICI` que
estava **defasado em 65 arquivos** (um único commit, sem histórico real,
contra o `origin/main` verdadeiro) e concluiu — errado — que a identidade
era por UID do Firebase Auth. O usuário identificou a divergência antes
de qualquer código ser escrito; a reauditoria (via `git show
origin/main:<arquivo>`, sem alterar o checkout local) confirmou o
contrato **real**:

- `usuarios/{login}` — **login é o ID do documento**, derivado do e-mail
  autenticado (`login = email.split('@')[0].toLowerCase().trim()`,
  idêntico a `loginDoEmail()`/`loginDoAuth()` do Escala-ICI). O campo
  `uid` no documento é metadado opcional, nunca usado para autenticar ou
  ler dados.
- `turnosMes`/`rascunhosTurnosMes` são identificados por
  `${equipeId}_${login}_${competencia}` (`idDocumento()` do contrato real
  — o segundo parâmetro se chama `usuarioUid` por legado, mas contém o
  login). A query real filtra por `login`, não por `usuarioUid`.
- `packages/contrato/src/jornada.ts` (corte de competência no dia 26,
  seleção de período) não mudou entre o checkout defasado e `origin/main`
  — essa parte da auditoria original permanece válida e foi portada para
  `firebase/Jornada.kt`.
- Nenhuma alteração de `firestore.indexes.json`/`firestore.rules` foi
  necessária no Escala-ICI: a query do KMP é só de igualdades
  (`login`+`equipeId`+`status`), servida automaticamente pelos índices de
  campo único do Firestore.

Recomendação para sessões futuras: **sempre** auditar contra
`git show origin/main:<arquivo>` (ou um checkout atualizado de verdade),
nunca contra um clone que possa estar parado num commit antigo.

## 3. Arquitetura (`commonMain`)

Pacote novo `br.com.leorvergani.escalaici.kmp.lab.firebase`:

| Arquivo | Papel |
|---|---|
| `EscalaIciFirebaseConfig.kt` + `GeneratedFirebaseConfig.kt` (gerado) | Config publica (ambiente, projectId, apiKey, hosts de emulador), lida de `local.firebase.properties` (gitignored) em build-time pela task Gradle `generateFirebaseConfig`. |
| `dto/RemoteDtos.kt` | DTOs `@Serializable` do contrato remoto (`TurnosMes`, `Dia`, `Totais`, `TipoTurno`, `Usuario`), campos confirmados via `git show origin/main`. |
| `FirestoreValueCodec.kt`, `RemoteDtoMappers.kt` | Decodificação manual do formato `{fields: {...}}` da API REST do Firestore — `usuario()` usa o ID do documento como login (fonte de verdade), não o campo. |
| `IdentityToolkitAuthClient.kt` | Login/refresh via Identity Toolkit + Secure Token REST (ou hosts do Emulator). |
| `FirestoreRestClient.kt` | Leitura autenticada (`GET` doc único, `:runQuery` com filtros de igualdade) — distingue 401 (token expirado) de 403 (sem permissão). |
| `AuthRepository.kt` (+`FirebaseAuthRepository`) | Interface do prompt FASE 15 §7; deriva login do e-mail; nunca usa Admin SDK/service account. |
| `SessionTokenStore.kt` (+ actuals) | Android: AES-256/GCM via Android Keystore sobre `SharedPreferences` (nunca texto simples). Web: `localStorage`. Um único slot por dispositivo, limpo por completo no logout. |
| `UsuarioRepository.kt`, `TiposTurnoRepository.kt`, `CatalogoPadrao.kt` | `usuarios/{login}` com validação `ativo`/`equipeId`; `tiposTurno` por equipe com o mesmo fallback (`CATALOGO_SOC`) do app real quando a coleção vem vazia. |
| `Jornada.kt` | Porte literal de `packages/contrato/src/jornada.ts` (corte dia 26, competências candidatas, seleção por data). |
| `CurrentScheduleResolver.kt` | GET pelo id determinístico (rápido) + query por igualdades (fallback); `escolherEscalaAtual()` é a decisão pura testada isoladamente — nunca seleciona período passado/futuro como atual (divergência deliberada do fallback "mais recente" do app real). |
| `EscalaIciScheduleMapper.kt` | `TurnosMes` + catálogo → `ScheduleSummary`/`ShiftDay` (modelo de UI já existente). |
| `EscalaIciScheduleCache.kt`, `RawKeyValueStore.kt` | Cache offline v2, chave `firebase_schedule_cache_v2:{login}`, isolado por usuário. |
| `ScheduleSyncState.kt` | Estados/erros tipados do prompt §28. |
| `LoggedScheduleSyncCoordinator.kt` | Orquestra cache→sessão→usuário→escala→catálogo→cache→`StateFlow`; renova o token uma vez em caso de 401 antes de reportar erro. |
| `CoordinatorFactory.kt` | Monta tudo com um único `HttpClient(CIO)` — mesmo engine já usado no restante do app, válido em Android e Wasm. |

`androidMain`/`wasmJsMain` só implementam armazenamento (`SessionTokenStore`,
`RawKeyValueStore`) — toda a lógica de negócio é `commonMain`.

## 4. Fluxo (zero configuração do usuário)

```
abrir app → cache (se houver) aparece imediatamente → restaura sessão
  → resolve usuarios/{login} → resolve turnosMes PUBLICADA vigente
  → resolve tiposTurno da equipe → mapeia → atualiza cache → Hoje
```

Sem seleção manual de equipe, mês, competência ou usuário em nenhuma tela.
`AuthRepository`/`LoggedScheduleSyncCoordinator` são agnósticos ao
mecanismo de login — trocar para Microsoft/Entra no futuro (fora do
escopo desta fase) não exige reescrever o resto do app.

## 5. UI

- `LoginGateScreen`: e-mail+senha reais; "Modo demonstração" isolado,
  nunca acionado automaticamente por falha real do Firebase.
- `App.kt`: `LoggedScheduleSyncCoordinator.state` dirige a tela (splash de
  carregamento → Hoje/Escala/Perfil com dados reais, ou tela de erro
  tipada com opção de voltar ao login). Cache mostrado imediatamente,
  sincronização silenciosa em segundo plano.
- `ProfileTab`: usa `usuarios/{login}` real; removido o aviso estático
  "Associação corporativa ainda não configurada".
- `PlantaoScreen`: sem coleção própria no backend novo (`Categoria.PLANTAO`
  existe no tipo, mas nenhum turno seedado a usa) — mostra "Plantão ainda
  não publicado neste backend" em vez do antigo aviso "(mock)"; removida a
  integração com o gateway Firebase antigo.

Compromisso documentado: o mapeamento `codigo`/`categoria` → `ShiftType`
(enum fechado já existente) continua fixo — ver KDoc de
`EscalaIciScheduleMapper` para o raciocínio (evitar reescrever
cores/ícones de UI sem navegador/emulador disponível nesta sessão para
validar visualmente).

## 6. Legado removido

`source/FirestoreRestGateway.kt`, `FirebaseGateway.kt`, `FirebaseSources.kt`,
`FirebaseDtos.kt`, `FirebaseCache.kt` (commonMain), `FirebasePlatform.android.kt`/
`.wasmJs.kt`, `FirebaseSourcesTest.kt`. `firebase/firestore.rules` e
`firebase/firestore.indexes.json` deste repositório passaram a ser cópia
exata de `origin/main` do Escala-ICI (nunca editar à mão — propor mudanças
lá). `firebase/test/firestore.rules.test.mjs` reescrito para o schema novo
por login.

**Não removido, deliberadamente**: `SourceContracts.kt`/`SourceResolution.kt`/
`LocalSourceAdapters.kt` (abstração genérica de fonte local/cache,
independente do Firebase antigo ou novo) e `SchedulePeriod`/`ScheduleAssignment`/
`OnCallPeriod`/`OnCallAssignment` em `DomainModels.kt` — usados por
`ScheduleRules.kt`, `PlantaoWorkbookParser.kt` e `WebLocalDataCache.kt`,
recursos independentes do Firebase que continuam funcionando. `teamId =
"soc"` também permanece nos mocks/parsers de XLS (a equipe real que essas
funcionalidades sempre representaram) — só foi removido do fluxo real de
login/escala, que agora sempre deriva `equipeId` de `usuarios/{login}`.

## 7. Ambientes e segredos

`local.firebase.properties` (gitignored, `.example` versionado): config
pública do Firebase (não é segredo, mas mesmo assim não versionada por
padrão do projeto) — lida só pela task `generateFirebaseConfig`.
`local.firebase.test.properties` (gitignored, `.example` versionado):
e-mail/senha de teste, lidos **somente** pelos testes de integração,
nunca pela geração de config nem embutidos em nenhum artefato.

## 8. Índices e Rules

Nenhuma alteração proposta em `firestore.indexes.json`/`firestore.rules`
do Escala-ICI — a query do KMP (`login`+`equipeId`+`status`, sem
`orderBy`) é servida pelos índices de campo único automáticos do
Firestore, e as Rules já cobrem exatamente a leitura necessária (própria
equipe, só `PUBLICADA` para quem não é gestor).

## 9. Testes

- **Unitários** (`commonTest/.../firebase/`, 40 testes novos): `Identity`
  (login a partir do e-mail), `Jornada` (corte dia 26, período
  atravessando mês), `CurrentScheduleResolverTest` (`escolherEscalaAtual`
  — nunca seleciona período passado/futuro), `EscalaIciScheduleMapperTest`
  (catálogo → `ShiftType`, fallback para código desconhecido),
  `EscalaIciScheduleCacheTest` (isolamento por login, `schemaVersion`),
  `AuthErrorMapperTest` (mensagens distintas por código), `RemoteDtoMappersTest`
  (decodificação do formato Firestore, login pelo ID do documento).
- **Firestore Rules no Emulator** (`firebase/test/firestore.rules.test.mjs`,
  `npm run test:rules` em `firebase/`) — **executado nesta sessão, 10/10
  passando** contra um Firestore Emulator real (`cloud-firestore-emulator-v1.22.0`)
  rodando as Rules copiadas de `origin/main`: usuários fictícios
  (`ana.silva`, `marina.lima` gestora, `carlos.souza` de outra equipe) —
  leitura do próprio perfil/colega de equipe, negação entre equipes,
  `turnosMes` PUBLICADA legível mas `rascunhosTurnosMes` nunca visível
  para colaborador comum, consulta por igualdades (`login`+`equipeId`+
  `status`) **confirmada sem precisar de índice composto novo**, leitura
  anônima sempre negada.
- **Integração Kotlin-JVM real** (`androidUnitTest/.../firebase/FirebaseIntegrationTest.kt`,
  exercitando o mesmo `HttpClient(CIO)`/`IdentityToolkitAuthClient`/
  `FirestoreRestClient` que Android e Web usam — nenhum mock/fake). So
  roda com `ESCALAICI_FIREBASE_EMULATOR=true`/`ESCALAICI_FIREBASE_STAGING=true`
  explicitos (nunca no `testDebugUnitTest` padrão):
  - **Emulator** (`scripts/run-emulator-integration-test.sh` semeia via
    REST e roda o teste): **PASS** — login → `usuarios/ana.silva` →
    `turnosMes` PUBLICADA → `tiposTurno` → `EscalaIciScheduleMapper`,
    ponta a ponta, contra o Emulator real.
  - **Staging** (`local.firebase.test.properties`): `marina.azevedo`
    (gestora) **PASS** — identidade/equipe/Rules confirmadas contra o
    Firebase real. `caio.monteiro` (colaborador) **FAIL, não mascarado**:
    `usuarios/caio.monteiro` resolve (`equipeId=EQ_SOC`), mas não existe
    nenhum `turnosMes` com `status=='PUBLICADA'` para
    `login=caio.monteiro`+`equipeId=EQ_SOC` no staging — lacuna de **dado**
    no ambiente, não de código (nenhum fallback foi criado para escondê-la).
  - **Dois bugs reais encontrados e corrigidos só por testar contra a API
    real** (o Emulator não os expunha):
    1. `IdentityToolkitAuthClient`: `Json` sem `encodeDefaults = true`
       omitia `returnSecureToken` (igual ao valor padrão `true`) do corpo
       da requisição; o Identity Toolkit real então devolvia o token sem
       `refreshToken`/`expiresIn`, quebrando o parse. O Emulator aceitava
       o corpo incompleto sem reclamar, por isso só apareceu em staging.
    2. `CurrentScheduleResolver.publicadaPorId`: um 403 do Firestore no
       caminho rápido (GET pelo ID adivinhado) abortava a resolução
       inteira em vez de cair para a consulta — mas o Firestore devolve
       403 (não 404) tanto para "não existe" quanto para "é de outra
       equipe" quando a regra referencia `resource.data`. Corrigido para
       tratar 403 nesse caminho como "não encontrado", nunca abortando.
- **Manual** (dispositivo Android real, navegador real, teste de
  publicação via Dashboard): pendente — sem emulador Android nem
  navegador interativo neste ambiente de execução.

## 10. Plantão (COSI)

Sem contrato/coleção própria no backend novo (`Categoria.PLANTAO` é
reservado, não seedado). Não foi reintroduzido `oncall_periods`/
`oncall_assignments` (schema antigo). Pendência documentada — próxima
fase deve investigar com o time do Escala-ICI se/quando um contrato de
plantão será modelado lá antes de qualquer implementação no KMP.

## 11. Versionamento

`versionCode` 13→14, `versionName` 0.6.3→0.7.0 (mudança arquitetural).

## 12. Limitações conhecidas

- `EscalaIciScheduleMapper` usa o enum `ShiftType` fechado (compromisso
  documentado na seção 5).
- Sem MSAL/Entra nesta fase (fora de escopo, `AuthRepository` já
  preparado para isso).
- Sem `orderBy`/paginação nas queries — volume esperado por usuário é
  pequeno (poucas competências por vez).

## 13. Registro de validação (FASE 15-FINAL)

### AUTOMATIZADO

| Caminho | Resultado |
|---|---|
| Kotlin/Ktor → Auth Emulator → Firestore Emulator (login→usuário→escala→catálogo→mapper) | **PASS** |
| Kotlin/Ktor → Auth staging → Firestore staging, `marina.azevedo` (gestora, identidade/equipe) | **PASS** |
| Kotlin/Ktor → Auth staging → Firestore staging, `caio.monteiro` (colaborador, ponta a ponta) | **FAIL — dado faltante, não bug**: `usuarios/caio.monteiro` existe (`equipeId=EQ_SOC`), mas não há nenhum `turnosMes` com `status=='PUBLICADA'` para esse login+equipe em staging. Nenhum fallback foi criado; a arquitetura não foi alterada para mascarar isso. |

### MANUAL (pendente — sem dispositivo Android/navegador interativo neste ambiente)

- [ ] Android: login → Hoje → Escala.
- [ ] Android: fechar/reabrir → sessão restaurada.
- [ ] Web/Wasm: login → Hoje → Escala.
- [ ] Web/Wasm: reload → sessão restaurada.
- [ ] Alteração publicada no Dashboard → Android atualizado.
- [ ] Alteração publicada no Dashboard → Web atualizado.

### RELEASE (pendente — keystore original não existe nesta máquina)

- [ ] `assembleRelease` na máquina com a keystore original.
- [ ] `apksigner verify` confirmando a **mesma assinatura** das versões anteriores.
- [ ] `EscalaICI-latest.apk`, `versionCode=14`, `versionName=0.7.0`.
- [ ] `version.json` atualizado.
- [ ] Sem upload automático.

**Correção registrada sobre o relatório anterior desta fase**: um APK com
o mesmo `applicationId` mas assinatura diferente (ex.: o
`composeApp-debug.apk` gerado nesta sessão, assinado com a chave de debug
padrão do AGP) **não instala lado a lado nem atualiza** uma instalação já
assinada com a keystore `escalaici-kmp-lab.jks` — o Android exige a mesma
assinatura tanto para atualizar quanto, no caso de mesmo `applicationId`,
para coexistir. Esse APK debug só é utilizável se (a) não houver instalação
anterior com esse `applicationId`, ou (b) a instalação anterior já tiver
sido assinada com a mesma chave de debug. Não é um substituto para o
release assinado pendente acima.

Nenhuma keystore nova foi gerada, nenhum `applicationId` foi alterado.
