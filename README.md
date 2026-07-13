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

| Funcionalidade | Web | Android | Fonte atual | Situação |
|---|---|---|---|---|
| Hoje | Sim | Existente | Dados locais/importados | FUNCIONAL |
| Escala | Sim | Existente | Dados locais/importados | FUNCIONAL |
| Alertas | Sim | Existente | Regras e dados locais | FUNCIONAL |
| Perfil | Sim | Existente | Estado local | FUNCIONAL |
| Plantão | Sim | Existente | Dados locais/importados | FUNCIONAL |
| Arquivo local XLS/XLSX | Sim | Existente | Seletor do dispositivo | FUNCIONAL |
| Dropbox | Código experimental | Existente | Configuração já existente | PARCIAL |
| Firebase | Não | Não validado | Nenhuma integração real | PENDENTE DE INTEGRAÇÃO |
| OneDrive | Não | Não | Nenhuma integração | PENDENTE DE INTEGRAÇÃO |
| Atualização do aplicativo | Não | Existente | Exclusiva do Android | NÃO DISPONÍVEL |
| Login corporativo no aplicativo | Não | Não validado | Nenhum MSAL real | PENDENTE DE INTEGRAÇÃO |
| Sincronização corporativa | Não | Não validado | Estado local | NÃO DISPONÍVEL |

Cloudflare Access protegerá externamente a entrada do site. Ele não autentica o usuário dentro do código do Escala ICI e não fornece automaticamente nome, e-mail ou token Microsoft ao aplicativo.

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
