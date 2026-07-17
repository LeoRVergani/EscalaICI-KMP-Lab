# Checklist de configuração humana — EscalaICI-KMP-Lab

Gerado durante o sprint automático noturno de 2026-07-15, em resposta ao
ADENDO "deixar o projeto pronto para configuração humana". Este documento
é um **índice**, não substitui os specs já existentes — ele aponta para
onde cada decisão/ação externa está documentada em detalhe e resume o que
falta, em ordem de urgência real (não de dificuldade).

Convenção de status usada aqui e em todo relatório deste sprint:
`PRONTO PARA TESTE` (código existe, falta só o passo humano) ·
`PRONTO PARA CONFIGURAÇÃO` (código aceita a config, falta cadastrá-la) ·
`BLOQUEADO EXTERNAMENTE` (depende de decisão/cadastro fora deste repo) ·
`PARCIAL` · `NÃO INICIADO`. Nunca "concluído"/"funcionando" para algo que
não foi testado de ponta a ponta com a configuração real.

---

## 0.🔴 URGENTE — prazo real, não é só organização

### 0.1 Regras do Firestore do projeto `escalaici` expiram em **2026-08-04**

**Status: BLOQUEADO EXTERNAMENTE — prazo em ~3 semanas a partir de hoje (2026-07-14).**

As regras publicadas hoje no projeto `escalaici` (banco `(default)`) são
regras de modo de teste: liberam toda leitura e escrita até 4 de agosto de
2026, independente de autenticação. Depois dessa data, se nada mudar, o
Firestore passa a **negar tudo** por padrão — o que quebraria a leitura do
KMP (Android e Web) e, dependendo de como o Firebase aplicar o corte, pode
afetar o Dashboard também caso as regras publicadas não tenham sido
substituídas por regras definitivas antes disso.

Detalhe completo, decisão provisória e alternativas avaliadas:
[`docs/ADR-FIREBASE-AUTH.md`](../ADR-FIREBASE-AUTH.md).

**O que Leonardo precisa decidir/fazer, antes de 2026-08-04:**
- [ ] Decidir a política de acesso definitiva (ver alternativas na seção
      "Alternativas" do ADR) — a mais indicada ali é "migrar clientes para
      Firebase Auth e então endurecer regras", mas isso depende do item 0.2
      (Custom Token) e do item 0.3 (`user_links`) abaixo, nenhum dos dois
      pronto ainda.
- [ ] Se não houver tempo para a migração completa antes do prazo, definir
      uma prorrogação manual do modo de teste no Console do Firebase
      (Firestore → Regras → editar a data de expiração) como paliativo, e
      registrar essa decisão nesta seção com a nova data.
- [ ] Testar qualquer regra nova primeiro no Emulator (project ID
      `demo-escalaici-kmp`, já é a prática adotada — ver ADR) antes de
      publicar em produção.

Este item não pode ser resolvido por nenhuma sessão de IA: é uma decisão
de política de segurança que só o dono do projeto Firebase pode tomar.

---

## 1. Login corporativo Microsoft (MSAL) no Android do Escala ICI

**Status: BLOQUEADO EXTERNAMENTE.** Passo a passo completo, com os valores
exatos de `client_id`/`tenant_id`/`package name`/signature hash e a
redirect URI pronta para colar:
[`docs/PENDENCIAS-EXTERNAS.md`](../PENDENCIAS-EXTERNAS.md), seção 1.

Resumo: falta cadastrar a plataforma Android do **Escala ICI**
(applicationId `br.com.leorvergani.escalaici`, assinatura própria) no App
Registration do Entra ID que já existe para o app oficial — sem isso, o
login Microsoft real no Android nunca vai funcionar neste app, mesmo depois
de a FASE 11.3 (login MSAL real) ser implementada em código. O hash da
redirect URI MSAL precisa ser recalculado porque o `applicationId` mudou.

Ver também [`01-IDENTIDADE-OFICIAL-DO-APP.md`](01-IDENTIDADE-OFICIAL-DO-APP.md)
nesta mesma pasta — a raiz do porquê disso ser necessário é a identidade
Android própria do Escala ICI, que exige cadastro externo próprio.

## 2. Firebase Auth / Custom Token para o KMP

