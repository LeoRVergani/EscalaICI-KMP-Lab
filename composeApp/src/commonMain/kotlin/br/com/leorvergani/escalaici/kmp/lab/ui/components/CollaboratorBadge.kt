package br.com.leorvergani.escalaici.kmp.lab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors

/**
 * Porte literal de `CollaboratorAvatar`/`SelectedCollaboratorBadge`
 * (`ui/components/CollaboratorComponents.kt`, app Android real).
 */
@Composable
internal fun LabCollaboratorAvatar(initials: String, modifier: Modifier = Modifier.size(26.dp)) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        LabColors.primary.copy(alpha = 0.78f),
                        Color(0xFF1D4ED8).copy(alpha = 0.72f),
                        Color(0xFF6D28D9).copy(alpha = 0.56f)
                    )
                )
            )
            .border(1.dp, LabColors.primary.copy(alpha = 0.24f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(initials.ifBlank { "AS" }, color = LabColors.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun SelectedCollaboratorBadge(
    collaborator: String?,
    modifier: Modifier = Modifier
) {
    val name = collaborator.orEmpty().ifBlank { "Perfil" }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        LabCollaboratorAvatar(initials = name.badgeInitials(), modifier = Modifier.size(28.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = LabColors.onSurface,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun String?.badgeInitials(): String {
    val clean = this?.trim().orEmpty()
    if (clean.isBlank()) return "AS"
    val parts = clean.split('.', '_', '-', ' ').filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        parts.take(2).joinToString("") { it.first().uppercaseChar().toString() }
    } else {
        clean.take(2).uppercase()
    }
}
