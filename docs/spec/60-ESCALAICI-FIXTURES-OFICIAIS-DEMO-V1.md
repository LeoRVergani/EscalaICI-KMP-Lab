# Spec 60 — Fixtures oficiais do workspace `demo-v1`

**Status:** implementada (geração determinística + integração Kotlin); validação final registrada nas seções 12-14
**Escopo:** Escala ICI KMP (Android + Web/PWA), ferramental Python (`scripts/`, `fixtures/demo/`)
**Fase:** FASE 14c-2 — segunda fase de código da série 14c (specs 56-59)
**Depende de:** spec 56/57 (contrato de responsáveis/workspace), spec 59 (resolução de identidade MSAL/Demo, cujo diretório Demo mínimo esta fase substitui)
**Não implementa:** publicação real no Firebase, backend Express, Dashboard, reset via API — tudo isso continua nas fases 14c-3 em diante (spec 58).

## 1. Objetivo

Substituir o catálogo Demo hardcoded criado na FASE 14c-1 (`DemoOrganizationData.kt`, dados soltos no código Kotlin) por uma **fonte única de verdade determinística e versionada**: um seed compacto legível por humanos, um gerador Python que o expande num pacote completo (validado contra o contrato JSON da spec 57), e um carregador Kotlin que consome esse pacote em runtime — sem duplicar regra de negócio entre o seed, o gerador e o app.

```text
fixtures/demo/demo-v1-seed.json  (fonte canônica, escrita à mão)
        ↓ scripts/generate_demo_v1.py (determinístico)
composeApp/.../composeResources/files/demo/demo-v1-publication-package.json  (pacote completo)
fixtures/demo/demo-v1-manifest.json  (contagens + SHA-256)
        ↓ scripts/validate_demo_v1.py
        ↓ Res.readBytes() em runtime real (Android/Web)
identity/DemoMemberDirectoryRepository, DemoMembershipRepository, DemoMemberRepository, DemoTeamRepository
```

## 2. Seed (`fixtures/demo/demo-v1-seed.json`)

Fonte canônica, compacta, escrita à mão (não gerada). Define, sem repetir os campos derivados que o gerador calcula:

- **Workspace**: `demo-v1`, `DEMO`, cenário `schedule-approval-basic`, `externalEffectsAllowed`/`notificationsEnabled` sempre `false`.
- **2 times**: `team-demo-soc` (SOC Demonstração), `team-demo-seguranca` (Segurança da Informação Demonstração).
- **5 membros fictícios** (domínio `example.invalid`): `member-demo-gestor-seguranca`, `member-demo-soc-01`, `member-demo-soc-02`, `member-demo-seguranca-01`, `member-demo-seguranca-02`.
- **5 vínculos de pertencimento** (`memberTeamMemberships`), um por membro, cada um o **time pessoal único** daquele membro — inclusive o gestor, cujo time pessoal é Segurança da Informação Demonstração.
- **2 vínculos de gestão** (`teamManagerAssignments`), ambos para o mesmo gestor: `PRIMARY_MANAGER` na própria equipe (Segurança) e `PRIMARY_APPROVER` na equipe que ele só aprova (SOC) — o cenário central da spec 56 seção 7 ("um responsável administrando vários times"), sem nunca precisar de um segundo vínculo de *pertencimento* para isso.
- **Um intervalo de período** (26/07/2026 a 25/08/2026) e **padrões de escala** por equipe (SOC: rotativo 6x1 com ciclo de 7 dias, folga no dia 6; Segurança: comercial, segunda a sexta).
- **3 solicitações de alteração** (pendente, aprovada, recusada), referenciando um assignment por `{memberId, dayIndex}` — o gerador resolve isso para o `assignmentId` real, garantindo que a solicitação sempre aponte para um `WORK_SHIFT` existente (erro em tempo de geração caso contrário).
- **Um registro de publicação** inicial (revisão 1).

Nenhum dado real: todos os e-mails terminam em `example.invalid`, todos os IDs usam o prefixo `demo`/`-demo-`.

## 3. Gerador (`scripts/generate_demo_v1.py`)

