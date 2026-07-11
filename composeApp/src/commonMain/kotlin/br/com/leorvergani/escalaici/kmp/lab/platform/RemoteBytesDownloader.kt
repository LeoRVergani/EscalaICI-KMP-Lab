package br.com.leorvergani.escalaici.kmp.lab.platform

/**
 * Baixa bytes crus de uma URL. Android usa Ktor (CIO); Web/Wasm usa `fetch`
 * nativo do navegador via interop com JS (`remote-download.js`) em vez do
 * engine Ktor — ver `repository/DropboxScaleRepository.kt` para o porquê:
 * o engine CIO no alvo Web/Wasm não estava rejeitando a coroutine de forma
 * confiável quando o `fetch` falhava, deixando a chamada suspensa para
 * sempre em vez de lançar. `fetch` direto garante que o callback do JS
 * sempre é chamado (sucesso ou erro), então a suspend function sempre
 * resolve.
 */
expect suspend fun downloadBytes(url: String): ByteArray
