package br.com.leorvergani.escalaici.kmp.lab.platform

actual suspend fun downloadDropboxSharedLink(sharedLinkUrl: String): ByteArray = downloadBytes(sharedLinkUrl)
