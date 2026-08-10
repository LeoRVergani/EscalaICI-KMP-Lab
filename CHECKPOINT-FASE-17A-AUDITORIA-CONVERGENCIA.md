# CHECKPOINT FASE 17A — Auditoria de Convergência do Escala ICI KMP

> Auditoria pura. Nenhum arquivo de código foi alterado, nenhum commit/push/merge/checkout foi executado. Todos os comandos git usados são somente-leitura.

**Repos auditados:**
- `EscalaICI-KMP-Lab` — `/home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab` (branch `feature/fase-16-trocas-reais`)
- `EscalaSOC` (referência 1) — `/home/lvergani/AndroidStudioProjects/EscalaSOC` (branch `feature/fase-7k3g-perfil-organizado`)
- `Escala-ICI` (referência 2) — **não estava clonado localmente**; clonado somente-leitura para `/tmp/.../scratchpad/Escala-ICI` a partir de `github.com/LeoRVergani/Escala-ICI` (ver seção 0 abaixo — achado crítico)

---

## 0. Achado crítico de pré-auditoria: caminho errado para "Escala-ICI"

`/home/lvergani/Projetos/escala-ici` (nome parecido, mesma pasta `Projetos/`) **não é** o repositório `LeoRVergani/Escala-ICI`. É um projeto local sem remote configurado (39 commits, nenhum `git remote`), um scaffold **NestJS + Prisma + PostgreSQL** do zero, sem Firestore, sem PWA implementada, sem `trocasEscala`/`notificacoesTroca`/`tiposTurno` em nenhum lugar do código — apenas specs em prosa (`docs/specs/*.md`) descrevendo intenção futura. `apps/admin-web` e `apps/mobile-flutter` contêm só `.gitkeep`.

O repositório real (`gh api repos/LeoRVergani/Escala-ICI` confirma: TypeScript, público, `pushed_at: 2026-08-08`) foi clonado para auditoria em `/tmp/claude-1000/.../scratchpad/Escala-ICI`. É um monorepo com:
- `apps/app` — PWA do colaborador (`EmployeeApp.tsx`)
- `apps/dashboard` — Dashboard do gestor/admin (`DashboardApp.tsx`)
- `packages/contrato` — tipos/regras compartilhados (fonte de verdade do schema)
- `firestore.rules` — regras reais de produção/staging

**Ação recomendada:** se alguém (humano ou agente) referenciar `/home/lvergani/Projetos/escala-ici` como fonte de verdade do Firestore, está olhando o repo errado.

---

## 1. Estado Git local (KMP)

```
branch atual: feature/fase-16-trocas-reais
toplevel: /home/lvergani/AndroidStudioProjects/EscalaICI-KMP-Lab
remote: origin -> https://github.com/LeoRVergani/EscalaICI-KMP-Lab.git
```

Working tree **sujo** (não é reflexo 1:1 do GitHub):

**Modificados (não staged):**
- `composeApp/src/commonMain/kotlin/.../firebase/TrocasEscala.kt`
- `composeApp/src/commonMain/kotlin/.../firebase/TrocasSession.kt`
- `composeApp/src/commonMain/kotlin/.../ui/TrocasScreen.kt`

**Untracked:**
- `.vscode/`
- `CHECKPOINT-FASE-16-PRE-STAGING.md`
- `auth-config.json`
- `composeApp/google-services.json`
- `composeApp/src/commonTest/kotlin/.../firebase/TrocasBadgeTest.kt`

`git diff --stat`: 3 arquivos, +130/-40 linhas (Trocas). `git diff --check`: sem conflitos de whitespace.

---

## 2. Estado GitHub remoto

`git fetch origin` executado sem alterar a working tree.

```
git log HEAD..origin/feature/fase-16-trocas-reais --oneline  → (vazio)
git log origin/feature/fase-16-trocas-reais..HEAD --oneline  → (vazio)
```

**Local e remoto estão exatamente sincronizados** (`fadd086`, "ci: configura Firebase no build Web"). `master`/`origin/master` está em `dc81bc6`, bem atrás (FASE 14a.1) — `feature/fase-16-trocas-reais` nunca foi mesclada em `master`. Há várias branches de FASE anteriores (14a até 14n) e branches de backup (`backup/overnight-kmp-before-20260715`, `backup/linux-kmp-antes-sync-20260714`) ainda presentes, todas com upstream correspondente no remoto.

