package com.seucaixa.caixacombo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.seucaixa.caixacombo.data.model.StatusVenda
import com.seucaixa.caixacombo.data.model.Venda
import com.seucaixa.caixacombo.ui.viewmodel.VendasViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendasScreen(
    onNavigateBack: () -> Unit,
    viewModel: VendasViewModel
) {
    val vendas by viewModel.vendas.collectAsState()
    val totalVendas by viewModel.totalVendas.collectAsState()
    val periodoSelecionado by viewModel.periodoSelecionado.collectAsState()
    
    var vendaSelecionada by remember { mutableStateOf<Venda?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de Vendas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
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
        ) {
            // Resumo
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
                        "Total em Vendas",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "R$ %.2f".format(totalVendas),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${vendas.size} vendas no período",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filtro de período
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = periodoSelecionado == Periodo.HOJE,
                    onClick = { viewModel.setPeriodo(Periodo.HOJE) },
                    label = { Text("Hoje") }
                )
                FilterChip(
                    selected = periodoSelecionado == Periodo.SEMANA,
                    onClick = { viewModel.setPeriodo(Periodo.SEMANA) },
                    label = { Text("Esta Semana") }
                )
                FilterChip(
                    selected = periodoSelecionado == Periodo.MES,
                    onClick = { viewModel.setPeriodo(Periodo.MES) },
                    label = { Text("Este Mês") }
                )
                FilterChip(
                    selected = periodoSelecionado == Periodo.TODOS,
                    onClick = { viewModel.setPeriodo(Periodo.TODOS) },
                    label = { Text("Todas") }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Lista de vendas
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vendas) { venda ->
                    VendaItem(
                        venda = venda,
                        onClick = { vendaSelecionada = venda }
                    )
                }
            }
        }
    }
    
    // Diálogo de detalhes da venda
    vendaSelecionada?.let { venda ->
        AlertDialog(
            onDismissRequest = { vendaSelecionada = null },
            title = {
                Column {
                    Text(
                        "Venda #${venda.numero}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(venda.dataHora)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Resumo
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
                                "Total",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "R$ %.2f".format(venda.total),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                formatFormaPagamento(venda.formaPagamento),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    // Itens da venda
                    Text(
                        "Itens da Venda",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    venda.itens.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
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
                                        "Qtd: %.0f".format(item.quantidade),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "R$ %.2f".format(item.total),
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
                TextButton(onClick = { vendaSelecionada = null }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
fun VendaItem(
    venda: Venda,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    venda.numero,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    dateFormat.format(Date(venda.dataHora)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${venda.itens.size} itens • ${formatFormaPagamento(venda.formaPagamento)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Itens da venda
                venda.itens.take(3).forEach { item ->
                    Text(
                        "• ${item.produtoNome} x${item.quantidade.toInt()} = R$ %.2f".format(item.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (venda.itens.size > 3) {
                    Text(
                        "... e mais ${venda.itens.size - 3} itens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "R$ %.2f".format(venda.total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                val statusColor = when (venda.status) {
                    StatusVenda.FINALIZADA -> MaterialTheme.colorScheme.tertiary
                    StatusVenda.CANCELADA -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Text(
                    venda.status.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}

fun formatFormaPagamento(forma: com.seucaixa.caixacombo.data.model.FormaPagamento): String {
    return when (forma) {
        com.seucaixa.caixacombo.data.model.FormaPagamento.DINHEIRO -> "Dinheiro"
        com.seucaixa.caixacombo.data.model.FormaPagamento.CARTAO_CREDITO -> "Cartão Crédito"
        com.seucaixa.caixacombo.data.model.FormaPagamento.CARTAO_DEBITO -> "Cartão Débito"
        com.seucaixa.caixacombo.data.model.FormaPagamento.PIX -> "PIX"
        com.seucaixa.caixacombo.data.model.FormaPagamento.BOLETO -> "Boleto"
        com.seucaixa.caixacombo.data.model.FormaPagamento.FIADO -> "Fiado"
    }
}

enum class Periodo {
    HOJE, SEMANA, MES, TODOS
}
