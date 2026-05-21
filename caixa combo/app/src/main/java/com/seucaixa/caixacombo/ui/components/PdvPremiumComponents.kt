package com.seucaixa.caixacombo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.model.Categoria
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.precoFormatado
import com.seucaixa.caixacombo.service.PollingService
import java.text.NumberFormat
import java.util.Locale

/**
 * Componentes visuais reutilizáveis para o PDV.
 *
 * Estes componentes foram criados para reduzir repetição na tela de checkout,
 * melhorar leitura em terminais Android e deixar o visual mais profissional.
 */

@Composable
fun PdvStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PdvStoneStatusBanner(
    isStoneAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isStoneAvailable) Color(0xFF15803D) else MaterialTheme.colorScheme.error
    val title = if (isStoneAvailable) "Stone pronta para pagamento" else "Stone indisponível"
    val description = if (isStoneAvailable) {
        "Terminal habilitado para receber crédito, débito e PIX Stone."
    } else {
        "Verifique se o app Stone está instalado, ativado e autenticado neste terminal."
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isStoneAvailable) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PdvEmptyCartState(
    modifier: Modifier = Modifier,
    title: String = "Carrinho vazio",
    description: String = "Busque um produto para iniciar a venda."
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PdvTotalCard(
    total: Double,
    itemCount: Int,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total da venda",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrencyPtBr(total),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = primaryColor
                )
            }
            PdvStatusChip(
                text = "$itemCount ${if (itemCount == 1) "item" else "itens"}",
                color = primaryColor,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )
        }
    }
}

@Composable
fun PdvPrimaryActionButton(
    text: String,
    enabled: Boolean,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Icon(
                imageVector = Icons.Default.Payment,
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PdvOfflineWarning(
    modifier: Modifier = Modifier,
    message: String = "Sem conexão com o servidor. Algumas ações podem não sincronizar agora."
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatCurrencyPtBr(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}

fun sanitizePdvSearchTerm(value: String): String {
    return value.trim().replace(Regex("\\s+"), " ")
}

@Composable
fun PdvCategoriaFilterRow(
    categorias: List<Categoria>,
    categoriaSelecionada: Categoria?,
    onCategoriaClick: (Categoria?) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    selectedContainerAlpha: Float = 1f,
    toggleOnReClick: Boolean = false
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            FilterChip(
                selected = categoriaSelecionada == null,
                onClick = { onCategoriaClick(null) },
                label = { Text("Todos", fontSize = fontSize) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedColor.copy(alpha = selectedContainerAlpha),
                    selectedLabelColor = if (selectedContainerAlpha < 1f) selectedColor else Color.White,
                    containerColor = unselectedContainerColor
                )
            )
        }
        items(categorias) { cat ->
            FilterChip(
                selected = categoriaSelecionada?.id == cat.id,
                onClick = {
                    if (toggleOnReClick && categoriaSelecionada?.id == cat.id) {
                        onCategoriaClick(null)
                    } else {
                        onCategoriaClick(cat)
                    }
                },
                label = { Text(cat.nome, fontSize = fontSize) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = selectedColor.copy(alpha = selectedContainerAlpha),
                    selectedLabelColor = if (selectedContainerAlpha < 1f) selectedColor else Color.White,
                    containerColor = unselectedContainerColor
                )
            )
        }
    }
}

@Composable
fun PdvProdutoCard(
    produto: Produto,
    vendidos: Int = 0,
    quantidadeNoCarrinho: Int = 0,
    primaryColor: Color,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    nameColor: Color = MaterialTheme.colorScheme.onSurface,
    imageHeight: Dp = 60.dp,
    nameFontSize: TextUnit = 12.sp,
    priceFontSize: TextUnit = 14.sp,
    showVendidos: Boolean = false,
    showDescricao: Boolean = false,
    cardElevation: Dp = 2.dp,
    cardColor: Color = Color.White,
    semEstoqueClickable: Boolean = false
) {
    val semEstoque = produto.estoque <= 0
    Card(
        modifier = modifier.fillMaxWidth().then(
            if (!semEstoque || semEstoqueClickable) Modifier.clickable { onCardClick() }
            else Modifier
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(imageHeight)
                        .clip(RoundedCornerShape(8.dp)).background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    ProdutoImagem(
                        imagem = produto.imagem,
                        contentDescription = produto.nome,
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        contentScale = ContentScale.Fit,
                        serverUrl = PollingService.getServerUrl()
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    produto.nome,
                    fontSize = nameFontSize,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = nameColor,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showDescricao && !produto.descricao.isNullOrBlank()) {
                    Text(
                        produto.descricao ?: "",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    produto.precoFormatado(),
                    fontSize = priceFontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (semEstoque) primaryColor.copy(alpha = 0.3f) else primaryColor
                )
                if (semEstoque) {
                    Text("ESGOTADO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                } else if (produto.estoque <= 5) {
                    Text("Estq: ${produto.estoque.toInt()}", fontSize = 9.sp, color = Color(0xFFFF9800))
                } else if (showVendidos) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        Text("Est:${"%.0f".format(produto.estoque)}", fontSize = 9.sp, color = Color.Gray)
                        Text(" | ", fontSize = 9.sp, color = Color.Gray.copy(alpha = 0.3f))
                        Text("V:$vendidos", fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
            if (quantidadeNoCarrinho > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp),
                    containerColor = Color(0xFF4CAF50)
                ) {
                    Text("${quantidadeNoCarrinho}x", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
