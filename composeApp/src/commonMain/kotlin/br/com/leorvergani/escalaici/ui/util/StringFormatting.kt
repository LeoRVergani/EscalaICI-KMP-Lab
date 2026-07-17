package br.com.leorvergani.escalaici.ui.util

internal fun String.initials(): String {
    val parts = trim()
        .split(Regex("""[\s._-]+"""))
        .filter { it.isNotBlank() }
    return parts
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "IC" }
}
