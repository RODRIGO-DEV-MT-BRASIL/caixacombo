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
    val operadorNomeLogado = remember {
        context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE)
            .getString("operador_nome", null)
    }

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
            onErro = { errorMessage = it }
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
            onErro = { errorMessage = it }
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
                title = { Text("Operações de Caixa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status do Caixa
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (caixaAberto)
                        MaterialTheme.colorScheme.tertiaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (caixaAberto) Icons.Default.LockOpen else Icons.Default.Lock,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = if (caixaAberto)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        if (caixaAberto) "CAIXA ABERTO" else "CAIXA FECHADO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    ultimaAbertura?.let { abertura ->
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        Text(
                            "Abertura: ${dateFormat.format(Date(abertura.dataHora))}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Operador: ${abertura.nomeOperador}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Valor inicial: R$ %.2f".format(abertura.valorInicial ?: 0.0),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Mostrar saldo atual se caixa estiver aberto
                    if (caixaAberto) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "SALDO ATUAL",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "R$ %.2f".format(saldoAtual),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Vendas por forma de pagamento
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FormaPagamentoItem("Dinheiro", vendasDinheiro, Color(0xFF4CAF50))
                            FormaPagamentoItem("Crédito", vendasCredito, Color(0xFF2196F3))
                            FormaPagamentoItem("Débito", vendasDebito, Color(0xFFFF9800))
                            FormaPagamentoItem("Pix", vendasPix, Color(0xFF9C27B0))
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Totais de operações
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            FormaPagamentoItem("Vendas", totalVendas, MaterialTheme.colorScheme.tertiary, prefix = "+")
                            FormaPagamentoItem("Sangrias", totalSangrias, MaterialTheme.colorScheme.error, prefix = "-")
                            FormaPagamentoItem("Supr.", totalSuprimentos, Color(0xFF10B981), prefix = "+")
                        }
                    }
                }
            }

            // Botões de Operação
            if (!caixaAberto) {
                // Caixa Fechado - Mostrar apenas Abertura
                Button(
                    onClick = {
                        if (caixaAbertoDiaAnterior) {
                            showCaixaDiaAnteriorDialog = true
                        } else {
                            showSenhaAberturaDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "ABRIR CAIXA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Caixa Aberto - Mostrar Fechamento, Sangria e Suprimento
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showSenhaFechamentoDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626)
                            )
                        ) {
                            Icon(Icons.Default.Lock, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "FECHAR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = { showSangriaDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B)
                            )
                        ) {
                            Icon(Icons.Default.MoneyOff, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "SANGRIA",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = { showSuprimentoDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            )
                        ) {
                            Icon(Icons.Default.AddCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "SUPR.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Abas de registros
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Aberturas") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Fechamentos") }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("Sangrias") }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("Suprimentos") }
                        )
                        Tab(
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            text = { Text("Dinheiro") }
                        )
                        Tab(
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 },
                            text = { Text("Crédito") }
                        )
                        Tab(
                            selected = selectedTab == 6,
                            onClick = { selectedTab = 6 },
                            text = { Text("Débito") }
                        )
                        Tab(
                            selected = selectedTab == 7,
                            onClick = { selectedTab = 7 },
                            text = { Text("PIX") }
                        )
                    }

                    HorizontalDivider()

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
            operadorNome = operadorNomeLogado
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
    operadorNome: String? = null
) {
    var nome by remember { mutableStateOf(operadorNome ?: "") }
    var valorField by remember { mutableStateOf(TextFieldValue("", selection = TextRange(0))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abertura de Caixa") },
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

                val valorFormatado = formatarValor(valorField.text)
                val valorDisplay = formatarDisplay(valorFormatado)

                OutlinedTextField(
                    value = criarTextFieldValue(valorFormatado),
                    onValueChange = { newValue ->
                        val digitsOnly = newValue.text.filter { it.isDigit() }
                        valorField = criarTextFieldValue(formatarValor(digitsOnly))
                    },
                    label = { Text("Valor Inicial") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("R$ 0,00") },
                    suffix = { Text(valorDisplay) }
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
                enabled = nome.isNotBlank() && valorField.text.toDoubleSafe() > 0
            ) {
                Text("Abrir Caixa")
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
    onErro: (String) -> Unit
) {
    var senha by remember { mutableStateOf("") }
    var tentativas by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(32.dp)) },
        title = { Text("Senha Necessária") },
        text = {
            Column {
                Text("Digite a senha de administrador para $motivo")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = senha,
                    onValueChange = { senha = it },
                    label = { Text("Senha") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
            TextButton(
                onClick = {
                    if (senha == senhaCorreta) {
                        onSenhaCorreta()
                    } else {
                        tentativas++
                        onErro("Senha incorreta!")
                    }
                },
                enabled = senha.isNotBlank()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
