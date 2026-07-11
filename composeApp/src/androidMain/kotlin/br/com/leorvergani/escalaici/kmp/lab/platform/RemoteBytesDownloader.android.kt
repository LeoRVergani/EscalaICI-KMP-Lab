package br.com.leorvergani.escalaici.kmp.lab.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.http.isSuccess

private val client by lazy {
    HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
        }
    }
}

actual suspend fun downloadBytes(url: String): ByteArray {
    val response = client.get(url)
    check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
    return response.body()
}
