package com.seucaixa.caixacombo.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.seucaixa.caixacombo.R
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.data.model.LogoConfig
import com.seucaixa.caixacombo.data.SecurePrefs
import kotlinx.coroutines.launch
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
    val usuarioDao = remember { AppDatabase.getDatabase(context).usuarioDao() }
    val configDao = remember { AppDatabase.getDatabase(context).configuracaoImpressaoDao() }
    val scope = rememberCoroutineScope()

    // Logo config do banco (atualiza automaticamente quando muda)
    val logoConfig by configDao.getLogoConfig().collectAsState(initial = null)
    var logoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        try {
            val logoFile = java.io.File(context.filesDir, "logo.png")
            if (logoFile.exists()) {
                logoBitmap = BitmapFactory.decodeFile(logoFile.absolutePath)
            } else {
                // Logo padrão caixacombo
                logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.caixacombo)
            }
        } catch (_: Exception) {}
    }

    var titulo by remember { mutableStateOf(sharedPreferences.getString("titulo_texto", "Rodrigo Dev MT") ?: "Rodrigo Dev MT") }
    var rodape by remember { mutableStateOf(sharedPreferences.getString("rodape_texto", "") ?: "") }
    var editandoTitulo by remember { mutableStateOf(false) }
    val deviceType = LocalContext.current.resources.configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
    val dm = LocalContext.current.resources.displayMetrics
    val isSmallScreen = (dm.widthPixels / dm.density) < 600
    val isD2s = com.seucaixa.caixacombo.BuildConfig.FLAVOR == "checkoutpos"
    var tituloTamanho by remember { mutableStateOf(sharedPreferences.getFloat("titulo_tamanho", if (isSmallScreen) 28f else 48f)) }
    var espacamentoAcima by remember { mutableStateOf(sharedPreferences.getFloat("espacamento_acima", 5f)) }

    // Relê SharedPreferences quando a tela fica visível (título pode ter mudado na config)
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE)
        titulo = prefs.getString("titulo_texto", "Rodrigo Dev MT") ?: "Rodrigo Dev MT"
        rodape = prefs.getString("rodape_texto", "") ?: ""
        tituloTamanho = prefs.getFloat("titulo_tamanho", if (isSmallScreen) 28f else 48f)
        espacamentoAcima = prefs.getFloat("espacamento_acima", 5f)
    }

    // Cores do sistema
    val primaryColor by remember {
        mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt())))
    }
    val backgroundColor by remember {
        mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt())))
    }

    // Login
    var codigo by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isSmallScreen) 0.9f else 0.65f)
                .padding(horizontal = if (isSmallScreen) 12.dp else 24.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Espaçamento acima configurável
            Spacer(modifier = Modifier.height(espacamentoAcima.dp))

            // Logo
            if (logoBitmap != null) {
                Image(
                    painter = BitmapPainter(logoBitmap!!.asImageBitmap()),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .height((logoConfig?.logoAltura ?: 347f).dp)
                        .width((logoConfig?.logoLargura ?: 567f).dp)
                )
                Spacer(modifier = Modifier.height((logoConfig?.logoEspacamentoAbaixo ?: 16f).dp))
            }

            // Título
            if (editandoTitulo) {
                OutlinedTextField(
                    value = titulo,
                    onValueChange = {
                        titulo = it
                        sharedPreferences.edit().putString("titulo_texto", it).apply()
                    },
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

            Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))

            // Campo PIN
            OutlinedTextField(
                value = codigo,
                onValueChange = { codigo = it; erro = "" },
                label = { Text("Código de Acesso") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = if (isSmallScreen) 14.sp else 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Erro
            if (erro.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    erro,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 16.dp))

            // Botão Entrar
            Button(
                onClick = {
                    if (codigo.isBlank()) {
                        erro = "Digite o código de acesso"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val usuario = usuarioDao.getUsuarioByCodigo(codigo)
                        isLoading = false
                        if (usuario != null && usuario.ativo) {
                            erro = ""
                            // Salvar operador logado (criptografado - Stone compliance)
                            SecurePrefs.saveOperator(context, usuario.nome, usuario.cargo.name, usuario.id)
                            // Também salvar em SharedPreferences comum para leitura rápida no PDV
                            sharedPreferences.edit()
                                .putString("operador_nome", usuario.nome)
                                .putString("operador_cargo", usuario.cargo.name)
                                .putLong("operador_id", usuario.id)
                                .apply()
                            if (caixaAberto) {
                                onNavigateToCheckout()
                            } else {
                                onNavigateToCaixa()
                            }
                        } else if (usuario != null && !usuario.ativo) {
                            erro = "Usuário inativo"
                        } else {
                            erro = "Código inválido"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSmallScreen) 44.dp else 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Login, null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Entrar", fontSize = if (isSmallScreen) 14.sp else 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 12.dp else 24.dp))

            // Rodapé
            if (rodape.isNotEmpty()) {
                Text(
                    rodape,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryColor.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                "Desenvolvedor Rodrigo Dev MT",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor.copy(alpha = 0.5f)
            )
            Text(
                "developer@rodrigodevmt.com.br",
                fontSize = 11.sp,
                color = primaryColor.copy(alpha = 0.45f)
            )
            Text(
                "www.rodrigodevmt.com.br",
                fontSize = 11.sp,
                color = primaryColor.copy(alpha = 0.45f)
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "WhatsApp",
                    modifier = Modifier.size(11.dp),
                    tint = primaryColor.copy(alpha = 0.45f)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    "WhatsApp (66) 9618-4323 / 99260-8881",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryColor.copy(alpha = 0.45f)
                )
            }
            // Espaçamento extra no final para D2S
            if (isD2s) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun getCurrentDateTime(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt-BR"))
    return dateFormat.format(Date())
}
