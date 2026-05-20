package com.seucaixa.caixacombo.ui.screens.checkout

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.theme.DeviceType
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import com.seucaixa.caixacombo.ui.viewmodel.ItemCarrinho
import com.seucaixa.caixacombo.data.SecurePrefs
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenMaster(
    viewModel: CheckoutViewModel,
    caixaAberto: Boolean,
    deviceType: DeviceType,
    onNavigateToHome: () -> Unit,
    onNavigateToProdutos: () -> Unit,
    onNavigateToVendas: () -> Unit,
    onNavigateToCaixa: () -> Unit,
    onNavigateToConfiguracaoTipoImpressao: () -> Unit = {},
    onNavigateToAcessos: () -> Unit = {},
    onNavigateToCadastro: () -> Unit = {},
    onSendStonePayment: ((Long, String, String, String, (StoneDeeplinkService.PaymentResult?) -> Unit) -> Unit)? = null,
    onLogout: () -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", Context.MODE_PRIVATE) }

    var usuarioLogado by remember { mutableStateOf<Usuario?>(null) }
    LaunchedEffect(Unit) {
        try {
            val operatorId = com.seucaixa.caixacombo.data.SecurePrefs.getOperatorId(context)
            if (operatorId > 0) {
                val dao = com.seucaixa.caixacombo.data.database.AppDatabase.getDatabase(context).usuarioDao()
                usuarioLogado = dao.getUsuarioById(operatorId)
            }
        } catch (e: Exception) { android.util.Log.e("CheckoutMaster", "Erro ao carregar usuário", e) }
    }

    val primaryColor by remember { mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt()))) }
    val backgroundColor by remember { mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFF5F5F5.toInt()))) }

    val produtos by viewModel.produtos.collectAsState()
    val carrinho by viewModel.carrinho.collectAsState()
    val total by viewModel.total.collectAsState()
    val busca by viewModel.busca.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val vendaFinalizada by viewModel.vendaFinalizada.collectAsState()
    val ultimaVenda by viewModel.ultimaVenda.collectAsState()

    var showFormaPagamentoDialog by remember { mutableStateOf(false) }
    var showValorDialog by remember { mutableStateOf(false) }
    var valorRecebido by remember { mutableStateOf("") }
    var showBuscaDialog by remember { mutableStateOf(false) }
    var buscaText by remember { mutableStateOf("") }
    var clienteSelecionado by remember { mutableStateOf<Cliente?>(null) }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
            delay(1000)
        }
    }

    if (vendaFinalizada && ultimaVenda != null) {
        DialogVendaSucesso(venda = ultimaVenda!!, onDismiss = { viewModel.resetVendaFinalizada() }, nomeCliente = null)
    }

    if (showBuscaDialog) {
        Dialog(onDismissRequest = { showBuscaDialog = false }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Buscar Produto", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    OutlinedTextField(value = buscaText, onValueChange = { buscaText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Código ou nome do produto") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { IconButton(onClick = { showBuscaDialog = false; viewModel.buscarProdutos(buscaText) }) { Icon(Icons.Default.Search, null) } }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        val filtered = if (buscaText.isBlank()) emptyList() else produtos.filter {
                            it.nome.contains(buscaText, ignoreCase = true) || (it.codigoBarras ?: "").contains(buscaText)
                        }.take(20)
                        items(filtered) { produto ->
                            ListItem(headlineContent = { Text(produto.nome, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = { Text("R$ ${"%.2f".format(produto.precoVenda)}", color = primaryColor) },
                                modifier = Modifier.clickable {
                                    viewModel.adicionarAoCarrinho(produto)
                                    showBuscaDialog = false
                                    buscaText = ""
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        TopAppBar(title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column { Text("MASTER", fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(currentTime, fontSize = 10.sp, color = Color.Gray) }
            }
        }, navigationIcon = { IconButton(onClick = onNavigateToHome) { Icon(Icons.Default.Home, null) } },
            actions = {
                IconButton(onClick = { showBuscaDialog = true }) { Icon(Icons.Default.Search, null) }
                IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, null) }
                IconButton(onClick = onNavigateToCaixa) { Icon(Icons.Default.AccountBalance, null) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor, titleContentColor = Color.White, actionIconContentColor = Color.White, navigationIconContentColor = Color.White)
        )

        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(modifier = Modifier.weight(0.55f).fillMaxHeight().padding(8.dp)) {
                Text("Carrinho (${carrinho.size} itens)", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(carrinho) { item ->
                        CarrinhoItemMaster(item = item, onRemove = { viewModel.removerDoCarrinho(item.produtoId) }, onUpdateQtd = { qtd -> viewModel.atualizarQuantidade(item.produtoId, qtd.toDouble()) }, primaryColor = primaryColor)
                    }
                }
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF2D2D2D)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("R$ ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, color = Color(0xFFFFD700), fontSize = 18.sp)
                }
                if (carrinho.isNotEmpty()) {
                    Button(onClick = { showFormaPagamentoDialog = true }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor), shape = RoundedCornerShape(8.dp)) {
                        Icon(Icons.Default.ShoppingCart, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("FINALIZAR VENDA", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.weight(0.45f).fillMaxHeight().background(Color(0xFFE8E8E8)).padding(6.dp)) {
                Text("Produtos", fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(produtos.take(30)) { produto ->
                        Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(Color.White).clickable { viewModel.adicionarAoCarrinho(produto) }.padding(4.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(produto.nome, fontSize = 8.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("R$ ${"%.2f".format(produto.precoVenda)}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                            }
                        }
                    }
                }
                NumericKeypad(onNumberClick = { num -> buscaText += num }, onClear = { buscaText = "" }, onBackspace = { if (buscaText.isNotEmpty()) buscaText = buscaText.dropLast(1) }, primaryColor = primaryColor)
            }
        }
    }

    if (showFormaPagamentoDialog) {
        FormaPagamentoDialogMaster(total = total, onDismiss = { showFormaPagamentoDialog = false }, onSelect = { forma ->
            showFormaPagamentoDialog = false
            if (forma == FormaPagamento.DINHEIRO) { showValorDialog = true } else { viewModel.finalizarVenda(forma, 0.0, null, null) }
        }, primaryColor = primaryColor)
    }

    if (showValorDialog) {
        ValorDialogoMaster(total = total, onDismiss = { showValorDialog = false }, onConfirm = { valor ->
            val numVal = valor.toDoubleSafe()
            val troco = if (numVal >= total) numVal - total else null
            viewModel.finalizarVenda(FormaPagamento.DINHEIRO, numVal, null, null)
            showValorDialog = false
        })
    }
}

@Composable
fun ValorDialogoMaster(total: Double, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var valor by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Valor Recebido", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = valor, onValueChange = { valor = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) }, singleLine = true
                )
                if (valor.isNotBlank()) {
                    val numVal = valor.toDoubleSafe()
                    if (numVal >= total) {
                        val troco = numVal - total
                        Text("Troco: R$ ${"%.2f".format(troco)}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Text("Faltam: R$ ${"%.2f".format(total - numVal)}", color = Color.Red, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(valor) }, enabled = valor.isNotBlank()) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun CarrinhoItemMaster(item: ItemCarrinho, onRemove: () -> Unit, onUpdateQtd: (Double) -> Unit, primaryColor: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).background(Color.White).padding(8.dp).clip(RoundedCornerShape(6.dp)), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.produtoNome, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("R$ ${"%.2f".format(item.precoUnitario)}", fontSize = 10.sp, color = Color.Gray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onUpdateQtd(item.quantidade - 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp)) }
            Text("${item.quantidade.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.widthIn(min = 20.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { onUpdateQtd(item.quantidade + 1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
        }
        Text("R$ ${"%.2f".format(item.total)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.width(60.dp), textAlign = TextAlign.End)
        IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red) }
    }
}

@Composable
fun NumericKeypad(onNumberClick: (String) -> Unit, onClear: () -> Unit, onBackspace: () -> Unit, primaryColor: Color) {
    val keys = listOf("1","2","3","4","5","6","7","8","9","C","0","⌫")
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        keys.chunked(3).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    val color = when(key) { "C" -> Color.Red; "⌫" -> Color(0xFFFF8C00); else -> primaryColor }
                    Box(modifier = Modifier.weight(1f).aspectRatio(1.5f).padding(2.dp).clip(RoundedCornerShape(6.dp)).background(color).clickable {
                        when(key) { "C" -> onClear(); "⌫" -> onBackspace(); else -> onNumberClick(key) }
                    }, contentAlignment = Alignment.Center) {
                        Text(key, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun FormaPagamentoDialogMaster(total: Double, onDismiss: () -> Unit, onSelect: (FormaPagamento) -> Unit, primaryColor: Color) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Forma de Pagamento", fontWeight = FontWeight.Bold) },
        text = { Column {
            Text("Total: R$ ${"%.2f".format(total)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryColor, modifier = Modifier.padding(bottom = 12.dp))
            Button(onClick = { onSelect(FormaPagamento.DINHEIRO) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Dinheiro", fontWeight = FontWeight.Bold) }
            Button(onClick = { onSelect(FormaPagamento.PIX) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("PIX", fontWeight = FontWeight.Bold) }
            Button(onClick = { onSelect(FormaPagamento.CARTAO_CREDITO) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Crédito", fontWeight = FontWeight.Bold) }
            Button(onClick = { onSelect(FormaPagamento.CARTAO_DEBITO) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("Débito", fontWeight = FontWeight.Bold) }
        }},
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}