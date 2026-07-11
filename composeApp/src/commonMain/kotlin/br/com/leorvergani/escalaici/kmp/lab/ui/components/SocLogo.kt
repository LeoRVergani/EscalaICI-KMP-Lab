package br.com.leorvergani.escalaici.kmp.lab.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors

/**
 * Mini calendário sem texto/letra (mesma paleta/gradiente do escudo antigo,
 * FASE 11.0b) — este app representa o ICI inteiro, não só o SOC, então o
 * logo do cabeçalho não deve mais carregar um "S". Mesmo motivo do ícone
 * do launcher/PWA: header com dois "aneis" de espiral, grade/marcador de
 * "hoje".
 */
@Composable
internal fun LabShieldLogo(
    modifier: Modifier = Modifier,
    accent: Color = LabColors.primary
) {
    Canvas(modifier = modifier.size(32.dp)) {
        val bodyTop = size.height * 0.20f
        val bodyHeight = size.height * 0.74f
        val bodyWidth = size.width * 0.84f
        val bodyLeft = size.width * 0.08f
        val corner = CornerRadius(size.width * 0.14f)
        val gradient = Brush.linearGradient(
            colors = listOf(accent, Color(0xFF4F7DFF), Color(0xFF7C3AED)),
            start = Offset.Zero,
            end = Offset(size.width, size.height)
        )

        drawRoundRect(brush = gradient, topLeft = Offset(bodyLeft, bodyTop), size = Size(bodyWidth, bodyHeight), cornerRadius = corner)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = corner,
            style = Stroke(width = 1.4.dp.toPx())
        )
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(bodyLeft, bodyTop + bodyHeight * 0.22f),
            end = Offset(bodyLeft + bodyWidth, bodyTop + bodyHeight * 0.22f),
            strokeWidth = 1.6.dp.toPx()
        )
        val ringRadius = size.width * 0.045f
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = ringRadius, center = Offset(bodyLeft + bodyWidth * 0.28f, bodyTop))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = ringRadius, center = Offset(bodyLeft + bodyWidth * 0.72f, bodyTop))
        drawRoundRect(
            color = Color.White.copy(alpha = 0.90f),
            topLeft = Offset(bodyLeft + bodyWidth * 0.58f, bodyTop + bodyHeight * 0.42f),
            size = Size(bodyWidth * 0.28f, bodyHeight * 0.28f),
            cornerRadius = CornerRadius(size.width * 0.02f)
        )
    }
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
