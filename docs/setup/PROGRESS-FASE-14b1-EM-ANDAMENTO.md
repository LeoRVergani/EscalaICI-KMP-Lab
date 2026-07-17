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
- [ ] Tarefa 3 (adapter MSAL Android): pendente.
- [ ] Tarefa 4 (interface): pendente.
- [ ] Tarefa 5 (documentação/checklist): pendente.
- [ ] Tarefa 6 (validação final): pendente.

## Commits de checkpoint (hash, ordem cronológica)

- checkpoint 1 (Tarefa 1): c9cc3de
- checkpoint 2 (Tarefa 2): (preencher após este commit)
