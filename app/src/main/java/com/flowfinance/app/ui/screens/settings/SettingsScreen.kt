package com.flowfinance.app.ui.screens.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.data.local.model.BackupMetadata
import com.flowfinance.app.ui.viewmodel.SettingsViewModel
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    onProfileClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val userData by viewModel.userData.collectAsState()

    // State for delete confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }
    val CONFIRMATION_PHRASE = stringResource(R.string.dialog_delete_phrase)

    // State for Backup Dialogs
    var showExportBackupDialog by remember { mutableStateOf(false) }
    var showImportBackupDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var backupPasswordConfirm by remember { mutableStateOf("") }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var importedMetadata by remember { mutableStateOf<BackupMetadata?>(null) }
    var isCheckingFile by remember { mutableStateOf(false) }
    var fileCheckError by remember { mutableStateOf<String?>(null) }

    // State for Notification Dialog
    var showNotificationDialog by remember { mutableStateOf(false) }
    var tempHour by remember { mutableStateOf(userData.reminderHour) }
    var tempMinute by remember { mutableStateOf(userData.reminderMinute) }
    var tempInterval by remember { mutableStateOf(userData.reminderIntervalDays) }
    
    // File Picker for Import
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            importUri = uri
            showImportBackupDialog = true
            backupPassword = "" 
        }
    }

    LaunchedEffect(importUri) {
        if (importUri != null && showImportBackupDialog) {
            isCheckingFile = true
            fileCheckError = null
            importedMetadata = null
            viewModel.getBackupMetadata(importUri!!) { metadata ->
                isCheckingFile = false
                if (metadata != null) {
                    importedMetadata = metadata
                } else {
                    fileCheckError = context.getString(R.string.dialog_import_file_error)
                }
            }
        }
    }
    
    // Update temp states when userData loads
    LaunchedEffect(userData) {
        if (!showNotificationDialog) {
            tempHour = userData.reminderHour
            tempMinute = userData.reminderMinute
            tempInterval = userData.reminderIntervalDays
        }
    }

    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text(stringResource(R.string.dialog_notification_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.dialog_notification_desc))

                    // Time Picker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.dialog_notification_time))
                        TextButton(onClick = {
                            TimePickerDialog(context, { _, hour, minute ->
                                tempHour = hour
                                tempMinute = minute
                            }, tempHour, tempMinute, true).show()
                        }) {
                            Text(String.format(Locale.getDefault(), "%02d:%02d", tempHour, tempMinute), style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    // Interval Picker Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.dialog_notification_interval))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (tempInterval > 1) tempInterval-- }) {
                                Text("-", style = MaterialTheme.typography.titleLarge)
                            }
                            Text("$tempInterval", modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { if (tempInterval < 30) tempInterval++ }) {
                                Text("+", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }

                    HorizontalDivider()

                    Button(
                        onClick = { viewModel.sendTestNotification() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(stringResource(R.string.dialog_notification_test))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateReminderSettings(tempHour, tempMinute, tempInterval, true)
                    showNotificationDialog = false
                    Toast.makeText(context, context.getString(R.string.dialog_notification_saved), Toast.LENGTH_SHORT).show()
                }) {
                    Text(stringResource(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNotificationDialog = false 
                    // Reset to stored values
                    tempHour = userData.reminderHour
                    tempMinute = userData.reminderMinute
                    tempInterval = userData.reminderIntervalDays
                }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false 
                deleteConfirmationText = ""
            },
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_delete_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.dialog_delete_confirm_text, CONFIRMATION_PHRASE))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = { deleteConfirmationText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(CONFIRMATION_PHRASE) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        Toast.makeText(context, context.getString(R.string.dialog_delete_success), Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                        deleteConfirmationText = ""
                    },
                    enabled = deleteConfirmationText.equals(CONFIRMATION_PHRASE, ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.dialog_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    deleteConfirmationText = ""
                }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
    
    if (showExportBackupDialog) {
        AlertDialog(
            onDismissRequest = { 
                showExportBackupDialog = false 
                backupPassword = ""
                backupPasswordConfirm = ""
            },
            title = { Text(stringResource(R.string.dialog_export_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.dialog_export_desc))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.dialog_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = backupPasswordConfirm,
                        onValueChange = { backupPasswordConfirm = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.dialog_confirm_password_label)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = backupPassword.isNotEmpty() && backupPasswordConfirm.isNotEmpty() && backupPassword != backupPasswordConfirm
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                         if (backupPassword == backupPasswordConfirm && backupPassword.isNotEmpty()) {
                             viewModel.exportBackup(backupPassword) { filePath ->
                                 showExportBackupDialog = false
                                 backupPassword = ""
                                 backupPasswordConfirm = ""
                                 if (filePath != null) {
                                     shareFile(context, filePath, isBackup = true)
                                 } else {
                                     Toast.makeText(context, "Erro ao gerar backup.", Toast.LENGTH_SHORT).show()
                                 }
                             }
                         } else {
                             Toast.makeText(context, context.getString(R.string.dialog_password_mismatch), Toast.LENGTH_SHORT).show()
                         }
                    },
                    enabled = backupPassword.isNotEmpty() && backupPassword == backupPasswordConfirm
                ) {
                    Text(stringResource(R.string.dialog_export_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExportBackupDialog = false
                    backupPassword = ""
                    backupPasswordConfirm = ""
                }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    if (showImportBackupDialog && importUri != null) {
        AlertDialog(
            onDismissRequest = { 
                showImportBackupDialog = false 
                backupPassword = ""
                importedMetadata = null
                fileCheckError = null
            },
            title = { Text(stringResource(R.string.dialog_import_title)) },
            text = {
                Column {
                     if (isCheckingFile) {
                         Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                             CircularProgressIndicator()
                         }
                     } else if (fileCheckError != null) {
                         Text(fileCheckError!!, color = MaterialTheme.colorScheme.error)
                     } else if (importedMetadata != null) {
                         Text(stringResource(R.string.dialog_import_found), fontWeight = FontWeight.Bold)
                         Spacer(modifier = Modifier.height(8.dp))
                         Text(stringResource(R.string.dialog_app_version, importedMetadata?.appVersion ?: ""))
                         
                         val formattedDate = try {
                             val date = LocalDateTime.parse(importedMetadata?.createdAt)
                             date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                         } catch (e: Exception) {
                             importedMetadata?.createdAt ?: ""
                         }
                         
                         Text(stringResource(R.string.dialog_date, formattedDate))
                         Spacer(modifier = Modifier.height(16.dp))
                         Text(stringResource(R.string.dialog_import_password_desc))
                         Spacer(modifier = Modifier.height(8.dp))
                         OutlinedTextField(
                            value = backupPassword,
                            onValueChange = { backupPassword = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.dialog_password_label)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.dialog_import_warning), color = MaterialTheme.colorScheme.error)
                     }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importBackup(importUri!!, backupPassword) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) {
                                showImportBackupDialog = false
                                backupPassword = ""
                                importedMetadata = null
                            }
                        }
                    },
                    enabled = importedMetadata != null && backupPassword.isNotEmpty()
                ) {
                    Text(stringResource(R.string.dialog_import_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showImportBackupDialog = false
                    backupPassword = ""
                    importedMetadata = null
                }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingsSection(title = stringResource(R.string.settings_profile_section)) {
            SettingsItem(
                icon = Icons.Default.Person,
                title = userData.userName,
                subtitle = stringResource(R.string.settings_profile_subtitle, userData.currency),
                onClick = onProfileClick
            )
        }

        SettingsSection(title = stringResource(R.string.settings_preferences_section)) {
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.DarkMode, contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.settings_dark_theme)) },
                trailingContent = {
                    Switch(
                        checked = userData.isDarkTheme,
                        onCheckedChange = { viewModel.updateTheme(it) }
                    )
                }
            )
            
            // New Notification Button
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.Notifications, contentDescription = null)
                },
                headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                },
                modifier = Modifier.clickable { showNotificationDialog = true }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_data_section)) {
            SettingsItem(
                icon = Icons.Default.Download,
                title = stringResource(R.string.settings_export_data),
                subtitle = stringResource(R.string.settings_export_subtitle),
                onClick = {
                    viewModel.exportDataToCsv { filePath ->
                        if (filePath != null) {
                            shareFile(context, filePath)
                        } else {
                            Toast.makeText(context, "Erro ao exportar dados.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
            SettingsItem(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.settings_clear_data),
                subtitle = stringResource(R.string.settings_clear_subtitle),
                onClick = { showDeleteDialog = true }
            )
        }

        SettingsSection(title = stringResource(R.string.settings_backup_section)) {
            SettingsItem(
                icon = Icons.Default.Save,
                title = stringResource(R.string.settings_export_backup),
                subtitle = stringResource(R.string.settings_export_backup_subtitle),
                onClick = { showExportBackupDialog = true }
            )
            SettingsItem(
                icon = Icons.Default.Upload,
                title = stringResource(R.string.settings_import_backup),
                subtitle = stringResource(R.string.settings_import_backup_subtitle),
                onClick = { 
                    filePickerLauncher.launch(arrayOf("*/*")) 
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // About Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FlowFinance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Versão ${stringResource(id = R.string.app_version_name)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_view_source)) },
                leadingContent = { Icon(Icons.Default.Code, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gabrick75/FlowFinance"))
                        context.startActivity(intent)
                    }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_view_docs)) },
                leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Gabrick75/FlowFinance/blob/main/README.md"))
                        context.startActivity(intent)
                    }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.settings_developed_by),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

fun shareFile(context: Context, filePath: String, isBackup: Boolean = false) {
    val file = File(filePath)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    
    val mimeType = when {
        filePath.endsWith(".xls") -> "application/vnd.ms-excel"
        filePath.endsWith(".flowbackup") -> "application/octet-stream"
        else -> "text/csv"
    }
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    val title = if (isBackup) "Salvar Backup" else "Compartilhar Exportação"
    context.startActivity(Intent.createChooser(intent, title))
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ListItem(
        leadingContent = {
            Icon(icon, contentDescription = null)
        },
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
