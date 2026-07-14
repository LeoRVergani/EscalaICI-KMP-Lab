# SPEC 50 — Migração final e matriz de paridade EscalaSOC × Escala ICI

**Status:** proposta; nenhuma linha de código funcional criada por esta spec
**Escopo:** comparação EscalaSOC (referência) × Escala ICI KMP (Android + Web/PWA)
**Fase:** FASE 14a (documentação) — condição de saída avaliada continuamente ao longo das FASES 14b-14h
**Princípio inegociável:** `EscalaSOC` não é desligado nem alterado por esta migração; nenhuma funcionalidade é removida antes de paridade validada (spec 42 do EscalaSOC, já em vigor).

## 1. Como ler esta matriz

- **Estado no EscalaSOC**: o que existe hoje, de verdade, no app de produção
  (referência, auditado por leitura de código nesta fase).
- **Estado no KMP Android**: o que existe hoje no `EscalaICI-KMP-Lab`,
  compilando para Android.
- **Estado no Web/PWA**: o que existe hoje no mesmo código-fonte, target
  wasmJs.
- **Condição para considerar migrada**: o critério objetivo e verificável que
  precisa ser satisfeito antes de qualquer decisão de substituir o EscalaSOC
  para um grupo de usuários nessa funcionalidade específica — alinhado aos 9
  critérios já definidos na spec 42 do EscalaSOC (paridade funcional, MSAL
  real, Firestore real, dados consistentes, testes, piloto, 2 semanas
  estáveis, rollback validado).

## 2. Matriz de paridade

