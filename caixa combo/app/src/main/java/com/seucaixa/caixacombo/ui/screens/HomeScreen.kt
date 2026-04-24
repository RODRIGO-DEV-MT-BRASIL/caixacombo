package com.seucaixa.caixacombo.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seucaixa.caixacombo.ui.viewmodel.ConfiguracaoImpressaoViewModel
import com.seucaixa.caixacombo.data.repository.ConfiguracaoImpressaoRepository
import com.seucaixa.caixacombo.data.database.AppDatabase
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    caixaAberto: Boolean = false,
    onNavigateToCheckout: () -> Unit,
    onNavigateToCaixa: () -> Unit,
    onNavigateToProdutos: () -> Unit,
    onNavigateToVendas: () -> Unit,
    onNavigateToConfiguracao: () -> Unit,
    onNavigateToConfiguracaoCores: () -> Unit,
    onNavigateToConfiguracaoTipoImpressao: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE) }
    
    // ViewModel de configuração de impressão para carregar logo
    val configuracaoImpressaoViewModel: ConfiguracaoImpressaoViewModel = viewModel(
        factory = ConfiguracaoImpressaoViewModel.Factory(
            ConfiguracaoImpressaoRepository(
                AppDatabase.getDatabase(context).configuracaoImpressaoDao()
            )
        )
    )
    val configuracaoImpressao by configuracaoImpressaoViewModel.configuracao.collectAsState()
    
    var isConnected by remember { mutableStateOf(false) }
    val currentTime = remember { mutableStateOf(getCurrentDateTime()) }
    var titulo by remember { mutableStateOf(sharedPreferences.getString("titulo_texto", "☀ QUINTAL BAR ☀") ?: "☀ QUINTAL BAR ☀") }
    var rodape by remember { mutableStateOf(sharedPreferences.getString("rodape_texto", "") ?: "") }
    var editandoTitulo by remember { mutableStateOf(false) }
    var tituloTamanho by remember { mutableStateOf(sharedPreferences.getFloat("titulo_tamanho", 48f)) }
    var espacamentoAcima by remember { mutableStateOf(sharedPreferences.getFloat("espacamento_acima", 32f)) }
    var espacamentoAbaixo by remember { mutableStateOf(sharedPreferences.getFloat("espacamento_abaixo", 16f)) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val senhaAdmin = remember { sharedPreferences.getString("senha_admin", "1985") }
    
    // Cores do sistema
    val primaryColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt()))) 
    }
    val backgroundColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt()))) 
    }
    
    // Atualizar hora a cada minuto
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000)
            currentTime.value = getCurrentDateTime()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Logo se configurado para HomeScreen
        if (configuracaoImpressao?.logoHomeScreen == true && !configuracaoImpressao?.logoBase64.isNullOrEmpty()) {
            val logoBitmap = remember(configuracaoImpressao?.logoBase64) {
                try {
                    val bytes = android.util.Base64.decode(configuracaoImpressao?.logoBase64, android.util.Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } catch (e: Exception) {
                    null
                }
            }
            
            if (logoBitmap != null) {
                if ((configuracaoImpressao?.logoEspacamentoAcima ?: 16f) > 0) {
                    Spacer(modifier = Modifier.height((configuracaoImpressao?.logoEspacamentoAcima ?: 16f).dp))
                }
                Image(
                    painter = BitmapPainter(logoBitmap.asImageBitmap()),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .height((configuracaoImpressao?.logoAltura ?: 80f).dp)
                        .width((configuracaoImpressao?.logoLargura ?: 300f).dp)
                )
                if ((configuracaoImpressao?.logoEspacamentoAbaixo ?: 16f) > 0) {
                    Spacer(modifier = Modifier.height((configuracaoImpressao?.logoEspacamentoAbaixo ?: 16f).dp))
                }
            }
        } else {
            // Espaçamento acima do título (configurável) - só aplicado quando não há logo
            Spacer(modifier = Modifier.height(espacamentoAcima.dp))
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Logo com opção de edição
        if (editandoTitulo) {
            OutlinedTextField(
                value = titulo,
                onValueChange = { titulo = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = tituloTamanho.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = primaryColor
                )
            )
        } else {
            Text(
                titulo,
                fontSize = tituloTamanho.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { editandoTitulo = true }
            )
        }
        
        // Espaçamento abaixo do título (configurável)
        Spacer(modifier = Modifier.height(espacamentoAbaixo.dp))
        
        // Data e Hora
        Text(
            currentTime.value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botões principais
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Botão Checkout (ir direto para vendas)
            Button(
                onClick = {
                    if (caixaAberto) {
                        onNavigateToCheckout()
                    } else {
                        onNavigateToCaixa()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Checkout",
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "INICIAR VENDAS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Botões de gestão
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fechar Caixa
                Button(
                    onClick = onNavigateToCaixa,
                    modifier = Modifier
                        .weight(1f)
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PointOfSale,
                            contentDescription = "Fechar Caixa",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "CAIXA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Gestão de Produtos
                Button(
                    onClick = {
                        pendingAction = onNavigateToProdutos
                        showPasswordDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Produtos",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "PRODUTOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Relatórios
                Button(
                    onClick = onNavigateToVendas,
                    modifier = Modifier
                        .weight(1f)
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = "Relatórios",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "RELATÓRIOS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Impressão
                Button(
                    onClick = {
                        pendingAction = onNavigateToConfiguracaoTipoImpressao
                        showPasswordDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(65.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Print,
                            contentDescription = "Impressão",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "IMPRESSÃO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.weight(1f))
        }

        // Rodapé fixo na parte inferior
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (rodape.isNotEmpty()) {
                Text(
                    rodape,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                "Sistema: Rodrigo Dev MT",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "WhatsApp: (45) 99104-6021",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    // Diálogo de senha
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                password = ""
                pendingAction = null
            },
            title = { Text("Senha de Administrador") },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Digite a senha") },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (password == senhaAdmin) {
                            showPasswordDialog = false
                            password = ""
                            pendingAction?.invoke()
                            pendingAction = null
                        }
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPasswordDialog = false
                        password = ""
                        pendingAction = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    }
}

private fun getCurrentDateTime(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt-BR"))
    return dateFormat.format(Date())
}