**Status: NÃO INICIADO (decisão de arquitetura pendente).**

O KMP não tem hoje nenhuma sessão Firebase Auth própria — só leitura
anônima (ver ADR-FIREBASE-AUTH.md, decisão provisória item 1). Para ter
identidade real equivalente ao Dashboard, o caminho é: login Microsoft
(MSAL, item 1 acima) → trocar o token Microsoft por um Firebase Custom
Token via Cloud Function (nunca gerado no cliente) → autenticar no
Firebase com esse token. Isso exige:
- [ ] Plano Firebase **Blaze** ativo no projeto `escalaici` (Cloud
      Functions não roda no plano Spark gratuito).
- [ ] Implementar e implantar a Cloud Function de troca de token (ainda
      não existe neste projeto).
- [ ] Ver spec de referência:
      [`docs/spec/46-ESCALAICI-AUTENTICACAO-CORPORATIVA-MSAL-FIREBASE.md`](../spec/46-ESCALAICI-AUTENTICACAO-CORPORATIVA-MSAL-FIREBASE.md).

## 3. Coleção `user_links` (vínculo usuário ↔ membro/time)

**Status: NÃO INICIADO.** Depende do item 2 (precisa de uma identidade
Firebase real para vincular). Spec de referência:
[`docs/spec/47-ESCALAICI-VINCULO-USUARIO-MEMBRO-E-TIME.md`](../spec/47-ESCALAICI-VINCULO-USUARIO-MEMBRO-E-TIME.md).

## 4. OneDrive (importação de escala) para o KMP

**Status: NÃO INICIADO — nunca configurado para este app.** O EscalaSOC
legado usa OneDrive real (10 estratégias de busca), mas o KMP nunca teve
essa integração cadastrada. Precisa de: registro de app com escopo Graph
`Files.Read`, consentimento admin ou por usuário, e código de import ainda
não escrito no KMP. Sem estimativa de esforço de código até haver decisão
de que o KMP realmente vai importar de OneDrive (hoje o KMP só lê
Firestore/Dropbox).

## 5. Dropbox (leitura de escala) — Android e Web

**Status: PRONTO — nenhuma ação pendente conhecida.** Confirmado pelo
usuário em 2026-07-11 (nota do topo de
[`PENDENCIAS-EXTERNAS.md`](../PENDENCIAS-EXTERNAS.md)): escopo
`sharing.read` e redirect URI já cadastrados no App Console do Dropbox
(`DropboxAuthConfig.kt`), com download real da escala funcionando tanto no
Android quanto na Web via OAuth. Não confundir com o item 4 (OneDrive,
nunca configurado) nem com `docs/FONTES-UNIVERSAIS.md`, cujo texto sobre
Dropbox/OneDrive serem "apenas identificadores arquiteturais" está
desatualizado em relação a este item — precisa de uma revisão de conteúdo
nesse arquivo (não faz parte deste sprint).

## 6. Publicar atualização do APK (`EscalaICI-latest.apk` + `version.json`)

**Status: PRONTO PARA TESTE — só falta o upload manual.** Os arquivos já
estão gerados localmente (`/home/lvergani/Downloads/dropbox_update_scripts/`).
Passo a passo exato: [`docs/PENDENCIAS-EXTERNAS.md`](../PENDENCIAS-EXTERNAS.md),
seção 2. **Nenhuma sessão de IA deve tentar automatizar esse upload** — é
regra explícita já registrada naquele arquivo.

## 7. Notificações e alarmes Android (pausa de 15 minutos)

**Status: NÃO INICIADO em código** (nem `AlarmManager`, nem
`NotificationManager`, nem `WorkManager` existem hoje em nenhum lugar do
Android do KMP — confirmado por auditoria de código nesta mesma sessão).
Não depende de nenhuma configuração externa para começar a ser
implementado (é só código Android nativo), mas **validar de ponta a ponta
exige emulador ou aparelho físico** — nunca declarar "notificação validada"
sem isso ter sido de fato disparado e observado. Spec de referência:
[`docs/spec/49-ESCALAICI-PAUSA-15-MINUTOS-E-NOTIFICACOES.md`](../spec/49-ESCALAICI-PAUSA-15-MINUTOS-E-NOTIFICACOES.md).

