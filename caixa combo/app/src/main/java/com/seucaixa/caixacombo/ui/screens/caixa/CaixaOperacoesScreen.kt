package com.seucaixa.caixacombo.ui.screens.caixa

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.seucaixa.caixacombo.data.model.FormaPagamento
import com.seucaixa.caixacombo.data.model.OperacaoCaixa
import com.seucaixa.caixacombo.ui.viewmodel.CaixaViewModel
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaixaOperacoesScreen(
    viewModel: CaixaViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToConfiguracao: () -> Unit = {}
) {
    val caixaAberto by viewModel.caixaAberto.collectAsState()
    val caixaAbertoDiaAnterior by viewModel.caixaAbertoDiaAnterior.collectAsState()
    val ultimaAbertura by viewModel.ultimaAbertura.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val saldoAtual by viewModel.saldoAtual.collectAsState()
    val totalVendas by viewModel.totalVendas.collectAsState()
    val totalSangrias by viewModel.totalSangrias.collectAsState()
    val totalSuprimentos by viewModel.totalSuprimentos.collectAsState()
    val vendasDinheiro by viewModel.vendasDinheiro.collectAsState()
    val vendasCredito by viewModel.vendasCredito.collectAsState()
    val vendasDebito by viewModel.vendasDebito.collectAsState()
    val vendasPix by viewModel.vendasPix.collectAsState()
    val vendasDinheiroList by viewModel.vendasDinheiroList.collectAsState()
    val vendasCreditoList by viewModel.vendasCreditoList.collectAsState()
    val vendasDebitoList by viewModel.vendasDebitoList.collectAsState()
    val vendasPixList by viewModel.vendasPixList.collectAsState()
    val aberturas by viewModel.aberturas.collectAsState()
    val fechamentos by viewModel.fechamentos.collectAsState()
    val sangrias by viewModel.sangrias.collectAsState()
    val suprimentos by viewModel.suprimentos.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val coresPrefs = remember { context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE) }
    val primaryColor = remember { Color(coresPrefs.getInt("primary_color", 0xFF6200EE.toInt())) }
    val backgroundColor = remember { Color(coresPrefs.getInt("background_color", 0xFFF5F5F5.toInt())) }
    val operadorNomeLogado = remember { coresPrefs.getString("operador_nome", null) }

    var showAberturaDialog by remember { mutableStateOf(false) }
    var showFechamentoDialog by remember { mutableStateOf(false) }
    var showSangriaDialog by remember { mutableStateOf(false) }
    var showSuprimentoDialog by remember { mutableStateOf(false) }
    var showCaixaDiaAnteriorDialog by remember { mutableStateOf(false) }
    var showSenhaAberturaDialog by remember { mutableStateOf(false) }
    var showSenhaFechamentoDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val senhaAdmin = remember { context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE).getString("senha_admin", "1985") ?: "1985" }

    // Dialog de senha unificado
    if (showSenhaAberturaDialog) {
        SenhaAdminDialog(
            motivo = "abrir o caixa",
            senhaCorreta = senhaAdmin,
            onSenhaCorreta = {
                showSenhaAberturaDialog = false
                showAberturaDialog = true
            },
            onDismiss = { showSenhaAberturaDialog = false },
            onErro = { errorMessage = it },
            primaryColor = primaryColor
        )
    }

    if (showSenhaFechamentoDialog) {
        SenhaAdminDialog(
            motivo = "fechar o caixa",
            senhaCorreta = senhaAdmin,
            onSenhaCorreta = {
                showSenhaFechamentoDialog = false
                showFechamentoDialog = true
            },
            onDismiss = { showSenhaFechamentoDialog = false },
            onErro = { errorMessage = it },
            primaryColor = primaryColor
        )
    }

    // Mostrar erro se houver
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Erro") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Caixa", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        if (ultimaAbertura != null) {
                            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                            Text(dateFormat.format(Date(ultimaAbertura!!.dataHora)), fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (caixaAberto) Color(0xFF065F46) else Color(0xFF7F1D1D)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (caixaAberto) Icons.Default.LockOpen else Icons.Default.Lock,
                                null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White.copy(alpha = 0.9f)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (caixaAberto) "Caixa Aberto" else "Caixa Fechado",
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White
                                )
                                if (ultimaAbertura != null) {
                                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    Text(
                                        dateFormat.format(Date(ultimaAbertura!!.dataHora)),
                                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            if (caixaAberto) {
                                Text(
                                    "R$ %.2f".format(saldoAtual),
                                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White
                                )
                            }
                        }

                        if (caixaAberto && ultimaAbertura != null) {
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                LabelValue("Operador", ultimaAbertura!!.nomeOperador)
                                LabelValue("Abertura", "R$ %.2f".format(ultimaAbertura!!.valorInicial ?: 0.0))
                            }
                        }
                    }
                }
            }

            // Saldo / Métricas (quando caixa aberto)
            if (caixaAberto) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Resumo do Período", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF374151))
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MetricItem("Vendas", "R$ %.2f".format(totalVendas), primaryColor)
                            MetricItem("Sangrias", "R$ %.2f".format(totalSangrias), Color(0xFFDC2626))
                            MetricItem("Supr.", "R$ %.2f".format(totalSuprimentos), primaryColor)
                        }
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFE5E7EB))
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MiniChip("Dinheiro", "R$ %.2f".format(vendasDinheiro), Color(0xFF16A34A))
                            MiniChip("Crédito", "R$ %.2f".format(vendasCredito), Color(0xFF2563EB))
                            MiniChip("Débito", "R$ %.2f".format(vendasDebito), Color(0xFFD97706))
                            MiniChip("PIX", "R$ %.2f".format(vendasPix), Color(0xFF7C3AED))
                        }
                    }
                }
            }

            // Botões de Ação
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                if (!caixaAberto) {
                    Button(
                        onClick = {
                            if (caixaAbertoDiaAnterior) showCaixaDiaAnteriorDialog = true
                            else showSenhaAberturaDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir Caixa", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Lock,
                            label = "Fechar",
                            color = Color(0xFFDC2626),
                            onClick = { showSenhaFechamentoDialog = true }
                        )
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.MoneyOff,
                            label = "Sangria",
                            color = Color(0xFFD97706),
                            onClick = { showSangriaDialog = true }
                        )
                        ActionCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AddCircle,
                            label = "Supr.",
                            color = Color(0xFF16A34A),
                            onClick = { showSuprimentoDialog = true }
                        )
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            }

            // Abas de Registros
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column {
                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 0.dp,
                            containerColor = Color(0xFFF9FAFB),
                            divider = {},
                            indicator = {}
                        ) {
                            listOf("Aberturas", "Fechamentos", "Sangrias", "Supr.", "Dinheiro", "Crédito", "Débito", "PIX").forEachIndexed { i, label ->
                                Tab(
                                    selected = selectedTab == i,
                                    onClick = { selectedTab = i },
                                    text = {
                                        Text(
                                            label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selectedTab == i) primaryColor else Color(0xFF6B7280)
                                        )
                                    },
                                    selectedContentColor = primaryColor,
                                    unselectedContentColor = Color(0xFF6B7280)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE5E7EB))

                        Box(modifier = Modifier.padding(12.dp)) {
                            when (selectedTab) {
                                0 -> AberturasTab(aberturas)
                                1 -> FechamentosTab(fechamentos)
                                2 -> SangriasTab(sangrias)
                                3 -> SuprimentosTab(suprimentos)
                                4 -> VendasPorFormaTab(vendasDinheiroList, "Dinheiro")
                                5 -> VendasPorFormaTab(vendasCreditoList, "Crédito")
                                6 -> VendasPorFormaTab(vendasDebitoList, "Débito")
                                7 -> VendasPorFormaTab(vendasPixList, "PIX")
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog de Abertura
    if (showAberturaDialog) {
        AberturaCaixaDialog(
            onConfirm = { nome, valor ->
                viewModel.abrirCaixa(
                    nomeOperador = nome,
                    valorInicial = valor,
                    onSuccess = {
                        showAberturaDialog = false
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            },
            onDismiss = { showAberturaDialog = false },
            operadorNome = operadorNomeLogado,
            primaryColor = primaryColor
        )
    }

    // Dialog de Fechamento
    if (showFechamentoDialog) {
        FechamentoCaixaDialog(
            vendasDinheiro = vendasDinheiro,
            vendasCredito = vendasCredito,
            vendasDebito = vendasDebito,
            vendasPix = vendasPix,
            sangrias = sangrias,
            onConfirm = { nome, valores, valorContado ->
                showFechamentoDialog = false
                viewModel.fecharCaixa(
                    nomeOperador = nome,
                    valoresInformados = valores,
                    valorContado = valorContado,
                    onSuccess = {
                        // Caixa fechado com sucesso
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            },
            onDismiss = {
                showFechamentoDialog = false
            },
            operadorNome = operadorNomeLogado
        )
    }

    // Dialog de Sangria
    if (showSangriaDialog) {
        SangriaDialog(
            saldoAtual = saldoAtual,
            onConfirm = { nome, valor, motivo ->
                viewModel.registrarSangria(nome, valor, motivo,
                    onSuccess = {
                        showSangriaDialog = false
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            },
            onDismiss = { showSangriaDialog = false },
            operadorNome = operadorNomeLogado
        )
    }

    // Dialog de Suprimento
    if (showSuprimentoDialog) {
        SuprimentoDialog(
            onConfirm = { nome, valor, motivo ->
                viewModel.registrarSuprimento(nome, valor, motivo,
                    onSuccess = {
                        showSuprimentoDialog = false
                    },
                    onError = { error ->
                        errorMessage = error
                    }
                )
            },
            onDismiss = { showSuprimentoDialog = false },
            operadorNome = operadorNomeLogado
        )
    }

    // Dialog de aviso para caixa aberto de dia anterior
    if (showCaixaDiaAnteriorDialog) {
        AlertDialog(
            onDismissRequest = { showCaixaDiaAnteriorDialog = false },
            title = { Text("Caixa Aberto de Dia Anterior") },
            text = {
                Column {
                    Text("O caixa foi aberto em um dia anterior e não foi fechado.")
                    Text("Por favor, feche o caixa antes de abrir um novo.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showCaixaDiaAnteriorDialog = false }) {
                    Text("Entendi")
                }
            }
        )
    }
}

@Composable
fun AberturaCaixaDialog(
    onConfirm: (String, Double) -> Unit,
    onDismiss: () -> Unit,
    operadorNome: String? = null,
    primaryColor: Color = Color(0xFF6200EE),
    backgroundColor: Color = Color.White
) {
    var nome by remember { mutableStateOf(operadorNome ?: "") }
    var valorField by remember { mutableStateOf(TextFieldValue("", selection = TextRange(0))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = backgroundColor,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AccountBalance, null, tint = primaryColor, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Abertura de Caixa", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = primaryColor)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Operador *") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryColor) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    )
                )

                val valorFormatado = formatarValor(valorField.text)
                val valorDisplay = formatarDisplay(valorFormatado)

                OutlinedTextField(
                    value = criarTextFieldValue(valorFormatado),
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.text.filter { it.isDigit() }
                        valorField = criarTextFieldValue(formatarValor(digitsOnly))
                    },
                    label = { Text("Valor Inicial") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null, tint = primaryColor) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("R$ 0,00") },
                    suffix = { Text(valorDisplay, color = primaryColor, fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valorInicial = valorField.text.toDoubleSafe()
                    if (nome.isNotBlank() && valorInicial > 0) {
                        onConfirm(nome, valorInicial)
                    }
                },
                enabled = nome.isNotBlank() && valorField.text.toDoubleSafe() > 0,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Abrir Caixa", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

@Composable
fun FechamentoCaixaDialog(
    vendasDinheiro: Double = 0.0,
    vendasCredito: Double = 0.0,
    vendasDebito: Double = 0.0,
    vendasPix: Double = 0.0,
    sangrias: List<OperacaoCaixa> = emptyList(),
    onConfirm: (String, Map<FormaPagamento, Double>, Double) -> Unit,
    onDismiss: () -> Unit,
    operadorNome: String? = null
) {
    var nome by remember { mutableStateOf(operadorNome ?: "") }

    // Usar TextFieldValue para controlar posição do cursor
    var dinheiroField by remember { mutableStateOf(TextFieldValue("")) }
    var cartaoCreditoField by remember { mutableStateOf(TextFieldValue("")) }
    var cartaoDebitoField by remember { mutableStateOf(TextFieldValue("")) }
    var pixField by remember { mutableStateOf(TextFieldValue("")) }
    var valorContadoField by remember { mutableStateOf(TextFieldValue("")) }

    // Pré-preencher campos com valores das vendas ao abrir o dialog
    LaunchedEffect(vendasDinheiro, vendasCredito, vendasDebito, vendasPix) {
        dinheiroField = TextFieldValue(
            text = String.format("%.2f", vendasDinheiro),
            selection = TextRange(0)
        )
        cartaoCreditoField = TextFieldValue(
            text = String.format("%.2f", vendasCredito),
            selection = TextRange(0)
        )
        cartaoDebitoField = TextFieldValue(
            text = String.format("%.2f", vendasDebito),
            selection = TextRange(0)
        )
        pixField = TextFieldValue(
            text = String.format("%.2f", vendasPix),
            selection = TextRange(0)
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fechamento de Caixa") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Resumo Financeiro Completo
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "📊 RESUMO FINANCEIRO",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Seção de Vendas
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "💰 VENDAS",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("  Dinheiro:", style = MaterialTheme.typography.bodySmall)
                            Text("R$ %.2f".format(vendasDinheiro), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("  Crédito:", style = MaterialTheme.typography.bodySmall)
                            Text("R$ %.2f".format(vendasCredito), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("  Débito:", style = MaterialTheme.typography.bodySmall)
                            Text("R$ %.2f".format(vendasDebito), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("  PIX:", style = MaterialTheme.typography.bodySmall)
                            Text("R$ %.2f".format(vendasPix), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("  TOTAL VENDAS:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("R$ %.2f".format(vendasDinheiro + vendasCredito + vendasDebito + vendasPix), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        // Seção de Sangrias
                        if (sangrias.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "📤 SANGRIAS",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                            sangrias.forEach { sangria ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "  ${sangria.observacao ?: "Sem motivo"}:",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "R$ %.2f".format(sangria.valor),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            HorizontalDivider()
                            val totalSangrias = sangrias.sumOf { it.valor }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("  TOTAL SANGRIAS:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text("-R$ %.2f".format(totalSangrias), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        // Saldo Esperado
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(thickness = 2.dp)
                        Spacer(modifier = Modifier.height(4.dp))
                        val totalVendas = vendasDinheiro + vendasCredito + vendasDebito + vendasPix
                        val totalSangrias = if (sangrias.isNotEmpty()) sangrias.sumOf { it.valor } else 0.0
                        val saldoEsperado = totalVendas - totalSangrias
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "💵 SALDO ESPERADO:",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "R$ %.2f".format(saldoEsperado),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Campo Nome do Operador
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Operador *") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Card de Confirmação de Valores
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "✅ CONFIRMAÇÃO DE VALORES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Valores pré-preenchidos com as vendas do período",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Campo Dinheiro
                        val dinheiroFormatado = formatarValor(dinheiroField.text)
                        val dinheiroDisplay = formatarDisplay(dinheiroFormatado)
                        OutlinedTextField(
                            value = criarTextFieldValue(dinheiroFormatado),
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.text.filter { it.isDigit() }
                                val formatado = formatarValor(digitsOnly)
                                dinheiroField = criarTextFieldValue(formatado)
                            },
                            label = { Text("DINHEIRO") },
                            leadingIcon = { Icon(Icons.Default.Payment, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("R$ 0,00") },
                            suffix = { Text(dinheiroDisplay) }
                        )

                        // Campo Cartão Crédito
                        val cartaoCreditoFormatado = formatarValor(cartaoCreditoField.text)
                        val cartaoCreditoDisplay = formatarDisplay(cartaoCreditoFormatado)
                        OutlinedTextField(
                            value = criarTextFieldValue(cartaoCreditoFormatado),
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.text.filter { it.isDigit() }
                                val formatado = formatarValor(digitsOnly)
                                cartaoCreditoField = criarTextFieldValue(formatado)
                            },
                            label = { Text("CARTÃO CRÉDITO") },
                            leadingIcon = { Icon(Icons.Default.Payment, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("R$ 0,00") },
                            suffix = { Text(cartaoCreditoDisplay) }
                        )

                        // Campo Cartão Débito
                        val cartaoDebitoFormatado = formatarValor(cartaoDebitoField.text)
                        val cartaoDebitoDisplay = formatarDisplay(cartaoDebitoFormatado)
                        OutlinedTextField(
                            value = criarTextFieldValue(cartaoDebitoFormatado),
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.text.filter { it.isDigit() }
                                val formatado = formatarValor(digitsOnly)
                                cartaoDebitoField = criarTextFieldValue(formatado)
                            },
                            label = { Text("CARTÃO DÉBITO") },
                            leadingIcon = { Icon(Icons.Default.Payment, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("R$ 0,00") },
                            suffix = { Text(cartaoDebitoDisplay) }
                        )

                        // Campo PIX
                        val pixFormatado = formatarValor(pixField.text)
                        val pixDisplay = formatarDisplay(pixFormatado)
                        OutlinedTextField(
                            value = criarTextFieldValue(pixFormatado),
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.text.filter { it.isDigit() }
                                val formatado = formatarValor(digitsOnly)
                                pixField = criarTextFieldValue(formatado)
                            },
                            label = { Text("PIX") },
                            leadingIcon = { Icon(Icons.Default.Payment, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("R$ 0,00") },
                            suffix = { Text(pixDisplay) }
                        )

                        // Campo Valor Contado em Caixa
                        val valorContadoFormatado = formatarValor(valorContadoField.text)
                        val valorContadoDisplay = formatarDisplay(valorContadoFormatado)
                        OutlinedTextField(
                            value = criarTextFieldValue(valorContadoFormatado),
                            onValueChange = { newValue ->
                                val digitsOnly = newValue.text.filter { it.isDigit() }
                                val formatado = formatarValor(digitsOnly)
                                valorContadoField = criarTextFieldValue(formatado)
                            },
                            label = { Text("VALOR CONTADO EM CAIXA") },
                            leadingIcon = { Icon(Icons.Default.Payment, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("R$ 0,00") },
                            suffix = { Text(valorContadoDisplay) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        // Usar "Operador" como padrão se nome estiver vazio
                        val nomeOperador = if (nome.isBlank()) "Operador" else nome
                        
                        // Converter valores diretamente - usar valor do campo ou 0.0 se vazio
                        val dinheiroValor = dinheiroField.text.toDoubleSafe()
                        val creditoValor = cartaoCreditoField.text.toDoubleSafe()
                        val debitoValor = cartaoDebitoField.text.toDoubleSafe()
                        val pixValor = pixField.text.toDoubleSafe()
                        val valorContado = valorContadoField.text.toDoubleSafe()
                        
                        val valoresMap = mapOf(
                            FormaPagamento.DINHEIRO to dinheiroValor,
                            FormaPagamento.CARTAO_CREDITO to creditoValor,
                            FormaPagamento.CARTAO_DEBITO to debitoValor,
                            FormaPagamento.PIX to pixValor
                        )
                        onConfirm(nomeOperador, valoresMap, valorContado)
                    } catch (e: Exception) {
                        // Erro ao processar fechamento
                    }
                }
            ) {
                Text("Fechar Caixa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun SangriaDialog(
    saldoAtual: Double,
    onConfirm: (String, Double, String) -> Unit,
    onDismiss: () -> Unit,
    operadorNome: String? = null
) {
    var nome by remember { mutableStateOf(operadorNome ?: "") }
    var valorField by remember { mutableStateOf(TextFieldValue("")) }
    var motivo by remember { mutableStateOf("") }

    val valorNumerico = valorField.text.toDoubleSafe()
    val saldoSuficiente = valorNumerico <= saldoAtual

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sangria (Retirada de Valores)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mostrar saldo disponível
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Saldo Disponível",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "R$ %.2f".format(saldoAtual),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Operador *") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                val valorFormatado = formatarValor(valorField.text)
                val valorSangriaDisplay = formatarDisplay(valorFormatado)
                OutlinedTextField(
                    value = criarTextFieldValue(valorFormatado),
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.text.filter { it.isDigit() }
                        valorField = criarTextFieldValue(formatarValor(digitsOnly))
                    },
                    label = { Text("Valor a Retirar *") },
                    leadingIcon = { Icon(Icons.Default.MoneyOff, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("R$ 0,00") },
                    suffix = { Text(valorSangriaDisplay) },
                    isError = !saldoSuficiente && valorField.text.isNotBlank(),
                    supportingText = {
                        if (!saldoSuficiente && valorField.text.isNotBlank()) {
                            Text(
                                "Valor maior que o saldo disponível!",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo *") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nome.isNotBlank() && motivo.isNotBlank() && valorNumerico > 0 && saldoSuficiente) {
                        onConfirm(nome, valorNumerico, motivo)
                    }
                },
                enabled = nome.isNotBlank() && motivo.isNotBlank() && valorNumerico > 0 && saldoSuficiente
            ) {
                Text("Confirmar Sangria")
            }
        }
    )
}

@Composable
fun SuprimentoDialog(
    onConfirm: (String, Double, String) -> Unit,
    onDismiss: () -> Unit,
    operadorNome: String? = null
) {
    var nome by remember { mutableStateOf(operadorNome ?: "") }
    var valorField by remember { mutableStateOf(TextFieldValue("")) }
    var motivo by remember { mutableStateOf("") }

    val valorNumerico = valorField.text.toDoubleSafe()
    val valorFormatado = formatarValor(valorField.text)
    val valorSuprimentoDisplay = formatarDisplay(valorFormatado)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Suprimento (Adição de Valores)") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome do Operador *") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = criarTextFieldValue(valorFormatado),
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.text.filter { it.isDigit() }
                        valorField = criarTextFieldValue(formatarValor(digitsOnly))
                    },
                    label = { Text("Valor a Adicionar *") },
                    leadingIcon = { Icon(Icons.Default.AddCircle, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("R$ 0,00") },
                    suffix = { Text(valorSuprimentoDisplay) }
                )

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo *") },
                    leadingIcon = { Icon(Icons.Default.Edit, null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nome.isNotBlank() && motivo.isNotBlank() && valorNumerico > 0) {
                        onConfirm(nome, valorNumerico, motivo)
                    }
                },
                enabled = nome.isNotBlank() && motivo.isNotBlank() && valorNumerico > 0
            ) {
                Text("Confirmar Suprimento")
            }
        }
    )
}

@Composable
fun AberturasTab(aberturas: List<OperacaoCaixa>) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (aberturas.isEmpty()) {
            Text(
                "Nenhuma abertura registrada",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            aberturas.forEach { abertura ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            dateFormat.format(Date(abertura.dataHora)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Operador: ${abertura.nomeOperador}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Valor: R$ ${String.format("%.2f", abertura.valorInicial ?: abertura.valor)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FechamentosTab(fechamentos: List<OperacaoCaixa>) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dataFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val horaFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val diaFormat = SimpleDateFormat("EEEE", Locale("pt-BR"))
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (fechamentos.isEmpty()) {
            Text(
                "Nenhum fechamento registrado",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            fechamentos.forEach { fechamento ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Data de fechamento
                        Text(
                            dateFormat.format(Date(fechamento.dataHora)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Data de abertura
                        fechamento.valorInicial?.let { valorInicial ->
                            val dataAbertura = Date(fechamento.dataHora)
                            Text(
                                "Abertura: ${dataFormat.format(dataAbertura)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Dia: ${diaFormat.format(dataAbertura).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pt-BR")) else it.toString() }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Hora: ${horaFormat.format(dataAbertura)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            "Operador: ${fechamento.nomeOperador}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Valor total
                        Text(
                            "Total Vendas: R$ ${String.format("%.2f", fechamento.valor)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Diferença (sobra ou falta)
                        fechamento.valorInicial?.let { valorInicial ->
                            val diferenca = fechamento.valor - valorInicial
                            if (diferenca != 0.0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                if (diferenca > 0) {
                                    Text(
                                        "sobra caixa",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "R$ ${String.format("%.2f", diferenca)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        "falta",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "R$ ${String.format("%.2f", Math.abs(diferenca))}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SangriasTab(sangrias: List<OperacaoCaixa>) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (sangrias.isEmpty()) {
            Text(
                "Nenhuma sangria registrada",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            sangrias.forEach { sangria ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            dateFormat.format(Date(sangria.dataHora)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Operador: ${sangria.nomeOperador}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Valor: R$ ${String.format("%.2f", sangria.valor)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        sangria.observacao?.let { obs ->
                            Text(
                                "Motivo: $obs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuprimentosTab(suprimentos: List<OperacaoCaixa>) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (suprimentos.isEmpty()) {
            Text(
                "Nenhum suprimento registrado",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            suprimentos.forEach { suprimento ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            dateFormat.format(Date(suprimento.dataHora)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Operador: ${suprimento.nomeOperador}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Valor: R$ ${String.format("%.2f", suprimento.valor)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        suprimento.observacao?.let { obs ->
                            Text(
                                "Motivo: $obs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VendasPorFormaTab(
    vendas: List<com.seucaixa.caixacombo.data.model.Venda>,
    formaPagamento: String
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    var selectedVenda by remember { mutableStateOf<com.seucaixa.caixacombo.data.model.Venda?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (vendas.isEmpty()) {
            Text(
                "Nenhuma venda em $formaPagamento registrada",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            vendas.forEach { venda ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedVenda = venda }
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            "Nº ${venda.numero} - ${dateFormat.format(Date(venda.dataHora))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Total: R$ ${String.format("%.2f", venda.total)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        venda.itens.forEach { item ->
                            Text(
                                "  ${item.quantidade}x ${item.produtoNome} - R$ ${String.format("%.2f", item.total)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog de preview da ficha
    selectedVenda?.let { venda ->
        DialogVendaPreview(
            venda = venda,
            onDismiss = { selectedVenda = null }
        )
    }
}

@Composable
fun DialogVendaPreview(
    venda: com.seucaixa.caixacombo.data.model.Venda,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "📄 Ficha da Venda #${venda.numero}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            dateFormat.format(Date(venda.dataHora)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Forma: ${venda.formaPagamento.name.replace("_", " ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Total: R$ ${String.format("%.2f", venda.total)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (venda.troco > 0) {
                            Text(
                                "Troco: R$ ${String.format("%.2f", venda.troco)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    "ITENS:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                venda.itens.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.produtoNome,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${item.quantidade} x R$ ${String.format("%.2f", item.precoUnitario)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                "R$ ${String.format("%.2f", item.total)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

// ==================== COMPONENTES AUXILIARES ====================

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = Color(0xFF6B7280))
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun MiniChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 9.sp, color = Color(0xFF6B7280))
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun FormaPagamentoItem(
    label: String,
    valor: Double,
    color: Color,
    prefix: String = ""
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${prefix}R$ %.2f".format(valor),
            style = MaterialTheme.typography.bodySmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SenhaAdminDialog(
    motivo: String,
    senhaCorreta: String,
    onSenhaCorreta: () -> Unit,
    onDismiss: () -> Unit,
    onErro: (String) -> Unit,
    primaryColor: Color = Color(0xFF6200EE)
) {
    var senha by remember { mutableStateOf("") }
    var tentativas by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = { Icon(Icons.Default.AdminPanelSettings, null, tint = primaryColor, modifier = Modifier.size(36.dp)) },
        title = { Text("Senha Necessária", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor) },
        text = {
            Column {
                Text("Digite a senha de administrador para $motivo", color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor,
                        cursorColor = primaryColor
                    ),
                    isError = tentativas > 0 && senha.isNotBlank() && senha != senhaCorreta,
                    supportingText = {
                        if (tentativas > 0 && senha.isNotBlank() && senha != senhaCorreta) {
                            Text("Senha incorreta! Tente novamente.", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (senha == senhaCorreta) {
                        onSenhaCorreta()
                    } else {
                        tentativas++
                        onErro("Senha incorreta!")
                    }
                },
                enabled = senha.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

// ==================== FUNÇÕES UTILITÁRIAS ====================

private fun formatarValor(input: String): String {
    val digitsOnly = input.filter { it.isDigit() }
    val valorCentavos = digitsOnly.toLongOrNull() ?: 0L
    val reais = valorCentavos / 100
    val centavos = valorCentavos % 100
    return String.format("%d.%02d", reais, centavos)
}

private fun criarTextFieldValue(texto: String): TextFieldValue {
    return TextFieldValue(
        text = texto,
        selection = TextRange(texto.length)
    )
}

private fun formatarDisplay(formatado: String): String {
    return if (formatado.isBlank() || formatado == "0.00") ""
    else String.format("R$ %.2f", formatado.toDoubleSafe()).replace(".", ",")
}
