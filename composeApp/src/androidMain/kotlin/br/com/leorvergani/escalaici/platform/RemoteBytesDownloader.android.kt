package br.com.leorvergani.escalaici.platform

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File

private val client by lazy {
    HttpClient {
        install(HttpTimeout) {
            // 60s: cobre tambem o download do APK de atualizacao (~60-100MB),
            // nao so o manifesto/planilha (pequenos, terminam bem antes disso).
            requestTimeoutMillis = 60_000
        }
    }
}

actual suspend fun downloadBytes(url: String): ByteArray {
    val response = client.get(url)
    check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
    return response.body()
}

/**
 * Baixa uma URL direto para um arquivo, em streaming — nunca guarda o
 * arquivo inteiro em memoria (diferente de `downloadBytes`). Usado só pelo
 * download do APK de atualização (`AppUpdateChecker.android.kt`), que pode
 * ter 60-100MB e não cabe com folga num `ByteArray` somado ao resto da
 * memória já usada pelo Compose/Skia — mesmo padrão do app oficial
 * (`AppUpdateManager.downloadApk()`, `input.copyTo(output)`), só que via
 * Ktor em vez de `HttpURLConnection`.
 */
internal suspend fun downloadToFile(url: String, destination: File) {
    client.prepareGet(url).execute { response ->
        check(response.status.isSuccess()) { "HTTP ${response.status.value}" }
        destination.outputStream().use { output ->
            response.bodyAsChannel().copyTo(output)
        }
    }
}
