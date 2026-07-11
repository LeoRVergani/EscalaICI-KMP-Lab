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

## 2. Azure AD — login Microsoft real (FASE 11.3, quando for implementada)

Ainda **não implementado** neste projeto. Quando essa fase for feita, o
Android vai precisar de uma **nova redirect URI registrada no Azure AD**
(app registration usado pelo `auth_config_single_account.json` do app
oficial), porque o KMP lab tem um `applicationId`
(`br.com.leorvergani.escalaici.kmp.lab`) e uma chave de assinatura
(`escalaici-kmp-lab.jks`) diferentes do app oficial — o hash de assinatura
que o MSAL usa na redirect URI do Android muda com a chave, então a URI já
cadastrada para o app oficial não serve para o lab.

Esta seção será preenchida com o valor exato a cadastrar (`msauth://...`)
quando a FASE 11.3 for implementada e o hash da chave `escalaici-kmp-lab.jks`
for calculado. Web (se/quando MSAL Web for implementado) vai precisar de
outra redirect URI própria, no mesmo padrão do item 1 acima.

---

## Como este arquivo é mantido

Atualizado a cada sub-fase que crie uma pendência externa nova. Ver
`README.md` (seção "Validação da FASE X") para o contexto técnico completo
de cada integração, e `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`
para o plano geral. Assim que um item acima for confirmado funcionando de
ponta a ponta, mova a entrada para o `docs/ROADMAP.md` como `DONE` e apague
daqui.
