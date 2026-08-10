package br.com.leorvergani.escalaici.kmp.lab.firebase

import io.ktor.client.HttpClient

/**
 * Engine HTTP por plataforma (FASE 17B.1). Android/JVM usa CIO (sockets
 * reais); Web/Wasm usa o engine `Js` do Ktor (fetch() nativo do navegador) -
 * CIO nao funciona dentro de um navegador real (lanca "Node.js net module
 * is not available" ao tentar enviar qualquer requisicao, achado da FASE
 * 17B). `IdentityToolkitAuthClient`/`FirestoreRestClient`/repositorios
 * continuam 100% comuns - so a escolha do engine e platform-specific.
 */
expect fun createPlatformHttpClient(): HttpClient
