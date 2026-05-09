package com.seucaixa.caixacombo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.seucaixa.caixacombo.service.PollingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var darkMode by remember { mutableStateOf(false) }
    var serverUrl by remember {
        mutableStateOf(PollingService.getServerUrl())
    }
    var showServerDialog by remember { mutableStateOf(false) }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("URL do Servidor") },
            text = {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    PollingService.configureServer(context, serverUrl)
                    showServerDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Servidor
            ConfiguracaoItem(
                icon = Icons.Default.Cloud,
                titulo = "Servidor Dashboard",
                subtitulo = PollingService.getServerUrl(),
                onClick = { showServerDialog = true }
            )

            HorizontalDivider()

            // Tema Escuro
            ConfiguracaoSwitchItem(
                icon = Icons.Default.DarkMode,
                titulo = "Tema Escuro",
                subtitulo = "Ativar modo escuro",
                checked = darkMode,
                onCheckedChange = { darkMode = it }
            )

            HorizontalDivider()

            // Idioma
            ConfiguracaoItem(
                icon = Icons.Default.Language,
                titulo = "Idioma",
                subtitulo = "Português (Brasil) - Em breve",
                onClick = { }
            )

            // Moeda
            ConfiguracaoItem(
                icon = Icons.Default.AttachMoney,
                titulo = "Moeda",
                subtitulo = "Real (R$) - Em breve",
                onClick = { }
            )

            // Impressora
            ConfiguracaoItem(
                icon = Icons.Default.Print,
                titulo = "Impressora",
                subtitulo = "Configurar impressora de recibos - Em breve",
                onClick = { }
            )

            HorizontalDivider()

            // Backup
            ConfiguracaoItem(
                icon = Icons.Default.Backup,
                titulo = "Backup de Dados",
                subtitulo = "Exportar/importar dados - Em breve",
                onClick = { }
            )

            // Sobre
            ConfiguracaoItem(
                icon = Icons.Default.Info,
                titulo = "Sobre",
                subtitulo = "Caixa Combo v1.0.0",
                onClick = { }
            )
        }
    }
}

@Composable
fun ConfiguracaoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(subtitulo) },
        leadingContent = { Icon(icon, null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun ConfiguracaoSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(titulo) },
        supportingContent = { Text(subtitulo) },
        leadingContent = { Icon(icon, null) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}
