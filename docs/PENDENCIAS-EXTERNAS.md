# Pendências externas — ações que só o dono das contas pode fazer

Este arquivo lista, em ordem de prioridade, toda ação fora do código que
falta para as integrações reais deste projeto funcionarem de ponta a
ponta. Nenhuma IA/sessão consegue fazer isso sozinha — são cadastros em
consoles de terceiros (Dropbox, Microsoft Azure) que exigem login com a
conta dona do app. Depois de cada ação, marque o checkbox e anote a data.

---

## 1. Dropbox — liberar o download real da escala na Web (FASE 11.1b)

**Por quê:** a Web não consegue baixar o link direto do Dropbox (bloqueio
de CORS do navegador, não é um bug daqui). O código já implementa OAuth
PKCE contra a API oficial do Dropbox como alternativa, mas o app cadastrado
no Dropbox ainda não tem permissão nem o endereço de retorno necessários.

**Onde:** [dropbox.com/developers/apps](https://www.dropbox.com/developers/apps)
→ faça login com a conta Dropbox dona do app → clique no app cujo **App
key** é `5by0pkzt2bgx95g` (é o mesmo App Key já usado pelo fluxo de admin
do app Android oficial, para publicar a escala).

**Passo a passo:**

- [ ] **Aba "Permissions"** (topo da página do app): marque a caixa do
      escopo **`sharing.read`** na lista de permissões e clique em
      **Submit** no fim da página. (O app hoje só tem `files.content.read`
      e `files.content.write` habilitados — usados pelo fluxo de admin,
      que é outra funcionalidade.)
- [ ] **Aba "Settings"**, seção **"OAuth 2"** → campo **"Redirect URIs"**:
      adicione a URI abaixo e clique em **Add**:
      ```
      http://localhost:8080/dropbox-callback.html
      ```
      (é a porta padrão do servidor de desenvolvimento Web deste projeto,
      `wasmJsBrowserDevelopmentRun` — se você rodar em outra porta, use
      essa porta na URI).
- [ ] Quando o PWA deste projeto tiver um endereço de produção público
      (hoje não tem — só roda local), volte aqui e adicione também
      `https://<seu-dominio-de-producao>/dropbox-callback.html` na mesma
      lista de Redirect URIs.

**Depois de feito:** rode `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`,
abra `http://localhost:8080/`, entre com o "Login de teste", vá em
Importar → "Procurar escalas (Dropbox)" e complete o login/consentimento
real do Dropbox que vai abrir num popup. Se funcionar, os dados reais da
escala aparecem na tela — me avise o resultado (ou o erro exato, se
houver) para eu ajustar o que for preciso.

**Nada a fazer no Android** — ele já baixa a escala real direto pelo link
compartilhado, sem OAuth, desde a FASE 11.1.

---

## 2. Azure AD — login Microsoft real no Android (FASE 11.3)

**Por quê:** o KMP lab tem um `applicationId`
(`br.com.leorvergani.escalaici.kmp.lab`) e uma chave de assinatura
(`escalaici-kmp-lab.jks`) diferentes do app oficial — o hash de assinatura
que o MSAL usa dentro da redirect URI do Android muda com a chave, então a
URI já cadastrada para o app oficial (`br.com.leorvergani.escalasoc`) não
serve para o lab. Sem isso cadastrado, o login Microsoft real abre o
navegador/broker mas a Microsoft recusa o retorno com erro de redirect URI
não reconhecida.

**Onde:** [portal.azure.com](https://portal.azure.com) → **Azure Active
Directory** → **App registrations** → abra o app registration cujo
`client_id` é `e5b5154d-e65e-4605-b221-73d7ee570580` (o mesmo usado pelo
app oficial, `tenant_id` `d2d23346-e737-4cac-96ec-fb25e7889f01`) →
**Authentication** (menu lateral).

**Passo a passo:**

- [ ] Em **Platform configurations**, clique em **Add a platform** →
      **Android** (se ainda não houver uma entrada Android separada para
      o lab; pode reaproveitar a mesma seção Android existente, adicionando
      mais uma redirect URI a ela).
- [ ] **Package name**: `br.com.leorvergani.escalaici.kmp.lab`
- [ ] **Signature hash**: `CNEvyhyc8lYTPFcNPDJzzJe1XyI=` (calculado a
      partir de `escalaici-kmp-lab.jks`, o keystore de assinatura já
      usado por toda build debug/release deste projeto desde a FASE 11.0c
      — ver seção 10 do spec 33).
- [ ] Isso gera a redirect URI completa, que também pode ser adicionada
      manualmente se o portal pedir o valor pronto:
      ```
      msauth://br.com.leorvergani.escalaici.kmp.lab/CNEvyhyc8lYTPFcNPDJzzJe1XyI%3D
      ```
- [ ] Clique em **Configure**/**Save**.

**Se algum dia recriar o keystore do lab** (`escalaici-kmp-lab.jks`), esse
hash muda e este passo precisa ser refeito — recalcular com:
```bash
keytool -exportcert -alias escalaici-kmp-lab -keystore escalaici-kmp-lab.jks -storepass <senha do keystore.properties> | openssl sha1 -binary | openssl base64
```

**Depois de feito**: aguarde a FASE 11.3 ser implementada (login MSAL real
no Android, ainda não codado neste projeto) e teste o botão "Login" na
tela de entrada do app — deve abrir o fluxo Microsoft real em vez da
mensagem "Login corporativo Microsoft ainda não disponível".

**Web**: se/quando o MSAL Web for implementado, vai precisar de outra
redirect URI própria (tipo **Single-page application**, não Android), no
mesmo espírito do item 1 acima — será documentada aqui quando essa fase
for planejada.

---

## Como este arquivo é mantido

Atualizado a cada sub-fase que crie uma pendência externa nova. Ver
`README.md` (seção "Validação da FASE X") para o contexto técnico completo
de cada integração, e `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`
para o plano geral. Assim que um item acima for confirmado funcionando de
ponta a ponta, mova a entrada para o `docs/ROADMAP.md` como `DONE` e apague
daqui.
