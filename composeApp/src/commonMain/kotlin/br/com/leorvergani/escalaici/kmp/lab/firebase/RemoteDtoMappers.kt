package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.CategoriaTurno
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TotaisRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Converte um documento Firestore (`{ fields: {...} }`) nos DTOs do contrato remoto do Escala-ICI. */
object RemoteDtoMappers {
    private val codec = FirestoreValueCodec

    fun usuario(document: JsonObject): UsuarioRemoteDto? {
        // `login` e o ID do documento `usuarios/{login}` - fonte de verdade da
        // identidade (lib/firebase/shared.ts:lerUsuario, origin/main do
        // Escala-ICI). O campo `login` dentro do documento deveria ser igual,
        // mas o ID e quem manda; so cai para o campo se o ID nao vier no payload.
        val id = documentId(document)
        val f = codec.fieldsOf(document)
        return UsuarioRemoteDto(
            login = id ?: codec.string(f, "login") ?: return null,
            uid = codec.string(f, "uid"),
            loginAliases = codec.stringList(f, "loginAliases"),
            nome = codec.string(f, "nome") ?: return null,
            email = codec.string(f, "email") ?: return null,
            cargo = codec.string(f, "cargo") ?: "",
            equipeId = codec.string(f, "equipeId") ?: return null,
            gestorUid = codec.string(f, "gestorUid"),
            nivelHierarquico = codec.int(f, "nivelHierarquico", default = 6),
            turnoPadrao = codec.string(f, "turnoPadrao") ?: "",
            ativo = codec.boolOrNull(f, "ativo") ?: true,
        )
    }

    fun tipoTurno(document: JsonObject): TipoTurnoRemoteDto? {
        val f = codec.fieldsOf(document)
        val categoria = codec.string(f, "categoria")?.let { runCatching { CategoriaTurno.valueOf(it) }.getOrNull() }
            ?: CategoriaTurno.TRABALHO
        // Documento legado sem `codigo` no payload: cai para o ultimo segmento
        // do ID (`{equipeId}_{codigo}`, ex. "EQ_SOC_M" -> "M"), igual
        // listarCatalogo() (lib/firebase/readRepository.ts:48) no app real -
        // nao e um fallback inventado pelo KMP.
        val codigoFallback = documentId(document)?.substringAfterLast('_')?.takeIf { it.isNotBlank() }
        return TipoTurnoRemoteDto(
            codigo = codec.string(f, "codigo") ?: codigoFallback ?: return null,
            descricao = codec.string(f, "descricao") ?: return null,
            categoria = categoria,
            horaInicio = codec.string(f, "horaInicio"),
            horaFim = codec.string(f, "horaFim"),
            duracaoMinutos = codec.int(f, "duracaoMinutos"),
            viraDia = codec.bool(f, "viraDia"),
            contaComoPlantao = codec.bool(f, "contaComoPlantao"),
            pesoPlantao = codec.int(f, "pesoPlantao"),
            corHex = codec.string(f, "corHex") ?: "#9E9E9E",
            aliasesXLS = codec.stringList(f, "aliasesXLS"),
        )
    }

    fun turnosMes(document: JsonObject): TurnosMesRemoteDto? {
        val f = codec.fieldsOf(document)
        val statusText = codec.string(f, "status") ?: return null
        val status = runCatching { TurnosMesStatus.valueOf(statusText) }.getOrNull() ?: return null
        val dias = codec.mapEntries(f, "dias").mapValues { (_, diaFields) -> dia(diaFields) }
        val totaisFields = codec.map(f, "totais")
        return TurnosMesRemoteDto(
            schemaVersion = codec.int(f, "schemaVersion", default = 1),
            usuarioUid = codec.string(f, "usuarioUid") ?: codec.string(f, "login") ?: return null,
            login = codec.string(f, "login") ?: return null,
            equipeId = codec.string(f, "equipeId") ?: return null,
            competencia = codec.string(f, "competencia") ?: return null,
            periodoInicio = codec.string(f, "periodoInicio") ?: return null,
            periodoFim = codec.string(f, "periodoFim") ?: return null,
            turnoPadrao = codec.string(f, "turnoPadrao") ?: "",
            status = status,
            dias = dias,
            totais = totais(totaisFields),
            importacaoId = codec.string(f, "importacaoId"),
            publicadoPor = codec.string(f, "publicadoPor"),
            publicadoEm = codec.string(f, "publicadoEm"),
            atualizadoEm = codec.string(f, "atualizadoEm"),
        )
    }

    private fun dia(fields: JsonObject): DiaRemoteDto = DiaRemoteDto(
        c = codec.string(fields, "c") ?: "",
        i = codec.string(fields, "i"),
        f = codec.string(fields, "f"),
        m = codec.intOrNull(fields, "m"),
        vd = codec.boolOrNull(fields, "vd"),
        seq = codec.intOrNull(fields, "seq"),
    )

    private fun totais(fields: JsonObject): TotaisRemoteDto = TotaisRemoteDto(
        min = codec.int(fields, "min"),
        diasTrabalhados = codec.int(fields, "diasTrabalhados"),
        df = codec.int(fields, "df"),
        du = codec.int(fields, "du"),
        x = codec.int(fields, "x"),
        he = codec.int(fields, "he"),
        bh = codec.int(fields, "bh"),
        an = codec.int(fields, "an"),
        folga = codec.int(fields, "folga"),
        afa = codec.int(fields, "afa"),
    )

    /** Extrai o ultimo segmento de `document.name` (`.../documents/usuarios/{login}`) - o ID real do documento. */
    private fun documentId(document: JsonObject): String? =
        document["name"]?.jsonPrimitive?.content?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
}