Lê o seed e expande cada entidade com os campos exigidos pelo contrato (`workspaceId`, `active`, `schemaVersion`, etc.) que seriam repetitivos escrever à mão. Regras determinísticas:

- Datas calculadas via `datetime.date` (aritmética determinística sobre datas fixas do seed — não é "hora atual").
- SOC: para cada dia do período, `dayIndex % 7 == 6` → `OFF`; caso contrário → `WORK_SHIFT` no turno do seed (Manhã para `soc-01`, Tarde para `soc-02`) — nunca mais de 6 dias consecutivos de trabalho.
- Segurança: `WORK_SHIFT` de segunda a sexta (`date.isoweekday()`), `OFF` no fim de semana.
- IDs de assignment: `assignment-demo-{memberId sem o prefixo member-demo-}-{YYYY-MM-DD}`.
- `scheduleAssignments` ordenado de forma estável (`teamId`, `memberId`, `date`).
- Contagem de `publicationRecords.countsCreated` = soma de todas as entidades geradas (exceto o próprio workspace e o registro de publicação).

**Determinismo**: rodar o gerador duas vezes produz o pacote **byte a byte idêntico** (confirmado nesta sessão, duas vezes, de forma independente — seção 12). Nenhum `datetime.now()`, `random` ou `uuid` em nenhum lugar do script.

## 4. Manifesto (`fixtures/demo/demo-v1-manifest.json`)

Contagens finais confirmadas (seção 12): **2 teams, 5 members, 5 memberTeamMemberships, 2 teamManagerAssignments, 2 schedulePeriods, 124 scheduleAssignments, 3 scheduleChangeRequests, 1 publicationRecord**. Inclui `sha256` do pacote gerado (não é segredo, documentado normalmente), `generatorVersion`, `generated: true`, `generatedAt` fixo.

## 5. Validador (`scripts/validate_demo_v1.py`)

Verifica, sem depender de nenhuma ferramenta Kotlin/Gradle: JSON válido; conformidade com `docs/contracts/organization-approval-v1.schema.json` (via `jsonschema`, quando instalado); `workspaceId == "demo-v1"` em **todas** as entidades; IDs únicos (por coleção e entre `teams`+`members`); todas as referências (membership→member/team, manager→member/team, assignment→member/team/period, request→member/team/manager/period/assignment) resolvem para algo existente; datas de assignment dentro do período; o gestor tem exatamente 1 vínculo de pertencimento ativo (Segurança) e 2 vínculos de gestão; cada membro comum tem exatamente 1 vínculo principal ativo (nunca ambíguo); nenhuma sequência SOC com mais de 6 `WORK_SHIFT` consecutivos (verificado percorrendo as datas, não apenas confiando na regra de geração); nenhum `WORK_SHIFT` de Segurança em fim de semana; nenhum domínio de e-mail fora de `example.invalid`; `externalEffectsAllowed`/`notificationsEnabled` sempre `false`; contagens e SHA-256 do manifesto batem com o pacote real.

## 6. Extensão do contrato JSON (`docs/contracts/organization-approval-v1.schema.json`)

Aditiva, sem quebrar as definições da spec 57/FASE 14c-0: novos `$defs` `schedulePeriod`, `scheduleAssignment`, `publicationRecord` (+ enums `scheduleAssignmentType`, `publicationRecordSource`) e as propriedades de topo correspondentes (`schedulePeriods`, `scheduleAssignments`, `publicationRecords`). Um único período por equipe (o modelo real já existente, `SPEC-DB-UNIVERSAL-TIMES-E-ESCALAS.md`, escopa período a uma equipe) — as duas equipes do cenário Demo usam o **mesmo intervalo de datas**, mas cada uma com seu próprio documento `schedulePeriods` (`period-demo-soc-...`/`period-demo-seguranca-...`), adaptando a sugestão original de um único ID de período ao padrão real por equipe.

## 7. Integração Kotlin (pacote `identity`)

