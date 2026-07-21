package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.auth.CorporateIdentity

suspend fun isDemoAuthorizedForIdentity(
    identity: CorporateIdentity,
    isDevelopmentBuild: Boolean,
    allowedDeveloperObjectIdsProvider: suspend () -> List<String>
): Boolean {
    val allowedByObjectId = runCatching {
        allowedDeveloperObjectIdsProvider().contains(identity.objectId)
    }.getOrDefault(false)
    if (allowedByObjectId) return true

    // Temporario: remover assim que a publicacao real tiver
    // allowedDeveloperObjectIds populado no workspace de desenvolvimento.
    return isDevelopmentBuild &&
        normalizeIdentity(identity.email) == TEMPORARY_DEMO_DEVELOPER_EMAIL
}

private const val TEMPORARY_DEMO_DEVELOPER_EMAIL = "lvergani@ici.tec.br"
