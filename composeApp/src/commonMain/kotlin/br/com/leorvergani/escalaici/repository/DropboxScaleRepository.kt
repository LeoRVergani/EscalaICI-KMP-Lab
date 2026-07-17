package br.com.leorvergani.escalaici.repository

import br.com.leorvergani.escalaici.model.RemoteScaleConfig
import br.com.leorvergani.escalaici.model.WorkbookImportResult
import br.com.leorvergani.escalaici.platform.downloadDropboxSharedLink
import br.com.leorvergani.escalaici.platform.readWorkbookFromBytes
import kotlinx.coroutines.CancellationException

/**
 * Baixa a escala real publicada no mesmo link Dropbox que o app Android usa
 * (`RemoteScaleConfig.DROPBOX_SCALE_URL`, shared link com `dl=1`). Nunca
 * lança: qualquer falha de rede/HTTP/autorização vira
 * `WorkbookImportResult.Failure` para não quebrar a tela durante uma
 * demonstração ao vivo.
 *
 * `platform.downloadDropboxSharedLink` isola a diferença de plataforma:
 * Android baixa o link direto (sem CORS, igual ao app real). Web/Wasm não
 * consegue — o link compartilhado não devolve `Access-Control-Allow-Origin`
 * para origens arbitrárias — então autentica via OAuth PKCE contra a API
 * oficial do Dropbox (`DropboxAuthConfig`/`dropbox-auth.js`), que suporta
 * CORS para chamadas autenticadas. Documentado em
 * `EscalaSOC/docs/spec/33-KMP-LAB-INTEGRACOES-REAIS.md`.
 */
class DropboxScaleRepository {
    suspend fun downloadCurrentScale(): WorkbookImportResult {
        val fileName = RemoteScaleConfig.DROPBOX_FILE_NAME
        return try {
            val bytes = downloadDropboxSharedLink(RemoteScaleConfig.DROPBOX_SCALE_URL)
            readWorkbookFromBytes(fileName, bytes)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            WorkbookImportResult.Failure(fileName, "Falha ao buscar a escala no Dropbox: ${error.message ?: error::class.simpleName}")
        }
    }
}
