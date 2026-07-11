package br.com.leorvergani.escalaici.kmp.lab.repository

import br.com.leorvergani.escalaici.kmp.lab.model.RemoteScaleConfig
import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult
import br.com.leorvergani.escalaici.kmp.lab.platform.downloadBytes
import br.com.leorvergani.escalaici.kmp.lab.platform.readWorkbookFromBytes
import kotlinx.coroutines.CancellationException

/**
 * Baixa a escala real publicada no mesmo link Dropbox que o app Android usa
 * (`RemoteScaleConfig.DROPBOX_SCALE_URL`, shared link com `dl=1`, sem
 * token/App Key — igual ao caminho de usuário comum do app real). Nunca
 * lança: qualquer falha de rede/HTTP vira `WorkbookImportResult.Failure`
 * para não quebrar a tela durante uma demonstração ao vivo.
 *
 * No Web/Wasm, este download **falha por CORS** (o `fetch()` do navegador é
 * bloqueado antes de qualquer resposta, `net::ERR_FAILED` — o link
 * compartilhado do Dropbox não devolve `Access-Control-Allow-Origin` para
 * origens arbitrárias como `http://localhost:8080`). Isso é uma limitação de
 * plataforma, não um bug daqui — documentado em
 * `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`. `platform.downloadBytes`
 * (ver `RemoteBytesDownloader`) isola essa diferença: Android usa Ktor, Web/Wasm
 * usa `fetch` nativo com timeout/abort próprios via `remote-download.js` — os
 * dois sempre completam (sucesso ou erro), nunca ficam suspensos para sempre.
 */
class DropboxScaleRepository {
    suspend fun downloadCurrentScale(): WorkbookImportResult {
        val fileName = RemoteScaleConfig.DROPBOX_FILE_NAME
        return try {
            val bytes = downloadBytes(RemoteScaleConfig.DROPBOX_SCALE_URL)
            readWorkbookFromBytes(fileName, bytes)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WorkbookImportResult.Failure(fileName, "Falha ao buscar a escala no Dropbox: ${error.message ?: error::class.simpleName}")
        }
    }
}
