package com.seucaixa.caixacombo.ui.screens.checkout

/**
 * Checkout Screen otimizado para dispositivos móveis (P2, tablets pequenos, smartphones)
 * - Telas menores (7-10")
 * - Layout vertical
 * - Bottom sheet para carrinho
 * - Design compacto
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seucaixa.caixacombo.data.model.Categoria
import com.seucaixa.caixacombo.data.model.FormaPagamento
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.estoqueFormatado
import com.seucaixa.caixacombo.data.model.precoFormatado
import com.seucaixa.caixacombo.ui.components.CustomKeyboard
import com.seucaixa.caixacombo.ui.components.OutlinedTextFieldWithCustomKeyboard
import com.seucaixa.caixacombo.ui.components.PdvCategoriaFilterRow
import com.seucaixa.caixacombo.ui.components.PdvProdutoCard
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ItemCarrinho
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import android.content.Context
import com.seucaixa.caixacombo.service.PollingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenMobile(
    viewModel: CheckoutViewModel,
    caixaAberto: Boolean,
    onNavigateToHome: () -> Unit,
    onNavigateToProdutos: () -> Unit,
    onNavigateToVendas: () -> Unit,
    onNavigateToCaixa: () -> Unit,
    onLogout: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val produtos by viewModel.produtos.collectAsState()
    val carrinho by viewModel.carrinho.collectAsState()
    val total by viewModel.total.collectAsState()
    val busca by viewModel.busca.collectAsState()
    val vendaFinalizada by viewModel.vendaFinalizada.collectAsState()
    val vendidosPorProduto by viewModel.vendidosPorProduto.collectAsState()
    val ultimaVenda by viewModel.ultimaVenda.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val categoriaSelecionada by viewModel.categoriaSelecionada.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val printPrefs = remember { context.getSharedPreferences("config_impressao", Context.MODE_PRIVATE) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showFormaPagamentoDialog by remember { mutableStateOf(false) }
    var formaPagamentoSelecionada by remember { mutableStateOf<FormaPagamento?>(null) }
    var showValorDialog by remember { mutableStateOf(false) }
    var showPixDialog by remember { mutableStateOf(false) }
    var showCreditoDialog by remember { mutableStateOf(false) }
    var showDebitoDialog by remember { mutableStateOf(false) }
    var receivedValue by remember { mutableStateOf("") }

    // Diálogo de sucesso com botões de impressão
    if (vendaFinalizada && ultimaVenda != null) {
        DialogVendaSucesso(
            venda = ultimaVenda!!,
            onDismiss = { viewModel.resetVendaFinalizada() },
            imprimirTotal = printPrefs.getBoolean("imprimir_total", true),
            imprimirFichas = printPrefs.getBoolean("imprimir_fichas", true)
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Caixa Combo",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(70.dp),
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botão Dashboard
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onNavigateToDashboard() }
                        ) {
                            IconButton(
                                onClick = onNavigateToDashboard,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Dashboard,
                                    "Dashboard",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                "Dashboard",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Botão Sair
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onLogout() }
                        ) {
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.Logout,
                                    "Sair",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                "Sair",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Botão do carrinho
                        BadgedBox(
                            badge = {
                                if (carrinho.isNotEmpty()) {
                                    Badge { Text("${carrinho.size}") }
                                }
                            }
                        ) {
                            IconButton(
                                onClick = { showBottomSheet = true },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    "Carrinho",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (carrinho.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBottomSheet = true },
                    icon = { Icon(Icons.Default.ShoppingCart, null) },
                    text = { 
                        Text("R$ %.2f".format(total))
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // Busca
            OutlinedTextField(
                value = busca,
                onValueChange = viewModel::buscarProdutos,
                placeholder = { Text("Buscar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Categorias - chips horizontais
            if (categorias.isNotEmpty()) {
                PdvCategoriaFilterRow(
                    categorias = categorias,
                    categoriaSelecionada = categoriaSelecionada,
                    onCategoriaClick = { viewModel.selecionarCategoria(it) },
                    modifier = Modifier.fillMaxWidth(),
                    selectedContainerAlpha = 0.2f,
                    toggleOnReClick = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Grid de produtos - 2 colunas
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(produtos, key = { it.id }) { produto ->
                    PdvProdutoCard(
                        produto = produto,
                        vendidos = vendidosPorProduto[produto.id] ?: 0,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        onCardClick = { viewModel.adicionarAoCarrinho(produto) },
                        imageHeight = 64.dp,
                        nameFontSize = 12.sp,
                        priceFontSize = 14.sp,
                        cardElevation = 0.dp,
                        cardColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
    
    // Bottom Sheet do Carrinho (mobile)
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            CarrinhoBottomSheetContent(
                carrinho = carrinho,
                total = total,
                onQuantidadeChange = viewModel::atualizarQuantidade,
                onRemover = viewModel::removerDoCarrinho,
                onLimpar = viewModel::limparCarrinho,
                onFinalizar = {
                    scope.launch {
                        sheetState.hide()
                        showBottomSheet = false
                        if (!caixaAberto) {
                            onNavigateToCaixa()
                        } else {
                            showFormaPagamentoDialog = true
                        }
                    }
                }
            )
        }
    }
    
    // Dialog de escolha de forma de pagamento
    if (showFormaPagamentoDialog) {
        EscolhaFormaPagamentoDialogMobile(
            onFormaSelecionada = { forma ->
                formaPagamentoSelecionada = forma
                showFormaPagamentoDialog = false
                when (forma) {
                    FormaPagamento.PIX -> showPixDialog = true
                    FormaPagamento.CARTAO_CREDITO -> showCreditoDialog = true
                    FormaPagamento.CARTAO_DEBITO -> showDebitoDialog = true
                    else -> showValorDialog = true
                }
            },
            onCancelar = {
                showFormaPagamentoDialog = false
                formaPagamentoSelecionada = null
            }
        )
    }

    // Dialog de valor (após escolher forma)
    if (showValorDialog && formaPagamentoSelecionada != null) {
        ValorPagamentoDialogMobile(
            total = total,
            formaPagamento = formaPagamentoSelecionada!!,
            receivedValue = receivedValue,
            onValorRecebidoChange = { receivedValue = it },
            onConfirmar = {
                val recebido = receivedValue.toDoubleSafe(total)
                if (viewModel.finalizarVenda(formaPagamentoSelecionada!!, recebido)) {
                    showValorDialog = false
                    receivedValue = ""
                    formaPagamentoSelecionada = null
                }
            },
            onCancelar = {
                showValorDialog = false
                receivedValue = ""
                formaPagamentoSelecionada = null
            }
        )
    }

    // Dialog PIX específico
    if (showPixDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showPixDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "📱 Pagamento PIX",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "Total: R$ ${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        "Tempo de pagamento: 5 minutos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Placeholder para QR Code
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = "QR Code PIX",
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "QR Code PIX",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Chave PIX:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Cole sua chave PIX aqui") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPixDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                showPixDialog = false
                                val recebido = total
                                if (viewModel.finalizarVenda(FormaPagamento.PIX, recebido)) {
                                    receivedValue = ""
                                    formaPagamentoSelecionada = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }

    // Dialog Crédito específico
    if (showCreditoDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showCreditoDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "💳 Cartão de Crédito",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "Total: R$ ${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Insira o cartão na máquina",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreditoDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                showCreditoDialog = false
                                val recebido = total
                                if (viewModel.finalizarVenda(FormaPagamento.CARTAO_CREDITO, recebido)) {
                                    receivedValue = ""
                                    formaPagamentoSelecionada = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }

    // Dialog Débito específico
    if (showDebitoDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDebitoDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "💳 Cartão de Débito",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        "Total: R$ ${String.format("%.2f", total)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        "Insira o cartão na máquina",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDebitoDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                        Button(
                            onClick = {
                                showDebitoDialog = false
                                val recebido = total
                                if (viewModel.finalizarVenda(FormaPagamento.CARTAO_DEBITO, recebido)) {
                                    receivedValue = ""
                                    formaPagamentoSelecionada = null
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar", fontWeight = FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarrinhoBottomSheetContent(
    carrinho: List<ItemCarrinho>,
    total: Double,
    onQuantidadeChange: (Long, Double) -> Unit,
    onRemover: (Long) -> Unit,
    onLimpar: () -> Unit,
    onFinalizar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Carrinho",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "${carrinho.size} itens",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HorizontalDivider()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Itens - expande dinamicamente conforme produtos são adicionados
        LazyColumn(
            modifier = Modifier
                .weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(carrinho) { item ->
                CarrinhoItemMobile(
                    item = item,
                    onQuantidadeChange = { qtd ->
                        onQuantidadeChange(item.produtoId, qtd)
                    },
                    onRemover = { onRemover(item.produtoId) }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        HorizontalDivider()
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Total:",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "R$ %.2f".format(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Botões
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onLimpar,
                modifier = Modifier.weight(1f)
            ) {
                Text("Limpar")
            }
            
            Button(
                onClick = onFinalizar,
                modifier = Modifier.weight(2f),
                enabled = carrinho.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Finalizar Venda", fontWeight = FontWeight.Bold)
            }
        }
        
        // Espaço para o gesture do bottom sheet
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CarrinhoItemMobile(
    item: ItemCarrinho,
    onQuantidadeChange: (Double) -> Unit,
    onRemover: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nome do produto
            Text(
                item.produtoNome,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            
            // Quantidade
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onQuantidadeChange(item.quantidade - 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                }
                
                Text(
                    "%.0f".format(item.quantidade),
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.Center
                )
                
                IconButton(
                    onClick = { onQuantidadeChange(item.quantidade + 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                }
            }
            
            // Preço
            Text(
                "R$ %.2f".format(item.total),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Botão apagar
            IconButton(
                onClick = onRemover,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

