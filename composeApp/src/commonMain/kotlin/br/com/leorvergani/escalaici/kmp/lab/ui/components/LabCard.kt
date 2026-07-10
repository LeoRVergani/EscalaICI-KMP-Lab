package br.com.leorvergani.escalaici.kmp.lab.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.leorvergani.escalaici.kmp.lab.ui.theme.LabColors

@Composable
internal fun HeroCard(
    gradient: List<Color> = listOf(Color(0xFF102A4E), Color(0xFF121D39), Color(0xFF3A0D56)),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFF4B6FA8).copy(alpha = 0.72f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradient))
        ) {
            NocHeroTexture()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content
            )
        }
    }
}

@Composable
private fun NocHeroTexture() {
    Canvas(modifier = Modifier.fillMaxWidth().height(172.dp)) {
        val grid = Color.White.copy(alpha = 0.06f)
        for (x in 0..size.width.toInt() step 42) {
            drawLine(grid, Offset(x.toFloat(), size.height * 0.12f), Offset(x.toFloat() + 72f, size.height), 0.8f)
        }
        for (i in 0..7) {
            val x = size.width * (0.12f + i * 0.11f)
            val y = size.height * (0.30f + (i % 3) * 0.16f)
            drawCircle(Color(0xFF60A5FA).copy(alpha = 0.14f), radius = 4f, center = Offset(x, y))
            drawCircle(Color.White.copy(alpha = 0.07f), radius = 16f, center = Offset(x, y), style = Stroke(width = 1f))
        }
    }
}

@Composable
internal fun LabCard(
    title: String,
    badge: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = LabColors.primary,
    borderColor: Color = LabColors.primary.copy(alpha = 0.38f),
    gradient: List<Color> = listOf(Color(0xFF0C1B2F), Color(0xFF0B1729)),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(gradient))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(iconTint.copy(alpha = 0.16f))
                            .border(1.dp, iconTint.copy(alpha = 0.28f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
                    }
                }
                Text(title, modifier = Modifier.weight(1f), color = LabColors.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                if (badge != null) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LabColors.primary.copy(alpha = 0.16f)).border(1.dp, LabColors.primary.copy(alpha = 0.32f), RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(badge, color = LabColors.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            content()
        }
    }
}
