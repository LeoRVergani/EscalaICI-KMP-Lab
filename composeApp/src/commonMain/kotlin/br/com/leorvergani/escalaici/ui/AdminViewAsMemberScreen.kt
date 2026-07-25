package br.com.leorvergani.escalaici.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.ui.components.LabCard
import br.com.leorvergani.escalaici.ui.theme.LabColors

/**
 * FASE 14J.1 (spec 68) - seletor "Visualizar como colaborador": lista só os colaboradores da
 * equipe/publicação já carregada (nunca outro workspace/revisão/período), busca por nome/login/
 * equipe, ordenada alfabeticamente. Nenhum e-mail completo é mostrado aqui.
 */
@Composable
internal fun AdminViewAsMemberScreen(
    collaborators: List<ViewableCollaborator>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    teamName: String,
    periodLabel: String,
    onSelect: (ViewableCollaborator) -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = LabColors.onSurface)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Visualizar como colaborador",
                    style = MaterialTheme.typography.titleLarge,
                    color = LabColors.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        item {
            Text(
                "Equipe: $teamName · Período: $periodLabel",
                color = LabColors.onSurfaceMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                label = { Text("Pesquisar nome ou login") }
            )
        }
        if (collaborators.isEmpty()) {
            item {
                LabCard(borderColor = LabColors.outline.copy(alpha = 0.30f)) {
                    Text(
                        "Nenhum colaborador ativo encontrado nesta equipe/período.",
                        color = LabColors.onSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            items(collaborators, key = { it.memberId }) { collaborator ->
                CollaboratorOptionCard(collaborator = collaborator, onClick = { onSelect(collaborator) })
            }
        }
    }
}

@Composable
private fun CollaboratorOptionCard(collaborator: ViewableCollaborator, onClick: () -> Unit) {
    LabCard(borderColor = LabColors.primary.copy(alpha = 0.24f)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        collaborator.displayName,
                        color = LabColors.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        collaborator.teamName,
                        color = LabColors.onSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Banner persistente exigido enquanto uma persona estiver sendo visualizada (spec 68) - aparece
 * em Hoje/Escala/Plantão/Alertas/Perfil/Trocas. Nunca deixa dúvida sobre qual conta está
 * realmente autenticada.
 */
@Composable
internal fun ViewAsBanner(
    authenticatedDisplayName: String,
    viewedDisplayName: String,
    onExit: () -> Unit
) {
    LabCard(
        borderColor = LabColors.tertiary.copy(alpha = 0.45f),
        icon = Icons.Filled.SupervisorAccount,
        iconTint = LabColors.tertiary
    ) {
        Text(
            "Modo de visualização administrativa",
            color = LabColors.tertiary,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Você está visualizando a escala de $viewedDisplayName. Sua conta continua sendo $authenticatedDisplayName.",
            color = LabColors.onSurfaceMuted,
            style = MaterialTheme.typography.bodySmall
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExit) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.width(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Voltar para minha visualização")
            }
        }
    }
}
