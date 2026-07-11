package br.com.leorvergani.escalaici.kmp.lab.model

/**
 * App Key do Dropbox (cliente público, PKCE, sem client secret) — o mesmo
 * já usado pelo fluxo ADM do app Android real (`AdminConfig.DROPBOX_APP_KEY`
 * em `EscalaSOC`, também PKCE). Reaproveitado aqui só para autorizar a
 * leitura do link compartilhado via API oficial do Dropbox no Web: o link
 * direto (`RemoteScaleConfig.DROPBOX_SCALE_URL`) é bloqueado por CORS no
 * navegador, mas as chamadas autenticadas da API do Dropbox
 * (`content.dropboxapi.com`) suportam CORS.
 *
 * Duas ações externas (só o dono da conta Dropbox pode fazer, no App
 * Console) são necessárias antes deste fluxo funcionar de ponta a ponta:
 * 1. Registrar `REDIRECT_URI` abaixo em "OAuth 2 > Redirect URIs" do app
 *    com este App Key (o app já tem outro redirect URI registrado, do
 *    Android — dá para ter os dois ao mesmo tempo).
 * 2. Garantir que o escopo `sharing.read` está habilitado em "Permissions"
 *    do mesmo app (o fluxo ADM usa `files.content.read`/`files.content.write`,
 *    que são escopos diferentes).
 *
 * Ver seção "FASE 11.1b" do spec 33 (`EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`)
 * para o estado atual dessas pendências.
 */
object DropboxAuthConfig {
    const val APP_KEY: String = "5by0pkzt2bgx95g"
    const val REDIRECT_URI: String = "http://localhost:8080/dropbox-callback.html"
    const val SCOPE: String = "sharing.read"
}
