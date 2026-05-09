package com.seucaixa.caixacombo.ui.screens.configuracao

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracaoCoresScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE) }
    
    var primaryColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt()))) 
    }
    var secondaryColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("secondary_color", 0xFF03DAC6.toInt()))) 
    }
    var tertiaryColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("tertiary_color", 0xFFFF9800.toInt()))) 
    }
    var backgroundColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt()))) 
    }
    var buttonSuccessColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("button_success_color", 0xFF4CAF50.toInt()))) 
    }
    var buttonErrorColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("button_error_color", 0xFFF44336.toInt()))) 
    }
    var surfaceColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("surface_color", 0xFFFFFBFE.toInt()))) 
    }
    var tituloTamanho by remember { 
        mutableStateOf(sharedPreferences.getFloat("titulo_tamanho", 31f)) 
    }
    var tituloTexto by remember { 
        mutableStateOf(sharedPreferences.getString("titulo_texto", "Rodrigo Dev MT") ?: "Rodrigo Dev MT") 
    }
    var rodapeTexto by remember { 
        mutableStateOf(sharedPreferences.getString("rodape_texto", "") ?: "") 
    }
    var espacamentoAcima by remember { 
        mutableStateOf(sharedPreferences.getFloat("espacamento_acima", 5f)) 
    }
    var espacamentoAbaixo by remember { 
        mutableStateOf(sharedPreferences.getFloat("espacamento_abaixo", 3f)) 
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuração de Cores") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Seção de Configurações de Texto
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Configurações de Texto HomeScreen",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        // Texto do Título
                        OutlinedTextField(
                            value = tituloTexto,
                            onValueChange = { tituloTexto = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Texto do Título") },
                            placeholder = { Text("Digite o título...") }
                        )

                        // Texto do Rodapé
                        OutlinedTextField(
                            value = rodapeTexto,
                            onValueChange = { rodapeTexto = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Texto do Rodapé") },
                            placeholder = { Text("Digite o rodapé (opcional)...") }
                        )

                        // Tamanho do Título
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Tamanho do Título: ${tituloTamanho.toInt()}sp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Slider(
                                value = tituloTamanho,
                                onValueChange = { tituloTamanho = it },
                                valueRange = 24f..72f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Espaçamento Acima do Título
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Espaçamento Acima: ${espacamentoAcima.toInt()}dp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Slider(
                                value = espacamentoAcima,
                                onValueChange = { espacamentoAcima = it },
                                valueRange = 0f..80f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Espaçamento Abaixo do Título
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Espaçamento Abaixo: ${espacamentoAbaixo.toInt()}dp",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                            Slider(
                                value = espacamentoAbaixo,
                                onValueChange = { espacamentoAbaixo = it },
                                valueRange = 0f..80f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Seção de Cores
                Text(
                    "Cores do Sistema",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                // Cor Primária
                ColorCard(
                    title = "Cor Primária",
                    color = primaryColor,
                    onColorChange = { 
                        primaryColor = it
                        sharedPreferences.edit().putInt("primary_color", it.hashCode()).apply()
                    },
                    colorOptions = listOf(
                        Color(0xFF6200EE),
                        Color(0xFF1976D2),
                        Color(0xFF388E3C),
                        Color(0xFFF57C00),
                        Color(0xFFE91E63),
                        Color(0xFF9C27B0)
                    )
                )

                // Cor Secundária
                ColorCard(
                    title = "Cor Secundária",
                    color = secondaryColor,
                    onColorChange = { 
                        secondaryColor = it
                        sharedPreferences.edit().putInt("secondary_color", it.hashCode()).apply()
                    },
                    colorOptions = listOf(
                        Color(0xFF03DAC6),
                        Color(0xFF00BCD4),
                        Color(0xFFFF9800),
                        Color(0xFF4CAF50),
                        Color(0xFF2196F3),
                        Color(0xFF9E9E9E)
                    )
                )

                // Cor Terciária
                ColorCard(
                    title = "Cor Terciária",
                    color = tertiaryColor,
                    onColorChange = { 
                        tertiaryColor = it
                        sharedPreferences.edit().putInt("tertiary_color", it.hashCode()).apply()
                    },
                    colorOptions = listOf(
                        Color(0xFFFF9800),
                        Color(0xFFFFEB3B),
                        Color(0xFF009688),
                        Color(0xFF795548),
                        Color(0xFF607D8B),
                        Color(0xFF8BC34A)
                    )
                )

                // Cor de Fundo
                ColorCard(
                    title = "Cor de Fundo",
                    color = backgroundColor,
                    onColorChange = { 
                        backgroundColor = it
                        sharedPreferences.edit().putInt("background_color", it.hashCode()).apply()
                    },
                    colorOptions = listOf(
                        Color(0xFFFFFBFE),
                        Color(0xFFF5F5F5),
                        Color(0xFFE0E0E0),
                        Color(0xFF212121),
                        Color(0xFF1A1A1A),
                        Color(0xFF2D2D2D)
                    )
                )

                // Cor de Botão Sucesso
                ColorCard(
                    title = "Cor Botão Sucesso",
                    color = buttonSuccessColor,
                    onColorChange = { 
                        buttonSuccessColor = it
                        sharedPreferences.edit().putInt("button_success_color", it.hashCode()).apply()
                    },
                    colorOptions = listOf(
                        Color(0xFF4CAF50),
                        Color(0xFF66BB6A),
                        Color(0xFF43A047),
                        Color(0xFF00E676),
                        Color(0xFF00C853),
                        Color(0xFF2E7D32)
                    )
                )

                // Cor de Botão Erro
                ColorCard(
                    title = "Cor Botão Erro",
                    color = buttonErrorColor,
                    onColorChange = { 
                        buttonErrorColor = it
                        sharedPreferences.edit().putInt("button_error_color", it.hashCode()).apply()
                    },
                    colorOptions = listOf(
                        Color(0xFFF44336),
                        Color(0xFFEF5350),
                        Color(0xFFE53935),
                        Color(0xFFFF5252),
                        Color(0xFFD32F2F),
                        Color(0xFFC62828)
                    )
                )
            }

            // Botões de ação
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botão Aplicar Cores (Reinicia o app)
                Button(
                    onClick = {
                        // Salvar todas as cores e espaçamentos
                        sharedPreferences.edit()
                            .putInt("primary_color", primaryColor.hashCode())
                            .putInt("secondary_color", secondaryColor.hashCode())
                            .putInt("tertiary_color", tertiaryColor.hashCode())
                            .putInt("background_color", backgroundColor.hashCode())
                            .putInt("button_success_color", buttonSuccessColor.hashCode())
                            .putInt("button_error_color", buttonErrorColor.hashCode())
                            .putInt("surface_color", surfaceColor.hashCode())
                            .putFloat("titulo_tamanho", tituloTamanho)
                            .putString("titulo_texto", tituloTexto)
                            .putString("rodape_texto", rodapeTexto)
                            .putFloat("espacamento_acima", espacamentoAcima)
                            .putFloat("espacamento_abaixo", espacamentoAbaixo)
                            .apply()
                        
                        // Reiniciar o app para aplicar as cores
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        android.os.Process.killProcess(android.os.Process.myPid())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        "APLICAR CORES",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
                
                // Botão Salvar (apenas salva, não reinicia)
                OutlinedButton(
                    onClick = {
                        sharedPreferences.edit()
                            .putInt("primary_color", primaryColor.hashCode())
                            .putInt("secondary_color", secondaryColor.hashCode())
                            .putInt("tertiary_color", tertiaryColor.hashCode())
                            .putInt("background_color", backgroundColor.hashCode())
                            .putInt("button_success_color", buttonSuccessColor.hashCode())
                            .putInt("button_error_color", buttonErrorColor.hashCode())
                            .putInt("surface_color", surfaceColor.hashCode())
                            .putFloat("titulo_tamanho", tituloTamanho)
                            .putString("titulo_texto", tituloTexto)
                            .putString("rodape_texto", rodapeTexto)
                            .putFloat("espacamento_acima", espacamentoAcima)
                            .putFloat("espacamento_abaixo", espacamentoAbaixo)
                            .apply()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "Salvar e Voltar",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }
    }
}

@Composable
fun ColorCard(
    title: String,
    color: Color,
    onColorChange: (Color) -> Unit,
    colorOptions: List<Color>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(color, RoundedCornerShape(12.dp))
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Selecione uma cor:")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { colorOption ->
                            ColorOption(
                                color = colorOption,
                                selected = color == colorOption,
                                onClick = { onColorChange(colorOption) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOption(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, RoundedCornerShape(8.dp))
            .then(if (selected) Modifier.size(44.dp) else Modifier)
            .clickable { onClick() }
    )
}