## 8. FASE 14b-0 — checklist de configuração pós-congelamento de identidade

**Status: PRONTO PARA CONFIGURAÇÃO.** O código já está congelado com o
`applicationId`/`namespace`/packages oficiais
(`br.com.leorvergani.escalaici`, sem `.kmp.lab`) — ver
[`docs/spec/52-ESCALAICI-IDENTIDADE-OFICIAL-ASSINATURA-E-RELEASE.md`](../spec/52-ESCALAICI-IDENTIDADE-OFICIAL-ASSINATURA-E-RELEASE.md).
Os passos abaixo são só configuração externa/humana, em ordem.

### 8.1 Confirmar o `applicationId`
- **Onde obter**: `composeApp/build.gradle.kts` (`android.namespace` e
  `android.defaultConfig.applicationId`).
- **Onde inserir**: nada a inserir — só confirmar visualmente que ambos
  dizem `br.com.leorvergani.escalaici` (sem `.kmp.lab`).
- **Público ou secreto**: público (aparece em qualquer APK instalado).
- **Como validar**: `grep -n "applicationId\|namespace" composeApp/build.gradle.kts`.
- **Erro comum**: confundir com o `applicationId` do EscalaSOC
  (`br.com.leorvergani.escalasoc`) — são apps diferentes, não reaproveitar.

### 8.2 Gerar os hashes de assinatura (debug e release)
- **Onde obter**: rode `./scripts/print-signing-info.sh` (debug, usa
  `~/.android/debug.keystore` automaticamente) e de novo com
  `KEYSTORE_PATH`/`KEYSTORE_ALIAS`/`KEYSTORE_PASSWORD` apontando para
  `escalaici-kmp-lab.jks` (release) — valores reais em `keystore.properties`
  (fora do Git). Ver seção 5.2 do spec 52 para os comandos exatos.
- **Onde inserir**: nos placeholders `<ANDROID_SIGNATURE_HASH_DEBUG>` /
  `<ANDROID_SIGNATURE_HASH_RELEASE>` de `auth-config.example.json` (ao
  copiá-lo para `auth-config.json`) e no cadastro do Entra (passo 8.3).
- **Público ou secreto**: o hash em si é público (é o que a Microsoft
  recebe); a senha usada para gerá-lo é secreta — nunca aparece na saída
  do script.
- **Como validar**: o script imprime "Nenhuma senha foi impressa. Nenhum
  arquivo foi criado ou modificado." ao final, sem erro.
- **Erro comum**: usar o hash do `debug.keystore` para testar o APK de
  release (ou vice-versa) — são keystores diferentes, hashes diferentes,
  a Microsoft recusa o redirect URI errado.

