package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Porte de `resolverJornadaDia`/`categoriaTrabalha` (`packages/contrato/src/jornada.ts`) - usado por Trocas para elegibilidade de dia/colega. */
class JornadaDiaTest {

    private val catalogo = mapOf(
        "M" to TipoTurnoRemoteDto(codigo = "M", descricao = "Manhã", categoria = CategoriaTurno.TRABALHO, horaInicio = "07:00", horaFim = "13:00", duracaoMinutos = 360, viraDia = false, contaComoPlantao = false, pesoPlantao = 0, corHex = "#3B82F6"),
        "T" to TipoTurnoRemoteDto(codigo = "T", descricao = "Tarde", categoria = CategoriaTurno.TRABALHO, horaInicio = "13:00", horaFim = "19:00", duracaoMinutos = 360, viraDia = false, contaComoPlantao = false, pesoPlantao = 0, corHex = "#F59E0B"),
        "DF" to TipoTurnoRemoteDto(codigo = "DF", descricao = "Descanso", categoria = CategoriaTurno.DESCANSO, duracaoMinutos = 0, viraDia = false, contaComoPlantao = false, pesoPlantao = 0, corHex = "#9E9E9E"),
        "PL" to TipoTurnoRemoteDto(codigo = "PL", descricao = "Plantão", categoria = CategoriaTurno.PLANTAO, horaInicio = "08:00", horaFim = "20:00", duracaoMinutos = 720, viraDia = false, contaComoPlantao = true, pesoPlantao = 1, corHex = "#18A874"),
        "HE" to TipoTurnoRemoteDto(codigo = "HE", descricao = "Hora extra", categoria = CategoriaTurno.EXTRA, horaInicio = "19:00", horaFim = "21:00", duracaoMinutos = 120, viraDia = false, contaComoPlantao = false, pesoPlantao = 0, corHex = "#8B5CF6"),
    )

    @Test
    fun diaNulo_retornaSemEscalaPublicada_naoTrabalha() {
        val jornada = resolverJornadaDia(null, catalogo, "2026-08-20")
        assertFalse(jornada.trabalha)
        assertEquals("Sem escala publicada", jornada.descricao)
        assertEquals("", jornada.codigo)
    }

    @Test
    fun diaDeTrabalho_resolveHorarioDoCatalogo() {
        val jornada = resolverJornadaDia(DiaRemoteDto(c = "M"), catalogo, "2026-08-20")
        assertTrue(jornada.trabalha)
        assertEquals("Manhã", jornada.descricao)
        assertEquals("07:00", jornada.inicio)
        assertEquals("13:00", jornada.fim)
        assertEquals("07:00–13:00", jornada.horario)
    }

    @Test
    fun diaDeDescanso_naoTrabalha() {
        val jornada = resolverJornadaDia(DiaRemoteDto(c = "DF"), catalogo, "2026-08-20")
        assertFalse(jornada.trabalha)
    }

    @Test
    fun diaDePlantao_contaComoTrabalho() {
        val jornada = resolverJornadaDia(DiaRemoteDto(c = "PL"), catalogo, "2026-08-20")
        assertTrue(jornada.trabalha)
    }

    @Test
    fun diaDeHoraExtra_contaComoTrabalho() {
        val jornada = resolverJornadaDia(DiaRemoteDto(c = "HE"), catalogo, "2026-08-20")
        assertTrue(jornada.trabalha)
    }

    @Test
    fun horarioProprioDoDia_prevalecemSobreOCatalogo() {
        val jornada = resolverJornadaDia(DiaRemoteDto(c = "M", i = "06:30", f = "12:30"), catalogo, "2026-08-20")
        assertEquals("06:30", jornada.inicio)
        assertEquals("12:30", jornada.fim)
    }

    @Test
    fun codigoDesconhecidoNoCatalogo_semHorario_naoTrabalha() {
        val jornada = resolverJornadaDia(DiaRemoteDto(c = "XX"), catalogo, "2026-08-20")
        assertFalse(jornada.trabalha)
        assertEquals("XX", jornada.descricao)
    }

    @Test
    fun horarioVazio_quandoInicioOuFimAusente() {
        val jornada = resolverJornadaDia(DiaRemoteDto(c = "XX"), catalogo, "2026-08-20")
        assertEquals("", jornada.horario)
    }
}
