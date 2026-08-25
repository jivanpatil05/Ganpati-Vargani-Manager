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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ganpati.vargani.core.theme.VarganiTheme
import com.ganpati.vargani.core.utils.CurrencyUtils
import kotlin.math.max

@Composable
fun LineChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
    animationDurationMillis: Int = 1000,
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
    val visiblePointCount = max(1, (entries.size * animatedProgress).toInt())

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val paddingH = 16f
            val paddingV = 16f
            val chartWidth = size.width - paddingH * 2
            val chartHeight = size.height - paddingV * 2
            val stepX = if (entries.size <= 1) 0f else chartWidth / (entries.size - 1)

            val points = entries.mapIndexed { index, entry ->
                val x = paddingH + index * stepX
                val y = paddingV + chartHeight - (entry.value / maxValue * chartHeight).toFloat()
                Offset(x, y)
            }

            val visiblePoints = points.take(visiblePointCount)
            if (visiblePoints.size >= 2) {
                val linePath = Path().apply {
                    moveTo(visiblePoints.first().x, visiblePoints.first().y)
                    visiblePoints.drop(1).forEach { lineTo(it.x, it.y) }
                }

                val fillPath = Path().apply {
                    moveTo(visiblePoints.first().x, paddingV + chartHeight)
                    lineTo(visiblePoints.first().x, visiblePoints.first().y)
                    visiblePoints.drop(1).forEach { lineTo(it.x, it.y) }
                    lineTo(visiblePoints.last().x, paddingV + chartHeight)
                    close()
                }

                drawPath(path = fillPath, color = fillColor)
                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round),
                )
            }

            visiblePoints.forEach { point ->
                drawCircle(
                    color = lineColor,
                    radius = 6f,
                    center = point,
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = point,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            entries.forEach { entry ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = entry.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = CurrencyUtils.formatCompact(entry.value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LineChartPreview() {
    VarganiTheme {
        LineChart(
            entries = listOf(
                ChartEntry("1 Aug", 3200.0),
                ChartEntry("5 Aug", 5400.0),
                ChartEntry("10 Aug", 4100.0),
                ChartEntry("15 Aug", 8900.0),
                ChartEntry("20 Aug", 6200.0),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
