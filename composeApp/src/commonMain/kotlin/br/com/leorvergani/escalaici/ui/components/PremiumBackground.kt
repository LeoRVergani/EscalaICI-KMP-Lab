package br.com.leorvergani.escalaici.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
internal fun LabPremiumBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050910))
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.26f), Color.Transparent),
                    center = Offset(130f, 0f),
                    radius = 780f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF581C87).copy(alpha = 0.24f), Color.Transparent),
                    center = Offset(900f, 420f),
                    radius = 860f
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF0B1221).copy(alpha = 0.76f), Color(0xFF050910)),
                    start = Offset.Zero,
                    end = Offset(0f, 1600f)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dotColor = Color(0xFF93C5FD).copy(alpha = 0.050f)
            val lineColor = Color(0xFF60A5FA).copy(alpha = 0.060f)

            for (x in 24 until size.width.toInt() step 54) {
                for (y in 32 until size.height.toInt() step 74) {
                    val offset = ((x + y) % 19).toFloat()
                    drawCircle(dotColor, radius = 1.05f, center = Offset(x + offset, y.toFloat()))
                }
            }

            drawLine(lineColor, Offset(size.width * 0.10f, size.height * 0.18f), Offset(size.width * 0.86f, size.height * 0.08f), 1f)
            drawLine(lineColor, Offset(size.width * 0.20f, size.height * 0.72f), Offset(size.width * 0.92f, size.height * 0.58f), 1f)
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * 0.90f, size.height * 0.08f),
                style = Stroke(width = 1.3f)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            content = content
        )
    }
}
