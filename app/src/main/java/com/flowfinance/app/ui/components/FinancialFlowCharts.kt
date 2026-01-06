package com.flowfinance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { 
        maxOf(it.salary, it.monthlyYield, it.accumulatedYield, it.accumulatedBalance, it.totalWealth) 
    }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val leftPadding = 60.dp.toPx() // Aumentado para acomodar valores com R$
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
}

@Composable
fun SalaryBarChart(
    data: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { it.salary }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val leftPadding = 60.dp.toPx()
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
            
            // Draw value above bar
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
}

@Composable
fun YieldAreaChart(
    data: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { maxOf(it.monthlyYield, it.accumulatedYield) }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val leftPadding = 60.dp.toPx()
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
}

@Composable
fun CombinedChart(
    data: List<MonthlyFinancialData>,
    currency: String,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { maxOf(it.salary, it.monthlyYield, it.accumulatedYield) }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val leftPadding = 60.dp.toPx()
        val bottomPadding = 20.dp.toPx()
        val chartWidth = size.width - leftPadding
        val chartHeight = size.height - bottomPadding

        drawStandardChartGrid(textMeasurer, maxVal, data, leftPadding, bottomPadding, currency)

        val stepX = chartWidth / data.size
        val barWidth = stepX * 0.4f

        // 1. Vertical Bars (Salary)
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

        // 2. Lines (Yields)
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
    
    // Horizontal Grid Lines & Y-Axis Labels (Values)
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
            text = formatCurrency(value.toDouble(), currency).substringBefore(","), // Simplificado para caber (sem centavos)
            style = TextStyle(fontSize = 9.sp, color = Color.Gray)
        )
        drawText(
            textLayoutResult = textResult,
            topLeft = Offset(leftPadding - textResult.size.width - 4.dp.toPx(), y - textResult.size.height / 2)
        )
    }
    
    // X-Axis Labels (Months)
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
