package br.com.leorvergani.escalaici.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest

class RunCatchingCancellableTest {
    @Test
    fun wrapsSuccessLikeRunCatching() = runTest {
        val result = runCatchingCancellable { 42 }

        assertEquals(42, result.getOrNull())
    }

    @Test
    fun wrapsOrdinaryFailureLikeRunCatching() = runTest {
        val result = runCatchingCancellable { error("boom") }

        assertTrue(result.isFailure)
        assertEquals("boom", result.exceptionOrNull()?.message)
    }

    @Test
    fun neverSwallowsCancellationException() = runTest {
        // Guarda de regressao real (spec 67, Checkpoint I): runCatching comum captura QUALQUER
        // Throwable, inclusive cancelamento de corrotina - deixando uma corrotina "logicamente
        // morta" continuar executando e sobrescrever estado compartilhado depois de cancelada.
        // Causa raiz real do card Hoje desatualizado e do segundo login preso apos logout.
        assertFailsWith<CancellationException> {
            runCatchingCancellable<Int> { throw CancellationException("cancelled") }
        }
    }
}
