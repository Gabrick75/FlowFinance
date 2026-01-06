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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowfinance.app.ui.viewmodel.MonthlyFinancialData
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
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val maxVal = data.maxOf { 
        maxOf(it.salary, it.monthlyYield, it.accumulatedYield, it.accumulatedBalance, it.totalWealth) 
    }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 2f)

        // Eixo Y e Labels (Simplificado)
        drawLine(
            color = Color.Gray,
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 2f
        )

        // Função auxiliar para desenhar linhas
        fun drawLineChart(values: List<Float>, color: Color) {
            if (values.size < 2) return
            val path = Path()
            val stepX = width / (values.size - 1)
            
            path.moveTo(0f, height - (values[0] / maxVal * height))
            for (i in 1 until values.size) {
                val x = i * stepX
                val y = height - (values[i] / maxVal * height)
                // Suavização simples (Bezier quadrática poderia ser usada para mais suavidade)
                path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
        }

        // Desenhar as 5 linhas
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
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { it.salary }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = (width / data.size) * 0.6f
        val stepX = width / data.size

        data.forEachIndexed { index, item ->
            val barHeight = (item.salary.toFloat() / maxVal) * height
            val x = index * stepX + (stepX - barWidth) / 2
            val y = height - barHeight

            drawRect(
                color = ColorSalary,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )
        }
    }
}

@Composable
fun YieldAreaChart(
    data: List<MonthlyFinancialData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { maxOf(it.monthlyYield, it.accumulatedYield) }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size - 1).coerceAtLeast(1)

        // Função para desenhar área
        fun drawArea(values: List<Float>, color: Color) {
            val path = Path()
            path.moveTo(0f, height) // Começa na base
            
            values.forEachIndexed { index, value ->
                val x = index * stepX
                val y = height - (value / maxVal * height)
                path.lineTo(x, y)
            }
            path.lineTo(width, height) // Fecha na base
            path.close()
            
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.5f), color.copy(alpha = 0.1f))
                )
            )
            // Linha superior
            val linePath = Path()
            linePath.moveTo(0f, height - (values[0] / maxVal * height))
            values.forEachIndexed { index, value ->
                if(index > 0) {
                    val x = index * stepX
                    val y = height - (value / maxVal * height)
                    linePath.lineTo(x, y)
                }
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
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { maxOf(it.salary, it.monthlyYield, it.accumulatedYield) }.toFloat().coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / data.size
        val barWidth = stepX * 0.4f

        // 1. Colunas (Salário)
        data.forEachIndexed { index, item ->
            val barHeight = (item.salary.toFloat() / maxVal) * height
            val x = index * stepX + (stepX - barWidth) / 2
            val y = height - barHeight
            drawRect(ColorSalary, topLeft = Offset(x, y), size = Size(barWidth, barHeight))
        }

        // 2. Linhas (Rendimentos)
        val lineStepX = width / (data.size - 1).coerceAtLeast(1)
        
        fun drawLine(values: List<Float>, color: Color) {
            if(values.size < 2) return
            val path = Path()
            path.moveTo(0f, height - (values[0] / maxVal * height))
            for(i in 1 until values.size) {
                path.lineTo(i * lineStepX, height - (values[i] / maxVal * height))
            }
            drawPath(path, color, style = Stroke(3.dp.toPx()))
        }

        // Ajuste: Linhas centralizadas nas colunas para melhor visualização combinada?
        // Simplificação: Linha desenhada de ponto a ponto cobrindo a largura
        
        // Rendimento Mensal (Linha)
        drawLine(data.map { it.monthlyYield.toFloat() }, ColorYield)
        
        // Rendimento Acumulado (Linha)
        drawLine(data.map { it.accumulatedYield.toFloat() }, ColorAccYield)
    }
}
