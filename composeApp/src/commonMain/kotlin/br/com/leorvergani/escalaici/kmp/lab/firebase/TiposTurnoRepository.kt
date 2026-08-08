package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto

/** Le `tiposTurno` filtrado por `equipeId` (prompt FASE 15 secao 15), com o mesmo fallback do app real quando a equipe ainda nao tem catalogo proprio seedado. */
class TiposTurnoRepository(private val firestore: FirestoreRestClient) {
    suspend fun catalogoPara(idToken: String, equipeId: String): Map<String, TipoTurnoRemoteDto> {
        val documentos = try {
            firestore.runQuery(idToken, "tiposTurno", listOf(FieldEquals.Text("equipeId", equipeId)))
        } catch (e: FirestoreUnauthorizedException) {
            throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, e.message ?: "Sessao expirada.")
        } catch (e: FirestorePermissionDeniedException) {
            throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, e.message ?: "Sem permissao para ler o catalogo de turnos.")
        } catch (e: FirestoreNetworkException) {
            throw EscalaIciException(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede ao ler o catalogo de turnos.")
        }
        val catalogo = documentos.mapNotNull(RemoteDtoMappers::tipoTurno).associateBy { it.codigo }
        return catalogo.ifEmpty { CatalogoPadrao.CATALOGO_SOC }
    }
}
