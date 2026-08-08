package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TotaisRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Snapshot compartilhado (usuários + turnosMes + catálogo) usado por Trocas e "quem trabalha nesse dia" - sem rede, dados fixos. */
class TeamScheduleSnapshotTest {

    private val catalogo = mapOf(
        "M" to TipoTurnoRemoteDto(codigo = "M", descricao = "Manhã", categoria = CategoriaTurno.TRABALHO, horaInicio = "07:00", horaFim = "13:00", duracaoMinutos = 360, viraDia = false, contaComoPlantao = false, pesoPlantao = 0, corHex = "#3B82F6"),
        "T" to TipoTurnoRemoteDto(codigo = "T", descricao = "Tarde", categoria = CategoriaTurno.TRABALHO, horaInicio = "13:00", horaFim = "19:00", duracaoMinutos = 360, viraDia = false, contaComoPlantao = false, pesoPlantao = 0, corHex = "#F59E0B"),
        "DF" to TipoTurnoRemoteDto(codigo = "DF", descricao = "Descanso", categoria = CategoriaTurno.DESCANSO, duracaoMinutos = 0, viraDia = false, contaComoPlantao = false, pesoPlantao = 0, corHex = "#9E9E9E"),
    )

    private fun usuario(login: String, ativo: Boolean = true) = UsuarioRemoteDto(
        login = login,
        nome = login.replaceFirstChar { it.uppercase() },
        email = "$login@ici.tec.br",
        cargo = "Analista",
        equipeId = "EQ_SOC",
        nivelHierarquico = 6,
        turnoPadrao = "M",
        ativo = ativo,
    )

    private fun turnosMes(login: String, dias: Map<String, DiaRemoteDto>) = TurnosMesRemoteDto(
        schemaVersion = 1,
        usuarioUid = login,
        login = login,
        equipeId = "EQ_SOC",
        competencia = "2026-08",
        periodoInicio = "2026-07-26",
        periodoFim = "2026-08-25",
        turnoPadrao = "M",
        status = TurnosMesStatus.PUBLICADA,
        dias = dias,
        totais = TotaisRemoteDto(),
    )

    private val snapshot = TeamScheduleSnapshot(
        equipeId = "EQ_SOC",
        competencia = "2026-08",
        usuariosAtivos = listOf(usuario("ana.silva"), usuario("carlos.souza"), usuario("marina.lima", ativo = false)),
        turnosMesPublicadas = listOf(
            turnosMes("ana.silva", mapOf("2026-08-20" to DiaRemoteDto(c = "M"), "2026-08-21" to DiaRemoteDto(c = "DF"))),
            turnosMes("carlos.souza", mapOf("2026-08-20" to DiaRemoteDto(c = "T"))),
        ),
        catalogo = catalogo,
    )

    @Test
    fun jornadaDoDia_resolveCodigoEHorarioDoUsuario() {
        val jornada = snapshot.jornadaDoDia("ana.silva", "2026-08-20")
        assertEquals("M", jornada.codigo)
        assertTrue(jornada.trabalha)
    }

    @Test
    fun jornadaDoDia_semDocumento_retornaSemEscala() {
        val jornada = snapshot.jornadaDoDia("usuario.sem.escala", "2026-08-20")
        assertEquals("Sem escala publicada", jornada.descricao)
    }

    @Test
    fun colegasNoDia_excluiOProprioUsuario() {
        val colegas = snapshot.colegasNoDia("ana.silva", "2026-08-20")
        assertTrue(colegas.none { (usuario, _) -> usuario.login == "ana.silva" })
    }

    @Test
    fun colegasNoDia_incluiSoQuemTrabalhaNaquelaData() {
        val colegas = snapshot.colegasNoDia("ana.silva", "2026-08-20")
        assertEquals(listOf("carlos.souza"), colegas.map { (usuario, _) -> usuario.login })
    }

    @Test
    fun colegasNoDia_excluiQuemNaoTrabalhaNessaData() {
        // ana.silva folga em 21/08 - nesse dia so ela apareceria elegível, mas o teste e do ponto de vista de outro login.
        val colegas = snapshot.colegasNoDia("carlos.souza", "2026-08-21")
        assertTrue(colegas.isEmpty())
    }

    @Test
    fun colegasNoDia_naoIncluiUsuariosInativos() {
        val colegas = snapshot.colegasNoDia("ana.silva", "2026-08-20")
        assertTrue(colegas.none { (usuario, _) -> usuario.login == "marina.lima" })
    }
}