| Funcionalidade | EscalaSOC | KMP Android | Web/PWA | Dependências | Testes | Bloqueadores | Condição para considerar migrada |
|---|---|---|---|---|---|---|---|
| Hoje | Real, dados do XLS importado/Dropbox | Funcional com dados locais/importados/mock | Funcional, mesma base | Sincronização real (spec 48) | Cobertura indireta via `ScheduleRulesTest` | Sem Firestore real conectado à identidade | Mostrar turno real do usuário autenticado, sem mock, com estado tipado quando não houver dado |
| Escala | Real, calendário mensal do XLS | Funcional (calendário, mocks quando não importado) | Funcional, mesma base | Spec 48 | `LabWorkbookParserTest` (8) | Mesmo de "Hoje" | Mesmo de "Hoje" |
| Importar | Real, seletor de arquivo + OneDrive (Graph) | Real (seletor local), Dropbox parcial | Real (seletor local), Dropbox parcial (Web bloqueado por config pendente) | OneDrive não portado no KMP; Dropbox só contingência (spec 46/48: Firestore é fonte primária) | 8 testes de parser | OneDrive real ausente no KMP (não é bloqueador de MVP — vira contingência administrativa, não fonte do colaborador) | Fonte primária do colaborador é Firestore (publicado pelo Dashboard); importação manual vira só contingência/administrativa |
| Alertas | Real, `GenerateScaleAlertsUseCase` | Funcional em modo demo (`GenerateLabAlerts`) | Funcional, mesma base | Spec 48 (dado real sincronizado) | Não auditado a fundo nesta fase | Alertas ainda geram sobre dado demo, não sobre escala real do usuário | Alertas calculados sobre escala real, sincronizada, do usuário autenticado |
| Perfil | Real (MSAL, Dropbox admin, notificações) | Estático/mock (login fake, toggles decorativos) | Mesmo estado do Android | Specs 46, 47, 49 | — | Login mock, pausa decorativa (ver linha "Pausa") | Identidade real (spec 46/47), controles funcionais (spec 49), sem nenhum toggle hardcoded |
| Plantão | Real (`PlantaoWorkbookParser`, Firestore) | Funcional (parser portado, mock quando não importado) | Mesma base | Spec 48 (query real por `teamId`/período) | 7 testes (`PlantaoWorkbookParserTest`) | Ainda busca-tudo-e-filtra no cliente (spec 48) | Mesma condição de "Hoje"/"Escala", aplicada ao domínio de plantão |
| Trocas | Real, Firestore (`FirestoreShiftSwapRepository`, não auditado a fundo) | Mock em memória (`ShiftSwapScreen`), sem persistência | Mesma base | Firestore real para `shift_swap_requests` (schema já previsto na spec 35) | — | Sem persistência real no KMP | Aceitar/recusar/cancelar persiste em Firestore, visível para ambas as partes, com regras de autorização (spec 46/47) |
| Clima | Não auditado nesta fase (fora do escopo dos 20 pontos de diagnóstico) | `WeatherChip` visual (ícone), sem dado real | Mesma base | A definir | — | Não é uma funcionalidade central de escala — avaliar se permanece no escopo do MVP oficial | Se mantida: dado real de alguma API de clima; se não: remover da UI sem promessa de dado real |
| Notificações | Real (`shift_reminders`, `app_updates`, `AlarmManager` inexato, sem boot receiver) | Nenhuma implementada | Nenhuma implementada | Spec 49 | — | Ausência total no KMP | Notificações reais Android (spec 49, corrigindo as lacunas do legado) + limitação documentada explícita no Web |
| Pausa | Real (`PauseWindow.kt`, `AlarmManager` inexato, sem boot receiver, sem receiver de fuso) | 100% decorativo (`enabled=false` hardcoded) | Mesmo estado do Android | Spec 49 | — | Nenhuma lógica de habilitação existe | Spec 49 seção 12 (critérios de aceite próprios) |
| MSAL | Real (`MicrosoftAuthManager.kt`, single-account, escopos `User.Read`+Graph) | Nenhum (login mock) | Nenhum | Spec 46 | — | App registration precisa de plataforma Android adicional (ação humana, `docs/PENDENCIAS-EXTERNAS.md`) | Login MSAL real Android funcionando ponta a ponta; Web com plano claro mesmo que implementado depois |
| Firebase | `FirebaseImportScreen` oculta (não auditada a fundo); sem Firebase Auth | Leitura anônima do Firestore (sem Auth), escrita nenhuma | Mesma base | Specs 46, 48 | — | Regras de produção abertas expiram 2026-08-04 (~3 semanas) | Firebase Auth real (custom token via ponte, spec 46) antes do prazo, Rules autenticadas publicadas com testes |
| OneDrive | Real, 10 estratégias de busca em cascata (Graph) | Nenhum | Nenhum | Fora do escopo crítico — Firestore é fonte primária (decisão arquitetural desta fase) | — | Nenhum (não bloqueador) | Não é condição de migração — OneDrive vira contingência administrativa, não path do colaborador comum |
| Cache | Local (implícito no fluxo de importação) | Dois caches (Firebase + arquivo importado), preserva em falha | Mesma base | Spec 48 | Nenhum teste dedicado (lacuna já documentada na spec 44 do EscalaSOC) | Falta query real (spec 48 seção 3) | Cache sobrevive a offline, nunca some em erro, com testes de corrupção/schema (spec 48 seção 9) |
| Atualização de APK | Real, `version.json` no Dropbox, streaming de download | Real (reaproveita `version.json`, campos `kmp*`, streaming, bug de OOM já corrigido) | Não aplicável (Web não instala APK) | Nenhuma nova — já portado | Não auditado a fundo | Upload continua manual (decisão intencional, spec 41) | Já considerada portada — manter processo manual, nunca automatizar upload |
| Permissões | Implícita (e-mails hardcoded para admin) | Nenhuma (`dashboard_permissions` só como schema, sem enforcement) | Mesma base | Spec 39 do EscalaSOC (papéis) + spec 47 (vínculo) | — | Sem enforcement real de permissão no app | `dashboard_permissions` aplicado de fato, com testes de acesso negado/concedido |
| Autenticação | MSAL real, sem Firebase Auth | Mock | Mock | Spec 46 | — | Ver linha MSAL | Ver linha MSAL |
| Segurança | Sem client secret; tokens Dropbox em `SharedPreferences` não criptografado (dívida técnica documentada, não corrigida) | Sem client secret; nenhum token de sessão real ainda existe | Mesma base | Spec 46 (armazenamento criptografado desde o início) | — | Nenhum (KMP ainda não tem token de sessão para proteger) | Escala ICI nunca repete a dívida técnica do legado — sessão sempre em armazenamento criptografado |
| Acessibilidade | Não auditado a fundo nesta fase | Não auditado a fundo nesta fase (fora dos 20 pontos de diagnóstico) | Idem | A definir | — | — | Avaliação dedicada de acessibilidade fica para fase própria, fora do escopo desta matriz inicial |
| Offline | Real (dado do XLS já importado localmente) | Cache preservado em falha (spec 48) | Mesma base | Spec 48 | — | Falta estados tipados (spec 48 seção 7) | Spec 48 critérios de aceite |
| Release | Manual, `version.json` compartilhado, keystore próprio | Idem (já alinhado, spec 41) | Não aplicável (Web via Cloudflare Pages) | Nenhuma nova | — | Nenhum | Já considerada portada |

