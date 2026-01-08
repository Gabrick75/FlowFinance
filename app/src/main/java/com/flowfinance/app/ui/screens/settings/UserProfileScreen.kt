package com.flowfinance.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userData by viewModel.userData.collectAsState()
    var name by remember(userData.userName) { mutableStateOf(userData.userName) }
    var selectedCurrency by remember(userData.currency) { mutableStateOf(userData.currency) }
    var selectedLanguage by remember(userData.language) { mutableStateOf(userData.language) }
    
    val maxNameLength = 50
    // Simple list of currencies for demo
    val currencies = listOf("BRL", "USD", "EUR")
    
    // Language options: Code -> Resource ID
    val languages = listOf(
        "" to R.string.lang_default,
        "en" to R.string.lang_en,
        "pt-BR" to R.string.lang_pt,
        "es" to R.string.lang_es
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Name Input
                Column {
                    Text(
                        text = stringResource(R.string.name_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= maxNameLength) name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.name_placeholder)) },
                        singleLine = true,
                        supportingText = { Text("${name.length} / $maxNameLength") }
                    )
                }

                // Currency Selection
                Column {
                    Text(
                        text = stringResource(R.string.currency_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    currencies.forEach { currency ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedCurrency == currency,
                                onClick = { selectedCurrency = currency }
                            )
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                // Language Selection
                Column {
                    Text(
                        text = stringResource(R.string.language_label),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    languages.forEach { (code, nameRes) ->
                        // Determine if selected.
                        // If selectedLanguage is empty or "en", we might consider "en" as default.
                        // Let's assume userData.language stores the code.
                        val isSelected = selectedLanguage == code
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedLanguage = code }
                            )
                            Text(
                                text = stringResource(nameRes),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.updateUserName(name)
                    viewModel.updateCurrency(selectedCurrency)
                    viewModel.updateLanguage(selectedLanguage)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_changes))
            }
        }
    }
}
