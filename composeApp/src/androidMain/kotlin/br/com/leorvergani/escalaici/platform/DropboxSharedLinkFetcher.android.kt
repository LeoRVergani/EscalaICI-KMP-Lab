package br.com.leorvergani.escalaici.platform

actual suspend fun downloadDropboxSharedLink(sharedLinkUrl: String): ByteArray = downloadBytes(sharedLinkUrl)
