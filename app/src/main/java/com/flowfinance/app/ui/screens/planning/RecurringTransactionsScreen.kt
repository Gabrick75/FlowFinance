package com.flowfinance.app.ui.screens.planning

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.data.local.entity.RecurringTransaction
import com.flowfinance.app.ui.components.CategoryChip
import com.flowfinance.app.ui.components.DateField
import com.flowfinance.app.ui.components.SimpleDatePickerDialog
import com.flowfinance.app.ui.components.recurrenceFrequencyLabel
import com.flowfinance.app.ui.theme.GreenIncome
import com.flowfinance.app.ui.theme.RedExpense
import com.flowfinance.app.ui.viewmodel.RecurringTransactionsViewModel
import com.flowfinance.app.util.RecurrenceFrequency
import com.flowfinance.app.util.TransactionType
import com.flowfinance.app.util.formatCurrency
import com.flowfinance.app.util.occurrenceDate
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecurringTransactionsScreen(
    onBackClick: () -> Unit,
    viewModel: RecurringTransactionsViewModel = hiltViewModel()
) {
    val recurringTransactions by viewModel.recurringTransactions.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var isCreating by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<RecurringTransaction?>(null) }

    if (isCreating || editingTransaction != null) {
        RecurringTransactionDialog(
            recurringTransaction = editingTransaction,
            categories = categories,
            onDismiss = {
                isCreating = false
                editingTransaction = null
            },
            onConfirm = { description, amount, type, categoryId, frequency, startDate, endDate ->
                val existing = editingTransaction
                if (existing != null) {
                    viewModel.updateRecurringTransaction(
                        existing, description, amount, type, categoryId, frequency, startDate, endDate
                    )
                } else {
                    viewModel.createRecurringTransaction(
                        description, amount, type, categoryId, frequency, startDate, endDate
                    )
                }
                isCreating = false
                editingTransaction = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.recurring_transactions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isCreating = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.recurring_transactions_new))
            }
        }
    ) { padding ->
        if (recurringTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.recurring_transactions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recurringTransactions, key = { it.id }) { rule ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteRecurringTransaction(rule)
                                true
                            } else false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color.Red.copy(alpha = 0.8f) else Color.Transparent
                            )
                            val scale by animateFloatAsState(
                                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.75f
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.transactions_delete_desc),
                                    modifier = Modifier.scale(scale),
                                    tint = Color.White
                                )
                            }
                        },
                        content = {
                            RecurringTransactionItem(
                                rule = rule,
                                category = categories.find { it.id == rule.categoryId },
                                onClick = { editingTransaction = rule },
                                onActiveChange = { viewModel.toggleActive(rule, it) }
                            )
                        },
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringTransactionItem(
    rule: RecurringTransaction,
    category: Category?,
    onClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    val nextDate = rule.frequency.occurrenceDate(rule.startDate, rule.occurrencesGenerated.toLong())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .clip(RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(category?.let { Color(it.color) } ?: MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                val icon = rememberCategoryIcon(category?.icon)
                if (icon != null) {
                    Icon(icon, contentDescription = category?.name, tint = Color.White)
                } else {
                    Text(
                        text = category?.name?.take(1)?.uppercase() ?: "?",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(text = rule.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                val statusText = if (!rule.isActive) {
                    stringResource(R.string.recurring_transactions_paused)
                } else {
                    "${recurrenceFrequencyLabel(rule.frequency)} • ${stringResource(R.string.recurring_transactions_next_date, nextDate.toString())}"
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            val amountColor = if (rule.type == TransactionType.INCOME) GreenIncome else RedExpense
            val prefix = if (rule.type == TransactionType.INCOME) "+" else "-"
            Text(
                text = "$prefix${formatCurrency(rule.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            Switch(checked = rule.isActive, onCheckedChange = onActiveChange)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringTransactionDialog(
    recurringTransaction: RecurringTransaction?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (
        description: String,
        amount: Double,
        type: TransactionType,
        categoryId: Int,
        frequency: RecurrenceFrequency,
        startDate: LocalDate,
        endDate: LocalDate?
    ) -> Unit
) {
    var description by remember { mutableStateOf(recurringTransaction?.description ?: "") }
    var amount by remember { mutableStateOf(recurringTransaction?.amount?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(recurringTransaction?.type ?: TransactionType.EXPENSE) }
    var selectedCategory by remember {
        mutableStateOf(categories.find { it.id == recurringTransaction?.categoryId })
    }
    var selectedFrequency by remember { mutableStateOf(recurringTransaction?.frequency ?: RecurrenceFrequency.MONTHLY) }
    var frequencyMenuExpanded by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(recurringTransaction?.startDate ?: LocalDate.now()) }
    var hasEndDate by remember { mutableStateOf(recurringTransaction?.endDate != null) }
    var endDate by remember { mutableStateOf(recurringTransaction?.endDate ?: LocalDate.now().plusMonths(1)) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        SimpleDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { startDate = it; showStartDatePicker = false }
        )
    }
    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { endDate = it; showEndDatePicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (recurringTransaction == null) stringResource(R.string.recurring_transactions_new)
                else stringResource(R.string.recurring_transactions_edit)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val types = TransactionType.values()
                    types.forEachIndexed { index, type ->
                        SegmentedButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                        ) {
                            Text(if (type == TransactionType.INCOME) stringResource(R.string.add_transaction_income) else stringResource(R.string.add_transaction_expense))
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.add_transaction_desc_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                    label = { Text(stringResource(R.string.add_transaction_value_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(stringResource(R.string.add_transaction_category_label), style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategory?.id == category.id,
                            onClick = { selectedCategory = category }
                        )
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = frequencyMenuExpanded,
                    onExpandedChange = { frequencyMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = recurrenceFrequencyLabel(selectedFrequency),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.recurring_frequency_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = frequencyMenuExpanded,
                        onDismissRequest = { frequencyMenuExpanded = false }
                    ) {
                        RecurrenceFrequency.values().forEach { frequency ->
                            DropdownMenuItem(
                                text = { Text(recurrenceFrequencyLabel(frequency)) },
                                onClick = {
                                    selectedFrequency = frequency
                                    frequencyMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                DateField(
                    label = stringResource(R.string.add_transaction_date_label),
                    date = startDate,
                    onClick = { showStartDatePicker = true }
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
                    Text(stringResource(R.string.recurring_end_date_toggle))
                }
                if (hasEndDate) {
                    DateField(
                        label = stringResource(R.string.recurring_end_date_label),
                        date = endDate,
                        onClick = { showEndDatePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    val category = selectedCategory
                    if (amountDouble != null && category != null && description.isNotBlank()) {
                        onConfirm(
                            description,
                            amountDouble,
                            selectedType,
                            category.id,
                            selectedFrequency,
                            startDate,
                            if (hasEndDate) endDate else null
                        )
                    }
                },
                enabled = amount.toDoubleOrNull() != null && selectedCategory != null && description.isNotBlank()
            ) {
                Text(
                    if (recurringTransaction == null) stringResource(R.string.manage_budgets_create)
                    else stringResource(R.string.manage_budgets_save)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}