---

## 3. Divergência local × remoto

- **Commits locais não enviados:** nenhum (HEAD == origin/HEAD para a branch atual).
- **Commits remotos não presentes localmente:** nenhum.
- **Divergência real está apenas na working tree**, não em commits: as 3 modificações em Trocas + os 5 arquivos untracked listados na seção 1 são trabalho local não commitado, potencialmente mais avançado que o último commit `fadd086`.

---

## 4. Versão atual real

`composeApp/build.gradle.kts`:

| Campo | Valor |
|---|---|
| `applicationId` | `br.com.leorvergani.escalaici.kmp.lab` |
| `namespace` | `br.com.leorvergani.escalaici.kmp.lab` |
| `compileSdk` | 36 |
| `minSdk` | 28 |
| `targetSdk` | 36 |
| `versionCode` | 15 |
| `versionName` | `0.7.1` |

`AppVersion.kt` (`model/AppVersion.kt`) está sincronizado manualmente com o Gradle: `CODE=15`, `LABEL="0.7.1"` — **sem divergência** entre working tree, HEAD local e `origin/feature/fase-16-trocas-reais` (nenhuma mudança de versão nas modificações não commitadas).

**Contexto comparativo:** o EscalaSOC de referência já está em `versionCode 37` / `versionName 1.20.8` (confirmado diretamente em `app/build.gradle.kts` na FASE 17A.1 — o commit `8236c79` mencionado na primeira leitura do log, "publica versionCode 35 (1.20.6)", refletia um estado anterior do histórico, já superado por commits subsequentes; corrigido aqui para não deixar documentação desatualizada). Isso é esperado — são apps com histórico de maturidade diferente — mas situa o KMP como um projeto ainda em estágio bem mais inicial de numeração de versão.

---

## 5. Estado Android KMP

Login, identidade, Trocas, Alertas, Perfil, Plantão e cache offline **todos implementados e funcionais no Android**, compartilhando quase 100% do código com Web via `commonMain` (ver seção 15). Only a genuína plataforma-específica: persistência de sessão (`SessionTokenStore.android.kt`, AES-256/GCM via Android Keystore) e o instalador de APK (`AppUpdateChecker.android.kt`).

---

## 6. Estado Web/Wasm

Web/Wasm reusa o mesmo `AuthRepository`/`IdentityToolkitAuthClient`/`FirestoreRestClient` comuns via Ktor CIO — **não existe fluxo de login separado para Web**. Persistência de sessão em Web usa `window.localStorage` puro (sem Keystore equivalente). Notificação usa `BrowserNotificationService` (Web Notifications API). Atualização de app é `NotSupported` no Web por design (instalação de APK não existe fora do Android). Nenhum popup/redirect OAuth existe no código — logo essa classe de bug (popup bloqueado, redirect URI) **não se aplica** ao estado atual.

---

## 7. Login atual

**KMP (Android + Web, idêntico):** Firebase Auth email+senha via REST puro (Identity Toolkit + Secure Token), sem SDK nativo do Firebase, sem MSAL. `MicrosoftAuthManager`/MSAL **não existe** no KMP — a entrada `msal = "4.9.0"` em `gradle/libs.versions.toml` é um alias órfão, nunca referenciado em `build.gradle.kts`. Identidade: `email → login (parte antes do @) → usuarios/{login}`. Modo Demo é um botão explícito na tela de login (`onDemoMode`), nunca setado por nenhum `catch`/erro — não há fallback silencioso. "Offline" no KMP se refere só a cache de escala, nunca a bypass de autenticação.

**EscalaSOC (referência):** dois fluxos coexistindo — MSAL legado (produção hoje, sem checagem de usuário ativo/inexistente no Firestore) **e** Firebase Auth com provider `microsoft.com` via Entra (FASE 7K-3C, com checagem completa via `usuarios/{login}`). Login por e-mail/senha existe só em build DEBUG, para testes. Logout só desconecta o MSAL, não o Firebase Auth — **inconsistência já identificada pelo próprio código-fonte auditado**.

