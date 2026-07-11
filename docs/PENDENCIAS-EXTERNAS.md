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

## 1. Microsoft Entra ID — login Microsoft real no Android (FASE 11.3)

**Por quê:** o KMP lab tem um `applicationId`
(`br.com.leorvergani.escalaici.kmp.lab`) e uma chave de assinatura
(`escalaici-kmp-lab.jks`) diferentes do app oficial — o hash de assinatura
que o MSAL usa dentro da redirect URI do Android muda com a chave, então a
URI já cadastrada para o app oficial (`br.com.leorvergani.escalasoc`) não
serve para o lab. Sem isso cadastrado, o login Microsoft real abre o
navegador/broker mas a Microsoft recusa o retorno com erro de redirect URI
não reconhecida.

**Onde:** [entra.microsoft.com](https://entra.microsoft.com) (Microsoft
Entra admin center — nome atual do que já foi "Azure AD") → **Identity**
→ **Applications** → **App registrations** → abra o app registration cujo
`client_id` é `e5b5154d-e65e-4605-b221-73d7ee570580` (o mesmo usado pelo
app oficial, `tenant_id` `d2d23346-e737-4cac-96ec-fb25e7889f01`) →
**Authentication** (menu lateral). (Também acessível pelo caminho antigo
`portal.azure.com` → Microsoft Entra ID, é o mesmo cadastro.)

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

## 2. Adicionar os campos do KMP no `version.json` (FASE 11.2d)

**Por quê:** o botão "Atualizar aplicativo" do KMP já busca o **mesmo**
`version.json` que o app Android oficial usa (o arquivo já existe no
Dropbox, hospedado ao lado do `EscalaSOC-latest.apk`) — só falta adicionar
4 campos novos, só para o KMP, sem mexer em nada que o app oficial já lê.
Testado no emulador: sem esses campos, o app mostra corretamente "Você já
está usando a versão mais recente." (comportamento seguro, não quebra
nada) — mas não vai detectar atualização nenhuma até você adicionar os
campos abaixo.

**Onde:** o mesmo `version.json` do link
`APP_UPDATE_MANIFEST_URL` (`DropboxCloudConfig.kt` no app oficial) —
abra esse arquivo no Dropbox (é o mesmo que você já edita/substitui a cada
release do app oficial) e adicione as 4 linhas novas, **sem apagar nem
mudar nenhuma das linhas que já existem** (elas continuam sendo lidas pelo
app oficial):

- [ ] `kmpVersionCode`: número, tem que ser **maior** que o `versionCode`
      instalado no celular para o app detectar a atualização (o build
      atual do KMP lab é `10`).
- [ ] `kmpVersionName`: string, ex. `"0.6.0"`.
- [ ] `kmpApkUrl`: o link do Dropbox do APK do KMP — você já me passou:
      `https://www.dropbox.com/scl/fi/bwujohqsmenbx6279yg86/EscalaICI-latest.apk?rlkey=wqbzjht0isix9x2g8pbfcyghx&st=kqchnpky&dl=1`
      (**precisa terminar em `dl=1`**, senão baixa a página de preview do
      Dropbox em vez do arquivo — o link que você mandou já está certo).
- [ ] `kmpChangelog` (opcional): texto curto que aparece pro usuário
      quando uma atualização é encontrada.

Conteúdo de hoje do `version.json` (conferido agora, `2026-07-11`) + as 4
linhas novas — copie o arquivo inteiro abaixo (se o conteúdo do app oficial
tiver mudado desde então, mantenha os valores atuais das 5 primeiras
chaves e só adicione as 4 últimas):

```json
{
  "versionCode": 31,
  "versionName": "1.20.2",
  "apkUrl": "https://www.dropbox.com/scl/fi/sgoi2ykk9m1c4ebw0ggwo/EscalaSOC-latest.apk?rlkey=jyhffmy7ir4stmhd04xkho83n&st=2sy7gzs6&dl=1",
  "releaseNotes": "FASE 7k-2: cache local mais inteligente - o app so baixa a escala e o plantao completos quando algo realmente mudou no Firebase, senao usa o cache na hora. Botao Atualizar mais estavel contra cliques repetidos.",
  "changelog": "FASE 7k-2: cache local mais inteligente - o app so baixa a escala e o plantao completos quando algo realmente mudou no Firebase, senao usa o cache na hora. Botao Atualizar mais estavel contra cliques repetidos.",
  "kmpVersionCode": 10,
  "kmpVersionName": "0.6.0",
  "kmpApkUrl": "https://www.dropbox.com/scl/fi/bwujohqsmenbx6279yg86/EscalaICI-latest.apk?rlkey=wqbzjht0isix9x2g8pbfcyghx&st=kqchnpky&dl=1",
  "kmpChangelog": "FASE 11.2c/d: importação real de Plantão, data real do dispositivo, atualização real do app."
}
```

- [ ] Confirme que o arquivo `EscalaICI-latest.apk` já hospedado no
      Dropbox nesse link é o build mais recente do lab (`versionCode 10`,
      `0.6.0`) — se você subiu uma versão mais antiga antes de eu terminar
      esta fase, re-suba o arquivo gerado em
      `~/Downloads/EscalaICI-KMP-Lab-latest.apk` (sobrescrevendo o que já
      está no Dropbox, o link compartilhado continua o mesmo).

**Depois de feito**: peça pra eu testar de novo (ou teste você mesmo: abra
o app KMP no celular → Perfil → "Atualizar aplicativo" → deve aparecer
"Nova versão disponível: v0.6.0..." e abrir o instalador do Android — pode
pedir pra permitir "instalar apps de fontes desconhecidas" na primeira
vez, é normal).

**Mantendo isso pra sempre**: a cada nova versão do KMP lab que eu
publicar, os campos `kmpVersionCode`/`kmpVersionName`/`kmpChangelog`
precisam ser atualizados de novo nesse mesmo arquivo (o `kmpApkUrl` só
muda se o link do Dropbox mudar — o conteúdo do arquivo pode ser
sobrescrito sem trocar o link). Vou lembrar de avisar quando isso for
necessário.

---

## Como este arquivo é mantido

Atualizado a cada sub-fase que crie uma pendência externa nova. Ver
`README.md` (seção "Validação da FASE X") para o contexto técnico completo
de cada integração, e `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`
para o plano geral. Assim que um item acima for confirmado funcionando de
ponta a ponta, mova a entrada para o `docs/ROADMAP.md` como `DONE` e apague
daqui.
