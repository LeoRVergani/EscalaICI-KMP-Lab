package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlin.test.Test
import kotlin.test.assertEquals

class IdentityTest {

    @Test
    fun loginFromEmail_extractsPrefixBeforeAt() {
        assertEquals("ana.silva", loginFromEmail("ana.silva@empresa.com"))
    }

    @Test
    fun loginFromEmail_lowercasesResult() {
        assertEquals("carlos.souza", loginFromEmail("Carlos.Souza@Empresa.COM"))
    }

    @Test
    fun loginFromEmail_trimsWhitespace() {
        assertEquals("marina.lima", loginFromEmail("  marina.lima@empresa.com  "))
    }
}
