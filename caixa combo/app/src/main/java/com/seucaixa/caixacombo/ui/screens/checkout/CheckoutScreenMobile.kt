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
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ItemCarrinho
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
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
    var showBottomSheet by remember { mutableStateOf(false) }
    var showFormaPagamentoDialog by remember { mutableStateOf(false) }
    var formaPagamentoSelecionada by remember { mutableStateOf<FormaPagamento?>(null) }
    var showValorDialog by remember { mutableStateOf(false) }
    var showPixDialog by remember { mutableStateOf(false) }
    var showCreditoDialog by remember { mutableStateOf(false) }
    var showDebitoDialog by remember { mutableStateOf(false) }
    var valorRecebido by remember { mutableStateOf("") }

    // Diálogo de sucesso com botões de impressão
    if (vendaFinalizada && ultimaVenda != null) {
        DialogVendaSucessoMobile(
            venda = ultimaVenda!!,
            onDismiss = { viewModel.resetVendaFinalizada() }
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
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = categoriaSelecionada == null,
                            onClick = { viewModel.selecionarCategoria(null) },
                            label = { Text("Todos", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    items(categorias) { cat ->
                        FilterChip(
                            selected = categoriaSelecionada?.id == cat.id,
                            onClick = { viewModel.selecionarCategoria(if (categoriaSelecionada?.id == cat.id) null else cat) },
                            label = { Text(cat.nome, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
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
                    ProdutoItemMobile(
                        produto = produto,
                        vendidos = vendidosPorProduto[produto.id] ?: 0,
                        onClick = { 
                            viewModel.adicionarAoCarrinho(produto)
                            // Carrinho só abre quando clicar no botão do carrinho
                        }
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
                                    valorRecebido = ""
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
                                    valorRecebido = ""
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
                                    valorRecebido = ""
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

@Composable
fun ProdutoItemMobile(
    produto: Produto,
    vendidos: Int = 0,
    onClick: () -> Unit
) {
    val semEstoque = produto.estoque <= 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!semEstoque) onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (semEstoque) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Imagem do produto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (produto.imagem != null) {
                    val imageUrl = if (produto.imagem!!.startsWith("http") || produto.imagem!!.startsWith("data:")) produto.imagem!! else "${PollingService.getServerUrl()}${produto.imagem}"
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = produto.nome,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Nome
            Text(
                produto.nome,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (semEstoque) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // Preço
            Text(
                produto.precoFormatado(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (semEstoque) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.primary
            )

            // Estoque
            if (semEstoque) {
                Text(
                    "ESGOTADO",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (produto.estoque <= 5) {
                Text(
                    "Estq: ${produto.estoque.toInt()}",
                    fontSize = 9.sp,
                    color = Color(0xFFFF9800)
                )
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

@Composable
fun DialogVendaSucessoMobile(
    venda: com.seucaixa.caixacombo.data.model.Venda,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val printService = remember { com.seucaixa.caixacombo.service.SunmiPrintService(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var isPrinting by remember { mutableStateOf(false) }
    
    // Ler configurações de impressão
    val sharedPreferences = remember { context.getSharedPreferences("config_impressao", android.content.Context.MODE_PRIVATE) }
    val imprimirTotal = remember { sharedPreferences.getBoolean("imprimir_total", true) }
    val imprimirFichas = remember { sharedPreferences.getBoolean("imprimir_fichas", true) }

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

                // Botões de impressão (condicionais)
                if (imprimirTotal || imprimirFichas) {
                    Text(
                        "Opções de Impressão:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Imprimir Total (se habilitado)
                if (imprimirTotal) {
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
                }

                // Imprimir Fichas Separadas (se habilitado)
                if (imprimirFichas) {
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
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