**Escala-ICI PWA (referência 2, `apps/app/src/EmployeeApp.tsx` + `lib/firebase/authRepository.ts`):** `signInWithEmailAndPassword` do Firebase Auth — **sem Microsoft/Entra implementado** (confirmado por `AUTENTICACAO-MICROSOFT.md`, marcado explicitamente "nada aqui está implementado"). Mesma normalização `email.split('@')[0].toLowerCase()` → `usuarios/{login}`.

**Conclusão:** o KMP está alinhado com a PWA real (email/senha, sem Microsoft), e **atrás** do EscalaSOC (que já tem Microsoft/Entra real via Firebase). Isso é esperado dado que o EscalaSOC é o app estabilizado mais maduro.

---

## 8. Problema do login Web — hipótese técnica

Não há fluxo de popup/redirect no KMP Web — toda autenticação é REST puro via Ktor CIO, chamado igual em Android e Wasm. Hipóteses fundamentadas em código:

1. **CORS/mixed-content no emulador**: `emulatorAuthHost`/`emulatorFirestoreHost` default para `127.0.0.1` (`build.gradle.kts:76-79`). Se o bundle Wasm for aberto em outra máquina/dispositivo que não o host do emulador, toda chamada falha por conectividade, aparecendo como `EscalaIciError.NETWORK_ERROR` — indistinguível na UI de "login não funciona", mas é config/rede, não lógica de auth.
2. **`local.firebase.properties` ausente no build do artefato Web**: se esse arquivo (gitignorado) não estiver presente no momento do build do Wasm, `generateFirebaseConfig` cai em defaults vazios (`apiKey` vazio, `effectiveApiKey` cai para `"demo-api-key"`) — o artefato Web apontaria para um projeto Firebase inexistente/vazio enquanto o Android (buildado com o properties real) funciona normalmente, se os dois artefatos não vierem do mesmo arquivo de propriedades.
3. Não há evidência de bug de popup bloqueado, porque não existe mecanismo de popup no código atual.

**Recomendação:** confirmar, no pipeline de build Web (`fadd086 "ci: configura Firebase no build Web"`), se `local.firebase.properties`/equivalente CI está realmente presente e populado no ambiente que gera o artefato Web publicado.

---

## 9. Estado Hoje

KMP `TodayTab.kt`: próximo turno (hero card com colegas do mesmo turno), resumo semanal (7 pills ancorados na data real do dispositivo), resumo de período (badge "não importada" quando aplicável), eventos (próximo turno, próxima folga, contagem de dias seguidos trabalhados), pausa (`pauseFor`), observações da escala. Estrutura e conteúdo **muito próximos** do `TodayScreen.kt` do EscalaSOC (mesmas seções, mesma ordem: hero → resumo semanal → eventos/pausa → observações → resumo do período).

---

## 10. Estado Escala

KMP `ScheduleTab.kt` + `PeriodCalendarView`: **navegação mês-a-mês com chevrons** (`moveMonth(-1/+1)`, `visibleMonth`, `firstMonth`/`lastMonth`), uma grade por mês civil. EscalaSOC `CalendarScreen.kt` **removeu** exatamente esse padrão (commit `511b9c8`) e hoje usa **uma única grade contínua** cobrindo o período operacional 26→25 completo, sem chevrons — só um botão de refresh, com título dinâmico (`"Julho — agosto de 2026"` etc. via `competenciaHeaderLabel`). **Isso confirma `PARIDADE_CALENDARIO_26_25 = PENDENTE` no KMP** (ver seção 8 do prompt original) — o KMP ainda usa o modelo antigo que o EscalaSOC já substituiu.

No lado do dado, o KMP hoje **confia no `periodoInicio`/`periodoFim` vindos do Firestore** (`turnosMes`) para o resumo/estatísticas, em vez de recalcular a regra 26→25 localmente — o `Jornada.competenciaOperacional(dia, corte=26)` que existe no EscalaSOC (`Jornada.kt:27-31`, porte literal de `jornada.ts` do Escala-ICI) **não tem equivalente ativo no KMP** para a navegação do calendário.

---

## 11. Estado Trocas

**Totalmente implementado no KMP e conectado a Firestore real** (não mockado) — `TrocasEscala.kt`, `TrocasSession.kt`, `TrocasScreen.kt`, com testes cobrindo badge, mapeamento remoto e integração real contra emulador (`TrocasEscalaIntegrationTest.kt`).

