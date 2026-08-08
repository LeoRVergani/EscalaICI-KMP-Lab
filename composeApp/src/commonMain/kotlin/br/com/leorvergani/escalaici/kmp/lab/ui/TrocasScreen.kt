package br.com.leorvergani.escalaici.kmp.lab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.firebase.EscalaIciException
import br.com.leorvergani.escalaici.kmp.lab.firebase.JornadaDia
import br.com.leorvergani.escalaici.kmp.lab.firebase.LIMITE_MENSAGEM_TROCA
import br.com.leorvergani.escalaici.kmp.lab.firebase.ROTULO_STATUS_TROCA
import br.com.leorvergani.escalaici.kmp.lab.firebase.SEVERIDADE_STATUS_TROCA
import br.com.leorvergani.escalaici.kmp.lab.firebase.SeveridadeStatusTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.TeamScheduleSnapshot
import br.com.leorvergani.escalaici.kmp.lab.firebase.TrocasBadge
import br.com.leorvergani.escalaici.kmp.lab.firebase.TrocasSession
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.NotificacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SolicitacaoTrocaRealDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.model.LabDate
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabCard
import br.com.leorvergani.escalaici.kmp.lab.ui.components.LabPremiumHeader
import br.com.leorvergani.escalaici.kmp.lab.ui.components.PageList
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes
import kotlinx.coroutines.launch

private enum class TrocasAba(val label: String) {
    MINHAS("Minhas"),
    PARA_RESPONDER("Para responder"),
    HISTORICO("Histórico"),
}

private val STATUS_HISTORICO = setOf(
    StatusTroca.RECUSADA_USUARIO,
    StatusTroca.CANCELADA_SOLICITANTE,
    StatusTroca.RECUSADA_GESTOR,
    StatusTroca.APROVADA_PUBLICADA,
    StatusTroca.EXPIRADA,
)

/**
 * Tela principal de Trocas (spec FASE 16, seções 12-15) - substitui
 * `ShiftSwapScreen`/o mock no fluxo normal. Sem aba Gestor (decisão
 * documentada no plano da fase: nem o Employee App real do Escala-ICI tem
 * aprovação nessa tela, só o Dashboard).
 */
