package br.com.leorvergani.escalaici.kmp.lab.firebase

/**
 * Deriva o login corporativo a partir do e-mail autenticado - equivalente
 * exato a `loginDoEmail()` (`lib/firebase/authRepository.ts`) e
 * `loginDoAuth()` (`firestore.rules`) do Escala-ICI real
 * (`origin/main`, confirmado por auditoria): prefixo antes do `@`,
 * minusculo, sem espacos nas pontas. Vale para login por senha hoje e para
 * qualquer provedor futuro (Microsoft/Entra) que devolva e-mail.
 */
fun loginFromEmail(email: String): String =
    email.trim().lowercase().substringBefore("@")