Comparação de contrato com `packages/contrato`/`lib/trocasEscala.ts` do Escala-ICI real: **os 7 status (`PENDENTE_USUARIO`→...→`APROVADA_PUBLICADA`/`EXPIRADA`), a tabela de transições `TRANSICOES_TROCA_REAL`, o limite de mensagem (280 caracteres) e `validarNovaSolicitacaoTroca` batem exatamente** entre KMP e o contrato oficial. Isso é uma paridade sólida e correta.

**Divergências funcionais reais:**
- **Sem aba/fluxo de Gestor no KMP** — decisão documentada: o app do colaborador só cria/responde/cancela; qualquer aprovação de gestor (`PENDENTE_GESTOR → {RECUSADA_GESTOR, APROVADA_PUBLICADA}`) não tem UI no KMP (nem deveria, é um app de colaborador — mas confirma que "paridade completa" com o EscalaSOC (que TEM aba Gestor) não é o objetivo aqui).
- **Sem enforcement de 6x1 / descanso 11h em Trocas no KMP.** No EscalaSOC, `avaliarElegibilidadeTroca()` **bloqueia** a troca se violar 6x1 ou 11h de descanso — divergência de regra de negócio explicitamente documentada no próprio EscalaSOC (`TROCA_REGRAS_JORNADA_ANDROID`) como decisão de produto mais estrita que a PWA. No KMP, `validarNovaSolicitacaoTroca` (`TrocasEscala.kt:74-99`) só checa: destinatário válido, ambos ativos, ambos com turno no dia, turnos não idênticos — **sem checagem de sequência de dias ou descanso**. Isso também não existe no contrato oficial (`lib/trocasEscala.ts` da Escala-ICI real não tem essa checagem) — então o KMP está alinhado com o contrato oficial e é o **EscalaSOC** que adicionou uma regra extra, não documentada como requisito universal.
- Notificações de troca são só in-app (Firestore `notificacoesTroca`), sem push, em ambos KMP e EscalaSOC — paridade correta aqui.

---

## 12. Estado Alertas

**Existe no KMP** (`AlertsTab.kt` + `LabAlerts.kt`) — hero, resumo de severidade, chips de filtro, e geradores: `generateRestAlerts` (11h descanso), `generateSixByOneAlerts` (6x1), `generateInconsistencyAlerts`. Estrutura muito próxima de `AlertsScreen.kt` do EscalaSOC (mesmo padrão hero/resumo/filtros).

**Divergência importante com o contrato oficial:** no Escala-ICI real, os alertas de 6x1/descanso (`lib/alertasEscala.ts`, `LIMITE_DIAS_CONSECUTIVOS_TRABALHO`, `MINIMO_DESCANSO_HORAS`) são **exclusivos do Dashboard (gestor)** — comentário explícito no código: *"módulo puro, só no Dashboard"*. A PWA do colaborador (`apps/app/EmployeeApp.tsx`) **não tem aba Alertas** (suas 5 telas são `hoje/minha/trocas/equipe/perfil`). Ou seja: **tanto o KMP quanto o EscalaSOC expõem Alertas ao colaborador final — algo que a PWA oficial nunca fez.** Isso não é um bug, mas é uma decisão de produto que já diverge da PWA em ambos os apps mobile; vale confirmar se é intencional antes de "convergir" o KMP para algo que nem a fonte de verdade da PWA expõe ao colaborador.

---

## 13. Estado Perfil

KMP `ProfileTab.kt` tem **duplicação de identidade** entre os cards "Perfil selecionado" e "Identidade da escala" (nome/login/equipe aparecem em ambos, além de reaparecer no header de toda aba via `LabPremiumHeader`). Contém também dois cards de placeholder/decorativo: o switch "Lembrete de pausa" (`enabled=false`) e a ação "Remover escala local" (`DisabledAction`, "Disponível em uma próxima etapa").

O EscalaSOC **já resolveu exatamente esse problema** na FASE 7K-3G (commit `77a2e24`): unificou em um único `ProfileHeaderCard` (nome+e-mail+status, uma vez só) e organizou em `Perfil → Escala atual → Notificações → Pausa de 15 minutos → Conta → Aplicativo`, deixando `DemoModeCard`/`ImportScaleEntryCard`/`AdminScaleCard`/`LocalStorageCard` como código órfão (definidos mas não chamados).