@Composable
internal fun TrocasScreen(
    trocasSession: TrocasSession,
    selectedCollaborator: String,
    loginAtual: String,
    today: LabDate,
    onOpenPlantao: () -> Unit,
    onRefresh: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    onBadgeChanged: (TrocasBadge) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var aba by remember { mutableStateOf(TrocasAba.MINHAS) }
    var trocas by remember { mutableStateOf<List<SolicitacaoTrocaRealDto>>(emptyList()) }
    var notificacoes by remember { mutableStateOf<List<NotificacaoTrocaDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var novaSolicitacaoAberta by remember { mutableStateOf(false) }
    var trocaSelecionada by remember { mutableStateOf<SolicitacaoTrocaRealDto?>(null) }

    suspend fun carregar() {
        loading = true
        errorMessage = null
        try {
            trocas = trocasSession.minhasTrocas()
            notificacoes = trocasSession.notificacoes()
            onBadgeChanged(
                TrocasBadge(
                    paraResponder = trocas.count { it.destinatarioLogin == loginAtual && it.status == StatusTroca.PENDENTE_USUARIO },
                    naoLidas = notificacoes.count { it.lidaEm == null },
                ),
            )
        } catch (e: EscalaIciException) {
            errorMessage = e.message
        } catch (e: Exception) {
            errorMessage = "Não foi possível carregar as trocas agora."
        }
        loading = false
    }

    LaunchedEffect(Unit) { carregar() }

    if (novaSolicitacaoAberta) {
        NovaSolicitacaoTrocaWizard(
            trocasSession = trocasSession,
            loginAtual = loginAtual,
            onCancel = { novaSolicitacaoAberta = false },
            onSubmitted = {
                novaSolicitacaoAberta = false
                feedback = "Solicitação enviada."
                scope.launch { carregar() }
            },
        )
        return
    }

    val paraResponderCount = trocas.count { it.destinatarioLogin == loginAtual && it.status == StatusTroca.PENDENTE_USUARIO }

    PageList {
        item {
            LabPremiumHeader(selectedCollaborator = selectedCollaborator, onOpenPlantao = onOpenPlantao, onRefresh = onRefresh, isRefreshing = isRefreshing)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Trocas de escala", color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Button(onClick = { novaSolicitacaoAberta = true }, colors = ButtonDefaults.buttonColors(containerColor = LabColors.primary)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("Nova solicitação")
                }
            }
        }
        item {
            TrocasAbaRow(selected = aba, onSelect = { aba = it }, paraResponderCount = paraResponderCount)
        }
        feedback?.let { message ->
            item { InlineBanner(message = message, color = LabColors.tertiary, onDismiss = { feedback = null }) }
        }
        errorMessage?.let { message ->
            item { InlineBanner(message = message, color = LabColors.red, onDismiss = { errorMessage = null }) }
        }
        if (loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LabColors.primary)
                }
            }
        } else {
            val filtradas = when (aba) {
                TrocasAba.MINHAS -> trocas.filter { it.solicitanteLogin == loginAtual }.sortedByDescending { it.atualizadoEm }
                TrocasAba.PARA_RESPONDER -> trocas.filter { it.destinatarioLogin == loginAtual && it.status == StatusTroca.PENDENTE_USUARIO }.sortedByDescending { it.atualizadoEm }
                TrocasAba.HISTORICO -> trocas.filter { it.status in STATUS_HISTORICO }.sortedByDescending { it.atualizadoEm }
            }
            if (filtradas.isEmpty()) {
                item {
                    LabCard(borderColor = LabColors.outline.copy(alpha = 0.30f)) {
                        Text(mensagemVazia(aba), color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                items(filtradas, key = { it.trocaId }) { troca ->
                    TrocaCard(troca = troca, loginAtual = loginAtual, onClick = { trocaSelecionada = troca })
                }
            }
        }
    }

    trocaSelecionada?.let { troca ->
        TrocaDetalheSheet(
            troca = troca,
            loginAtual = loginAtual,
            onDismiss = { trocaSelecionada = null },
            onAceitar = {
                scope.launch {
                    runCatching { trocasSession.responder(troca.trocaId, aceitar = true) }
                        .onSuccess { trocaSelecionada = null; feedback = "Troca aceita — aguardando o gestor."; carregar() }
                        .onFailure { errorMessage = it.mensagemAmigavel() }
                }
            },
            onRecusar = { motivo ->
                scope.launch {
                    runCatching { trocasSession.responder(troca.trocaId, aceitar = false, motivoRecusa = motivo) }
                        .onSuccess { trocaSelecionada = null; feedback = "Troca recusada."; carregar() }
                        .onFailure { errorMessage = it.mensagemAmigavel() }
                }
            },
            onCancelar = {
                scope.launch {
                    runCatching { trocasSession.cancelar(troca.trocaId) }
                        .onSuccess { trocaSelecionada = null; feedback = "Solicitação cancelada."; carregar() }
                        .onFailure { errorMessage = it.mensagemAmigavel() }
                }
            },
        )
    }
}

private fun Throwable.mensagemAmigavel(): String = (this as? EscalaIciException)?.message ?: message ?: "Não foi possível concluir a ação."

private fun mensagemVazia(aba: TrocasAba): String = when (aba) {
    TrocasAba.MINHAS -> "Você ainda não criou nenhuma solicitação de troca."
    TrocasAba.PARA_RESPONDER -> "Nenhuma solicitação aguardando sua resposta."
    TrocasAba.HISTORICO -> "Nenhuma troca concluída ainda."
}

@Composable
private fun InlineBanner(message: String, color: Color, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(LabShapes.cardSmall)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.32f), LabShapes.cardSmall)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(message, modifier = Modifier.weight(1f), color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TrocasAbaRow(selected: TrocasAba, onSelect: (TrocasAba) -> Unit, paraResponderCount: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TrocasAba.entries.forEach { aba ->
            val active = aba == selected
            Row(
                modifier = Modifier
                    .clip(LabShapes.chip)
                    .background(LabColors.primary.copy(alpha = if (active) 0.20f else 0.08f))
                    .border(1.dp, LabColors.primary.copy(alpha = if (active) 0.48f else 0.22f), LabShapes.chip)
                    .clickable { onSelect(aba) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    aba.label,
                    color = if (active) LabColors.primary else LabColors.onSurfaceMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                )
                if (aba == TrocasAba.PARA_RESPONDER && paraResponderCount > 0) {
                    Box(modifier = Modifier.clip(LabShapes.chip).background(LabColors.red.copy(alpha = 0.85f)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text("$paraResponderCount", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrocaCard(troca: SolicitacaoTrocaRealDto, loginAtual: String, onClick: () -> Unit) {
    val outroNome = if (troca.solicitanteLogin == loginAtual) troca.destinatarioNome else troca.solicitanteNome
    val dataLabel = LabDate.parseIso(troca.data)?.periodToken() ?: troca.data
    LabCard(borderColor = LabColors.primary.copy(alpha = 0.24f)) {
        Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Você e $outroNome", color = LabColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$dataLabel · ${troca.turnoSolicitanteAntes} ⇄ ${troca.turnoDestinatarioAntes}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            StatusChip(status = troca.status)
        }
    }
}

@Composable
private fun StatusChip(status: StatusTroca) {
    val color = status.corSeveridade()
    Box(modifier = Modifier.clip(LabShapes.chip).background(color.copy(alpha = 0.15f)).border(1.dp, color.copy(alpha = 0.34f), LabShapes.chip).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(ROTULO_STATUS_TROCA[status] ?: status.name, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

private fun StatusTroca.corSeveridade(): Color = when (SEVERIDADE_STATUS_TROCA[this]) {
    SeveridadeStatusTroca.SUCCESS -> LabColors.tertiary
    SeveridadeStatusTroca.WARNING -> LabColors.yellow
    SeveridadeStatusTroca.DANGER -> LabColors.red
    SeveridadeStatusTroca.NEUTRAL, null -> LabColors.onSurfaceMuted
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrocaDetalheSheet(
    troca: SolicitacaoTrocaRealDto,
    loginAtual: String,
    onDismiss: () -> Unit,
    onAceitar: () -> Unit,
    onRecusar: (String?) -> Unit,
    onCancelar: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var motivoRecusa by remember { mutableStateOf("") }
    var mostrandoRecusa by remember { mutableStateOf(false) }
    val souDestinatarioPendente = troca.destinatarioLogin == loginAtual && troca.status == StatusTroca.PENDENTE_USUARIO
    val souSolicitantePendente = troca.solicitanteLogin == loginAtual && troca.status == StatusTroca.PENDENTE_USUARIO
    val dataLabel = LabDate.parseIso(troca.data)?.fullDateLabel() ?: troca.data

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = LabColors.surface) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Detalhe da troca", color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatusChip(status = troca.status)
            Text(dataLabel, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            LabCard(borderColor = LabColors.outline.copy(alpha = 0.30f)) {
                Text("Solicitante: ${troca.solicitanteNome}", color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text("Turno atual: ${troca.turnoSolicitanteAntes} · ${troca.horarioSolicitanteAntes}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.size(6.dp))
                Text("Colega: ${troca.destinatarioNome}", color = LabColors.onSurface, style = MaterialTheme.typography.bodyMedium)
                Text("Turno atual: ${troca.turnoDestinatarioAntes} · ${troca.horarioDestinatarioAntes}", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
            troca.mensagemSolicitante?.let { mensagem ->
                Text("Mensagem: $mensagem", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
            troca.motivoRecusa?.let { motivo ->
                Text("Motivo da recusa: $motivo", color = LabColors.red, style = MaterialTheme.typography.bodySmall)
            }
            when {
                souDestinatarioPendente && mostrandoRecusa -> {
                    OutlinedTextField(
                        value = motivoRecusa,
                        onValueChange = { if (it.length <= LIMITE_MENSAGEM_TROCA) motivoRecusa = it },
                        label = { Text("Motivo (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onRecusar(motivoRecusa.trim().ifBlank { null }) }, colors = ButtonDefaults.buttonColors(containerColor = LabColors.red)) {
                            Text("Confirmar recusa")
                        }
                        TextButton(onClick = { mostrandoRecusa = false }) { Text("Voltar", color = LabColors.onSurfaceMuted) }
                    }
                }
                souDestinatarioPendente -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onAceitar, colors = ButtonDefaults.buttonColors(containerColor = LabColors.primary)) { Text("Aceitar") }
                        TextButton(onClick = { mostrandoRecusa = true }) { Text("Recusar", color = LabColors.red) }
                    }
                }
                souSolicitantePendente -> {
                    TextButton(onClick = onCancelar) { Text("Cancelar solicitação", color = LabColors.red) }
                }
                else -> Unit
            }
            Spacer(Modifier.size(4.dp))
        }
    }
}

/**
 * Passo a passo "Nova solicitação" (spec seções 16-18): dia → colega →
 * confirmar. Reaproveita o mesmo [TeamScheduleSnapshot] nos três passos -
 * uma única busca ao abrir o assistente, nunca uma nova consulta por
 * clique (ajuste obrigatório da FASE 16).
 */
@Composable
private fun NovaSolicitacaoTrocaWizard(
    trocasSession: TrocasSession,
    loginAtual: String,
    onCancel: () -> Unit,
    onSubmitted: () -> Unit,
) {
    var passo by remember { mutableStateOf(1) }
    var snapshot by remember { mutableStateOf<TeamScheduleSnapshot?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var dataEscolhida by remember { mutableStateOf<String?>(null) }
    var colegaEscolhido by remember { mutableStateOf<UsuarioRemoteDto?>(null) }
    var mensagem by remember { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            snapshot = trocasSession.teamSnapshot()
        } catch (e: EscalaIciException) {
            loadError = e.message
        } catch (e: Exception) {
            loadError = "Não foi possível carregar sua equipe agora."
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (passo > 1) passo-- else onCancel() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = LabColors.onSurface)
            }
            Spacer(Modifier.size(4.dp))
            Text("PASSO $passo DE 3", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }

        val currentSnapshot = snapshot
        when {
            loadError != null -> LabCard(borderColor = LabColors.red.copy(alpha = 0.4f)) {
                Text(loadError.orEmpty(), color = LabColors.red, style = MaterialTheme.typography.bodySmall)
            }
            currentSnapshot == null -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LabColors.primary)
            }
            passo == 1 -> PassoEscolherDia(
                snapshot = currentSnapshot,
                loginAtual = loginAtual,
                onEscolher = { data -> dataEscolhida = data; passo = 2 },
            )
            passo == 2 && dataEscolhida != null -> PassoEscolherColega(
                snapshot = currentSnapshot,
                loginAtual = loginAtual,
                data = dataEscolhida!!,
                onEscolher = { colega -> colegaEscolhido = colega; passo = 3 },
            )
            passo == 3 && dataEscolhida != null && colegaEscolhido != null -> PassoConfirmar(
                snapshot = currentSnapshot,
                loginAtual = loginAtual,
                data = dataEscolhida!!,
                colega = colegaEscolhido!!,
                mensagem = mensagem,
                onMensagemChange = { if (it.length <= LIMITE_MENSAGEM_TROCA) mensagem = it },
                enviando = enviando,
                errorMessage = submitError,
                onEnviar = {
                    val colega = colegaEscolhido ?: return@PassoConfirmar
                    val data = dataEscolhida ?: return@PassoConfirmar
                    enviando = true
                    submitError = null
                    scope.launch {
                        runCatching {
                            trocasSession.criarSolicitacao(
                                snapshot = currentSnapshot,
                                data = data,
                                destinatarioLogin = colega.login,
                                destinatarioNome = colega.nome,
                                destinatarioAtivo = colega.ativo,
                                mensagem = mensagem,
                            )
                        }.onSuccess {
                            enviando = false
                            onSubmitted()
                        }.onFailure {
                            enviando = false
                            submitError = it.mensagemAmigavel()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun PassoEscolherDia(snapshot: TeamScheduleSnapshot, loginAtual: String, onEscolher: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Escolha o seu dia", color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val dias = remember(snapshot, loginAtual) { diasElegiveisDoUsuario(snapshot, loginAtual) }
        if (dias.isEmpty()) {
            Text("Nenhum dia elegível encontrado na sua escala publicada.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dias, key = { it.first }) { (data, jornada) ->
                DiaOuColegaOption(
                    titulo = LabDate.parseIso(data)?.let { "${it.dayOfWeekShort()} ${it.periodToken()}" } ?: data,
                    subtitulo = "${jornada.descricao} · ${jornada.horario}",
                    codigo = jornada.codigo,
                    onClick = { onEscolher(data) },
                )
            }
        }
    }
}

/** Dias em que `loginAtual` trabalha na escala PUBLICADA atual (spec seção 16) - a própria `TurnosMes` do usuário, já dentro do [snapshot] compartilhado. */
private fun diasElegiveisDoUsuario(snapshot: TeamScheduleSnapshot, loginAtual: String): List<Pair<String, JornadaDia>> {
    val meuDocumento = snapshot.turnosMesPublicadas.firstOrNull { it.login == loginAtual } ?: return emptyList()
    return meuDocumento.dias.keys.sorted().mapNotNull { data ->
        val jornada = snapshot.jornadaDoDia(loginAtual, data)
        if (jornada.trabalha) data to jornada else null
    }
}

@Composable
private fun PassoEscolherColega(snapshot: TeamScheduleSnapshot, loginAtual: String, data: String, onEscolher: (UsuarioRemoteDto) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Escolha o colega", color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        val colegas = remember(snapshot, loginAtual, data) { snapshot.colegasNoDia(loginAtual, data) }
        if (colegas.isEmpty()) {
            Text("Ninguém mais está escalado nesse dia.", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colegas, key = { it.first.login }) { (usuario, jornada) ->
                DiaOuColegaOption(
                    titulo = usuario.nome,
                    subtitulo = "${jornada.descricao} · ${jornada.horario}",
                    codigo = jornada.codigo,
                    onClick = { onEscolher(usuario) },
                )
            }
        }
    }
}

@Composable
private fun PassoConfirmar(
    snapshot: TeamScheduleSnapshot,
    loginAtual: String,
    data: String,
    colega: UsuarioRemoteDto,
    mensagem: String,
    onMensagemChange: (String) -> Unit,
    enviando: Boolean,
    errorMessage: String?,
    onEnviar: () -> Unit,
) {
    val minhaJornada = snapshot.jornadaDoDia(loginAtual, data)
    val jornadaColega = snapshot.jornadaDoDia(colega.login, data)
    val dataLabel = LabDate.parseIso(data)?.periodToken() ?: data

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Confirmar solicitação", color = LabColors.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        LabCard(borderColor = LabColors.primary.copy(alpha = 0.30f)) {
            Text(
                "Você troca ${minhaJornada.descricao} ($dataLabel) com ${colega.nome}, que está em ${jornadaColega.descricao}.",
                color = LabColors.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        OutlinedTextField(
            value = mensagem,
            onValueChange = onMensagemChange,
            label = { Text("Mensagem (opcional)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("${mensagem.length} / $LIMITE_MENSAGEM_TROCA caracteres", color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.labelSmall)
        errorMessage?.let { Text(it, color = LabColors.red, style = MaterialTheme.typography.bodySmall) }
        Button(
            onClick = onEnviar,
            enabled = !enviando,
            colors = ButtonDefaults.buttonColors(containerColor = LabColors.primary),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (enviando) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Enviar solicitação")
            }
        }
    }
}

@Composable
private fun DiaOuColegaOption(titulo: String, subtitulo: String, codigo: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = LabShapes.cardSmall,
        color = LabColors.surfaceElevated.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, LabColors.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, color = LabColors.onSurface, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitulo, color = LabColors.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
            }
            Box(modifier = Modifier.clip(LabShapes.chip).background(LabColors.primary.copy(alpha = 0.16f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text(codigo, color = LabColors.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
