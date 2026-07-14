# KMP-MVP-1B-SIMPLES — leitura Firestore Web e Android

## Escopo entregue

O Dashboard React continua sendo o único responsável por importar, editar e
publicar. O KMP Web/PWA e Android somente leem `teams`, `members`,
`schedule_periods`, `schedule_assignments`, `oncall_periods` e
`oncall_assignments`. Não foram criadas coleções, operações de escrita,
autenticação, listeners em tempo real ou integrações administrativas.

## Contrato e fluxo

Os DTOs neutros e `FirebaseScheduleGateway` vivem em `commonMain`, sem classes
Firebase. O gateway oferece apenas carga da equipe, períodos ativos, membros,
assignments e verificação de `updatedAt`. A implementação REST faz exclusivamente
requisições `GET` ao projeto público configurado.

`FirebaseScheduleSource` e `FirebaseOnCallSource` validam período, relações,
duplicidade, datas e membros antes de mapear o lote. Plantões preservam data e
hora completas, inclusive quando terminam no dia seguinte. Escala e plantão
falham e armazenam cache de forma independente.

## Cache e fallback

A Web usa `localStorage`; Android usa `SharedPreferences`. As chaves Firebase
são separadas das chaves de arquivo local e entre escala/plantão. O envelope
armazena payload válido, tipo `FIREBASE`, período, atualização remota,
sincronização local, última tentativa e schema local. Não armazena token, senha,
credencial ou configuração administrativa.

- remoto válido: `Success`;
- rede indisponível com cache válido: `OfflineCache`;
- rede indisponível sem cache: `RecoverableError`;
- documento inválido: cache anterior preservado;
- modo AUTO: Firebase, cache Firebase, arquivo, cache local, demonstração;
- modo LOCAL: arquivo local não é substituído silenciosamente.

## Auditoria de dados públicos

Em 14 de julho de 2026 foram auditados somente nomes de campos, sem copiar
valores pessoais. `members` continha identificação lógica, nome de exibição,
nome de escala, equipe, função/papel, padrão de trabalho, status e timestamps.
Não foram encontrados telefone, endereço, documento pessoal, token, credencial
ou dado médico. E-mail existe remotamente, mas não é usado pela UI.

Foram confirmados os tipos de assignment `WORK_SHIFT`, `OFF` e `VACATION`, e
os nomes de turno `Madrugada`, `Manhã`, `Tarde` e `Noite`.

## Validação

- `:composeApp:wasmJsTest`: aprovado, 83 testes no total;
- `scripts/build-web.ps1`: aprovado;
- `:composeApp:testDebugUnitTest`: aprovado;
- `:composeApp:assembleDebug`: aprovado com assinatura debug padrão para a
  validação local, sem mudar a configuração/keystore existente;
- `cloudflare-dist`: `index.html`, `composeApp.js`, service worker e exatamente
  dois arquivos Wasm;
- WEB-MOBILE-2: `100dvh` principal e `visualViewport` apenas fallback
  preservados.

Os testes novos usam gateway fake e cobrem período válido/ausente, assignment
inválido/duplicado, membro ausente, rede com/sem cache, `updatedAt`, cache
parcial, separação escala/plantão, modos LOCAL/AUTO e contrato sem escrita.
Nenhum teste unitário acessa produção.

## Risco e próximos passos

As regras publicadas permitem leitura e escrita anônimas até 4 de agosto de
2026. Esta leitura é temporária e não reduz o risco atual. Antes da expiração,
Dashboard, Android legado e KMP precisam de estratégia comum de identidade,
regras testadas no Emulator, implantação gradual e rollback. Não se deve
publicar `deny-all` sem confirmar o impacto no Dashboard.

Nesta fase não houve `firebase deploy`, alteração no Console, escrita
Firestore, mudança no Dashboard nem push Git automático.
