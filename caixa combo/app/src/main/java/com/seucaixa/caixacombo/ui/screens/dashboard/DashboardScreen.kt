package com.seucaixa.caixacombo.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.model.FormaPagamento
import com.seucaixa.caixacombo.data.model.Venda
import java.text.SimpleDateFormat
import java.util.*

data class DashboardData(
    val vendasHoje: List<Venda> = emptyList(),
    val vendasSemana: List<Venda> = emptyList(),
    val vendasMes: List<Venda> = emptyList()
) {
    val totalHoje: Double get() = vendasHoje.sumOf { it.total }
    val totalSemana: Double get() = vendasSemana.sumOf { it.total }
    val totalMes: Double get() = vendasMes.sumOf { it.total }
    val qtdVendasHoje: Int get() = vendasHoje.size
    val ticketMedioHoje: Double get() = if (vendasHoje.isNotEmpty()) totalHoje / qtdVendasHoje else 0.0

    val vendasPorHora: Map<String, Double> get() {
        val map = mutableMapOf<String, Double>()
        for (h in 0..23) map["${h}h"] = 0.0
        vendasHoje.forEach { v ->
            val hour = Calendar.getInstance().apply { timeInMillis = v.dataHora }.get(Calendar.HOUR_OF_DAY)
            val key = "${hour}h"
            map[key] = (map[key] ?: 0.0) + v.total
        }
        return map
    }

    val vendasPorDiaSemana: Map<String, Double> get() {
        val dias = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
        val map = dias.associateWith { 0.0 }.toMutableMap()
        val cal = Calendar.getInstance()
        vendasSemana.forEach { v ->
            cal.timeInMillis = v.dataHora
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val idx = if (dow == Calendar.SUNDAY) 6 else dow - 2
            if (idx in 0..6) {
                val key = dias[idx]
                map[key] = (map[key] ?: 0.0) + v.total
            }
        }
        return map
    }

    val vendasPorFormaPagamento: Map<String, Double> get() {
        val map = mutableMapOf<String, Double>()
        vendasHoje.forEach { v ->
            val key = when (v.formaPagamento) {
                FormaPagamento.DINHEIRO -> "Dinheiro"
                FormaPagamento.CARTAO_CREDITO -> "Crédito"
                FormaPagamento.CARTAO_DEBITO -> "Débito"
                FormaPagamento.PIX -> "PIX"
                FormaPagamento.BOLETO -> "Boleto"
                FormaPagamento.FIADO -> "Fiado"
            }
            map[key] = (map[key] ?: 0.0) + v.total
        }
        return map
    }

    val topProdutos: List<Pair<String, Int>> get() {
        val map = mutableMapOf<String, Int>()
        vendasHoje.forEach { v ->
            v.itens.forEach { item ->
                map[item.produtoNome] = (map[item.produtoNome] ?: 0) + item.quantidade.toInt()
            }
        }
        return map.entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    data: DashboardData,
    primaryColor: Color,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", android.content.Context.MODE_PRIVATE) }
    val backgroundColor = Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt()))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(primaryColor)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Dashboard, null, tint = Color.White, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dashboard", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.weight(1f))
            val currentTime = remember { mutableStateOf("") }
            LaunchedEffect(Unit) {
                while (true) {
                    currentTime.value = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
                    kotlinx.coroutines.delay(60000)
                }
            }
            Text(currentTime.value, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // KPI Cards Row
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KPICard("Vendas Hoje", data.qtdVendasHoje.toString(), Icons.Default.ShoppingCart, primaryColor, Modifier.weight(1f))
                    KPICard("Total Hoje", "R$ %.2f".format(data.totalHoje), Icons.Default.AttachMoney, Color(0xFF4CAF50), Modifier.weight(1f))
                    KPICard("Ticket Médio", "R$ %.2f".format(data.ticketMedioHoje), Icons.Default.Receipt, Color(0xFFFF9800), Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KPICard("Semana", "R$ %.2f".format(data.totalSemana), Icons.Default.DateRange, Color(0xFF2196F3), Modifier.weight(1f))
                    KPICard("Mês", "R$ %.2f".format(data.totalMes), Icons.Default.CalendarMonth, Color(0xFF9C27B0), Modifier.weight(1f))
                }
            }

            // Vendas por Hora
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timeline, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vendas por Hora (Hoje)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryColor)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        BarChart(data = data.vendasPorHora, primaryColor = primaryColor, modifier = Modifier.fillMaxWidth().height(180.dp))
                    }
                }
            }

            // Vendas da Semana
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, null, tint = Color(0xFF2196F3), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vendas da Semana", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2196F3))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        BarChart(data = data.vendasPorDiaSemana, primaryColor = Color(0xFF2196F3), modifier = Modifier.fillMaxWidth().height(160.dp))
                    }
                }
            }

            // Forma de pagamento
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PieChart, null, tint = Color(0xFFFF9800), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Formas de Pagamento", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFF9800))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            DonutChart(data = data.vendasPorFormaPagamento, modifier = Modifier.size(180.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                data.vendasPorFormaPagamento.entries.forEachIndexed { idx, entry ->
                                    val colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFFF44336), Color(0xFF607D8B))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(12.dp).background(colors[idx % colors.size], CircleShape))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(entry.key, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("R$ %.2f".format(entry.value), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Top Produtos
            if (data.topProdutos.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Top Produtos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF4CAF50))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            data.topProdutos.forEachIndexed { idx, (nome, qtd) ->
                                val maxQtd = data.topProdutos.maxOf { it.second }
                                val progress = if (maxQtd > 0) qtd.toFloat() / maxQtd else 0f
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(nome, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text("${qtd}x", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = primaryColor,
                                        trackColor = primaryColor.copy(alpha = 0.15f),
                                    )
                                    if (idx < data.topProdutos.lastIndex) Spacer(modifier = Modifier.height(10.dp))
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
private fun KPICard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BarChart(
    data: Map<String, Double>,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = data.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val animatedProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 1.8f)
        val spacing = (size.width - barWidth * data.size) / (data.size + 1)
        val chartHeight = size.height - 24.dp.toPx()

        data.entries.forEachIndexed { idx, (label, value) ->
            val x = spacing + idx * (barWidth + spacing)
            val barHeight = (value / maxValue * chartHeight * animatedProgress.value).toFloat().coerceAtLeast(2.dp.toPx())
            val y = chartHeight - barHeight

            drawRoundRect(
                brush = Brush.verticalGradient(colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f))),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            if (value > 0) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "R$ %.0f".format(value),
                    topLeft = Offset(x + barWidth / 2 - 16.dp.toPx(), y - 14.dp.toPx()),
                    style = TextStyle(color = primaryColor, fontSize = 8.sp)
                )
            }

            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(x + barWidth / 2 - 8.dp.toPx(), chartHeight + 2.dp.toPx()),
                style = TextStyle(color = Color.Gray, fontSize = 8.sp)
            )
        }
    }
}

@Composable
private fun DonutChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFFF44336), Color(0xFF607D8B))
    val total = data.values.sum().coerceAtLeast(1.0)
    val animatedProgress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 28.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            var startAngle = -90f

            data.entries.forEachIndexed { idx, (_, value) ->
                val sweep = (value / total * 360 * animatedProgress.value).toFloat()
                drawArc(
                    color = colors[idx % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep
            }
        }
        Text(
            "R$ %.0f".format(total),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
