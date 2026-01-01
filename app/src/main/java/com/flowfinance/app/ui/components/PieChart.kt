package com.flowfinance.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.flowfinance.app.data.local.model.CategorySummary

@Composable
fun PieChart(
    data: List<CategorySummary>,
    modifier: Modifier = Modifier,
    radiusOuter: Dp = 90.dp,
    chartBarWidth: Dp = 20.dp,
    animDuration: Int = 1000
) {
    val totalSum = data.sumOf { it.totalAmount }
    val floatValue = mutableListOf<Float>()

    // Calculate angles
    data.forEachIndexed { index, summary ->
        floatValue.add(index, 360 * (summary.totalAmount.toFloat() / totalSum.toFloat()))
    }

    val animation = remember { Animatable(0f) }

    LaunchedEffect(key1 = data) {
        animation.animateTo(1f, animationSpec = tween(durationMillis = animDuration))
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(radiusOuter * 2f)
        ) {
            var lastValue = -90f

            floatValue.forEachIndexed { index, value ->
                drawArc(
                    color = Color(data[index].category.color),
                    startAngle = lastValue,
                    sweepAngle = value * animation.value,
                    useCenter = false,
                    topLeft = Offset(chartBarWidth.toPx() / 2, chartBarWidth.toPx() / 2),
                    size = Size(
                        (size.width - chartBarWidth.toPx()),
                        (size.height - chartBarWidth.toPx())
                    ),
                    style = Stroke(chartBarWidth.toPx())
                )
                lastValue += value
            }
        }
    }
}
