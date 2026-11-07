package com.N3looG.thingtracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.N3looG.thingtracker.R
import com.N3looG.thingtracker.ui.theme.*
import com.N3looG.thingtracker.viewmodel.LoanViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: LoanViewModel,
    onOpenLanguageDialog: () -> Unit,
    onOpenPrivacyDialog: () -> Unit,
    onOpenManualDialog: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exportStatusText by remember { mutableStateOf("") }
    var pendingJsonToSave by remember { mutableStateOf("") }

    // File picker for saving JSON file directly to device storage / Documents / Downloads
    val saveDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingJsonToSave.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(pendingJsonToSave.toByteArray(Charsets.UTF_8))
                    exportStatusText = "Fișierul JSON a fost salvat cu succes în telefon!"
                }
            } catch (_: Exception) {
                exportStatusText = "Eroare la salvarea fișierului."
            }
            pendingJsonToSave = ""
        }
    }

    // File picker for import backup
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val jsonString = reader.readText()
                    coroutineScope.launch {
                        viewModel.importFromJson(jsonString)
                        exportStatusText = "Backup importat cu succes!"
                    }
                }
            } catch (_: Exception) {
                exportStatusText = "Eroare la importarea backup-ului. Asigurați-vă că este un fișier JSON valid."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.nav_settings),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Language Option
        SettingsItem(
            icon = Icons.Default.Language,
            title = stringResource(id = R.string.dialog_language_title),
            onClick = onOpenLanguageDialog
        )

        // User Manual Option
        SettingsItem(
            icon = Icons.Default.Book,
            title = stringResource(id = R.string.manual_title),
            onClick = onOpenManualDialog
        )

        // Privacy Policy Option
        SettingsItem(
            icon = Icons.Default.Info,
            title = stringResource(id = R.string.privacy_title),
            onClick = onOpenPrivacyDialog
        )

        HorizontalDivider(color = TechSurface, thickness = 1.dp)

        // 1. Save JSON Backup to Phone Storage (Documents / Downloads)
        SettingsItem(
            icon = Icons.Default.Download,
            title = "Salvează fișierul JSON în Telefon",
            onClick = {
                coroutineScope.launch {
                    pendingJsonToSave = viewModel.exportToJson()
                    val fileName = "thing_tracker_backup_${System.currentTimeMillis()}.json"
                    saveDocLauncher.launch(fileName)
                }
            }
        )

        // 2. Share JSON Backup File as Attachment (WhatsApp, Google Drive, Email)
        SettingsItem(
            icon = Icons.Default.Share,
            title = "Trimite/Partajează fișierul JSON (WhatsApp, Drive)",
            onClick = {
                coroutineScope.launch {
                    val json = viewModel.exportToJson()
                    try {
                        val cacheFile = File(context.cacheDir, "thing_tracker_backup.json")
                        cacheFile.writeText(json, Charsets.UTF_8)

                        val fileUri: Uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            cacheFile
                        )

                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, fileUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Partajează Backup JSON"))
                        exportStatusText = "Pregătit pentru trimitere..."
                    } catch (_: Exception) {
                        exportStatusText = "Eroare la partajarea fișierului."
                    }
                }
            }
        )

        // 3. Import JSON Backup
        SettingsItem(
            icon = Icons.Default.Upload,
            title = "Import Backup (JSON)",
            onClick = {
                importLauncher.launch(arrayOf("application/json", "*/*"))
            }
        )

        if (exportStatusText.isNotBlank()) {
            Text(
                text = exportStatusText,
                fontSize = 13.sp,
                color = TechPrimary
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = TechSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TechSecondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = TechTextPrimary
            )
        }
    }
}
