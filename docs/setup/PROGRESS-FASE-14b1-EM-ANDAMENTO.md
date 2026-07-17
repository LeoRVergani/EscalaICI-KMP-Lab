# PROGRESSO FASE 14b-1 (arquivo de trabalho — apagar antes do commit final)

Existe só para sobreviver a uma queda do VS Code/sessão. Não é entrega da
fase — remover antes do commit final (não deve ir para o Git final, mas
cada checkpoint intermediário pode incluí-lo).

## Checkpoints

- [x] Tarefa 1 (auditoria + desenho): `docs/spec/53-ESCALAICI-MSAL-ANDROID-E-IDENTIDADE-CORPORATIVA.md`
  criado. Decisões-chave: pacote comum `auth` com `CorporateIdentity`/
  `CorporateAuthState`/`CorporateAuthError`/`CorporateAuthConfigurationState`/
  `CorporateAuthRepository`; interativo via `CorporateAuthHost` opaco
  (não Activity direto no contrato comum); config MSAL via
  `buildConfigField` lido de `auth-config.json` raiz (não resource raw,
  não File externo) + JSON gerado em runtime em `filesDir` só quando
  configurado; gate de entrada no app (`sessionMemberId`) NÃO muda —
  login corporativo não deriva Member. Ver spec 53 completa para detalhes.
- [x] Tarefa 2 (contratos comuns): pacote `br.com.leorvergani.escalaici.auth`
  em commonMain (`CorporateIdentity`, `CorporateAuthConfigurationState`,
  `CorporateAuthError` + `defaultMessage()`, `CorporateAuthState`,
  `CorporateAuthHost` marcador, `CorporateAuthRepository`,
  `FakeCorporateAuthRepository`). Codex implementou os 7 arquivos; Claude
  revisou e adicionou `yield()` no `signInInteractive` da fake para o
  estado `Authenticating` ficar observável em teste. Testes em
  `composeApp/src/commonTest/.../auth/CorporateAuthRepositoryTest.kt`
  (21 testes, cobrindo os 17 cenários pedidos) escritos por Claude —
  `:composeApp:testDebugUnitTest` completo passou (todas as classes,
  não só as novas).
- [x] Tarefa 3 (adapter MSAL Android): Codex implementou `MsalCorporateAuthRepository`
  (androidMain) + `CorporateAuthHostProvider` expect/actual (commonMain/androidMain/
  wasmJsMain) + `AndroidCorporateAuthHost`. Dependência `msal:4.9.0` adicionada via
  `androidMainImplementation` com GAV em string (exclude de `opentelemetry-bom` e
  `display-mask`, conflitos conhecidos do MSAL). Config lida de `auth-config.json`
  (gitignorado) em tempo de build via `buildConfigField`; sem config real, todos os
  campos ficam vazios/`NOT_CONFIGURED` e o app continua compilando e abrindo
  normalmente (confirmado no manifest mesclado: path do redirect cai em
  `/NOT_CONFIGURED`, nunca crasha). `BrowserTabActivity` registrada no
  AndroidManifest com `manifestPlaceholders` por variante (debug/release).
  `PlatformCapabilities.supportsCorporateAuth` adicionado; `MainActivity` seta
  `true` no Android. Repositório MSAL ainda não é instanciado em lugar nenhum
  (isso é Tarefa 4 — wiring de UI). Claude revisou: nenhum token/log sensível
  (grep vazio), nenhuma persistência manual de token (cache delegado à lib MSAL
  via `SingleAccountPublicClientApplication`), nenhum client secret, continuations
  protegidas contra double-resume (`resumeIfActive`), sem override de
  `onActivityResult` necessário (MSAL moderno usa `BrowserTabActivity` do
  manifest). Observação não bloqueante: `MsalServiceException` mapeia hoje para
  `TenantNotAllowed` de forma ampla (não distingue todos os códigos AADSTS) —
  só será possível refinar com testes contra o Entra real, fora do escopo desta
  fase. Validado: `compileDebugKotlinAndroid`, `testDebugUnitTest` (mantém as
  21 novas + suíte completa) e `assembleDebug` — todos BUILD SUCCESSFUL.
- [x] Tarefa 4 (interface): sessão anterior do VS Code caiu com o diff pronto
  mas sem commit; retomado e validado nesta sessão. `LoginGateScreen`
  cobre os 5 estados (`!supportsCorporateAuth` → "Login corporativo Web
  ainda não configurado."; `NOT_CONFIGURED` → mensagem real + "Ver
  instruções de configuração" (AlertDialog) + "Entrar no modo
  demonstração"; `Authenticating` com progresso; `Failed` com mensagem
  tipada + "Tentar novamente"; `Authenticated`/`Demo` com texto de
  status), texto estático enganoso removido. `ProfileTab` mostra nome,
  login, tenantId e aviso de que o vínculo membro/time é fase futura
  quando `Authenticated`. `App.kt` injeta `corporateAuthRepository` e
  chama `restoreSession()` só quando `CONFIGURED`. `MainActivity` passa
  `MsalCorporateAuthRepository(this)`; `Main.kt` (wasmJs) não foi tocado
  — `supportsCorporateAuth` default `false` já cai no ramo Web. Validado:
  `testDebugUnitTest` (21 testes de auth, 0 falhas) e `assembleDebug`
  BUILD SUCCESSFUL (ambos UP-TO-DATE, confirmando que o diff já havia
  sido validado antes da queda).
- [x] Tarefa 5 (documentação/checklist): `docs/PENDENCIAS-EXTERNAS.md`
  seção 1 e `docs/setup/00-CHECKLIST-CONFIGURACAO-AMANHA.md` seção 1/8.3
  atualizados — status passa de "BLOQUEADO EXTERNAMENTE" (código
  inexistente) para "PRONTO PARA CONFIGURAÇÃO" (código implementado na
  FASE 14b-1, falta só o cadastro no Entra + `auth-config.json` local).
  Removidas as referências ao botão "Login"/mensagem antiga, atualizadas
  para "Entrar com conta corporativa"/"Autenticação corporativa ainda não
  configurada neste ambiente.". `auth-config.example.json` e
  `msal-config.example.json` (criados na Tarefa 3) já cobrem todos os
  placeholders pedidos (`tenant_id`, `client_id`, package, signature hash
  debug/release, redirect URIs, scopes) — nenhuma alteração necessária
  neles. Confirmado: nenhum valor real (client_secret, senha, token)
  entrado em nenhum arquivo versionado; `auth-config.json`/
  `msal-config.json` reais não existem localmente e seguem no
  `.gitignore`.
- [ ] Tarefa 6 (validação final): pendente.

## Commits de checkpoint (hash, ordem cronológica)

- checkpoint 1 (Tarefa 1): c9cc3de
- checkpoint 2 (Tarefa 2): 2c9b6eb
- checkpoint 3 (Tarefa 3): b2247e5
- checkpoint 4 (Tarefa 4): 89de5e9
- checkpoint 5 (Tarefa 5): 8028080