**Matriz de ação recomendada para o KMP (baseada na forma-alvo do EscalaSOC):**

| Card atual no KMP | Ação recomendada |
|---|---|
| "Perfil selecionado" (avatar, nome, período, fonte, e-mail, Sair) | MANTER — vira o card único de identidade |
| "Identidade da escala" (login/scaleName/equipe) | UNIFICAR com "Perfil selecionado" (mesma duplicação que o EscalaSOC já eliminou) |
| "Resumo do perfil" | MANTER — mapeia para "Escala atual" |
| "Notificações" | MANTER |
| "Pausa de 15 minutos" | MANTER, mas REMOVER DA UI o switch decorativo "Lembrete de pausa" (não funcional) |
| "Resumo" (duplica conteúdo da Pausa) | UNIFICAR com "Pausa de 15 minutos" |
| "Dados da escala" / "Armazenamento local" | MANTER (mutuamente exclusivos, ok), mas REMOVER DA UI a ação "Remover escala local" enquanto não implementada, ou implementá-la |
| "Aplicativo" | MANTER |

Não implementar nada disso agora — apenas registrado como direção para uma fase futura.

---

## 14. Estado Plantão

KMP `PlantaoScreen.kt`: hero + calendário mensal com chevrons + detalhe do dia, com fallback para `mockOnCallAssignments()`. O próprio código já documenta a lacuna: *"Plantao COSI ainda nao tem contrato/colecao propria no Firebase novo... reservado, nao implementado."*

**Confirmado no contrato oficial:** `Categoria.PLANTAO`/`contaComoPlantao`/`pesoPlantao` existem no `TipoTurno` da Escala-ICI real (`packages/contrato/src/tipos.ts`), mas **nenhum turno seedado os usa** (`contaComoPlantao: false, pesoPlantao: 0` em todos os 12 itens do catálogo `CatalogoSoc`/seed) — não existe coleção Firestore própria para Plantão nem na PWA nem no Dashboard oficiais. O EscalaSOC também trata Plantão como uma feature paralela com import manual de planilha (`PlantaoWorkbookParser`), não como algo sincronizado do Firebase. **Os três projetos concordam: Plantão é uma feature "à parte", sem contrato Firestore oficial ainda.** O KMP não está divergindo aqui — está no mesmo estágio que os outros dois.

---

## 15. Offline/cache

KMP tem dois caches independentes, por design: `EscalaIciScheduleCache` (schedule sincronizado do Firebase, chave por login, versionado `schemaVersion=2`, isolado por usuário, testado exaustivamente) e `LocalDataCache` (arquivo importado manualmente, JSON em `localStorage` no Web). O padrão "mostra cache primeiro, depois sincroniza, nunca apaga cache em falha remota" (`syncShowingCacheFirst`) é idêntico em espírito ao `LoggedScheduleSyncCoordinator.refresh()`/`fallbackFrom` do EscalaSOC. Causas de erro tipadas (`EscalaIciError` enum, 17 valores incluindo causas específicas de Trocas) substituem mensagens genéricas — mesma direção do commit "OVERNIGHT ONDA 3.1" mencionado no histórico do KMP.

---

## 16. Paridade com EscalaSOC

| Item | KMP | EscalaSOC | Gap |
|---|---|---|---|
| Login | Firebase e-mail/senha só | MSAL legado + Firebase Microsoft (Entra) + debug e-mail/senha | KMP não tem SSO corporativo real ainda |
| Calendário 26→25 | Grade mês-a-mês com chevrons (modelo antigo) | Grade única contínua, sem chevrons | **PENDENTE** — ver seção 10 |
| Perfil | Identidade duplicada em 2 cards, 2 placeholders decorativos | Identidade única, cards órfãos removidos da UI | Ação recomendada na seção 13 |
| Trocas | Sem aba Gestor, sem regra 6x1/11h bloqueante | Tem aba Gestor, bloqueia 6x1/11h na troca | Divergência de escopo intencional (app de colaborador) |
| Alertas | Existe, mesma estrutura | Existe, mesma estrutura | Paridade OK |
| Plantão | Import manual, sem contrato Firebase | Import manual (OneDrive/planilha), sem contrato Firebase | Paridade OK (ambos "à parte") |
| Notificações | Só Web Notifications API manual (1 botão de teste) | AlarmManager com 5+ tipos de lembrete automático | KMP muito atrás aqui |
| Atualização de app | Android real, Web `NotSupported` | Android real (Dropbox manifest) | Paridade estrutural OK |
| Identidade Firestore | `email→login→usuarios/{login}` | `email→login→usuarios/{login}` | Paridade OK |