- `DemoFixturePackage.kt`: `data class` `@Serializable` espelhando 1:1 a forma do pacote gerado (`DemoFixtureWorkspace`, `DemoFixtureTeam`, `DemoFixtureMember`, `DemoFixtureMembership`, `DemoFixtureManagerAssignment` + `DemoFixturePermissions`, `DemoFixtureSchedulePeriod`, `DemoFixtureScheduleAssignment`, `DemoFixtureScheduleChangeRequest`, `DemoFixturePublicationRecord`), mesmo estilo já usado em `FirebaseDtos.kt`. Funções `toMembers()`/`toTeams()`/`toMemberships()`/`loginByMemberId()` mapeiam para os modelos de domínio reais (`Member`/`Team`/`MemberTeamMembership`, já existentes desde a FASE 14c-1) — nenhum modelo paralelo criado.
- `DemoFixtureLoader.kt`: separa a função pura `parseDemoFixturePackage(json: String)` (sem I/O, testável com uma string) da função `loadDemoFixturePackage()` (com I/O, via `Res.readBytes("files/demo/demo-v1-publication-package.json")`, o acessador gerado pelo plugin `org.jetbrains.compose` já aplicado ao projeto). `DemoFixtureCache` (objeto com `Mutex`) garante que o arquivo só é lido/decodificado uma vez por processo.
- `OrganizationRepositories.kt`: as 4 classes `DemoMemberDirectoryRepository`/`DemoMembershipRepository`/`DemoMemberRepository`/`DemoTeamRepository` (mesmos nomes usados como default de `OrganizationIdentityResolver`, arquivo não alterado) passaram a carregar a fixture via `DemoFixtureCache.get()` dentro de cada método `suspend`, reaproveitando as classes `InMemory*Repository` já existentes para a busca — nenhuma lógica de correspondência duplicada.
- `DemoOrganizationData.kt` **apagado** — não há mais dois catálogos Demo independentes.
- `DemoPersona.kt` (catálogo de 3 personas selecionáveis na UI) **não foi alterado** — já não duplicava dados organizacionais (só `personaId`/`memberId`/`displayName`/login e e-mail fictícios/descrição), exatamente como a fase exigia.

### 7.1 Limitação de teste descoberta e documentada (achado real desta fase)

`Res.readBytes` (Compose Resources) **funciona no app real** (Android device/emulador, Web via `wasmJsBrowserDistribution`), mas **falha em qualquer teste automatizado** desta sessão:
- `testDebugUnitTest` (JVM puro): `IllegalStateException: Android context is not initialized` — não há Activity/Context real num teste JVM puro.
- `wasmJsTest` (Karma/Chromium): `MissingResourceException` — o recurso não é empacotado no bundle de teste por padrão.

Confirmado por um teste isolado ("spike"), depois removido do código final. Por causa disso, a estratégia de teste separa completamente a lógica pura da leitura de recurso:

- **`commonTest/DemoFixtureParsingTest.kt`**: testa `parseDemoFixturePackage()` com um JSON mínimo escrito inline (não o arquivo real de 124 assignments) — cobre todos os campos de todas as entidades, o mapeamento para os modelos de domínio, e um caso de JSON inválido (campo obrigatório faltando lança `SerializationException`, nunca um objeto parcial silencioso).
- **`androidUnitTest/DemoFixtureRealFileTest.kt`**: roda em JVM puro (não em Robolectric/Android real), então pode usar `java.io.File` (API só-JVM, indisponível em `commonTest`/Wasm) para ler o **arquivo real gerado** do disco e confirmar que ele parseia com os DTOs Kotlin — pega qualquer divergência entre o que o gerador Python realmente escreve e o que o Kotlin espera. Confirma as contagens reais (5 membros, 2 times, 124 assignments, gestor com 1 vínculo pessoal + 2 vínculos de gestão).
- **`DemoOrganizationResolutionTest.kt`** (já existente, FASE 14c-1): os testes que exercitavam `DemoMemberDirectoryRepository()` real (agora dependente de `Res.readBytes`) foram adaptados para injetar um `DemoFixturePackage` sintético construído inline no próprio teste — preserva a cobertura (incluindo o teste dedicado de `MultipleActiveTeams`, usando dados sintéticos com o gestor em 2 memberships sem principal, propositalmente diferente do cenário real da fixture oficial, que resolve de forma limpa) sem depender de leitura de recurso.

