# Spec 70 — Cache local de identidade para entrada instantânea

**Status:** proposta (não implementada) — depende de decisão do usuário antes de qualquer código.
**Escopo previsto:** `EscalaICI-KMP-Lab` (Android + Web/Wasm), `commonMain` + `androidMain` +
`wasmJsMain`

## 1. Pedido original

> "Dá também pra salvar no localstorage que a pessoa já estava logada pra não ficar tentando
> verificar sempre, assim ao clicar no app é instantâneo a entrada."

## 2. Situação hoje

Ao abrir o app, o fluxo de entrada (`App.kt`, `LaunchedEffect(corporateAuthState,
requestedEntryContext)`) sempre passa por: restaurar sessão MSAL (`restoreSession()`, chamada de
rede/token silencioso) → resolver identidade organizacional (`DefaultOrganizationIdentityResolver`,
leitura Firestore) → só então renderizar Hoje. Mesmo com sessão MSAL válida em cache local (o MSAL
já faz isso), o usuário vê uma tela de carregamento a cada abertura, porque a resolução de
identidade/publicação é sempre buscada do zero.

## 3. Proposta: cache "stale-while-revalidate" da última identidade resolvida

Ideia central: guardar localmente (Android: `DataStore`/arquivo já usado por
`localDataCache`/`NotificationSettingsStore`; Web: `localStorage`) o **último resultado de
resolução de identidade bem-sucedido** — não credenciais, não token, só o resultado já público na
tela (nome, `teamId`, `workspaceId`, `memberId`, `publicationRevision`, resumo da semana atual).

Fluxo proposto:

1. Ao abrir o app, se existir um cache local de identidade **e** a sessão MSAL local indicar que
   havia uma conta ativa (`ISingleAccountPublicClientApplication` já teria isso, sem round-trip de
   rede), renderiza Hoje IMEDIATAMENTE com os dados cacheados (mesmo padrão de "otimista" já usado
   pelo cache de publicação hoje).
2. Em paralelo, dispara a resolução real (MSAL silent + Firestore) como já acontece.
3. Quando a resolução real chega: se bater com o cache (mesma revisão/mesmos dados), nada muda na
   tela — sem "piscar". Se divergir (nova publicação, membro alterado, revisão diferente),
   atualiza a tela normalmente, do jeito que já acontece hoje quando a Home já está visível e o
   Firebase muda algo em segundo plano — comportamento já existente para o caso "app aberto e a
   publicação muda".
4. Se a resolução real **falhar** (ex.: sem rede) mas houver cache, mantém o cache visível com um
   indicador discreto de "dados podem estar desatualizados" (mesmo padrão de estado offline já
   usado em `DemoDataOrigin`).
5. Se o MSAL indicar que NÃO há conta ativa (logout externo, sessão expirada), o cache é descartado
   e a tela de login aparece normalmente — nunca mostra dados de sessão anterior sem sessão válida.

## 4. O que NÃO muda

- Nenhum dado sensível (token, e-mail completo, senha) é gravado — mesmo princípio de log seguro já
  aplicado em `ResolutionDiagnostics` nesta sessão.
- A autenticação em si continua 100% via MSAL; isso é só cache do RESULTADO de apresentação, nunca
  um substituto de login.
- Nenhuma ação sensível (troca, aprovação, escrita) usa o cache — sempre a resolução mais recente.

## 5. Esforço e risco

- Reaproveita infraestrutura já existente (`expect/actual` de armazenamento local, já usado por
  `localDataCache`); não é uma peça nova de arquitetura.
- Principal risco: código teria dois caminhos de exibição (cache vs. real) — motivo para escrever
  testes específicos de "cache diverge da resolução real" e "sem conta ativa descarta cache" antes
  de considerar pronto, seguindo o mesmo padrão de testes puros já usado em `AdminViewAsMember.kt`/
  `ResolvedScheduleSummaryDecision.kt`.
- Estimativa: comparável em tamanho ao Checkpoint I desta sessão (1 dia de trabalho focado,
  incluindo testes e validação manual de "abrir com internet", "abrir sem internet com cache",
  "abrir sem internet sem cache", "abrir após logout externo").

## 6. Ação recomendada

Aguardando confirmação do usuário para virar uma FASE numerada (ex.: FASE 14K ou próxima
disponível) antes de qualquer implementação.
