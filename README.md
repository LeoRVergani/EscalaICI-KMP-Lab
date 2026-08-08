# Escala ICI

Aplicativo multiplataforma para consulta de escalas e plantões.

O produto usa Compose Multiplatform e atualmente possui targets Android e Web/Wasm. Esta fase prepara exclusivamente a aplicação Web para validação no GitHub e hospedagem estática no Cloudflare Pages.

## Status dos targets

| Target | Estado | Observação |
|---|---|---|
| Web/Wasm | FUNCIONAL | Testes Web e distribuição estática validados com Java 21 |
| Android | EXISTENTE | Não é compilado, publicado ou alterado pela preparação Web |
| Web com fallback JS | NÃO DISPONÍVEL | O projeto não possui target JS; o modo publicado é `WEB_WASM` |

## Executar no Windows

O repositório ainda não possui `gradlew.bat`. O script abaixo chama diretamente o `gradle-wrapper.jar` já versionado, sem instalar ou atualizar o Gradle:

```powershell
.\scripts\invoke-gradle-wrapper.ps1 --version
.\scripts\invoke-gradle-wrapper.ps1 :composeApp:wasmJsTest
.\scripts\invoke-gradle-wrapper.ps1 :composeApp:wasmJsBrowserDevelopmentRun
```

O servidor de desenvolvimento fica em `http://127.0.0.1:8080/`.

Para testar e gerar a distribuição pronta para hospedagem:

```powershell
.\scripts\build-web.ps1
```

O resultado é copiado para `cloudflare-dist/`, que permanece fora do Git na branch principal. A saída real do Gradle fica em `composeApp/build/dist/wasmJs/productionExecutable/`.

No Linux e no GitHub Actions:

```sh
./scripts/build-web.sh
```

## Situação funcional

Desde a FASE 15 (`docs/spec/FASE-15-FIREBASE-UNIFICADO.md`), Hoje/Escala/Perfil
são alimentados pelo Firebase real do `Escala-ICI` (staging) quando o usuário
está logado — não mais por dados locais/mock por padrão.

| Funcionalidade | Web | Android | Fonte atual | Situação |
|---|---|---|---|---|
| Login (e-mail+senha real) | Sim | Existente | Identity Toolkit REST (Escala-ICI) | FUNCIONAL |
| Hoje | Sim | Existente | Firestore (`turnosMes` PUBLICADA da equipe real) | FUNCIONAL |
| Escala | Sim | Existente | Firestore (`turnosMes` PUBLICADA da equipe real) | FUNCIONAL |
| Alertas | Sim | Existente | Regras locais sobre os dados do Firestore | FUNCIONAL |
| Perfil | Sim | Existente | `usuarios/{login}` real | FUNCIONAL |
| Plantão | Sim | Existente | Sem contrato no backend novo — mostra aviso explícito | PENDENTE (fora do Escala-ICI) |
| Modo demonstração | Sim | Existente | Mock local, isolado, nunca fallback automático | FUNCIONAL |
| Arquivo local XLS/XLSX | Sim | Existente | Seletor do dispositivo (não alimenta Hoje/Escala se logado) | FUNCIONAL |
| Dropbox | Código experimental | Existente | Configuração já existente | PARCIAL |
| Firebase (Escala-ICI, staging) | Sim | Sim | Identity Toolkit + Firestore REST, autenticado | FUNCIONAL |
| OneDrive | Não | Não | Nenhuma integração | PENDENTE DE INTEGRAÇÃO |
| Atualização do aplicativo | Não | Existente | Exclusiva do Android | NÃO DISPONÍVEL |
| Login corporativo Microsoft/Entra | Não | Não | Fora do escopo da FASE 15 (`AuthRepository` já preparado) | PENDENTE DE INTEGRAÇÃO |
| Sincronização corporativa | Sim | Sim | Cache offline v2 por login + refresh automático | FUNCIONAL |

Cloudflare Access protegerá externamente a entrada do site. Ele não autentica o usuário dentro do código do Escala ICI e não fornece automaticamente nome, e-mail ou token Microsoft ao aplicativo.

## FASE 14a — Specs de autenticação, sincronização, pausa e migração

A tabela acima ("Login corporativo", "Sincronização corporativa") continua
refletindo o estado real: nenhuma dessas linhas mudou nesta fase, que é
exclusivamente documentação e auditoria. As cinco specs que fecham o desenho
necessário para implementar essas linhas ficam em `docs/spec/`:
`46-ESCALAICI-AUTENTICACAO-CORPORATIVA-MSAL-FIREBASE.md`,
`47-ESCALAICI-VINCULO-USUARIO-MEMBRO-E-TIME.md`,
`48-ESCALAICI-SINCRONIZACAO-ESCALA-CACHE-OFFLINE.md`,
`49-ESCALAICI-PAUSA-15-MINUTOS-E-NOTIFICACOES.md` e
`50-ESCALAICI-MIGRACAO-FINAL-E-PARIDADE.md`. Índice completo em
`docs/SPECS-ESCALAICI.md`, roadmap de fases em `docs/ROADMAP.md` (FASE
14a-14h).

**Risco de calendário conhecido**: as Firestore Rules hoje publicadas em
produção liberam leitura e escrita totalmente livres até **4 de agosto de
2026** (`firebase/firestore.production.snapshot.rules`). Nenhuma regra
autenticada pode ser publicada com segurança antes de existir uma sessão
Firebase Auth real nos clientes que hoje leem sem token (Android legado e
KMP) — ver `docs/ADR-FIREBASE-AUTH.md` e a spec 46.

## FASE 14a.1 — Endurecimento emergencial das regras do Firestore

Correção emergencial, entre a FASE 14a e a FASE 14d: `firebase/firestore.rules`
foi endurecida para **eliminar toda escrita anônima**, testada com 22 casos no
Firestore Emulator (`cd firebase && npm run test:rules`). A leitura anônima
mínima que o KMP já usa hoje (`teams`, `members`, `schedule_periods`,
`schedule_assignments`, `oncall_periods`, `oncall_assignments`) foi mantida no
nível atual — não ampliada — porque a listagem sem filtro que o app faz hoje
não pode ser restringida por regra sem quebrar (ver
`docs/spec/51-ESCALAICI-FIRESTORE-HARDENING-TRANSITORIO.md` para os riscos
residuais documentados e a troca explicitamente aprovada). **Esta regra não
foi implantada em produção** — o comando de deploy e o checklist de validação
ficam registrados na spec 51 para execução humana.

## Estrutura resumida

```text
composeApp/src/commonMain/   UI, modelos e regras compartilhadas
composeApp/src/wasmJsMain/   entrada Web e recursos PWA
composeApp/src/androidMain/  implementação Android existente
scripts/                     execução do wrapper e build Web
web/cloudflare/              headers e fallback de rotas
.github/workflows/           validação e publicação da branch estática
```

## GitHub e Cloudflare

O workflow `Web CI` testa e gera um artefato inspecionável. O workflow `Publish Cloudflare branch` substitui a branch órfã `cloudflare-pages` apenas pelos arquivos estáticos compilados.

Consulte [docs/WEB-GITHUB-CLOUDFLARE.md](docs/WEB-GITHUB-CLOUDFLARE.md) para o primeiro envio ao GitHub, configuração do Cloudflare Pages e proteção com Cloudflare Access.

O repositório deve começar como privado. Nenhuma licença open source é concedida automaticamente.