A verificação de que a leitura de recurso funciona de fato **em runtime real** fica para a validação manual no emulador (seção 13) — não há substituto automatizado para isso nesta fase.

## 8. Consistência com specs anteriores

- `assignedManagerMemberId` de cada solicitação é resolvido a partir do único `teamManagerAssignments` da equipe do solicitante (spec 56 seção 9.3 — fotografia do responsável no momento da criação).
- O gestor nunca resolve para `MultipleActiveTeams` na fixture oficial (spec 59 seção 3.2 passo 5) — ele tem exatamente 1 vínculo de pertencimento ativo (seu time pessoal); administra o segundo time exclusivamente via `team_manager_assignments`, nunca via um segundo membership.
- `roleDisplayName` continua `null` na resolução (limitação já documentada na spec 59 — `roles` ainda não conectado).

## 9. Firestore

Nenhuma escrita. As fixtures existem apenas localmente (arquivo versionado no Git + resource embarcado no APK/bundle Web) — publicação real no Firebase fica para a FASE 14c-4 (spec 58).

## 10. Segurança

Nenhum dado real em nenhum lugar: seed, pacote gerado, manifesto e testes usam exclusivamente IDs fictícios e e-mails `example.invalid`. O SHA-256 do pacote não é segredo. Nenhuma credencial, token ou segredo em nenhum script Python.

## 11. Reset futuro

O design já antecipa a FASE 14c-4 (reset do workspace Demo via backend Express, spec 56 seção 12.9): o manifesto guarda `seedVersion`/`generatorVersion`/`sha256`, permitindo a um backend futuro detectar se a fixture publicada está desatualizada em relação ao seed versionado, sem precisar reimplementar a lógica de expansão (o mesmo `scripts/generate_demo_v1.py` pode ser invocado por esse backend, ou portado, reaproveitando o mesmo seed).

## 12. Validação automatizada

```text
python3 scripts/generate_demo_v1.py   (x2, para confirmar determinismo)
python3 scripts/validate_demo_v1.py
./gradlew :composeApp:testDebugUnitTest :composeApp:assembleDebug \
  :composeApp:assembleRelease :composeApp:wasmJsBrowserDistribution :composeApp:wasmJsTest
```

Resultados completos (contagem de testes, sucesso de cada artefato) registrados no relatório final desta fase.

## 13. Validação manual no emulador

Registrada no relatório final desta fase — fluxo Demo com os 3 personagens resolvendo a partir do arquivo real gerado (não mais dos dados hardcoded da FASE 14c-1), confirmando em particular que o Gestor de Segurança Demo resolve de forma limpa (equipe pessoal única, sem `MultipleActiveTeams`) e que a sessão MSAL corporativa/restauração silenciosa continuam intactas.

## 14. Versionamento

`versionCode`/`versionName` atualizados após toda a bateria de testes/build passar — detalhes no relatório final.

## 15. Critérios de aceite

1. Existe uma única fonte de verdade Demo (o seed) — nenhum dado Demo duplicado em mais de um lugar.
2. O gerador é determinístico (byte a byte idêntico em execuções repetidas).
3. O pacote gerado valida contra o contrato JSON da spec 57 sem nenhum erro.
4. O gestor administra duas equipes (spec 56 seção 7) sem nunca resolver para `MultipleActiveTeams` na fixture oficial.
5. Nenhuma sequência de trabalho SOC excede 6 dias consecutivos; Segurança nunca trabalha em fim de semana.
6. Toda solicitação de alteração referencia um assignment real e existente.
7. `DemoOrganizationData.kt` foi removido; nenhum catálogo Demo duplicado permanece no código.
8. Testes automatizados cobrem a lógica de parsing/mapeamento sem depender de `Res.readBytes` (limitação de ambiente documentada, não contornável nesta fase).
9. Nenhum dado real ou segredo em qualquer arquivo desta fase.
10. Validação manual no emulador confirma o carregamento real da fixture, sem regressão no fluxo MSAL corporativo.
