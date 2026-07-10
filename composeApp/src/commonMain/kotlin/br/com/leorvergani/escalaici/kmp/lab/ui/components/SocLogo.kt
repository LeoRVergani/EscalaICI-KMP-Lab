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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors

/**
 * Porte literal do shield in-app (`SocShieldLogo`/`SocAppTitle`, app Android
 * real, `ui/components/SocLogo.kt`). Path fracionario do proprio `size`,
 * gradiente azul->roxo, "S" tracado e linha diagonal — os mesmos valores do
 * app real, sem nenhum path/hex diferente.
 */
@Composable
internal fun LabShieldLogo(
    modifier: Modifier = Modifier,
    accent: Color = LabColors.primary
) {
    Canvas(modifier = modifier.size(32.dp)) {
        val shield = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.04f)
            lineTo(size.width * 0.88f, size.height * 0.18f)
            lineTo(size.width * 0.82f, size.height * 0.66f)
            quadraticTo(size.width * 0.5f, size.height * 0.96f, size.width * 0.18f, size.height * 0.66f)
            lineTo(size.width * 0.12f, size.height * 0.18f)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.linearGradient(
                colors = listOf(accent, Color(0xFF4F7DFF), Color(0xFF7C3AED)),
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            )
        )
        drawPath(path = shield, color = Color.White.copy(alpha = 0.20f), style = Stroke(width = 1.4.dp.toPx()))

        val mark = Path().apply {
            moveTo(size.width * 0.66f, size.height * 0.23f)
            cubicTo(size.width * 0.40f, size.height * 0.22f, size.width * 0.32f, size.height * 0.35f, size.width * 0.52f, size.height * 0.45f)
            cubicTo(size.width * 0.74f, size.height * 0.56f, size.width * 0.60f, size.height * 0.74f, size.width * 0.30f, size.height * 0.72f)
        }
        drawPath(path = mark, color = Color.White.copy(alpha = 0.86f), style = Stroke(width = 3.dp.toPx()))
        drawLine(
            color = Color.White.copy(alpha = 0.35f),
            start = Offset(size.width * 0.30f, size.height * 0.82f),
            end = Offset(size.width * 0.70f, size.height * 0.30f),
            strokeWidth = 1.2.dp.toPx()
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
