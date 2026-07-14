# SPEC — Convergência Dashboard, Web/PWA e Android

**Objetivo:** concluir o Escala ICI rapidamente usando um contrato único entre Dashboard e KMP.

## 1. Projetos separados

### Escala ICI KMP

- Web/PWA;
- Android;
- futuro iOS;
- leitura de escala e plantão;
- cache;
- arquivo local;
- Firebase somente leitura no estágio atual.

### Dashboard React

- cadastro de times;
- cadastro de modelos;
- criação guiada;
- edição;
- validação;
- publicação;
- importação;
- aprovação futura de trocas.

### EscalaSOC Android antigo

- projeto legado;
- não misturar alterações;
- usar como referência de comportamento e schema;
- migrar depois sem bloquear o MVP KMP.

## 2. Contrato mínimo

Novos períodos:

```text
teamId
scheduleType
workPatternId
startDate
endDate
status
active
schemaVersion
updatedAt
publishedAt
```

Novos assignments:

```text
periodId
teamId
memberId
date
assignmentType
shiftDefinitionId
shiftType
status
schemaVersion
updatedAt
```

Plantão:

```text
periodId
teamId
memberId
startDate
startTime
endDate
endTime
status
schemaVersion
```

## 3. Ordem para terminar rápido

### Marco 1 — Publicar a leitura Firebase atual

- revisar os quatro commits KMP;
- push;
- GitHub Actions;
- Cloudflare;
- testar Web real;
- testar Android no Linux.

### Marco 2 — Schema v2 sem quebra

- DTOs aceitam campos opcionais;
- Dashboard grava `scheduleType` e `schemaVersion`;
- KMP mantém fallback para documentos antigos;
- sem migração destrutiva.

### Marco 3 — Wizard do Dashboard

- time;
- tipo;
- período;
- geração;
- preview;
- rascunho.

### Marco 4 — KMP suporta todos os tipos

- calendário genérico;
- turnos remotos;
- plantão;
- alertas por tipo;
- nenhuma regra hard-coded por time.

### Marco 5 — Autenticação e vínculo

- Firebase Auth Microsoft;
- `user_links`;
- carregar time pessoal;
- remover login demonstrativo da produção.

### Marco 6 — Trocas

- solicitação;
- aceite do colega;
- aprovação do responsável;
- transação final no Dashboard;
- notificações depois.

## 4. O que não bloqueia o MVP

Fica para depois:

- Cloud Function;
- App Check;
- claims avançadas;
- OneDrive/Graph;
- Push remoto;
- iOS;
- migração completa do app legado;
- algoritmo inteligente de distribuição;
- regras complexas por organização.

## 5. Teste mínimo por entrega

### Dashboard

```text
npm run check
npm run build
```

Além disso:

- criar cada tipo;
- salvar rascunho;
- preview;
- publicar em ambiente autorizado;
- reabrir período.

### KMP Web

```text
wasmJsTest
build-web.ps1
```

Validar:

- exatamente dois Wasm;
- Cloudflare;
- rotação;
- cache;
- fonte Firebase.

### KMP Android

```text
testDebugUnitTest
assembleDebug
```

Validar:

- instalar no emulador;
- ler Firebase;
- desligar rede e validar cache;
- atualização APK preservada.

## 6. Versionamento de schema

```text
schemaVersion = 1
```

Documentos legados.

```text
schemaVersion = 2
```

Documentos gerados pelo novo Dashboard.

Leitores:

- aceitam 1;
- preferem 2;
- rejeitam versão futura desconhecida;
- preservam cache válido.

## 7. Critério de conclusão do MVP

O MVP fica pronto quando:

1. administrador escolhe time e tipo;
2. Dashboard gera rascunho;
3. administrador edita e publica;
4. Web/PWA lê;
5. Android lê;
6. cache funciona offline;
7. plantão funciona separado;
8. arquivo local permanece como fallback;
9. histórico permanece;
10. nenhum fluxo principal depende de mock.
