package br.com.leorvergani.escalaici.identity

fun normalizeIdentity(raw: String?): String? =
    raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
