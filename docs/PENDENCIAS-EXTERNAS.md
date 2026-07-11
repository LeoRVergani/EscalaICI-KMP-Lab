# Pendências externas — ações que só o dono das contas pode fazer

Este arquivo lista, em ordem de prioridade, toda ação fora do código que
falta para as integrações reais deste projeto funcionarem de ponta a
ponta. Nenhuma IA/sessão consegue fazer isso sozinha — são cadastros em
consoles de terceiros (Dropbox, Microsoft Azure) que exigem login com a
conta dona do app. Depois de cada ação, marque o checkbox e anote a data.

**Dropbox (FASE 11.1b) — DONE, confirmado pelo usuário em 2026-07-11.**
Escopo `sharing.read` + redirect URI cadastrados, download real da escala
funcionando na Web via OAuth. Removido deste checklist — ver
`docs/ROADMAP.md`, entrada "FASE 11.1b", para o registro histórico.

---

## 1. Azure AD — login Microsoft real no Android (FASE 11.3)

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
