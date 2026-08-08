package br.com.leorvergani.escalaici.kmp.lab.platform

/**
 * Timestamp UTC atual em ISO-8601 com milissegundos fixos
 * (`yyyy-MM-ddTHH:mm:ss.SSSZ`, equivalente a `new Date().toISOString()` do
 * TS) - usado pelos campos `criadoEm`/`atualizadoEm`/`respondidoEm`/etc de
 * Trocas (FASE 16). Largura fixa é deliberada: esses campos são comparados
 * como string (`atualizadoEm.localeCompare`/ordenação de histórico), e uma
 * largura variável quebraria essa ordenação.
 */
expect fun nowIso(): String
