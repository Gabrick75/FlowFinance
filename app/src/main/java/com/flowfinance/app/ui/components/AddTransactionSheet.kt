package com.flowfinance.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.ui.screens.planning.rememberCategoryIcon
import com.flowfinance.app.ui.viewmodel.AddTransactionViewModel
import com.flowfinance.app.util.RecurrenceFrequency
import com.flowfinance.app.util.TransactionType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    sheetState: SheetState,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }
    var frequencyMenuExpanded by remember { mutableStateOf(false) }
    var hasEndDate by remember { mutableStateOf(false) }
    var recurrenceEndDate by remember { mutableStateOf(LocalDate.now().plusMonths(1)) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val maxDescriptionLength = 35

    val categories by if (selectedType == TransactionType.INCOME) {
        viewModel.incomeCategories.collectAsState()
    } else {
        viewModel.expenseCategories.collectAsState()
    }

    LaunchedEffect(categories) {
        if (selectedCategory != null && categories.none { it.id == selectedCategory!!.id }) {
            selectedCategory = null
        }
    }

    if (showDatePicker) {
        SimpleDatePickerDialog(
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedDate = it; showDatePicker = false }
        )
    }

    if (showEndDatePicker) {
        SimpleDatePickerDialog(
            initialDate = recurrenceEndDate,
            onDismiss = { showEndDatePicker = false },
            onConfirm = { recurrenceEndDate = it; showEndDatePicker = false }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.add_transaction_title),
                style = MaterialTheme.typography.titleLarge
            )

            // Transaction Type Selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = TransactionType.values()
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size)
                    ) {
                        Text(text = if (type == TransactionType.INCOME) stringResource(R.string.add_transaction_income) else stringResource(R.string.add_transaction_expense))
                    }
                }
            }

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                label = { Text(stringResource(R.string.add_transaction_value_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("R$ ") }
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= maxDescriptionLength) description = it },
                label = { Text(stringResource(R.string.add_transaction_desc_label)) },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("${description.length} / $maxDescriptionLength") }
            )

            // Category Selector
            Text(
                text = stringResource(R.string.add_transaction_category_label),
                style = MaterialTheme.typography.titleMedium
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory?.id == category.id,
                        onClick = { selectedCategory = category }
                    )
                }
            }

            // Date Selector
            DateField(
                label = stringResource(R.string.add_transaction_date_label),
                date = selectedDate,
                onClick = { showDatePicker = true }
            )

            // Repeat Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.add_transaction_repeat_label),
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
            }

            if (isRecurring) {
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = hasEndDate, onCheckedChange = { hasEndDate = it })
                    Text(stringResource(R.string.recurring_end_date_toggle))
                }

                if (hasEndDate) {
                    DateField(
                        label = stringResource(R.string.recurring_end_date_label),
                        date = recurrenceEndDate,
                        onClick = { showEndDatePicker = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull()
                    if (amountDouble != null && selectedCategory != null) {
                        viewModel.saveTransaction(
                            description = description,
                            amount = amountDouble,
                            type = selectedType,
                            categoryId = selectedCategory!!.id,
                            date = selectedDate,
                            recurrence = if (isRecurring) selectedFrequency else null,
                            recurrenceEndDate = if (isRecurring && hasEndDate) recurrenceEndDate else null
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotEmpty() && description.isNotEmpty() && selectedCategory != null
            ) {
                Text(stringResource(R.string.add_transaction_save))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onClick: () -> Unit
) {
    val datePattern = stringResource(R.string.date_format)
    OutlinedTextField(
        value = date.format(DateTimeFormatter.ofPattern(datePattern, Locale.getDefault())),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
        modifier = Modifier.fillMaxWidth(),
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect {
                        if (it is PressInteraction.Release) {
                            onClick()
                        }
                    }
                }
            }
    )
}

@Composable
fun recurrenceFrequencyLabel(frequency: RecurrenceFrequency): String {
    return when (frequency) {
        RecurrenceFrequency.DAILY -> stringResource(R.string.recurring_frequency_daily)
        RecurrenceFrequency.WEEKLY -> stringResource(R.string.recurring_frequency_weekly)
        RecurrenceFrequency.MONTHLY -> stringResource(R.string.recurring_frequency_monthly)
        RecurrenceFrequency.YEARLY -> stringResource(R.string.recurring_frequency_yearly)
    }
}

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary else Color(category.color).copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            val icon = rememberCategoryIcon(category.icon)

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = category.name,
                    tint = Color(category.color)
                )
            } else {
                Text(
                    text = category.name.take(1).uppercase(),
                    color = Color(category.color),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
