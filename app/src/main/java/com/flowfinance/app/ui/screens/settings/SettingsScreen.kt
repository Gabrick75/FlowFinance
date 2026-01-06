package com.flowfinance.app.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.flowfinance.app.R
import com.flowfinance.app.ui.viewmodel.SettingsViewModel
import java.io.File

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
    val CONFIRMATION_PHRASE = "tenho certeza"

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false 
                deleteConfirmationText = ""
            },
            title = { Text("Atenção: Apagar Tudo") },
            text = {
                Column {
                    Text("Esta ação é irreversível. Todos os seus dados serão apagados permanentemente.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Digite \"$CONFIRMATION_PHRASE\" para confirmar:")
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
                        Toast.makeText(context, "Dados apagados com sucesso.", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                        deleteConfirmationText = ""
                    },
                    enabled = deleteConfirmationText.equals(CONFIRMATION_PHRASE, ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Apagar Tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    deleteConfirmationText = ""
                }) {
                    Text("Cancelar")
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
            text = "Configurações",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SettingsSection(title = "Perfil") {
            SettingsItem(
                icon = Icons.Default.Person,
                title = userData.userName,
                subtitle = "Toque para editar perfil e moeda (${userData.currency})",
                onClick = onProfileClick
            )
        }

        SettingsSection(title = "Preferências") {
            ListItem(
                leadingContent = {
                    Icon(Icons.Default.DarkMode, contentDescription = null)
                },
                headlineContent = { Text("Tema Escuro") },
                trailingContent = {
                    Switch(
                        checked = userData.isDarkTheme,
                        onCheckedChange = { viewModel.updateTheme(it) }
                    )
                }
            )
        }

        SettingsSection(title = "Dados") {
            SettingsItem(
                icon = Icons.Default.Download,
                title = "Exportar Dados (CSV)",
                subtitle = "Salvar planilha de transações",
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
                title = "Limpar Dados",
                subtitle = "Apagar todas as transações",
                onClick = { showDeleteDialog = true }
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
                headlineContent = { Text("Ver código fonte") },
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
                headlineContent = { Text("Ver documentação") },
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
                text = "Desenvolvido por Gabrick75",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

fun shareFile(context: Context, filePath: String) {
    val file = File(filePath)
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    
    context.startActivity(Intent.createChooser(intent, "Compartilhar Exportação"))
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
