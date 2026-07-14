# Fontes universais — KMP-MVP-1A / 1B-SIMPLES

Esta fase define contratos multiplataforma para escolher e carregar dados sem conectar rede. Todo o código vive em `commonMain` e não depende de JavaScript, Android, Firebase ou APIs JVM.

## Tipos e metadados

`ScheduleSourceType` consolida `LOCAL_FILE`, `LOCAL_CACHE`, `FIREBASE`, `ONEDRIVE`, `DROPBOX` e `DEMO`. `FIREBASE` possui adaptador somente leitura desde a KMP-MVP-1B-SIMPLES; OneDrive e Dropbox continuam apenas como identificadores arquiteturais para dados de escala.

`SourceMetadata` registra tipo, arquivo opcional, período, atualização da origem, sincronização local, conectividade, uso de cache, versão remota e mensagem curta. Tokens, URLs secretas, credenciais e conteúdo de planilha não fazem parte do modelo.

`DataLoadResult<T>` diferencia `Success`, `Empty`, `OfflineCache`, `RecoverableError` e `FatalError`. Avisos, instante de carregamento e possibilidade de nova tentativa acompanham o resultado; exceção não é usada como estado normal de UI.

## Contratos e separação de domínio

`ScheduleSource` trabalha com `ScheduleSourceData`/`ScheduleSummary`. `OnCallSource` trabalha separadamente com `OnCallSourceData`, `OnCallPeriod` e `OnCallAssignment`. Cada contrato pode carregar o ativo ou um período, verificar atualização, atualizar seu cache e invalidar somente seu próprio domínio.

Os casos de uso `LoadActiveSchedule`, `LoadActiveOnCall`, `RefreshSchedule`, `RefreshOnCall` e `ResolvePreferredSource` evitam que telas implementem regras de prioridade. Um resultado é escolhido por inteiro; assignments de fontes distintas nunca são combinados silenciosamente.

## Prioridade e cache

A política inicial é:

1. fonte remota válida (`FIREBASE`, `ONEDRIVE` ou `DROPBOX`);
2. cache válido dessa fonte remota;
3. arquivo local confirmado;
4. cache local do arquivo;
5. demonstração.

A ordem das fontes remotas é configurável. Dados demonstrativos nunca substituem cache real. Importação inválida não substitui o último arquivo confirmado.

`LocalScheduleCacheSource` e `LocalOnCallCacheSource` adaptam o `LocalDataCache` v1 existente sem migração. As chaves, parsers e serialização atuais foram preservados. Invalidar escala não apaga plantão e vice-versa.

## Conexão Firebase e limitações

`FirebaseScheduleSource` e `FirebaseOnCallSource` usam um gateway REST que só
expõe leitura. Há cache Firebase próprio, separado do cache de arquivo, com
payload validado, período, `updatedAt`, `cachedAt`, última tentativa e schema
local. Falha de rede retorna `OfflineCache` quando possível; lote remoto
inválido não substitui o último cache válido.

Em AUTO, Firebase válido precede cache Firebase, arquivo local, cache local e
demonstração. Em LOCAL, o arquivo selecionado permanece ativo. A UI mostra
apenas fonte, período, sincronização, disponibilidade offline, falha e nova
tentativa, sem IDs técnicos. Não há escrita, listener em tempo real, Firebase
Auth, OneDrive/Graph, edição, trocas nem migração destrutiva de cache.
