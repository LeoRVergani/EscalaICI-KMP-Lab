# Web, GitHub e Cloudflare Pages

Este documento descreve a publicação do Escala ICI Web/Wasm. O repositório Git deve ter como raiz somente a pasta que contém `settings.gradle.kts`, `gradlew` e `composeApp`.

## Ambiente e execução local

O ambiente validado usa Java 21, Gradle 9.4.1, Kotlin 2.2.10 e Compose Multiplatform 1.9.0. O target Web existente é somente `wasmJs`; portanto, o modo registrado é `WEB_WASM`.

O repositório ainda não contém `gradlew.bat`. No Windows, use o invocador que chama diretamente o JAR oficial já presente:

```powershell
.\scripts\invoke-gradle-wrapper.ps1 --version
.\scripts\invoke-gradle-wrapper.ps1 :composeApp:wasmJsTest
.\scripts\invoke-gradle-wrapper.ps1 :composeApp:wasmJsBrowserDevelopmentRun
```

O servidor validado usa:

```text
http://127.0.0.1:8080/
```

Para criar a distribuição estática:

```powershell
.\scripts\build-web.ps1
```

O script executa os testes Web, solicita a distribuição do Compose e copia a saída Wasm real de `composeApp/build/dist/wasmJs/productionExecutable/` para `cloudflare-dist/`. Também inclui `_headers` e `_redirects`.

Em redes com inspeção TLS, o Java local pode precisar usar temporariamente o repositório confiável do Windows. Essa configuração deve existir somente na sessão e ser restaurada ao final; não deve ser escrita no projeto, no perfil ou no Git.

Pendência de portabilidade `WEB-BUILD-0`: gerar oficialmente `gradlew.bat` em ambiente controlado usando exatamente a versão atual do Wrapper e revisar que `gradlew`, o JAR e `gradle-wrapper.properties` não foram atualizados acidentalmente.

## PWA e arquivos Cloudflare

O site inclui manifest, service worker e ícones. O nome público e o nome curto são `Escala ICI`; `start_url` e `scope` são relativos e compatíveis com Pages. O service worker usa cache versionado, remove caches anteriores na ativação e oferece fallback de navegação para `index.html`.

No navegador móvel, o host Web dimensiona `#webApp` e o canvas do Compose com `100dvh` como fonte principal. Navegadores antigos mantêm `100vh` e usam `visualViewport.height` (ou `innerHeight`) apenas como fallback, com nova medição após rotação; o evento `visualViewport.scroll` não altera a altura. `env(safe-area-inset-bottom)` reserva a área de gestos sem padding fixo. Assim, o navegador normal acompanha a área visual disponível, enquanto a PWA standalone continua ocupando toda a tela sem espaço inferior artificial. Valide em celular real com a barra de endereço visível e recolhida e repita a sequência retrato–paisagem–retrato.

`web/cloudflare/_redirects` contém o fallback SPA:

```text
/* /index.html 200
```

`web/cloudflare/_headers` aplica somente headers mínimos. Não há CSP, COOP ou COEP presumidos.

## Primeiro envio ao GitHub

Crie manualmente um repositório dedicado, preferencialmente `PRIVATE`. Não use a pasta do aplicativo Android antigo, dashboard ou uma pasta pai como raiz.

Antes do primeiro envio, confira:

```powershell
git remote -v
git branch --show-current
git status --short
```

Depois de criar o repositório vazio no GitHub, execute substituindo apenas os placeholders reais:

```powershell
git remote add origin <URL_DO_REPOSITORIO>
git push -u origin master
```

Nenhum remoto ou URL é criado automaticamente por esta preparação.

## GitHub Actions

O workflow `Web CI` roda em push e pull request para `master`, além de execução manual. Ele usa Ubuntu, Temurin 21, valida o Wrapper, executa `scripts/build-web.sh` e publica `cloudflare-dist` como artefato de inspeção.

O workflow `Publish Cloudflare branch` roda apenas em push para `master` ou manualmente. Ele possui `contents: write`, compila o site e substitui a branch órfã `cloudflare-pages`. Essa branch contém somente o site e um `BUILD_INFO.txt` com commit de origem, data UTC e modo `WEB_WASM`.

Depois do primeiro push:

1. Abra a aba GitHub Actions.
2. Confirme que `Web CI` passou.
3. Execute ou confirme a execução de `Publish Cloudflare branch`.
4. Verifique que `cloudflare-pages` contém somente arquivos estáticos.

Os workflows não usam keystore, credenciais, planilhas, Firebase, APK ou segredos de implantação.

## Configurar Cloudflare Pages

No painel Cloudflare:

```text
Workers & Pages
→ Create
→ Pages
→ Import an existing Git repository
```

Selecione o repositório dedicado e configure:

```text
Production branch: cloudflare-pages
Framework preset: None
Build command: exit 0
Build output directory: .
```

A branch já contém o site compilado, então o Cloudflare não precisa executar Java ou Gradle. Não configure variáveis secretas para esse fluxo. Use inicialmente o endereço `<nome-do-projeto>.pages.dev`; não presuma domínio institucional.

## Proteção de acesso com Microsoft Entra ID

Cloudflare Access e autenticação dentro do aplicativo são responsabilidades separadas:

```text
Cloudflare Access → controla quem pode abrir o site
MSAL/Graph futuro → autenticará e autorizará recursos dentro do aplicativo
```

Para proteger o site:

1. Configure Microsoft Entra ID como provedor de identidade no Cloudflare Zero Trust.
2. Crie uma aplicação Access do tipo self-hosted para o domínio Pages ou domínio personalizado.
3. Crie uma política `Allow` por e-mail corporativo, domínio ou grupo aprovado.
4. Teste separadamente um usuário permitido e um não permitido.
5. Mantenha o site bloqueado antes de divulgar o endereço.

Não reutilize cookie, JWT ou segredo do Cloudflare como token Microsoft Graph. Não invente tenant, client ID, client secret, grupo ou domínio.

Cloudflare Access protege a entrada do site, mas não autentica o usuário dentro do código do Escala ICI.

O aplicativo não receberá automaticamente nome, e-mail ou token Microsoft nesta fase. Isso exigirá uma fase futura com Cloudflare Access JWT/Worker ou MSAL no aplicativo, após decisão arquitetural e autorização.

## Situação atual das fontes

| Fonte | Estado Web | Observação |
|---|---|---|
| Arquivo local | FUNCIONAL | O usuário seleciona XLS/XLSX e confirma a importação local |
| Firebase | PENDENTE DE INTEGRAÇÃO | Não há Firebase real configurado |
| OneDrive/Graph | PENDENTE DE INTEGRAÇÃO | Não há MSAL ou Microsoft Graph implementado |
| Dropbox | PARCIAL | Há código experimental existente, mas não foi validado nem preparado para publicação nesta fase |

Não há sincronização corporativa automática. `FONTES-1`, OneDrive, MSAL, Graph, Firebase e novas integrações Dropbox ficam para fases posteriores.

## Checklist antes de disponibilizar

1. Confirmar que ambos os workflows passam no GitHub.
2. Inspecionar o artefato e a branch `cloudflare-pages`.
3. Validar o site em `.pages.dev`, incluindo atualização e cache do service worker.
4. Configurar e testar Cloudflare Access.
5. Confirmar que nenhum dado real, planilha ou credencial foi adicionado.
6. Manter o repositório privado até revisão de segurança e conteúdo.
