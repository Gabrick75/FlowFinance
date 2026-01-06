package com.flowfinance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.flowfinance.app.ui.viewmodel.MonthlyFinancialData
import com.flowfinance.app.util.formatCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale

// Cores para os gráficos
val ColorSalary = Color(0xFF42A5F5) // Azul
val ColorYield = Color(0xFF66BB6A) // Verde
val ColorAccYield = Color(0xFFFFA726) // Laranja
val ColorBalance = Color(0xFFAB47BC) // Roxo
val ColorWealth = Color(0xFFEF5350) // Vermelho

@Composable
fun GeneralOverviewChart(
    data: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = false
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { 
        maxOf(it.salary, it.monthlyYield, it.accumulatedYield, it.accumulatedBalance, it.totalWealth) 
    }.toFloat().coerceAtLeast(1f)
    
    var selectedPoint by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    var chartSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val leftPadding = with(density) { 60.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { chartSize = it.toSize() }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTapGestures { offset ->
                            val index = getIndexFromTap(offset, chartSize.width, leftPadding, data.size)
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

            drawStandardChartGrid(textMeasurer, maxVal, data, leftPadding, bottomPadding, currency)

            fun drawLineChart(values: List<Float>, color: Color) {
                if (values.size < 2) return
                val path = Path()
                val stepX = chartWidth / data.size
                
                val startX = leftPadding + stepX / 2
                val startY = chartHeight - (values[0] / maxVal * chartHeight)
                path.moveTo(startX, startY)
                
                for (i in 1 until values.size) {
                    val x = leftPadding + i * stepX + stepX / 2
                    val y = chartHeight - (values[i] / maxVal * chartHeight)
                    path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
            }

            drawLineChart(data.map { it.salary.toFloat() }, ColorSalary)
            drawLineChart(data.map { it.monthlyYield.toFloat() }, ColorYield)
            drawLineChart(data.map { it.accumulatedYield.toFloat() }, ColorAccYield)
            drawLineChart(data.map { it.accumulatedBalance.toFloat() }, ColorBalance)
            drawLineChart(data.map { it.totalWealth.toFloat() }, ColorWealth)
        }
        
        if (selectedPoint != null && showTooltip) {
            val (index, tapOffset) = selectedPoint!!
            val item = data[index]
            val dateFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))
            val dateStr = item.yearMonth.format(dateFormatter)
            
            val chartWidth = chartSize.width - leftPadding
            val stepX = chartWidth / data.size
            val xPos = leftPadding + index * stepX + stepX / 2
            
            ChartTooltip(
                title = dateStr,
                content = {
                    Text("Salário: ${formatCurrency(item.salary, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorSalary)
                    Text("Rend. Mês: ${formatCurrency(item.monthlyYield, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorYield)
                    Text("Saldo Acum.: ${formatCurrency(item.accumulatedBalance, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorBalance)
                },
                targetPosition = Offset(xPos, tapOffset.y),
                containerSize = chartSize,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
fun SalaryBarChart(
    data: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = false
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { it.salary }.toFloat().coerceAtLeast(1f)
    
    var selectedPoint by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    var chartSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val leftPadding = with(density) { 60.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { chartSize = it.toSize() }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTapGestures { offset ->
                            val index = getIndexFromTap(offset, chartSize.width, leftPadding, data.size)
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

            drawStandardChartGrid(textMeasurer, maxVal, data, leftPadding, bottomPadding, currency)

            val stepX = chartWidth / data.size
            val barWidth = stepX * 0.6f

            data.forEachIndexed { index, item ->
                val barHeight = (item.salary.toFloat() / maxVal) * chartHeight
                val x = leftPadding + index * stepX + (stepX - barWidth) / 2
                val y = chartHeight - barHeight
                
                drawRect(
                    color = ColorSalary,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
                
                val textResult = textMeasurer.measure(
                    text = formatCurrency(item.salary, currency),
                    style = TextStyle(fontSize = 9.sp, color = Color.Gray)
                )
                if (y - textResult.size.height > 0) {
                     drawText(
                        textLayoutResult = textResult,
                        topLeft = Offset(x + (barWidth - textResult.size.width)/2, y - textResult.size.height - 2.dp.toPx())
                    )
                }
            }
        }
        
        if (selectedPoint != null && showTooltip) {
            val (index, tapOffset) = selectedPoint!!
            val item = data[index]
            val dateFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))
            
            val chartWidth = chartSize.width - leftPadding
            val stepX = chartWidth / data.size
            val xPos = leftPadding + index * stepX + stepX / 2
            
            ChartTooltip(
                title = item.yearMonth.format(dateFormatter),
                content = {
                    Text("Salário: ${formatCurrency(item.salary, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorSalary)
                },
                targetPosition = Offset(xPos, tapOffset.y),
                containerSize = chartSize,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
fun YieldAreaChart(
    data: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = false
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { maxOf(it.monthlyYield, it.accumulatedYield) }.toFloat().coerceAtLeast(1f)
    
    var selectedPoint by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    var chartSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val leftPadding = with(density) { 60.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { chartSize = it.toSize() }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTapGestures { offset ->
                            val index = getIndexFromTap(offset, chartSize.width, leftPadding, data.size)
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

            drawStandardChartGrid(textMeasurer, maxVal, data, leftPadding, bottomPadding, currency)

            val stepX = chartWidth / data.size

            fun drawArea(values: List<Float>, color: Color) {
                val path = Path()
                
                val startX = leftPadding + stepX / 2
                path.moveTo(startX, chartHeight)
                
                values.forEachIndexed { index, value ->
                    val x = leftPadding + index * stepX + stepX / 2
                    val y = chartHeight - (value / maxVal * chartHeight)
                    path.lineTo(x, y)
                }
                
                val endX = leftPadding + (values.size - 1) * stepX + stepX / 2
                path.lineTo(endX, chartHeight)
                
                path.close()
                
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.1f)),
                        startY = 0f,
                        endY = chartHeight
                    )
                )
                
                val linePath = Path()
                linePath.moveTo(startX, chartHeight - (values[0] / maxVal * chartHeight))
                
                for(i in 1 until values.size) {
                    val x = leftPadding + i * stepX + stepX / 2
                    val y = chartHeight - (values[i] / maxVal * chartHeight)
                    linePath.lineTo(x, y)
                }
                drawPath(linePath, color, style = Stroke(2.dp.toPx()))
            }

            drawArea(data.map { it.accumulatedYield.toFloat() }, ColorAccYield)
            drawArea(data.map { it.monthlyYield.toFloat() }, ColorYield)
        }
        
        if (selectedPoint != null && showTooltip) {
            val (index, tapOffset) = selectedPoint!!
            val item = data[index]
            val dateFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))
            
            val chartWidth = chartSize.width - leftPadding
            val stepX = chartWidth / data.size
            val xPos = leftPadding + index * stepX + stepX / 2
            
            ChartTooltip(
                title = item.yearMonth.format(dateFormatter),
                content = {
                    Text("Rend. Mensal: ${formatCurrency(item.monthlyYield, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorYield)
                    Text("Rend. Acum: ${formatCurrency(item.accumulatedYield, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorAccYield)
                },
                targetPosition = Offset(xPos, tapOffset.y),
                containerSize = chartSize,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
fun CombinedChart(
    data: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier,
    showTooltip: Boolean = false
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { maxOf(it.salary, it.monthlyYield, it.accumulatedYield) }.toFloat().coerceAtLeast(1f)
    
    var selectedPoint by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    var chartSize by remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current
    val leftPadding = with(density) { 60.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { chartSize = it.toSize() }
                .pointerInput(Unit) {
                    if (showTooltip) {
                        detectTapGestures { offset ->
                            val index = getIndexFromTap(offset, chartSize.width, leftPadding, data.size)
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

            drawStandardChartGrid(textMeasurer, maxVal, data, leftPadding, bottomPadding, currency)

            val stepX = chartWidth / data.size
            val barWidth = stepX * 0.4f

            data.forEachIndexed { index, item ->
                val barHeight = (item.salary.toFloat() / maxVal) * chartHeight
                val x = leftPadding + index * stepX + (stepX - barWidth) / 2
                val y = chartHeight - barHeight
                
                drawRect(
                    color = ColorSalary,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )
            }

            fun drawLine(values: List<Float>, color: Color) {
                if(values.size < 2) return
                val path = Path()
                
                val startX = leftPadding + stepX / 2
                val startY = chartHeight - (values[0] / maxVal * chartHeight)
                path.moveTo(startX, startY)
                
                for(i in 1 until values.size) {
                    val x = leftPadding + i * stepX + stepX / 2
                    val y = chartHeight - (values[i] / maxVal * chartHeight)
                    path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(3.dp.toPx()))
            }

            drawLine(data.map { it.monthlyYield.toFloat() }, ColorYield)
            drawLine(data.map { it.accumulatedYield.toFloat() }, ColorAccYield)
        }
        
        if (selectedPoint != null && showTooltip) {
            val (index, tapOffset) = selectedPoint!!
            val item = data[index]
            val dateFormatter = DateTimeFormatter.ofPattern("MMM/yy", Locale("pt", "BR"))
            
            val chartWidth = chartSize.width - leftPadding
            val stepX = chartWidth / data.size
            val xPos = leftPadding + index * stepX + stepX / 2
            
            ChartTooltip(
                title = item.yearMonth.format(dateFormatter),
                content = {
                    Text("Salário: ${formatCurrency(item.salary, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorSalary)
                    Text("Rend. Mensal: ${formatCurrency(item.monthlyYield, currency)}", style = MaterialTheme.typography.bodySmall, color = ColorYield)
                },
                targetPosition = Offset(xPos, tapOffset.y),
                containerSize = chartSize,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

private fun DrawScope.drawStandardChartGrid(
    textMeasurer: TextMeasurer,
    maxVal: Float,
    data: List<MonthlyFinancialData>,
    leftPadding: Float,
    bottomPadding: Float,
    currency: String
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
            text = formatCurrency(value.toDouble(), currency).substringBefore(","),
            style = TextStyle(fontSize = 9.sp, color = Color.Gray)
        )
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(leftPadding - textResult.size.width - 4.dp.toPx(), y - textResult.size.height / 2)
        )
    }
    
    val dateFormatter = DateTimeFormatter.ofPattern("MMM", Locale("pt", "BR"))
    val stepX = chartWidth / data.size
    
    data.forEachIndexed { index, item ->
        val x = leftPadding + index * stepX + stepX / 2
        
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

private fun getIndexFromTap(offset: Offset, chartWidth: Float, leftPadding: Float, dataSize: Int): Int? {
    if (dataSize == 0) return null
    if (offset.x < leftPadding) return null
    
    val width = chartWidth - leftPadding
    val stepX = width / dataSize
    
    val index = ((offset.x - leftPadding) / stepX).toInt()
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