---

## 17. Paridade com Escala ICI PWA

| Item | KMP | PWA (`apps/app`) | Observação |
|---|---|---|---|
| Login | E-mail/senha via REST | E-mail/senha via SDK JS (`signInWithEmailAndPassword`) | Mesmo provedor, paridade conceitual OK |
| Identidade | `usuarios/{login}` | `usuarios/{login}` | Paridade exata |
| Abas | Hoje / Escala / Trocas / **Alertas** / Perfil / Plantão | Hoje / Minha (escala) / Trocas / **Equipe** / Perfil | PWA não tem Alertas (é só-Dashboard); PWA tem "Equipe", KMP não tem aba dedicada (mostra colegas dentro do Hoje) |
| Trocas | 7 status, sem aprovação de gestor na UI | 7 status, gestor aprova no Dashboard (não na PWA) | Contrato de status idêntico |
| Calendário | Mês-a-mês | Não auditado em detalhe (fora do escopo desta rodada) | — |

---

## 18. Paridade Firebase/Firestore

Confirmado lendo `firestore.rules` e `packages/contrato/src/tipos.ts`/`lib/trocasEscala.ts` do repo real:

- `usuarios/{login}` — chave é o login (parte antes do `@`), **nunca uid**. `loginDoAuth() = request.auth.token.email.lower().split('@')[0]`. **KMP está alinhado.**
- `tiposTurno/{docId}` — campos `codigo, descricao, categoria, horaInicio?, horaFim?, duracaoMinutos, viraDia, contaComoPlantao, pesoPlantao, corHex, aliasesXLS`. Não auditado se o KMP consome exatamente esses campos (fora do escopo desta rodada — recomenda-se checagem pontual antes de qualquer tela nova de catálogo).
- `turnosMes/{docId}` — `schemaVersion, usuarioUid, login, equipeId, competencia, periodoInicio, periodoFim, turnoPadrao, status ('RASCUNHO'|'PUBLICADA'), dias, totais, importacaoId?, publicadoPor?, publicadoEm?, atualizadoEm?`. O KMP lê `periodoInicio`/`periodoFim` diretamente do documento (seção 10) — correto e alinhado.
- `trocasEscala/{id}` — `SolicitacaoTrocaReal` com 7 status, histórico append-only, `snapshotValidacao` para detectar escala mudada. **Contrato batendo exatamente com `TrocasEscala.kt` do KMP** (transições, limite de mensagem, validação).
- `notificacoesTroca/{id}` — `NotificacaoTroca` com 6 tipos de notificação. **Batendo com `NotificacaoTrocaDto` do KMP.**

Nenhuma divergência de schema encontrada nas coleções auditadas (usuarios, trocasEscala, notificacoesTroca). `tiposTurno`/`turnosMes` não foram comparados campo-a-campo contra o parser/DTO Kotlin nesta rodada — recomendado para uma FASE 17B focada em dados.

---

## 19. Matriz completa

