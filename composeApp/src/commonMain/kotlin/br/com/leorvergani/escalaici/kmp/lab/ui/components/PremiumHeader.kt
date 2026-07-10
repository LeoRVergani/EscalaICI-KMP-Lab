package br.com.leorvergani.escalaici.kmp.lab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabShapes

/**
 * Porte literal de `PremiumHeader` (`ui/components/PremiumComponents.kt`,
 * app Android real), presente no topo das 5 abas. Unica adaptacao: o app
 * real le o colaborador selecionado de um coordenador de sessao global
 * (`LoggedScheduleSyncCoordinator`/`ScaleSession`); o laboratorio nao tem
 * esse singleton, entao `selectedCollaborator` vem por parametro.
 */
@Composable
internal fun LabPremiumHeader(
    modifier: Modifier = Modifier,
    selectedCollaborator: String?,
    showNotificationIcon: Boolean = true,
    compact: Boolean = true,
    onNotificationClick: () -> Unit = {},
    onOpenPlantao: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LabAppTitle(compact = compact)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (showNotificationIcon) {
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Outlined.Notifications,
                        contentDescription = "Notificações",
                        tint = LabColors.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (onOpenPlantao != null) {
                PlantaoHeaderButton(onClick = onOpenPlantao)
            }
            SelectedCollaboratorBadge(collaborator = selectedCollaborator)
        }
    }
}

@Composable
private fun PlantaoHeaderButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = LabShapes.chip,
        color = LabColors.surface.copy(alpha = 0.70f),
        border = BorderStroke(1.dp, LabColors.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(Icons.Default.Schedule, contentDescription = null, tint = LabColors.primary, modifier = Modifier.size(15.dp))
            Text("Plantão", color = LabColors.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
