package com.seucaixa.caixacombo.ui.screens.checkout

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.ui.components.CustomKeyboard
import com.seucaixa.caixacombo.ui.components.OutlinedTextFieldWithCustomKeyboard
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.theme.DeviceType
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ItemCarrinho
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenPOS(
    viewModel: CheckoutViewModel,
    caixaAberto: Boolean,
    deviceType: DeviceType,
    onNavigateToHome: () -> Unit,
    onNavigateToProdutos: () -> Unit,
    onNavigateToVendas: () -> Unit,
    onNavigateToCaixa: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE) }
    
    // Cores do sistema
    val primaryColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt()))) 
    }
    val backgroundColor by remember { 
        mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt()))) 
    }
    
    val produtos by viewModel.produtos.collectAsState()
    val carrinho by viewModel.carrinho.collectAsState()
    val total by viewModel.total.collectAsState()
    val busca by viewModel.busca.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val categoriaSelecionada by viewModel.categoriaSelecionada.collectAsState()
    val vendidosPorProduto by viewModel.vendidosPorProduto.collectAsState()
    val vendaFinalizada by viewModel.vendaFinalizada.collectAsState()
    val ultimaVenda by viewModel.ultimaVenda.collectAsState()
    val precisaSincronizar by viewModel.precisaSincronizar.collectAsState()
    val produtosPendentes by viewModel.produtosPendentes.collectAsState()
    
    var showFormaPagamentoDialog by remember { mutableStateOf(false) }
    var formaPagamentoSelecionada by remember { mutableStateOf<FormaPagamento?>(null) }
    var showValorDialog by remember { mutableStateOf(false) }
    var valorRecebido by remember { mutableStateOf("") }
    var showSyncAlert by remember { mutableStateOf(false) }
    
    // Mostrar alerta de sincronização se necessário
    LaunchedEffect(precisaSincronizar) {
        if (precisaSincronizar) {
            showSyncAlert = true
        }
    }
    
    // Diálogo de sucesso com botões de impressão
    if (vendaFinalizada && ultimaVenda != null) {
        DialogVendaSucesso(
            venda = ultimaVenda!!,
            onDismiss = { viewModel.resetVendaFinalizada() }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛒", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Caixa Combo", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = primaryColor
                ),
                actions = {
                    Row {
                        // Apenas botão Home
                        TopBarAction("Home", Icons.Default.Home, onNavigateToHome, Color.White)
                    }
                }
            )
        }
    ) { padding ->

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(backgroundColor)
        ) {

            // PRODUTOS
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Campo de busca moderno
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    OutlinedTextField(
                        value = busca,
                        onValueChange = viewModel::buscarProdutos,
                        singleLine = true,
                        placeholder = { Text("Buscar produto...", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryColor) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Transparent,
                            containerColor = Color.Transparent
                        )
                    )
                }

                if (categorias.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = if (categoriaSelecionada == null) 0 else categorias.indexOf(categoriaSelecionada) + 1,
                        containerColor = Color.Transparent,
                        contentColor = primaryColor,
                        edgePadding = 0.dp
                    ) {
                        Tab(
                            selected = categoriaSelecionada == null,
                            onClick = { viewModel.selecionarCategoria(null) },
                            text = { Text("Todos", fontWeight = if (categoriaSelecionada == null) FontWeight.Bold else FontWeight.Normal) }
                        )
                        categorias.forEach {
                            Tab(
                                selected = categoriaSelecionada?.id == it.id,
                                onClick = { viewModel.selecionarCategoria(it) },
                                text = { Text(it.nome, fontWeight = if (categoriaSelecionada?.id == it.id) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                val numColunas = if (deviceType == DeviceType.POS) 4 else 2

                LazyVerticalGrid(
                    columns = GridCells.Fixed(numColunas),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(produtos, key = { it.id }) { produto ->
                        ProdutoCardPOS(
                            produto = produto,
                            vendidos = vendidosPorProduto[produto.id] ?: 0,
                            onClick = { viewModel.adicionarAoCarrinho(produto) }
                        )
                    }
                }
            }

            // CARRINHO - Design moderno
            Card(
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header do carrinho
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🛒", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Carrinho", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                        Text(
                            "${carrinho.size} itens",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)

                    if (carrinho.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🛒", fontSize = 48.sp)
                                Text("Carrinho vazio", color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(carrinho) { item ->
                                CarrinhoItemPOS(
                                    item = item,
                                    onQuantidadeChange = {
                                        if (it > 0) viewModel.atualizarQuantidade(item.produtoId, it)
                                        else viewModel.removerDoCarrinho(item.produtoId)
                                    },
                                    onRemover = {
                                        viewModel.removerDoCarrinho(item.produtoId)
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = Color.LightGray)

                    // Total
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "TOTAL",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "R$ %.2f".format(total),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        }
                    }

                    // Botões
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.limparCarrinho() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Limpar", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = {
                                if (caixaAberto) {
                                    showFormaPagamentoDialog = true
                                } else {
                                    onNavigateToCaixa()
                                }
                            },
                            modifier = Modifier.weight(2f),
                            enabled = carrinho.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Finalizar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    
    // Dialog de escolha de forma de pagamento
    if (showFormaPagamentoDialog) {
        EscolhaFormaPagamentoDialogPOS(
            onFormaSelecionada = { forma ->
                formaPagamentoSelecionada = forma
                showFormaPagamentoDialog = false
                showValorDialog = true
            },
            onCancelar = {
                showFormaPagamentoDialog = false
                formaPagamentoSelecionada = null
            }
        )
    }

    // Dialog de valor (após escolher forma)
    if (showValorDialog && formaPagamentoSelecionada != null) {
        ValorPagamentoDialogPOS(
            total = total,
            formaPagamento = formaPagamentoSelecionada!!,
            valorRecebido = valorRecebido,
            onValorRecebidoChange = { valorRecebido = it },
            onConfirmar = {
                val recebido = valorRecebido.toDoubleSafe(total)
                if (viewModel.finalizarVenda(formaPagamentoSelecionada!!, recebido)) {
                    showValorDialog = false
                    valorRecebido = ""
                    formaPagamentoSelecionada = null
                }
            },
            onCancelar = {
                showValorDialog = false
                valorRecebido = ""
                formaPagamentoSelecionada = null
            }
        )
    }
    
    // Diálogo de alerta de sincronização
    if (showSyncAlert && precisaSincronizar) {
        AlertDialog(
            onDismissRequest = { showSyncAlert = false },
            title = { 
                Text(
                    "🔄 Sincronização de Produtos",
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Column {
                    Text(
                        "Existem $produtosPendentes produtos locais que precisam ser sincronizados com o servidor.",
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Deseja sincronizar agora?",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sincronizarProdutos()
                        showSyncAlert = false
                    }
                ) {
                    Text("Sincronizar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSyncAlert = false }
                ) {
                    Text("Depois")
                }
            }
        )
    }
}

@Composable
fun TopBarAction(label: String, icon: ImageVector, onClick: () -> Unit, iconColor: Color = MaterialTheme.colorScheme.onPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, label, tint = iconColor)
        }
        Text(label, fontSize = 10.sp, color = iconColor)
    }
}

@Composable
fun ProdutoCardPOS(
    produto: Produto,
    vendidos: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp, max = 130.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            // Coluna principal com as informações
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Linha 1: [📦] Nome Produto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📦",
                        fontSize = 16.sp
                    )
                    Text(
                        produto.nome,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Linha 2: Descrição do produto (indentada)
                if (!produto.descricao.isNullOrBlank()) {
                    Text(
                        produto.descricao,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Linha 3: Estoque
                Text(
                    "Estoque: ${produto.estoqueFormatado()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (produto.estoque > 10) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    maxLines = 1
                )

                // Linha 4: Vendidos
                if (vendidos > 0) {
                    Text(
                        "Vendidos: $vendidos",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }

            // Código de barras no canto inferior esquerdo
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                produto.codigoBarras?.let { codigo ->
                    Text(
                        codigo,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )
                }
            }

            // Preço no canto inferior direito
            Text(
                produto.precoFormatado(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
fun CarrinhoItemPOS(
    item: ItemCarrinho,
    onQuantidadeChange: (Double) -> Unit,
    onRemover: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Info do produto
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.produtoNome,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2
                )
                Text(
                    "R$ %.2f x %.0f".format(item.precoUnitario, item.quantidade),
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                Text(
                    "Subtotal: R$ %.2f".format(item.total),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }

            // Controles de quantidade
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        val nova = item.quantidade - 1
                        if (nova <= 0) onRemover() else onQuantidadeChange(nova)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }

                Text(
                    "${item.quantidade}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = { onQuantidadeChange(item.quantidade + 1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onRemover,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun DialogVendaSucesso(
    venda: com.seucaixa.caixacombo.data.model.Venda,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val printService = remember { com.seucaixa.caixacombo.service.SunmiPrintService(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }

    // Mostrar erro se houver
    errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Erro de Impressão") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isPrinting) onDismiss() },
        title = {
            Text(
                "✅ Venda Finalizada!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info da venda
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
                            "Total: R$ %.2f".format(venda.total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Forma: ${venda.formaPagamento.name.replace("_", " ")}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Nº ${venda.numero}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Botões de impressão
                Text(
                    "Opções de Impressão:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )

                // Imprimir Total
                Button(
                    onClick = {
                        try {
                            isPrinting = true
                            printService.imprimirVenda(venda)
                            onDismiss()
                        } catch (e: Exception) {
                            android.util.Log.e("DialogVendaSucesso", "Erro ao imprimir: ${e.message}", e)
                            errorMessage = "Erro ao imprimir: ${e.message}"
                            isPrinting = false
                        }
                    },
                    enabled = !isPrinting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Print, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Imprimir Total")
                }

                // Imprimir Fichas Separadas
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            try {
                                isPrinting = true
                                var totalFichas = 0
                                venda.itens.forEach { item ->
                                    // Imprimir uma ficha por unidade
                                    repeat(item.quantidade.toInt()) { index ->
                                        printService.imprimirFichaProducao(
                                            item = item,
                                            numeroVenda = venda.numero,
                                            dataHora = venda.dataHora,
                                            formaPagamento = venda.formaPagamento.name.replace("_", " "),
                                            quantidadeUnidade = 1
                                        )
                                        totalFichas++
                                        // Delay entre impressões para garantir que saiam separadas
                                        kotlinx.coroutines.delay(800)
                                    }
                                }
                                android.util.Log.d("DialogVendaSucesso", "Total de fichas impressas: $totalFichas")
                                onDismiss()
                            } catch (e: Exception) {
                                android.util.Log.e("DialogVendaSucesso", "Erro ao imprimir fichas: ${e.message}", e)
                                errorMessage = "Erro ao imprimir fichas: ${e.message}"
                                isPrinting = false
                            }
                        }
                    },
                    enabled = !isPrinting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.ContentCopy, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val totalUnidades = venda.itens.sumOf { it.quantidade.toInt() }
                    Text("Imprimir Fichas ($totalUnidades unidades)")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}