| FUNCIONALIDADE | ESCALASOC | ESCALA ICI PWA | KMP ANDROID | KMP WEB | AÇÃO NECESSÁRIA |
|---|---|---|---|---|---|
| Login Microsoft/Entra | ✅ real (Firebase+Entra) | ❌ não implementado | ❌ não implementado | ❌ não implementado | Nenhuma ainda — PWA (fonte de verdade) também não tem; não é regressão do KMP |
| Login Firebase e-mail/senha | ✅ (debug only) | ✅ (principal) | ✅ (principal) | ✅ (principal) | Nenhuma |
| Restauração de sessão | ✅ | não auditado | ✅ | ✅ | Nenhuma |
| Logout | ⚠️ só desconecta MSAL, não Firebase | não auditado | ✅ completo | ✅ completo | KMP já está correto; é o EscalaSOC que tem a inconsistência |
| Usuário inexistente/inativo | ✅ (fluxo novo) / ❌ (fluxo MSAL legado) | ✅ | ✅ | ✅ | Nenhuma no KMP |
| Demo mode isolado | ⚠️ existe mas com card órfão | não aplicável | ✅ limpo, só botão explícito | ✅ | Nenhuma no KMP |
| Hoje — próximo turno/resumo/pausa | ✅ | não auditado em detalhe | ✅ | ✅ | Nenhuma |
| Calendário 26→25 grade única | ✅ (já migrado) | não auditado em detalhe | ❌ (modelo mês-a-mês antigo) | ❌ | **PARIDADE_CALENDARIO_26_25 = PENDENTE** |
| Trocas — CRUD completo | ✅ | ✅ (sem aprovação na PWA) | ✅ | ✅ | Nenhuma |
| Trocas — aprovação de gestor | ✅ (na tela) | ✅ (no Dashboard) | ❌ (não é o escopo) | ❌ | Confirmar que é decisão de escopo, não lacuna |
| Trocas — regra 6x1/11h bloqueante | ✅ (mais estrito que o contrato) | ❌ (contrato não bloqueia) | ❌ | ❌ | **PARIDADE_REGRAS_TROCA_GLOBAL_PENDENTE** — decidir se KMP deve adotar a regra extra do EscalaSOC ou seguir o contrato oficial (não portar sem essa decisão, ver FASE 17A.1 seção 7) |
| Alertas (6x1/descanso informativos) | ✅ (na tela do colaborador) | ❌ (só no Dashboard) | ✅ (na tela do colaborador) | ✅ | Confirmar decisão de produto — nem KMP nem EscalaSOC seguem a PWA aqui |
| Perfil — identidade única (sem duplicação) | ✅ (FASE 7K-3G) | não auditado | ❌ (duplicada em 2 cards) | ❌ | Unificar (ver seção 13) |
| Perfil — cards placeholder visíveis | ✅ (removidos da UI) | não auditado | ❌ (2 ainda visíveis, desabilitados) | ❌ | Remover ou implementar |
| Plantão | ⚠️ import manual, sem contrato Firebase | ❌ (reservado, não seedado) | ⚠️ import manual, sem contrato Firebase | ⚠️ | Nenhuma — paridade OK (todos "à parte") |
| Notificações automáticas (turno/pausa) | ✅ (AlarmManager, 5+ tipos) | não auditado | ❌ (só 1 botão manual de teste) | ❌ | Gap real — considerar para fase futura |
| Atualização in-app | ✅ (Android, manifest Dropbox) | não aplicável | ✅ (Android, manifest próprio) | N/A (`NotSupported`, correto) | Nenhuma |
| Offline/cache com causas tipadas | ✅ (`fallbackFrom` + mensagens) | não auditado | ✅ (`EscalaIciError`, 17 causas) | ✅ | Nenhuma |
| Identidade `usuarios/{login}` (nunca uid) | ✅ | ✅ | ✅ | ✅ | Nenhuma — paridade confirmada nas 4 bases |
| Responsividade Web | não aplicável | não auditado em detalhe | não auditado em detalhe | não auditado em detalhe | Pendente para FASE 17B |

---

## 20. Ordem recomendada das próximas fases

1. **FASE 17B (dados) — antes de qualquer UI:** comparar campo-a-campo `tiposTurno`/`turnosMes` do contrato oficial (`packages/contrato/src/tipos.ts`) contra os DTOs/parsers Kotlin do KMP (`RemoteDtoMappers.kt`, `EscalaIciScheduleMapper.kt`). Esta auditoria não cobriu isso em detalhe.
2. **Calendário 26→25** — migrar o KMP do modelo mês-a-mês com chevrons para a grade única contínua, replicando `PeriodCalendarGrid`/`competenciaHeaderLabel` do EscalaSOC. É a divergência visual mais visível e mais fácil de justificar (já resolvida na referência).
3. **Perfil** — unificar identidade duplicada e remover/implementar os dois placeholders decorativos, seguindo a forma-alvo da seção 13.
4. **Decisão de produto sobre 6x1/11h em Trocas** — confirmar com quem definiu a regra do EscalaSOC se ela deve ser retroportada ao KMP (e ao contrato oficial) ou se é uma decisão isolada daquele app.
5. **Decisão de produto sobre Alertas no colaborador** — confirmar se deve continuar existindo no app do colaborador (KMP/EscalaSOC) mesmo a PWA oficial não expondo isso fora do Dashboard.
6. **Login corporativo (Microsoft/Entra) no KMP** — maior esforço, e nem a PWA oficial tem ainda; priorizar só depois dos itens acima, a menos que haja pressão externa para SSO.
7. **Notificações automáticas** (lembrete de turno/pausa) — gap real de funcionalidade vs. EscalaSOC, mas de menor risco de regressão; pode vir depois do calendário e do perfil.

