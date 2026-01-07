package com.flowfinance.app.ui.screens.panel

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.ui.screens.settings.shareFile
import com.flowfinance.app.ui.viewmodel.CategoryTrendsViewModel
import com.flowfinance.app.util.formatCurrency
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTrendsSheetScreen(
    onBackClick: () -> Unit,
    viewModel: CategoryTrendsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trends_sheet_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opções")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.sheet_download_csv)) },
                            onClick = {
                                showMenu = false
                                viewModel.exportSheetToCsv { path ->
                                    if (path != null) {
                                        shareFile(context, path)
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.sheet_export_error), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .horizontalScroll(horizontalScrollState)
        ) {
            // Header Row
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
                TableCell(text = stringResource(R.string.sheet_col_date), weight = 1f, isHeader = true)
                uiState.categories.forEach { category ->
                     TableCell(text = category.name, weight = 1f, isHeader = true)
                }
            }

            // Data Rows
            LazyColumn {
                itemsIndexed(uiState.monthlyData) { index, item ->
                    val bgColor = if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                    Row(modifier = Modifier.background(bgColor)) {
                        val dateStr = item.yearMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                        
                        TableCell(text = dateStr, weight = 1f)
                        
                        uiState.categories.forEach { category ->
                            val amount = item.expensesByCategory[category.name] ?: 0.0
                            TableCell(text = formatCurrency(amount, uiState.currency), weight = 1f)
                        }
                    }
                }
            }
        }
    }
}
