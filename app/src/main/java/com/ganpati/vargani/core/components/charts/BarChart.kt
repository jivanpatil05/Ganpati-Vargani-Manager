package com.ganpati.vargani.core.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ganpati.vargani.core.theme.VarganiTheme
import kotlin.math.max

@Composable
fun BarChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    animationDurationMillis: Int = 900,
) {
    if (entries.isEmpty()) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(entries) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDurationMillis, easing = FastOutSlowInEasing),
        )
    }

    val maxValue = entries.maxOf { it.value }.coerceAtLeast(1.0)
    val animatedProgress = progress.value

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val barCount = entries.size
            val gap = size.width * 0.04f / barCount.coerceAtLeast(1)
            val barWidth = (size.width - gap * (barCount + 1)) / barCount.coerceAtLeast(1)
            val chartHeight = size.height - 24f

            entries.forEachIndexed { index, entry ->
                val barHeight = (entry.value / maxValue * chartHeight * animatedProgress).toFloat()
                val left = gap + index * (barWidth + gap)
                val top = size.height - barHeight - 8f

                drawRoundRect(
                    color = barColor.copy(alpha = 0.85f),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight.coerceAtLeast(0f)),
                    cornerRadius = CornerRadius(8f, 8f),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            entries.forEach { entry ->
                Text(
                    text = entry.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BarChartPreview() {
    VarganiTheme {
        BarChart(
            entries = listOf(
                ChartEntry("Mon", 4200.0),
                ChartEntry("Tue", 6800.0),
                ChartEntry("Wed", 5100.0),
                ChartEntry("Thu", 9200.0),
                ChartEntry("Fri", 7500.0),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "BarChart — empty")
@Composable
private fun BarChartEmptyPreview() {
    VarganiTheme {
        Text("Empty chart renders nothing")
    }
}
