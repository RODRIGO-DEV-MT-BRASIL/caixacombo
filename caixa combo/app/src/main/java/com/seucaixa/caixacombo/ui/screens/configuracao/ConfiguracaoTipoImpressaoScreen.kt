package com.seucaixa.caixacombo.ui.screens.configuracao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seucaixa.caixacombo.ui.viewmodel.ConfiguracaoImpressaoViewModel
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracaoTipoImpressaoScreen(
    viewModel: ConfiguracaoImpressaoViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("config_impressao", Context.MODE_PRIVATE) }
    val configuracao by viewModel.configuracao.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    // Launcher para selecionar imagem da galeria
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(it) ?: return@let
                val bytes = inputStream.readBytes()
                val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                viewModel.updateLogoBase64(base64)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val tabs = listOf("Opções", "Cabeçalho", "Rodapé", "Logo", "Cores")
    
    // Estados das configurações
    var imprimirTotal by remember { 
        mutableStateOf(sharedPreferences.getBoolean("imprimir_total", true)) 
    }
    var imprimirFichas by remember { 
        mutableStateOf(sharedPreferences.getBoolean("imprimir_fichas", true)) 
    }
    
    // Estados das cores
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
        mutableStateOf(sharedPreferences.getString("titulo_texto", "☀ QUINTAL BAR ☀") ?: "☀ QUINTAL BAR ☀") 
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
                title = { Text("Configuração de Impressão") },
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
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontSize = 11.sp) }
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Opções de Impressão
                        // Explicação
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Opções de Impressão ao Finalizar Venda",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Selecione quais opções de impressão serão mostradas na tela de venda finalizada.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Opção Imprimir Total
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Imprimir Total",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Mostra botão para imprimir comprovante completo da venda",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = imprimirTotal,
                                    onCheckedChange = { imprimirTotal = it }
                                )
                            }
                        }

                        // Opção Imprimir Fichas
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Imprimir Fichas",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Mostra botão para imprimir fichas de produção separadas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = imprimirFichas,
                                    onCheckedChange = { imprimirFichas = it }
                                )
                            }
                        }

                        // Preview das opções selecionadas
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Preview - Opções que serão mostradas:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                if (!imprimirTotal && !imprimirFichas) {
                                    Text(
                                        "Nenhuma opção de impressão será mostrada",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    if (imprimirTotal) {
                                        Text(
                                            "✓ Imprimir Total",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (imprimirFichas) {
                                        Text(
                                            "✓ Imprimir Fichas",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Botão Salvar
                        Button(
                            onClick = {
                                sharedPreferences.edit()
                                    .putBoolean("imprimir_total", imprimirTotal)
                                    .putBoolean("imprimir_fichas", imprimirFichas)
                                    .apply()
                                onBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = imprimirTotal || imprimirFichas
                        ) {
                            Text(
                                "Salvar Configurações",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    1 -> {
                        // Cabeçalho
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Cabeçalho",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.titulo ?: "",
                                    onValueChange = viewModel::updateTitulo,
                                    label = { Text("Título (ex: Quintal Bar)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = configuracao?.cnpj ?: "",
                                        onValueChange = viewModel::updateCnpj,
                                        label = { Text("CNPJ") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = configuracao?.inscricaoEstadual ?: "",
                                        onValueChange = viewModel::updateInscricaoEstadual,
                                        label = { Text("IE") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = configuracao?.telefone ?: "",
                                        onValueChange = viewModel::updateTelefone,
                                        label = { Text("Telefone") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    
                                    OutlinedTextField(
                                        value = configuracao?.email ?: "",
                                        onValueChange = viewModel::updateEmail,
                                        label = { Text("Email") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                                
                                OutlinedTextField(
                                    value = configuracao?.endereco ?: "",
                                    onValueChange = viewModel::updateEndereco,
                                    label = { Text("Endereço") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.cidade ?: "",
                                    onValueChange = viewModel::updateCidade,
                                    label = { Text("Cidade - UF") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.cep ?: "",
                                    onValueChange = viewModel::updateCep,
                                    label = { Text("CEP") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                viewModel.salvarConfiguracao(
                                    onSuccess = { onBack() },
                                    onError = {}
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Salvar Cabeçalho",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    2 -> {
                        // Rodapé
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Rodapé",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha1 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha1,
                                    label = { Text("Linha 1") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha2 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha2,
                                    label = { Text("Linha 2") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha3 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha3,
                                    label = { Text("Linha 3") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha4 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha4,
                                    label = { Text("Linha 4") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                viewModel.salvarConfiguracao(
                                    onSuccess = { onBack() },
                                    onError = {}
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Salvar Rodapé",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    3 -> {
                        // Logo
                        // Seção de Seleção de Logo
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Logo",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Text(
                                    "Selecione uma imagem para usar como logo",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                
                                // Placeholder para upload de logo
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { 
                                            if (configuracao?.logoBase64.isNullOrEmpty()) {
                                                imagePickerLauncher.launch("image/*")
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (configuracao?.logoBase64.isNullOrEmpty()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Image,
                                                contentDescription = "Logo",
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Clique para selecionar logo",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            val logoBitmap = remember(configuracao?.logoBase64) {
                                                try {
                                                    val bytes = android.util.Base64.decode(configuracao?.logoBase64, android.util.Base64.DEFAULT)
                                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            }
                                            
                                            if (logoBitmap != null) {
                                                androidx.compose.foundation.Image(
                                                    painter = BitmapPainter(logoBitmap.asImageBitmap()),
                                                    contentDescription = "Logo selecionada",
                                                    modifier = Modifier
                                                        .size(
                                                            width = (configuracao?.logoLargura ?: 150f).dp.coerceAtMost(200.dp),
                                                            height = (configuracao?.logoAltura ?: 80f).dp.coerceAtMost(150.dp)
                                                        )
                                                        .clickable { 
                                                            imagePickerLauncher.launch("image/*")
                                                        }
                                                )
                                            }
                                            
                                            TextButton(
                                                onClick = { viewModel.updateLogoBase64("") }
                                            ) {
                                                Text("Remover logo", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "Tamanho e Espaçamento:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // Altura da logo
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Altura: ${configuracao?.logoAltura?.toInt() ?: 80}dp", fontSize = 12.sp)
                                    Slider(
                                        value = configuracao?.logoAltura ?: 80f,
                                        onValueChange = viewModel::updateLogoAltura,
                                        valueRange = 40f..300f,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    )
                                }
                                
                                // Largura da logo
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Largura: ${configuracao?.logoLargura?.toInt() ?: 300}dp", fontSize = 12.sp)
                                    Slider(
                                        value = configuracao?.logoLargura ?: 300f,
                                        onValueChange = viewModel::updateLogoLargura,
                                        valueRange = 100f..500f,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    )
                                }
                                
                                // Espaçamento acima
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Espaço acima: ${configuracao?.logoEspacamentoAcima?.toInt() ?: 16}dp", fontSize = 12.sp)
                                    Slider(
                                        value = configuracao?.logoEspacamentoAcima ?: 16f,
                                        onValueChange = viewModel::updateLogoEspacamentoAcima,
                                        valueRange = 0f..64f,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    )
                                }
                                
                                // Espaçamento abaixo
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Espaço abaixo: ${configuracao?.logoEspacamentoAbaixo?.toInt() ?: 16}dp", fontSize = 12.sp)
                                    Slider(
                                        value = configuracao?.logoEspacamentoAbaixo ?: 16f,
                                        onValueChange = viewModel::updateLogoEspacamentoAbaixo,
                                        valueRange = 0f..64f,
                                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "Onde usar a logo:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("HomeScreen", fontSize = 12.sp)
                                                Switch(
                                                    checked = configuracao?.logoHomeScreen ?: false,
                                                    onCheckedChange = viewModel::updateLogoHomeScreen
                                                )
                                            }
                                        }
                                        
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Abertura", fontSize = 12.sp)
                                                Switch(
                                                    checked = configuracao?.logoAbertura ?: false,
                                                    onCheckedChange = viewModel::updateLogoAbertura
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Fechamento", fontSize = 12.sp)
                                                Switch(
                                                    checked = configuracao?.logoFechamento ?: false,
                                                    onCheckedChange = viewModel::updateLogoFechamento
                                                )
                                            }
                                        }
                                        
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Venda", fontSize = 12.sp)
                                                Switch(
                                                    checked = configuracao?.logoVenda ?: false,
                                                    onCheckedChange = viewModel::updateLogoVenda
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Sangria", fontSize = 12.sp)
                                                Switch(
                                                    checked = configuracao?.logoSangria ?: false,
                                                    onCheckedChange = viewModel::updateLogoSangria
                                                )
                                            }
                                        }
                                        
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Suprimento", fontSize = 12.sp)
                                                Switch(
                                                    checked = configuracao?.logoSuprimento ?: false,
                                                    onCheckedChange = viewModel::updateLogoSuprimento
                                                )
                                            }
                                        }
                                    }
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Ficha", fontSize = 12.sp)
                                                Switch(
                                                    checked = configuracao?.logoFicha ?: false,
                                                    onCheckedChange = viewModel::updateLogoFicha
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = {
                                viewModel.salvarConfiguracao(
                                    onSuccess = { onBack() },
                                    onError = {}
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                "Salvar Logo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    4 -> {
                        // Cores
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
                                    fontWeight = FontWeight.Bold,
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
                                        fontWeight = FontWeight.Medium
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
                                        fontWeight = FontWeight.Medium
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
                                        fontWeight = FontWeight.Medium
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
                            fontWeight = FontWeight.Bold
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
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
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
                                intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
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
    }
}