### 8.3 Cadastrar plataforma Android no Entra
- **Onde obter**: [entra.microsoft.com](https://entra.microsoft.com) →
  Identity → App registrations → app existente (`client_id
  e5b5154d-e65e-4605-b221-73d7ee570580`) → Authentication.
- **Onde inserir**: **Add a platform → Android** → package name
  `br.com.leorvergani.escalaici` → signature hash (um cadastro para o
  hash de debug, outro para o de release — dois valores de
  `redirect_uri`, ver seção 5.1 do spec 52).
- **Público ou secreto**: público (cadastro visível só a quem tem acesso
  ao App Registration, mas os valores em si — package/hash — não são
  segredo).
- **Como validar**: depois da FASE 14b (login MSAL implementado), abrir o
  app e tentar o login real — se o hash/package baterem, o browser/broker
  retorna ao app; se não, a Microsoft mostra erro de "redirect URI not
  registered".
- **Erro comum**: cadastrar só um hash (esquecer debug **ou** release) —
  o app funciona num ambiente e falha silenciosamente no outro.

### 8.4 Cadastrar SPA Web no Entra
- **Onde obter**: mesmo App Registration → Authentication → **Add a
  platform → Single-page application**.
- **Onde inserir**: `redirect_uri_local` (`http://localhost:8080/`, para
  teste local) e `redirect_uri_production` (domínio Cloudflare Pages do
  Escala ICI Web — a definir na FASE 14b, ver
  [`docs/WEB-GITHUB-CLOUDFLARE.md`](../WEB-GITHUB-CLOUDFLARE.md)).
- **Público ou secreto**: público.
- **Como validar**: nenhuma validação possível antes do MSAL Web estar
  implementado (FASE 14b) — por ora, só confirmar que a URL cadastrada
  bate exatamente com a URL do deploy (protocolo, domínio, barra final).
- **Erro comum**: cadastrar `http://localhost:8080` sem a barra final
  quando o app gera `http://localhost:8080/` (ou vice-versa) — Entra trata
  como URIs diferentes.

### 8.5 Inserir `tenant_id`
- **Onde obter**: já conhecido —
  `d2d23346-e737-4cac-96ec-fb25e7889f01` (mesmo tenant do EscalaSOC, ver
  `docs/PENDENCIAS-EXTERNAS.md`).
- **Onde inserir**: `auth-config.json` (copiado de
  `auth-config.example.json`) e/ou `msal-config.json` (copiado de
  `msal-config.example.json`), campo `tenant_id`.
- **Público ou secreto**: público (identifica a organização, não concede
  acesso sozinho).
- **Como validar**: confirmar que bate com o valor mostrado em Entra →
  Overview → Tenant ID do mesmo App Registration.
- **Erro comum**: copiar o `tenant_id` de outro projeto/tenant por engano
  — o login falha com erro de tenant não autorizado.

### 8.6 Inserir `client_id`
- **Onde obter**: já conhecido —
  `e5b5154d-e65e-4605-b221-73d7ee570580` (mesmo App Registration do
  EscalaSOC — reaproveitado, não criar um novo).
- **Onde inserir**: mesmos arquivos do passo 8.5, campo `client_id`.
- **Público ou secreto**: público (é o "Application (client) ID", não um
  client secret — este fluxo é público/PKCE, sem client secret).
- **Como validar**: bate com Entra → Overview → Application (client) ID.
- **Erro comum**: confundir `client_id` com `object_id` (também um GUID,
  mas serve para outra coisa no Entra).

### 8.7 Inserir redirect URIs
- **Onde obter**: resultado dos passos 8.2 (hashes) + 8.1 (package) —
  monta-se como `msauth://<package>/<hash>` (Android) ou a URL do deploy
  (Web).
- **Onde inserir**: `auth-config.json`, campos `android.redirect_uri_debug`
  / `android.redirect_uri_release` / `web.redirect_uri_production`; e no
  próprio cadastro do Entra (passos 8.3/8.4).
- **Público ou secreto**: público.
- **Como validar**: o valor cadastrado no Entra deve ser
  **caractere-a-caractere** igual ao gerado pelo app/script — copie e
  cole, não digite manualmente.
- **Erro comum**: usar `%3D` em vez de `=` (ou vice-versa) inconsistente
  entre o que o portal do Entra espera e o que o app envia — depende do
  campo do portal (alguns aceitam texto pronto, outros geram sozinhos a
  partir do hash puro).

### 8.8 Configurar Firebase (depois, fora desta fase)
- **Onde obter**: [console.firebase.google.com](https://console.firebase.google.com)
  → projeto `escalaici`.
- **Onde inserir**: não aplicável nesta fase — o app usa
  `FirestoreRestGateway` (REST puro), sem `google-services.json`. Só será
  necessário se/quando a FASE 14d (Firebase Custom Token) for implementada.
- **Público ou secreto**: n/a nesta fase.
- **Como validar**: n/a nesta fase.
- **Erro comum**: criar um `google-services.json` "por precaução" agora —
  não fazer isso, o app não usa SDK nativo do Firebase e o arquivo ficaria
  órfão (e o enunciado desta fase proíbe criar esse arquivo falso).

### 8.9 Configurar assinatura de release
- **Onde obter**: `keystore.properties.example` (copiar para
  `keystore.properties`, já ignorado pelo Git) — decisão de manter a
  keystore física existente (`escalaici-kmp-lab.jks`) documentada na
  seção 5 do spec 52.
- **Onde inserir**: `keystore.properties` na raiz do projeto (fora do
  Git), com `storeFile`/`storePassword`/`keyAlias`/`keyPassword` reais.
- **Público ou secreto**: **secreto** (senha da keystore) — nunca
  versionar, nunca colar em chat/log.
- **Como validar**: rodar `./gradlew :composeApp:assembleRelease` (sem
  imprimir a senha no terminal) e confirmar que o APK gerado está
  assinado: `apksigner verify --print-certs app-release.apk` (ou
  `jarsigner -verify`).
- **Erro comum**: `keystore.properties` com caminho relativo que só
  funciona a partir de um diretório específico — use caminho absoluto.

### 8.10 Gerar APK
- **Onde obter**: `./gradlew :composeApp:assembleDebug` (teste) ou
  `:composeApp:assembleRelease` (assinado, requer 8.9 configurado).
- **Onde inserir**: o artefato sai em
  `composeApp/build/outputs/apk/{debug,release}/`.
- **Público ou secreto**: n/a (artefato binário, não é segredo, mas não
  deve ir para o Git — ver `.gitignore`, `build/` já ignorado).
- **Como validar**: `ls -la composeApp/build/outputs/apk/*/`.
- **Erro comum**: rodar `assembleRelease` sem `keystore.properties`
  configurado — o build usa `signingConfig` só condicionalmente (ver
  `composeApp/build.gradle.kts`), então o APK sai sem assinatura de
  release se o arquivo não existir (não falha o build, mas o APK não
  instala como atualização válida em cima de um anterior assinado).

### 8.11 Publicar manualmente no Dropbox
- **Onde obter**: copiar o APK gerado (8.10) para
  `/home/lvergani/Downloads/dropbox_update_scripts/EscalaICI-latest.apk` e
  atualizar `version.json` nessa mesma pasta (usar
  `version.json.example` deste repo como referência de formato, campos
  `kmp*`).
- **Onde inserir**: upload manual pelo site/app do Dropbox, sobrescrevendo
  o conteúdo do link já cadastrado (nunca apagar e recriar o arquivo — o
  link muda). Ver `docs/PENDENCIAS-EXTERNAS.md`, seção 2.
- **Público ou secreto**: o link do Dropbox funciona como acesso público
  de leitura (quem tem o link baixa) — não é secreto, mas não deve ser
  divulgado fora da equipe.
- **Como validar**: baixar o link em uma aba anônima do navegador e
  conferir se o APK baixado bate em tamanho/data com o gerado localmente.
- **Erro comum**: subir o APK mas esquecer o `version.json` (ou
  vice-versa) — o app compara `kmpVersionCode` do `version.json` com a
  versão instalada; se só um dos dois for atualizado, ou nada muda ou o
  app tenta baixar uma versão que ainda não está no link certo.

### 8.12 Validar atualização
- **Onde obter**: aparelho com uma versão anterior instalada (ou instalar
  a v0.7.0 manualmente uma vez primeiro, ver nota de mudança de
  `applicationId` em `docs/PENDENCIAS-EXTERNAS.md`, seção 2).
- **Onde inserir**: n/a — é teste, não configuração.
- **Público ou secreto**: n/a.
- **Como validar**: abrir o app → Perfil → "Atualizar aplicativo" → deve
  detectar a nova versão, baixar em streaming (sem travar por OOM) e abrir
  o instalador do Android.
- **Erro comum**: testar num aparelho que ainda tem a versão
  `...kmp.lab` instalada — o Android não atualiza por cima de um
  `applicationId` diferente; é preciso desinstalar a versão antiga antes.

---

## O que este checklist NÃO cobre ainda

O adendo original pediu um pacote de 17 documentos numerados em
`docs/setup/`. Neste sprint só foram produzidos os dois mais urgentes/
estruturais: este índice e `01-IDENTIDADE-OFICIAL-DO-APP.md`. Os demais
15 (ex.: guias passo a passo dedicados por integração, com `.example` de
cada arquivo de configuração, scripts de diagnóstico por integração,
painel de diagnóstico na UI) **não foram escritos** — ficam como trabalho
pendente, não como "concluído resumidamente". O conteúdo técnico de cada
um já existe espalhado nos specs 46/47/48/49/50/51 e nos arquivos citados
acima; falta consolidar em formato de guia passo a passo dedicado.

## Como manter isto atualizado

Sempre que um item acima for resolvido, mova a entrada para
`docs/ROADMAP.md` como `DONE` (mesma convenção já usada por
`PENDENCIAS-EXTERNAS.md`) e remova ou marque como resolvida aqui.
