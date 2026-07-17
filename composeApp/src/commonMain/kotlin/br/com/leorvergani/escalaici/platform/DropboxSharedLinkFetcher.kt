package br.com.leorvergani.escalaici.platform

/**
 * Baixa os bytes do arquivo publicado num shared link do Dropbox. Android
 * usa o link direto (`downloadBytes`, sem CORS — funciona igual ao app
 * real). Web/Wasm autentica via OAuth PKCE contra a API oficial do Dropbox
 * (`sharing/get_shared_link_file`), a única forma de contornar o bloqueio
 * de CORS do link direto num navegador — ver `DropboxAuthConfig`.
 */
expect suspend fun downloadDropboxSharedLink(sharedLinkUrl: String): ByteArray
