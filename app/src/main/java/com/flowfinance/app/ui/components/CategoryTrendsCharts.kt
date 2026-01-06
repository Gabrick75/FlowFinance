package com.flowfinance.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.flowfinance.app.data.local.model.CategorySummary
import com.flowfinance.app.ui.viewmodel.MonthlyExpenseData
import com.flowfinance.app.util.formatCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CategoryPieChart(
    data: List<CategorySummary>,
    currency: String,
    modifier: Modifier = Modifier,
    radiusOuter: Dp = 90.dp,
    chartBarWidth: Dp = 20.dp,
    animDuration: Int = 1000
) {
    if (data.isEmpty()) return

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

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(radiusOuter * 2f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
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
            Text(
                text = formatCurrency(totalSum, currency),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Legend
        Column {
            data.forEach { summary ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(summary.category.color), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = summary.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${formatCurrency(summary.totalAmount, currency)} (${String.format("%.1f", (summary.totalAmount / totalSum) * 100)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHorizontalBarChart(
    data: List<CategorySummary>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val maxVal = data.maxOf { it.totalAmount }

    Column(modifier = modifier) {
        data.forEach { summary ->
            val progress = (summary.totalAmount / maxVal).toFloat()
            
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = summary.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatCurrency(summary.totalAmount, currency),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Color(summary.category.color), RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryTrendsLineChart(
    data: List<MonthlyExpenseData>,
    categories: List<com.flowfinance.app.data.local.entity.Category>,
    currency: String,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = false
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    
    // Find max value across all categories and months
    val maxVal = data.flatMap { it.expensesByCategory.values }.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f
    
    var selectedPoint by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    var chartSize by remember { mutableStateOf(Size.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    val density = LocalDensity.current
    val leftPadding = with(density) { 60.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { chartSize = it.toSize() }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            val chartWidth = chartSize.width - leftPadding
                            val maxScroll = (chartWidth * scale) - chartWidth
                            offsetX = (offsetX + pan.x).coerceIn(-maxScroll, 0f)
                        }
                    }
                }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTapGestures { offset ->
                            val index = getIndexFromTap(offset, chartSize.width, leftPadding, data.size, scale, offsetX)
                            selectedPoint = if (index != null && (selectedPoint?.first != index)) {
                                index to offset
                            } else {
                                null
                            }
                        }
                    }
                }
        ) {
            val bottomPadding = 20.dp.toPx()
            val chartWidth = size.width - leftPadding
            val chartHeight = size.height - bottomPadding

            drawStandardChartGrid(textMeasurer, maxVal, data, leftPadding, bottomPadding, currency, scale, offsetX)

            clipRect(left = leftPadding, top = 0f, right = size.width, bottom = chartHeight) {
                categories.forEach { category ->
                    val color = Color(category.color)
                    val values = data.map { it.expensesByCategory[category.name]?.toFloat() ?: 0f }
                    
                    if (values.isNotEmpty()) {
                        val path = Path()
                        val stepX = (chartWidth / data.size) * scale
                        
                        val startX = leftPadding + offsetX + stepX / 2
                        val startY = chartHeight - (values[0] / maxVal * chartHeight)
                        path.moveTo(startX, startY)
                        
                        for (i in 1 until values.size) {
                            val x = leftPadding + offsetX + i * stepX + stepX / 2
                            val y = chartHeight - (values[i] / maxVal * chartHeight)
                            path.lineTo(x, y)
                        }
                        drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }
        }
        
        if (selectedPoint != null && showTooltip) {
            val (index, tapOffset) = selectedPoint!!
            val item = data[index]
            val dateFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))
            val dateStr = item.yearMonth.format(dateFormatter)
            
            val chartWidth = chartSize.width - leftPadding
            val stepX = (chartWidth / data.size) * scale
            val xPos = leftPadding + offsetX + index * stepX + stepX / 2
            
            if (xPos >= leftPadding && xPos <= chartSize.width) {
                ChartTooltip(
                    title = dateStr,
                    content = {
                        categories.forEach { category ->
                            val amount = item.expensesByCategory[category.name] ?: 0.0
                            if (amount > 0) {
                                Text(
                                    "${category.name}: ${formatCurrency(amount, currency)}", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = Color(category.color)
                                )
                            }
                        }
                    },
                    targetPosition = Offset(xPos, tapOffset.y),
                    containerSize = chartSize,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

@Composable
fun CategoryStackedAreaChart(
    data: List<MonthlyExpenseData>,
    categories: List<com.flowfinance.app.data.local.entity.Category>,
    currency: String,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = false
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    
    // Find max total expenses across months
    val maxVal = data.maxOf { it.totalExpenses }.toFloat().coerceAtLeast(1f)
    
    var selectedPoint by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    var chartSize by remember { mutableStateOf(Size.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val leftPadding = with(density) { 60.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { chartSize = it.toSize() }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            val chartWidth = chartSize.width - leftPadding
                            val maxScroll = (chartWidth * scale) - chartWidth
                            offsetX = (offsetX + pan.x).coerceIn(-maxScroll, 0f)
                        }
                    }
                }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTapGestures { offset ->
                            val index = getIndexFromTap(offset, chartSize.width, leftPadding, data.size, scale, offsetX)
                            selectedPoint = if (index != null && (selectedPoint?.first != index)) {
                                index to offset
                            } else {
                                null
                            }
                        }
                    }
                }
        ) {
            val bottomPadding = 20.dp.toPx()
            val chartWidth = size.width - leftPadding
            val chartHeight = size.height - bottomPadding

            drawStandardChartGrid(textMeasurer, maxVal, data, leftPadding, bottomPadding, currency, scale, offsetX)

            clipRect(left = leftPadding, top = 0f, right = size.width, bottom = chartHeight) {
                val stepX = (chartWidth / data.size) * scale
                
                // We need to stack the categories.
                // For each X (month), calculate Y start for each category
                
                // Pre-calculate points for each category
                val stackedPoints = Array(categories.size) { FloatArray(data.size) } // [CategoryIndex][MonthIndex] = Y top position
                val basePoints = FloatArray(data.size) { chartHeight } // Starts at bottom
                
                categories.forEachIndexed { catIndex, category ->
                    data.forEachIndexed { monthIndex, item ->
                        val amount = item.expensesByCategory[category.name]?.toFloat() ?: 0f
                        val height = (amount / maxVal) * chartHeight
                        val currentBase = if (catIndex == 0) chartHeight else stackedPoints[catIndex - 1][monthIndex]
                        stackedPoints[catIndex][monthIndex] = currentBase - height
                    }
                }
                
                // Draw areas from back to front or front to back? 
                // We should draw each category area defined by its top line and the top line of the previous category
                
                categories.forEachIndexed { catIndex, category ->
                    val color = Color(category.color)
                    val path = Path()
                    
                    // Start from first point top
                    val startX = leftPadding + offsetX + stepX / 2
                    path.moveTo(startX, stackedPoints[catIndex][0])
                    
                    // Top line
                    for (i in 1 until data.size) {
                        val x = leftPadding + offsetX + i * stepX + stepX / 2
                        path.lineTo(x, stackedPoints[catIndex][i])
                    }
                    
                    // Down to bottom (or previous category top) at the last X
                    val endX = leftPadding + offsetX + (data.size - 1) * stepX + stepX / 2
                    val prevHeightLast = if (catIndex == 0) chartHeight else stackedPoints[catIndex - 1][data.size - 1]
                    path.lineTo(endX, prevHeightLast)
                    
                    // Bottom line (backwards)
                    for (i in data.size - 2 downTo 0) {
                        val x = leftPadding + offsetX + i * stepX + stepX / 2
                        val prevHeight = if (catIndex == 0) chartHeight else stackedPoints[catIndex - 1][i]
                        path.lineTo(x, prevHeight)
                    }
                    
                    path.close()
                    drawPath(path, color.copy(alpha = 0.8f))
                }
            }
        }
        
        if (selectedPoint != null && showTooltip) {
            val (index, tapOffset) = selectedPoint!!
            val item = data[index]
            val dateFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))
            
            val chartWidth = chartSize.width - leftPadding
            val stepX = (chartWidth / data.size) * scale
            val xPos = leftPadding + offsetX + index * stepX + stepX / 2
            
            if (xPos >= leftPadding && xPos <= chartSize.width) {
                ChartTooltip(
                    title = item.yearMonth.format(dateFormatter),
                    content = {
                        Text("Total: ${formatCurrency(item.totalExpenses, currency)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        categories.forEach { category ->
                            val amount = item.expensesByCategory[category.name] ?: 0.0
                            if (amount > 0) {
                                Text(
                                    "${category.name}: ${formatCurrency(amount, currency)}", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = Color(category.color)
                                )
                            }
                        }
                    },
                    targetPosition = Offset(xPos, tapOffset.y),
                    containerSize = chartSize,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

// Reuse helper functions from FinancialFlowCharts.kt if possible, or duplicate them here to be self-contained
// Duplicating for now to ensure this file works independently given context constraints

private fun DrawScope.drawStandardChartGrid(
    textMeasurer: TextMeasurer,
    maxVal: Float,
    data: List<MonthlyExpenseData>,
    leftPadding: Float,
    bottomPadding: Float,
    currency: String,
    scale: Float = 1f,
    offsetX: Float = 0f
) {
    val chartWidth = size.width - leftPadding
    val chartHeight = size.height - bottomPadding
    
    val steps = 4
    for (i in 0..steps) {
        val value = (maxVal / steps) * i
        val y = chartHeight - (chartHeight / steps) * i
        
        drawLine(
            color = Color.LightGray.copy(alpha = 0.5f),
            start = Offset(leftPadding, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        
        val textResult = textMeasurer.measure(
            text = formatCurrency(value.toDouble(), currency, 0),
            style = TextStyle(fontSize = 9.sp, color = Color.Gray)
        )
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(leftPadding - textResult.size.width - 4.dp.toPx(), y - textResult.size.height / 2)
        )
    }
    
    val dateFormatter = DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))
    val stepX = (chartWidth / data.size) * scale
    
    clipRect(left = leftPadding, top = 0f, right = size.width, bottom = size.height) {
        data.forEachIndexed { index, item ->
            val x = leftPadding + offsetX + index * stepX + stepX / 2
            
            // Only draw if visible (simple optimization)
            if (x >= leftPadding - 20 && x <= size.width + 20) {
                val textResult = textMeasurer.measure(
                    text = item.yearMonth.format(dateFormatter),
                    style = TextStyle(fontSize = 10.sp, color = Color.Gray)
                )
                
                drawText(
                    textLayoutResult = textResult,
                    topLeft = Offset(x - textResult.size.width / 2, chartHeight + 4.dp.toPx())
                )
            }
        }
    }
}

private fun getIndexFromTap(offset: Offset, chartWidth: Float, leftPadding: Float, dataSize: Int, scale: Float = 1f, offsetX: Float = 0f): Int? {
    if (dataSize == 0) return null
    if (offset.x < leftPadding) return null
    
    val width = chartWidth - leftPadding
    val stepX = (width / dataSize) * scale
    
    val index = ((offset.x - leftPadding - offsetX) / stepX).toInt()
    return if (index in 0 until dataSize) index else null
}

@Composable
private fun ChartTooltip(
    title: String,
    content: @Composable () -> Unit,
    targetPosition: Offset,
    containerSize: Size,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val margin = with(density) { 10.dp.roundToPx() }
    
    Column(
        modifier = modifier
            .zIndex(1f)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val width = placeable.width
                val height = placeable.height
                
                // Center horizontally
                var x = (targetPosition.x - width / 2).toInt()
                x = x.coerceIn(0, (containerSize.width - width).toInt())
                
                // Vertical
                var y = (targetPosition.y - height - margin).toInt()
                if (y < 0) {
                     y = (targetPosition.y + margin).toInt()
                }
                
                layout(width, height) {
                    placeable.place(x, y)
                }
            }
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        content()
    }
}
