package com.ganpati.vargani.core.components.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ganpati.vargani.core.theme.VarganiTheme
import com.ganpati.vargani.core.theme.VarganiThemeExtras
import com.ganpati.vargani.core.utils.CurrencyUtils
import kotlin.math.min

@Composable
fun PieChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    colors: List<Color> = VarganiThemeExtras.extendedColors.chartColors,
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

    val total = entries.sumOf { it.value }.coerceAtLeast(1.0)
    val animatedSweep = 360f * progress.value
    val surfaceColor = MaterialTheme.colorScheme.surface

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            val diameter = min(size.width, size.height)
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f
            var remainingSweep = animatedSweep

            entries.forEachIndexed { index, entry ->
                val sliceSweep = (entry.value / total * 360f).toFloat()
                val drawSweep = min(sliceSweep, remainingSweep.coerceAtLeast(0f))
                if (drawSweep > 0f) {
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = drawSweep,
                        useCenter = true,
                        topLeft = topLeft,
                        size = arcSize,
                    )
                    startAngle += sliceSweep
                    remainingSweep -= drawSweep
                }
            }

            val holeRadius = diameter * 0.28f
            drawCircle(
                color = surfaceColor,
                radius = holeRadius / 2f,
                center = Offset(size.width / 2f, size.height / 2f),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEachIndexed { index, entry ->
                val percentage = (entry.value / total * 100).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        shape = CircleShape,
                        color = colors[index % colors.size],
                    ) {}
                    Column {
                        Text(
                            text = entry.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${CurrencyUtils.format(entry.value)} ($percentage%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PieChartPreview() {
    VarganiTheme {
        PieChart(
            entries = listOf(
                ChartEntry("Cash", 45_200.0),
                ChartEntry("UPI", 78_500.0),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "PieChart — multi slice")
@Composable
private fun PieChartMultiPreview() {
    VarganiTheme {
        PieChart(
            entries = listOf(
                ChartEntry("Suresh", 12000.0),
                ChartEntry("Priya", 8500.0),
                ChartEntry("Amit", 6200.0),
                ChartEntry("Others", 4300.0),
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