Verificar staging (`escala-ici-staging`) antes de iniciar qualquer fase que grave dados — nenhuma escrita foi feita nesta auditoria.

---

## 21. Arquivos que deverão mudar (fases futuras, não nesta)

- `composeApp/src/commonMain/kotlin/.../ui/ScheduleTab.kt` + `model/ScheduleModels.kt` (`LabYearMonth`, `moveMonth`, `PeriodCalendarView`) — calendário 26→25
- `composeApp/src/commonMain/kotlin/.../ui/ProfileTab.kt` — unificação de identidade, remoção de placeholders
- Possivelmente `composeApp/src/commonMain/kotlin/.../firebase/TrocasEscala.kt` — se a decisão do item 4 da seção 20 for adotar 6x1/11h
- Possivelmente `composeApp/src/commonMain/kotlin/.../ui/AlertsTab.kt` — se a decisão do item 5 for remover Alertas do app do colaborador

## 22. Arquivos que NÃO devem mudar

- `TrocasEscala.kt` / `TrocasSession.kt` / `TrocasScreen.kt` / `TrocasBadgeTest.kt` — trabalho local não commitado da FASE 16, em andamento; **não sobrescrever** (regra 16 do prompt original respeitada)
- `firestore.rules`, contrato oficial (`packages/contrato`) — pertencem ao repo `Escala-ICI`, não ao KMP; qualquer mudança de contrato deve nascer lá, não aqui
- `AppVersion.kt`/`build.gradle.kts` (campos de versão) — nenhuma mudança de versão foi solicitada nesta fase

## 23. Riscos

- **`.vscode/`, `auth-config.json`, `composeApp/google-services.json` estão untracked e SEM entrada no `.gitignore`** — risco real de serem commitados por acidente num `git add -A`/`git add .` futuro. `keystore.properties`, `*.jks`, `.android/debug.keystore`, `local.firebase*.properties` **estão corretamente ignorados** — sem risco nesses.
- Trabalho local não commitado em Trocas (3 arquivos modificados) pode ser perdido se alguém rodar `git checkout .`/`git reset --hard` sem stash antes — nenhuma dessas operações foi executada nesta auditoria.
- `/home/lvergani/Projetos/escala-ici` sendo confundido com o repo real `Escala-ICI` em automações futuras (ver seção 0) — maior risco de "auditoria fantasma" desta rodada, já mitigado aqui ao clonar o repo correto.
- Regra 6x1/11h divergente entre EscalaSOC (bloqueia) e contrato oficial (não bloqueia) — se não for decisão consciente, é uma divergência de regra de negócio entre produção e o repo "fonte de verdade".

## 24. `git status --short` (estado final, inalterado)

```
 M composeApp/src/commonMain/kotlin/br/com/leorvergani/escalaici/kmp/lab/firebase/TrocasEscala.kt
 M composeApp/src/commonMain/kotlin/br/com/leorvergani/escalaici/kmp/lab/firebase/TrocasSession.kt
 M composeApp/src/commonMain/kotlin/br/com/leorvergani/escalaici/kmp/lab/ui/TrocasScreen.kt
?? .vscode/
?? CHECKPOINT-FASE-16-PRE-STAGING.md
?? auth-config.json
?? composeApp/google-services.json
?? composeApp/src/commonTest/kotlin/br/com/leorvergani/escalaici/kmp/lab/firebase/TrocasBadgeTest.kt
```
(este próprio arquivo `CHECKPOINT-FASE-17A-AUDITORIA-CONVERGENCIA.md` aparecerá como novo untracked após ser salvo)

## 25. `git diff --check`

```
(sem saída — nenhum problema de whitespace/conflito nas modificações atuais)
```
