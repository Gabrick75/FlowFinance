package com.flowfinance.app.ui.screens.planning

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Commute
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.data.local.entity.Category
import com.flowfinance.app.ui.viewmodel.DeleteResult
import com.flowfinance.app.ui.viewmodel.ManageBudgetsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBudgetsScreen(
    onBackClick: () -> Unit,
    viewModel: ManageBudgetsViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    var showCreateCategoryDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.deleteResult.collectLatest {
            when (it) {
                is DeleteResult.Success -> scope.launch {
                    snackbarHostState.showSnackbar(it.message)
                }
                is DeleteResult.Failure -> Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showCreateCategoryDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateCategoryDialog = false },
            onCreate = { name, color, iconName ->
                viewModel.createNewCategory(name, color.toArgb(), iconName)
                showCreateCategoryDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gerenciar Metas") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateCategoryDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nova Categoria")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(categories, key = { it.id }) { category ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.deleteCategory(category)
                            // We don't want to immediately dismiss, we wait for VM feedback
                            // but for simplicity for now, we will assume it might fail and stay
                            false
                        } else false
                    },
                    positionalThreshold = { it * .25f }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color by animateColorAsState(
                            if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart && !category.isDefault) Color.Red.copy(alpha = 0.8f) else Color.Transparent
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
                                contentDescription = "Delete",
                                modifier = Modifier.scale(scale),
                                tint = Color.White
                            )
                        }
                    },
                    content = {
                         BudgetCategoryItem(category = category, onBudgetChange = { newBudget ->
                            viewModel.updateBudget(category, newBudget)
                        })
                    },
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = !category.isDefault
                )
            }
        }
    }
}

@Composable
fun CreateCategoryDialog(onDismiss: () -> Unit, onCreate: (String, Color, String) -> Unit) {
    var newCategoryName by remember { mutableStateOf("") }

    val colors = listOf(
        Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFA726), Color(0xFF66BB6A), Color(0xFFAB47BC)
    )
    var selectedColor by remember { mutableStateOf(colors.first()) }

    val icons = remember { getCategoryIconMap() }
    var selectedIconName by remember { mutableStateOf(icons.keys.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Categoria") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nome da Categoria") },
                    singleLine = true
                )

                // Color Picker
                Text("Cor", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = color }
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }

                // Icon Picker
                Text("Ícone", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(icons.entries.toList()) { (name, icon) ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (selectedIconName == name) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                .clickable { selectedIconName = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newCategoryName.isNotBlank()) {
                        onCreate(newCategoryName, selectedColor, selectedIconName)
                    }
                },
                enabled = newCategoryName.isNotBlank()
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


@Composable
fun BudgetCategoryItem(
    category: Category,
    onBudgetChange: (Double?) -> Unit
) {
    var budget by remember(category.budgetLimit) { mutableStateOf(category.budgetLimit?.toString() ?: "") }

    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(8.dp).clip(RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(category.color)),
                contentAlignment = Alignment.Center
            ) {
                val icon = rememberCategoryIcon(category.icon)
                if (icon != null) {
                    Icon(icon, contentDescription = category.name, tint = Color.White)
                } else {
                    Text(
                        text = category.name.take(1).uppercase(),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(modifier = Modifier.padding(8.dp))
            Text(text = category.name, style = MaterialTheme.typography.titleMedium)
        }

        OutlinedTextField(
            value = budget,
            onValueChange = { 
                budget = it
                onBudgetChange(it.toDoubleOrNull())
            },
            modifier = Modifier.width(120.dp),
            label = { Text("Meta") },
            prefix = { Text("R$ ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
    }
}

@Composable
fun rememberCategoryIcon(iconName: String?): ImageVector? {
    return getCategoryIconMap()[iconName]
}

fun getCategoryIconMap(): Map<String, ImageVector> {
    return mapOf(
        "restaurant" to Icons.Default.Fastfood,
        "attractions" to Icons.Default.SportsEsports,
        "commute" to Icons.Default.Commute,
        "health_and_safety" to Icons.Default.HealthAndSafety,
        "school" to Icons.Default.School,
        "payments" to Icons.Default.TrendingUp,
        "trending_up" to Icons.Default.TrendingUp
    )
}
