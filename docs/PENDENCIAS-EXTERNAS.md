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

## 1. Microsoft Entra ID — login Microsoft real no Android (FASE 11.3 / código pronto na FASE 14b-1)

**Status: BLOQUEADO EXTERNAMENTE — código implementado, falta só o cadastro
no Entra e o `auth-config.json` local com os valores reais.**

**Por quê:** o Escala ICI tem um `applicationId`
(`br.com.leorvergani.escalaici`) e uma chave de assinatura próprios — o
hash de assinatura que o MSAL usa dentro da redirect URI do Android muda
com a chave e também precisa ser recalculado quando o `applicationId`
muda. Sem isso cadastrado, o login Microsoft real abre o navegador/broker
mas a Microsoft recusa o retorno com erro de redirect URI não reconhecida.

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
      o Escala ICI; pode reaproveitar a mesma seção Android existente,
      adicionando mais uma redirect URI a ela).
- [ ] **Package name**: `br.com.leorvergani.escalaici`
- [ ] **Signature hash**: `CNEvyhyc8lYTPFcNPDJzzJe1XyI=` (hash antigo
      mantido aqui apenas como referência textual; precisa ser recalculado
      porque o `applicationId` mudou para `br.com.leorvergani.escalaici`).
- [ ] Isso gera a redirect URI completa, que também pode ser adicionada
      manualmente se o portal pedir o valor pronto:
      ```
      msauth://br.com.leorvergani.escalaici/CNEvyhyc8lYTPFcNPDJzzJe1XyI%3D
      ```
- [ ] Clique em **Configure**/**Save**.

**Se o `applicationId` ou a assinatura mudarem**, esse hash muda e este
passo precisa ser refeito — recalcular com:
```bash
keytool -exportcert -alias escalaici-kmp-lab -keystore escalaici-kmp-lab.jks -storepass <senha do keystore.properties> | openssl sha1 -binary | openssl base64
```

**Depois de feito**: preencha `auth-config.json` (raiz do projeto, fora do
Git — copie de `auth-config.example.json`) com os valores reais de
`tenant_id`/`client_id`/`android.signature_hash_debug`/
`android.signature_hash_release`/`android.redirect_uri_debug`/
`android.redirect_uri_release`, recompile e teste o botão "Entrar com
conta corporativa" na tela de entrada do app — deve abrir o fluxo
Microsoft real em vez da mensagem "Autenticação corporativa ainda não
configurada neste ambiente". O código do login MSAL Android já está
implementado (FASE 14b-1, ver
[`docs/spec/53-ESCALAICI-MSAL-ANDROID-E-IDENTIDADE-CORPORATIVA.md`](spec/53-ESCALAICI-MSAL-ANDROID-E-IDENTIDADE-CORPORATIVA.md));
o que falta é só este cadastro externo + o preenchimento do arquivo local.

**Web**: se/quando o MSAL Web for implementado, vai precisar de outra
redirect URI própria (tipo **Single-page application**, não Android), no
mesmo espírito do item 1 acima — será documentada aqui quando essa fase
for planejada.

---

## 2. Subir `EscalaICI-latest.apk` + `version.json` pro Dropbox (manual, sempre)

**Status dos campos do `version.json`**: ✅ DONE — usuário confirmou em
2026-07-11 que já adicionou `kmpVersionCode`/`kmpVersionName`/`kmpApkUrl`/
`kmpChangelog` no `version.json` real, exatamente como pedido na FASE
11.2d. **O que falta agora é só o upload** dos arquivos mais recentes.

**Por quê o upload é manual**: rodar os scripts automáticos de publicação
(`publicar_update.sh`/`upload_update_dropbox.py`, que chamam a API do
Dropbox) estava dando erro para este app — o usuário sobe manualmente
pelo site/app do Dropbox. **Nenhuma IA/sessão deve tentar automatizar esse
upload** — nem rodando os scripts antigos (são só do EscalaSOC, ganharam
aviso no topo) nem chamando a API do Dropbox por conta própria.

**Onde os arquivos ficam prontos, localmente**:
`/home/lvergani/Downloads/dropbox_update_scripts/EscalaICI-latest.apk` e
`.../version.json` — gerados/atualizados a cada release rodando
`./gerar_update_escalaici_local.sh` nessa pasta (ver seção "FASE 11.2e" do
`README.md` deste projeto para o procedimento completo).

- [x] ~~Suba `EscalaICI-latest.apk` (versionCode `13`, `0.6.3` — junta FASE
      12b/12b-2/12b-3...) para o Dropbox~~ **Superado pela FASE 14b-0**: o
      `applicationId` mudou de `br.com.leorvergani.escalaici.kmp.lab` para
      `br.com.leorvergani.escalaici` (ver spec 52). Um APK `0.6.3` com o
      package antigo não é mais compatível com o fluxo de atualização do
      app novo — não faz sentido subir esse artefato antigo agora. O
      próximo upload pendente é o da v`0.7.0` (abaixo), não o da v0.6.3.
- [ ] Suba `EscalaICI-latest.apk` (versionCode `14`, `0.7.0` — FASE 14b-0:
      novo `applicationId`/`namespace`/packages Kotlin, sem `.kmp.lab`,
      mesmo nome visível "Escala ICI") para o Dropbox, no mesmo link já
      cadastrado (sobrescrever o conteúdo, sem apagar o arquivo — senão o
      link muda).
- [ ] Suba `version.json` (mesma pasta) para o Dropbox, mesmo link.

**Atenção — mudança de `applicationId` nesta versão**: como o Android não
permite trocar `applicationId` nem assinatura de um app já instalado sem
desinstalar antes, qualquer celular com a v0.6.3 (package
`...kmp.lab`) instalada **não vai receber a v0.7.0 via "Atualizar
aplicativo"** — é preciso desinstalar a versão antiga e instalar a v0.7.0
manualmente uma vez (`adb install -r EscalaICI-latest.apk` ou baixando o
link do Dropbox direto no navegador do celular). A partir da v0.7.0, o
fluxo de atualização automática volta a funcionar normalmente para as
próximas versões (mesmo `applicationId`, mesma assinatura).

**Depois de feito**: peça pra eu testar (ou teste você mesmo: abra o app
Escala ICI no celular → Perfil → "Atualizar aplicativo" → deve aparecer
"Nova versão disponível: v0.7.0..." e abrir o instalador do Android — pode
pedir pra permitir "instalar apps de fontes desconhecidas" na primeira
vez, é normal, só acontece 1x).

**Mantendo isso pra sempre**: a cada nova versão real do Escala ICI, o
procedimento é sempre: build release → `gerar_update_escalaici_local.sh`
→ editar `kmpVersionCode`/`kmpVersionName`/`kmpChangelog` no `version.json`
local → você sobe os dois arquivos manualmente. Vou lembrar de avisar
quando um release novo estiver pronto pra subir.

---

## Como este arquivo é mantido

Atualizado a cada sub-fase que crie uma pendência externa nova. Ver
`README.md` (seção "Validação da FASE X") para o contexto técnico completo
de cada integração, e `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`
para o plano geral. Assim que um item acima for confirmado funcionando de
ponta a ponta, mova a entrada para o `docs/ROADMAP.md` como `DONE` e apague
daqui.
