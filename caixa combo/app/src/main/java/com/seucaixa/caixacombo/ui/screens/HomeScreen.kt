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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.data.model.LogoConfig
import com.seucaixa.caixacombo.data.SecurePrefs
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    var titulo by remember { mutableStateOf(sharedPreferences.getString("titulo_texto", "Mais controle, mais agilidade, mais vendas.") ?: "Mais controle, mais agilidade, mais vendas.") }
    var rodape by remember { mutableStateOf(sharedPreferences.getString("rodape_texto", "") ?: "") }
    var editandoTitulo by remember { mutableStateOf(false) }
    val deviceType = LocalContext.current.resources.configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
    val dm = LocalContext.current.resources.displayMetrics
    val isSmallScreen = (dm.widthPixels / dm.density) < 600
    val isD2s = android.os.Build.MODEL.equals("D2s", ignoreCase = true)
    var tituloTamanho by remember { mutableStateOf(sharedPreferences.getFloat("titulo_tamanho", if (isSmallScreen) 28f else 48f)) }
    var espacamentoAcima by remember { mutableStateOf(sharedPreferences.getFloat("espacamento_acima", 5f)) }

    // Relê SharedPreferences quando a tela fica visível (título pode ter mudado na config)
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE)
        titulo = prefs.getString("titulo_texto", "Mais controle, mais agilidade, mais vendas.") ?: "Mais controle, mais agilidade, mais vendas."
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
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Detectar se teclado está aberto
    val isKeyboardOpen = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isSmallScreen) 0.9f else 0.65f)
                .padding(horizontal = if (isSmallScreen) 12.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Espaçamento acima - reduzido quando teclado aberto
            if (!isKeyboardOpen.value) {
                Spacer(modifier = Modifier.height(espacamentoAcima.dp))
            }

            // Logo - reduzir tamanho quando teclado aberto
            if (logoBitmap != null) {
                val logoAltura = if (isKeyboardOpen.value) (logoConfig?.logoAltura ?: 347f) * 0.3f else (logoConfig?.logoAltura ?: 347f)
                val logoLargura = if (isKeyboardOpen.value) (logoConfig?.logoLargura ?: 567f) * 0.3f else (logoConfig?.logoLargura ?: 567f)
                Image(
                    painter = BitmapPainter(logoBitmap!!.asImageBitmap()),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .height(logoAltura.dp)
                        .width(logoLargura.dp)
                )
                Spacer(modifier = Modifier.height(if (isKeyboardOpen.value) 4.dp else (logoConfig?.logoEspacamentoAbaixo ?: 16f).dp))
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
                    fontSize = if (isD2s) (tituloTamanho * 0.75f).sp else tituloTamanho.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    textAlign = TextAlign.Center,
                    maxLines = if (isSmallScreen) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (isSmallScreen) (tituloTamanho * 1.15f).sp else tituloTamanho.sp,
                    modifier = Modifier.clickable { editandoTitulo = true }
                )
            }

            Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))

            // Campo de Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; erro = "" },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusEvent { focusState ->
                        isKeyboardOpen.value = focusState.isFocused
                    },
                leadingIcon = {
                    Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(12.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = if (isSmallScreen) 14.sp else 18.sp,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )
            Spacer(modifier = Modifier.height(if (isSmallScreen) 8.dp else 12.dp))
            // Campo de PIN
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it; erro = "" },
                label = { Text("PIN") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusEvent { focusState ->
                        isKeyboardOpen.value = focusState.isFocused
                    },
                leadingIcon = {
                    Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
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
                    // Login com email + PIN
                    if (email.isBlank() || pin.isBlank()) {
                        erro = "Digite email e PIN"
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        try {
                            val serverUrl = com.seucaixa.caixacombo.service.PollingService.getServerUrl()
                            if (serverUrl != null) {
                                withContext(Dispatchers.IO) {
                                    val url = java.net.URL("$serverUrl/api/auth/login")
                                    val conn = url.openConnection() as java.net.HttpURLConnection
                                    conn.requestMethod = "POST"
                                    conn.setRequestProperty("Content-Type", "application/json")
                                    conn.doOutput = true
                                    conn.connectTimeout = 8000
                                    conn.readTimeout = 8000
                                    val data = org.json.JSONObject().apply {
                                        put("email", email)
                                        put("pin", pin)
                                    }
                                    conn.outputStream.use { it.write(data.toString().toByteArray()) }
                                    val responseCode = conn.responseCode
                                    if (responseCode == 200) {
                                        val response = conn.inputStream.bufferedReader().readText()
                                        val json = org.json.JSONObject(response)
                                        val user = json.getJSONObject("user")
                                        val nome = user.optString("nome", "")
                                        val cargo = user.optString("cargo", "caixa")
                                        val permissoes = user.optJSONObject("permissoes")
                                        val funcId = user.optLong("id", -1)
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            SecurePrefs.saveOperator(context, nome, cargo, funcId)
                                            sharedPreferences.edit()
                                                .putString("operador_nome", nome)
                                                .putString("operador_cargo", cargo)
                                                .putLong("operador_id", funcId)
                                                .apply()
                                            // Salvar permissões
                                            val permPrefs = context.getSharedPreferences("funcionario_permissoes", Context.MODE_PRIVATE)
                                            permPrefs.edit()
                                                .putString("cargo", cargo)
                                                .putBoolean("vendas", permissoes?.optBoolean("vendas", true) ?: true)
                                                .putBoolean("caixa", permissoes?.optBoolean("caixa", true) ?: true)
                                                .putBoolean("produtos", permissoes?.optBoolean("produtos", false) ?: false)
                                                .putBoolean("categorias", permissoes?.optBoolean("categorias", false) ?: false)
                                                .putBoolean("relatorios", permissoes?.optBoolean("relatorios", false) ?: false)
                                                .putBoolean("desconto", permissoes?.optBoolean("desconto", false) ?: false)
                                                .putBoolean("cancelar_venda", permissoes?.optBoolean("cancelar_venda", false) ?: false)
                                                .putBoolean("operacoes_caixa", permissoes?.optBoolean("operacoes_caixa", true) ?: true)
                                                .apply()
                                            erro = ""
                                            if (caixaAberto) {
                                                onNavigateToCheckout()
                                            } else {
                                                onNavigateToCaixa()
                                            }
                                        }
                                    } else {
                                        val errorResponse = conn.errorStream?.bufferedReader()?.readText()
                                        val errorMsg = try {
                                            org.json.JSONObject(errorResponse ?: "").optString("error", "Credenciais inválidas")
                                        } catch (e: Exception) {
                                            "Credenciais inválidas"
                                        }
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            erro = errorMsg
                                        }
                                    }
                                }
                            } else {
                                isLoading = false
                                erro = "Erro de conexão"
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            erro = "Erro ao fazer login"
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
                "CaixaCombo PDV",
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
