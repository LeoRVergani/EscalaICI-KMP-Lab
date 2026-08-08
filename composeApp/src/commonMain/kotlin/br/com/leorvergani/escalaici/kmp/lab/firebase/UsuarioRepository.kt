package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto

/** Le `usuarios/{login}` e valida o que o app precisa antes de seguir (prompt FASE 15 secao 9). */
class UsuarioRepository(private val firestore: FirestoreRestClient) {
    suspend fun resolveUsuario(idToken: String, login: String): UsuarioRemoteDto {
        val document = try {
            firestore.getDocument(idToken, "usuarios", login)
        } catch (e: FirestoreUnauthorizedException) {
            throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, e.message ?: "Sessao expirada.")
        } catch (e: FirestorePermissionDeniedException) {
            throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, e.message ?: "Sem permissao para ler o perfil.")
        } catch (e: FirestoreNetworkException) {
            throw EscalaIciException(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede ao ler o perfil.")
        } ?: throw EscalaIciException(EscalaIciError.USER_NOT_FOUND, "Seu login nao esta cadastrado na escala. Procure o gestor.")

        val usuario = RemoteDtoMappers.usuario(document)
            ?: throw EscalaIciException(EscalaIciError.INVALID_REMOTE_DATA, "Perfil de usuario com dados invalidos.")

        if (!usuario.ativo) {
            throw EscalaIciException(EscalaIciError.USER_INACTIVE, "Seu cadastro esta inativo. Procure o gestor.")
        }
        if (usuario.equipeId.isBlank()) {
            throw EscalaIciException(EscalaIciError.TEAM_NOT_FOUND, "Seu usuario nao tem equipe associada.")
        }
        return usuario
    }

    /** Usuarios ativos da equipe (`equipeId==X AND ativo==true`) - usado por Trocas para escolher o colega e por "quem trabalha nesse dia" (FASE 16). */
    suspend fun listarAtivosPorEquipe(idToken: String, equipeId: String): List<UsuarioRemoteDto> {
        val documentos = try {
            firestore.runQuery(
                idToken,
                "usuarios",
                listOf(
                    FieldEquals.Text("equipeId", equipeId),
                    FieldEquals.Bool("ativo", true),
                ),
            )
        } catch (e: FirestoreUnauthorizedException) {
            throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, e.message ?: "Sessao expirada.")
        } catch (e: FirestorePermissionDeniedException) {
            throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, e.message ?: "Sem permissao para listar a equipe.")
        } catch (e: FirestoreNetworkException) {
            throw EscalaIciException(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede ao listar a equipe.")
        }
        return documentos.mapNotNull(RemoteDtoMappers::usuario)
    }
}