## 3. Bloqueadores centrais para o MVP oficial (consolidado)

Por ordem de urgência:

1. **Regras Firestore abertas expiram 2026-08-04** (~3 semanas a partir desta
   auditoria) — sem SPEC 46 (ponte MSAL↔Firebase Auth) e SPEC 48 (queries
   reais) implementadas antes disso, ou publicar Rules autenticadas quebra
   Android legado + KMP, ou manter modo aberto além do prazo é um risco de
   segurança/operacional que precisa de decisão humana explícita antes da
   data.
2. **MSAL real ausente no KMP** (SPEC 46) — pré-requisito de tudo mais
   (identidade, permissões, sincronização por usuário real).
3. **`user_links` inexistente** (SPEC 47) — sem isso, não há como resolver
   `memberId`/`teamId` a partir de uma sessão autenticada.
4. **Pausa e notificações 100% decorativas** (SPEC 49) — funcionalidade
   visível ao usuário final, hoje sem nenhuma lógica real.
5. **Trocas de escala sem persistência** — mock em memória, não sobrevive a
   reabertura do app.

## 4. O que NÃO bloqueia o MVP oficial (decisão explícita desta spec)

- OneDrive real no app do colaborador comum — Firestore é a fonte primária
  (decisão arquitetural desta fase); OneDrive/XLS vira contingência
  administrativa, não caminho do usuário final.
- Web com MSAL completo desde o primeiro release — pode seguir com Android
  primeiro, desde que o plano Web (redirect URI SPA, seção 4 da spec 46)
  esteja registrado e não seja esquecido.
- Clima — não é central ao domínio de escala; decisão de manter/remover cabe
  a uma fase de polimento, não ao MVP de autenticação/sincronização.
- Acessibilidade aprofundada — avaliação própria, não bloqueia esta série de
  fases (14a-14h), mas deve ser retomada antes do MVP oficial final (spec 45
  do EscalaSOC já lista isso como critério de qualidade geral).

## 5. Critérios de aceite desta spec

1. A matriz cobre todas as 21 funcionalidades pedidas (Hoje, Escala,
   Importar, Alertas, Perfil, Plantão, Trocas, clima, notificações, pausa,
   MSAL, Firebase, OneDrive, cache, atualização de APK, permissões,
   autenticação, segurança, acessibilidade, offline, release).
2. Nenhuma linha da matriz recomenda desligar/alterar o EscalaSOC.
3. Os bloqueadores centrais (seção 3) estão ordenados por urgência real
   (prazo de calendário primeiro).
4. Toda funcionalidade "não bloqueadora" (seção 4) tem justificativa
   explícita, não é apenas omitida.
