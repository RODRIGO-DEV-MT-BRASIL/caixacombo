package com.seucaixa.caixacombo.ui.screens.checkout

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.ui.theme.DeviceType
import com.seucaixa.caixacombo.ui.viewmodel.CheckoutViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreenPremium(
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

    val primaryColor by remember { mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt()))) }
    val secondaryColor by remember { mutableStateOf(Color(sharedPreferences.getInt("secondary_color", 0xFF03DAC5.toInt()))) }

    val produtos by viewModel.produtos.collectAsState()
    val carrinho by viewModel.carrinho.collectAsState()
    val total by viewModel.total.collectAsState()
    val busca by viewModel.busca.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val categoriaSelecionada by viewModel.categoriaSelecionada.collectAsState()
    val vendaFinalizada by viewModel.vendaFinalizada.collectAsState()
    val ultimaVenda by viewModel.ultimaVenda.collectAsState()

    var showFormaPagamentoDialog by remember { mutableStateOf(false) }
    var showValorDialog by remember { mutableStateOf(false) }
    var valorRecebido by remember { mutableStateOf("") }
    var showBusca by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    var currentTime by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = SimpleDateFormat("dd MMM HH:mm", Locale("pt", "BR")).format(Date())
            delay(1000)
        }
    }

    if (vendaFinalizada && ultimaVenda != null) {
        AlertDialog(onDismissRequest = { viewModel.resetVendaFinalizada() },
            title = { Text("🎉 Venda Concluída!", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("R$ ${"%.2f".format(ultimaVenda!!.total)}", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(ultimaVenda!!.formaPagamento.name) }, leadingIcon = { Icon(Icons.Default.Payment, null, modifier = Modifier.size(16.dp)) })
                    if (ultimaVenda!!.troco > 0) AssistChip(onClick = {}, label = { Text("Troco R$ ${"%.2f".format(ultimaVenda!!.troco)}") }, leadingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.size(16.dp)) })
                }
            }},
            confirmButton = { Button(onClick = { viewModel.resetVendaFinalizada() }) { Text("OK") } }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(primaryColor, secondaryColor))).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PREMIUM", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                    Text(currentTime, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                }
                IconButton(onClick = onNavigateToHome) { Icon(Icons.Default.Home, null, tint = Color.White) }
                IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, null, tint = Color.White) }
            }
        }

        if (showBusca) {
            OutlinedTextField(value = searchText, onValueChange = { searchText = it; viewModel.buscarProdutos(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                placeholder = { Text("Buscar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = { IconButton(onClick = { showBusca = false; searchText = ""; viewModel.buscarProdutos("") }) { Icon(Icons.Default.Close, null) } },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = primaryColor)
            )
        } else {
            IconButton(onClick = { showBusca = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(4.dp)) {
                Icon(Icons.Default.Search, null, tint = Color.Gray)
            }
        }

        LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
            item {
                FilterChip(selected = categoriaSelecionada == null, onClick = { viewModel.selecionarCategoria(null) },
                    label = { Text("Todos") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor))
            }
            items(categorias) { cat ->
                FilterChip(selected = categoriaSelecionada?.id == cat.id, onClick = { viewModel.selecionarCategoria(cat) },
                    label = { Text(cat.nome) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = primaryColor))
            }
        }

        LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filtered = if (busca.isNotBlank()) produtos.filter { it.nome.contains(busca, true) || (it.codigoBarras ?: "").contains(busca) }
                else if (categoriaSelecionada != null) produtos.filter { it.categoriaId == categoriaSelecionada!!.id }
                else produtos
            items(filtered.take(60)) { produto ->
                Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { viewModel.adicionarAoCarrinho(produto) },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(8.dp)).background(primaryColor.copy(alpha = 0.1f)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShoppingBag, null, tint = primaryColor, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(produto.nome, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Text("R$ ${"%.2f".format(produto.precoVenda)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Carrinho", fontSize = 10.sp, color = Color.Gray)
                    Text("${carrinho.size} itens", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("TOTAL", fontSize = 10.sp, color = Color.Gray)
                    Text("R$ ${"%.2f".format(total)}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { if (carrinho.isNotEmpty()) showFormaPagamentoDialog = true },
                    enabled = carrinho.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.ShoppingCartCheckout, null); Spacer(Modifier.width(6.dp)); Text("VENDER", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showFormaPagamentoDialog) {
        AlertDialog(onDismissRequest = { showFormaPagamentoDialog = false },
            title = { Text("Pagamento", fontWeight = FontWeight.Bold) },
            text = { Column {
                Text("R$ ${"%.2f".format(total)}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryColor, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
                Button(onClick = { showFormaPagamentoDialog = false; showValorDialog = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("💵 Dinheiro", fontWeight = FontWeight.Bold) }
                Button(onClick = { showFormaPagamentoDialog = false; viewModel.finalizarVenda(FormaPagamento.PIX, 0.0, null, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("📱 PIX", fontWeight = FontWeight.Bold) }
                Button(onClick = { showFormaPagamentoDialog = false; viewModel.finalizarVenda(FormaPagamento.CARTAO_CREDITO, 0.0, null, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("💳 Crédito", fontWeight = FontWeight.Bold) }
                Button(onClick = { showFormaPagamentoDialog = false; viewModel.finalizarVenda(FormaPagamento.CARTAO_DEBITO, 0.0, null, null) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = primaryColor)) { Text("💳 Débito", fontWeight = FontWeight.Bold) }
            }},
            confirmButton = {}, dismissButton = { TextButton(onClick = { showFormaPagamentoDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showValorDialog) {
        AlertDialog(onDismissRequest = { showValorDialog = false },
            title = { Text("Valor Recebido", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = valorRecebido, onValueChange = { valorRecebido = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(), label = { Text("Valor") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("R$", fontWeight = FontWeight.Bold) },
                    singleLine = true, shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = { Button(onClick = {
                val valor = valorRecebido.toDoubleSafe()
                val troco = if (valor >= total) valor - total else 0.0
                viewModel.finalizarVenda(FormaPagamento.DINHEIRO, valor, null, null)
                showValorDialog = false; valorRecebido = ""
            }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showValorDialog = false }) { Text("Cancelar") } }
        )
    }
}