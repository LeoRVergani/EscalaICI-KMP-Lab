package br.com.leorvergani.escalaici.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.ui.theme.LabColors
import escalaici_kmp_lab.composeapp.generated.resources.Res
import escalaici_kmp_lab.composeapp.generated.resources.escala_ici_orbit_mark
import org.jetbrains.compose.resources.painterResource

/**
 * Marca unica "orbita" do Escala ICI (mesma arte do launcher/splash/PWA e do
 * Dashboard, FASE 14M) - nenhum outro simbolo deve representar o app.
 */
@Composable
internal fun LabShieldLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.escala_ici_orbit_mark),
        contentDescription = null,
        modifier = modifier
    )
}

@Composable
internal fun LabAppTitle(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    uppercase: Boolean = false
) {
    val escala = if (uppercase) "ESCALA" else "Escala"
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        LabShieldLogo(modifier = Modifier.size(if (compact) 24.dp else 32.dp))
        Spacer(Modifier.width(if (compact) 7.dp else 10.dp))
        Text(
            text = escala,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = LabColors.onSurface,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "ICI",
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
            color = LabColors.primary,
            fontWeight = FontWeight.Black
        )
    }
}
