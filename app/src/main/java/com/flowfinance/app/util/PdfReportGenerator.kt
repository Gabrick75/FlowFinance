package com.flowfinance.app.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import com.flowfinance.app.R
import com.flowfinance.app.data.local.model.CategorySummary
import com.flowfinance.app.data.local.model.TransactionWithCategory
import com.flowfinance.app.ui.theme.GreenIncome
import com.flowfinance.app.ui.theme.RedExpense
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds a one-file, shareable monthly report (summary + charts + full transaction table)
 * by drawing directly onto [PdfDocument] pages. No Compose capture is involved: the app's
 * charts are Compose Canvas drawings tied to a live composition, so re-rendering them
 * off-screen for a PDF would need a fake Activity/lifecycle host - drawing the same shapes
 * again with android.graphics is simpler and works identically on every API level.
 */
object PdfReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val ROW_HEIGHT = 18f

    private val COL_DATE_X = MARGIN
    private val COL_DESC_X = MARGIN + 55f
    private val COL_CATEGORY_X = COL_DESC_X + 150f
    private val COL_TYPE_X = COL_CATEGORY_X + 95f
    private val COL_VALUE_RIGHT_X = (PAGE_WIDTH - MARGIN)

    fun generate(
        context: Context,
        yearMonth: YearMonth,
        transactionsByDate: Map<LocalDate, List<TransactionWithCategory>>,
        currency: String
    ): File {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas: Canvas = page.canvas
        var y = MARGIN

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
        }

        fun ensureSpace(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN) {
                newPage()
            }
        }

        val greenArgb = GreenIncome.toArgb()
        val redArgb = RedExpense.toArgb()

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 20f; typeface = Typeface.DEFAULT_BOLD
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 12f }
        val timestampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 9f }
        val dividerPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 13f; typeface = Typeface.DEFAULT_BOLD
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 11f }
        val summaryValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f; typeface = Typeface.DEFAULT_BOLD }
        val legendNamePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9.5f }
        val legendValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 9.5f }
        val barLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 10f; textAlign = Paint.Align.CENTER
        }
        val barValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
        }
        val tableHeaderBgPaint = Paint().apply { color = Color.parseColor("#EEEEEE") }
        val tableHeaderTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK; textSize = 9f; typeface = Typeface.DEFAULT_BOLD
        }
        val tableCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 9f }
        val tableRowDividerPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

        fun drawSectionTitle(title: String) {
            ensureSpace(sectionPaint.textSize + 10f)
            y += 6f
            canvas.drawText(title, MARGIN, y + sectionPaint.textSize, sectionPaint)
            y += sectionPaint.textSize + 10f
        }

        // ---- Header ----
        canvas.drawText("FlowFinance", MARGIN, y + titlePaint.textSize, titlePaint)
        y += titlePaint.textSize + 4f

        val monthLabel = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        canvas.drawText(
            context.getString(R.string.pdf_report_subtitle, monthLabel),
            MARGIN, y + subtitlePaint.textSize, subtitlePaint
        )
        y += subtitlePaint.textSize + 6f

        val generatedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        canvas.drawText(
            context.getString(R.string.pdf_report_generated_at, generatedAt),
            MARGIN, y + timestampPaint.textSize, timestampPaint
        )
        y += timestampPaint.textSize + 14f

        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
        y += 20f

        // ---- Data ----
        val allTransactions = transactionsByDate.values.flatten().sortedBy { it.transaction.date }
        val incomeTotal = allTransactions.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount }
        val expenseTotal = allTransactions.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount }
        val balance = incomeTotal - expenseTotal

        val expensesByCategory = allTransactions
            .filter { it.transaction.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (category, txs) -> CategorySummary(category, txs.sumOf { it.transaction.amount }) }
            .sortedByDescending { it.totalAmount }

        if (allTransactions.isEmpty()) {
            canvas.drawText(context.getString(R.string.pdf_report_no_transactions), MARGIN, y + bodyPaint.textSize, bodyPaint)
        } else {
            // ---- Summary ----
            fun drawSummaryRow(label: String, value: Double, valueColor: Int) {
                ensureSpace(ROW_HEIGHT)
                canvas.drawText(label, MARGIN, y + bodyPaint.textSize, bodyPaint)
                val valueText = formatCurrency(value, currency)
                summaryValuePaint.color = valueColor
                canvas.drawText(
                    valueText,
                    COL_VALUE_RIGHT_X - summaryValuePaint.measureText(valueText),
                    y + bodyPaint.textSize,
                    summaryValuePaint
                )
                y += ROW_HEIGHT
            }

            drawSummaryRow(context.getString(R.string.pdf_report_summary_income), incomeTotal, greenArgb)
            drawSummaryRow(context.getString(R.string.pdf_report_summary_expense), expenseTotal, redArgb)
            drawSummaryRow(
                context.getString(R.string.pdf_report_summary_balance),
                balance,
                if (balance >= 0) greenArgb else redArgb
            )

            // ---- Expense breakdown pie chart (only if there is something to break down) ----
            if (expensesByCategory.isNotEmpty()) {
                drawSectionTitle(context.getString(R.string.pdf_report_expenses_by_category))

                val diameter = 130f
                val strokeWidth = 22f
                ensureSpace(diameter + 10f)
                val arcBounds = RectF(
                    MARGIN + strokeWidth / 2, y + strokeWidth / 2,
                    MARGIN + diameter - strokeWidth / 2, y + diameter - strokeWidth / 2
                )
                val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    this.strokeWidth = strokeWidth
                }
                var startAngle = -90f
                val totalExpense = expensesByCategory.sumOf { it.totalAmount }
                expensesByCategory.forEach { summary ->
                    val sweep = (360f * (summary.totalAmount / totalExpense)).toFloat()
                    arcPaint.color = summary.category.color
                    canvas.drawArc(arcBounds, startAngle, sweep, false, arcPaint)
                    startAngle += sweep
                }
                y += diameter + 12f

                expensesByCategory.forEach { summary ->
                    ensureSpace(14f)
                    val swatchPaint = Paint().apply { color = summary.category.color }
                    canvas.drawRect(MARGIN, y, MARGIN + 10f, y + 10f, swatchPaint)
                    val name = TextUtils.ellipsize(summary.category.name, legendNamePaint, 220f, TextUtils.TruncateAt.END).toString()
                    canvas.drawText(name, MARGIN + 16f, y + 9f, legendNamePaint)
                    val pct = (summary.totalAmount / totalExpense) * 100
                    val valueText = "${formatCurrency(summary.totalAmount, currency)} (${String.format(Locale.getDefault(), "%.1f", pct)}%)"
                    canvas.drawText(
                        valueText,
                        COL_VALUE_RIGHT_X - legendValuePaint.measureText(valueText),
                        y + 9f,
                        legendValuePaint
                    )
                    y += 14f
                }
            }

            // ---- Income vs Expense bar chart ----
            drawSectionTitle(context.getString(R.string.pdf_report_income_vs_expense))

            val barMaxHeight = 70f
            val barWidth = 60f
            val gap = 50f
            ensureSpace(barMaxHeight + 40f)
            val baseY = y + barMaxHeight

            val maxVal = maxOf(incomeTotal, expenseTotal, 0.01)
            val incomeBarHeight = (incomeTotal / maxVal * barMaxHeight).toFloat()
            val expenseBarHeight = (expenseTotal / maxVal * barMaxHeight).toFloat()

            val chartCenterX = MARGIN + (barWidth * 2 + gap) / 2
            val incomeCenterX = chartCenterX - gap / 2 - barWidth / 2
            val expenseCenterX = chartCenterX + gap / 2 + barWidth / 2

            val incomeFillPaint = Paint().apply { color = greenArgb }
            val expenseFillPaint = Paint().apply { color = redArgb }

            canvas.drawRect(incomeCenterX - barWidth / 2, baseY - incomeBarHeight, incomeCenterX + barWidth / 2, baseY, incomeFillPaint)
            canvas.drawRect(expenseCenterX - barWidth / 2, baseY - expenseBarHeight, expenseCenterX + barWidth / 2, baseY, expenseFillPaint)

            barValuePaint.color = greenArgb
            canvas.drawText(formatCurrency(incomeTotal, currency), incomeCenterX, baseY - incomeBarHeight - 6f, barValuePaint)
            barValuePaint.color = redArgb
            canvas.drawText(formatCurrency(expenseTotal, currency), expenseCenterX, baseY - expenseBarHeight - 6f, barValuePaint)

            canvas.drawText(context.getString(R.string.add_transaction_income), incomeCenterX, baseY + 14f, barLabelPaint)
            canvas.drawText(context.getString(R.string.add_transaction_expense), expenseCenterX, baseY + 14f, barLabelPaint)

            y = baseY + 28f

            // ---- Transaction table ----
            fun drawTableHeader() {
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, tableHeaderBgPaint)
                val baseline = y + ROW_HEIGHT - 5f
                canvas.drawText(context.getString(R.string.pdf_report_col_date), COL_DATE_X + 2f, baseline, tableHeaderTextPaint)
                canvas.drawText(context.getString(R.string.pdf_report_col_description), COL_DESC_X + 2f, baseline, tableHeaderTextPaint)
                canvas.drawText(context.getString(R.string.pdf_report_col_category), COL_CATEGORY_X + 2f, baseline, tableHeaderTextPaint)
                canvas.drawText(context.getString(R.string.pdf_report_col_type), COL_TYPE_X + 2f, baseline, tableHeaderTextPaint)
                val valueHeader = context.getString(R.string.pdf_report_col_value)
                canvas.drawText(
                    valueHeader,
                    COL_VALUE_RIGHT_X - tableHeaderTextPaint.measureText(valueHeader),
                    baseline,
                    tableHeaderTextPaint
                )
                y += ROW_HEIGHT
            }

            drawSectionTitle(context.getString(R.string.pdf_report_transactions_section))
            drawTableHeader()

            val dateFormatter = DateTimeFormatter.ofPattern(context.getString(R.string.date_format), Locale.getDefault())
            val incomeLabel = context.getString(R.string.add_transaction_income)
            val expenseLabel = context.getString(R.string.add_transaction_expense)

            allTransactions.forEach { item ->
                if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                    newPage()
                    drawTableHeader()
                }
                val baseline = y + ROW_HEIGHT - 5f
                canvas.drawText(item.transaction.date.format(dateFormatter), COL_DATE_X + 2f, baseline, tableCellPaint)
                val desc = TextUtils.ellipsize(item.transaction.description, tableCellPaint, 145f, TextUtils.TruncateAt.END).toString()
                canvas.drawText(desc, COL_DESC_X + 2f, baseline, tableCellPaint)
                val cat = TextUtils.ellipsize(item.category.name, tableCellPaint, 90f, TextUtils.TruncateAt.END).toString()
                canvas.drawText(cat, COL_CATEGORY_X + 2f, baseline, tableCellPaint)

                val isIncome = item.transaction.type == TransactionType.INCOME
                canvas.drawText(if (isIncome) incomeLabel else expenseLabel, COL_TYPE_X + 2f, baseline, tableCellPaint)

                val valuePaint = Paint(tableCellPaint).apply { color = if (isIncome) greenArgb else redArgb }
                val sign = if (isIncome) "+" else "-"
                val valueText = "$sign${formatCurrency(item.transaction.amount, currency)}"
                canvas.drawText(valueText, COL_VALUE_RIGHT_X - valuePaint.measureText(valueText), baseline, valuePaint)

                canvas.drawLine(MARGIN, y + ROW_HEIGHT, PAGE_WIDTH - MARGIN, y + ROW_HEIGHT, tableRowDividerPaint)
                y += ROW_HEIGHT
            }
        }

        document.finishPage(page)

        val fileName = "flowfinance_report_$yearMonth.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }
}
