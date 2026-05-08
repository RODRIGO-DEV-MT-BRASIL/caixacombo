package com.seucaixa.caixacombo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Switch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracoesScreen(
    onNavigateBack: () -> Unit
) {
    var darkMode by remember { mutableStateOf(false) }
    
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
