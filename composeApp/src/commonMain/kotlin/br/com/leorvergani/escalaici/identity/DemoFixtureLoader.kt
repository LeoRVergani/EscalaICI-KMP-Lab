package br.com.leorvergani.escalaici.identity

import escalaici_kmp_lab.composeapp.generated.resources.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

private val demoFixtureJson = Json { ignoreUnknownKeys = true }

/** Funcao pura: nao faz I/O, so decodifica uma string JSON ja em memoria. */
fun parseDemoFixturePackage(json: String): DemoFixturePackage =
    demoFixtureJson.decodeFromString(DemoFixturePackage.serializer(), json)

/**
 * Le o recurso real via Compose Resources.
 * Funciona no runtime real Android/Wasm, mas nao nos testes JVM/Karma do projeto.
 */
suspend fun loadDemoFixturePackage(): DemoFixturePackage {
    val bytes: ByteArray = Res.readBytes("files/demo/demo-v1-publication-package.json")
    return parseDemoFixturePackage(bytes.decodeToString())
}

object DemoFixtureCache {
    private val mutex = Mutex()
    private var cached: DemoFixturePackage? = null

    suspend fun get(): DemoFixturePackage {
        cached?.let { return it }
        return mutex.withLock {
            cached ?: loadDemoFixturePackage().also { cached = it }
        }
    }
}
