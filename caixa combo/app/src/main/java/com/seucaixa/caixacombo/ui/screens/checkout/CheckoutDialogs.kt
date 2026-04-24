package com.seucaixa.caixacombo.ui.screens.checkout

/**
 * Dialogs de finalização de venda
 * Versões para POS (telas grandes) e Mobile (telas pequenas)
 */

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType as ComposeKeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.seucaixa.caixacombo.data.model.FormaPagamento
import com.seucaixa.caixacombo.ui.theme.FontDimensions
import com.seucaixa.caixacombo.ui.theme.CheckoutDimensions
import com.seucaixa.caixacombo.ui.components.CustomKeyboard
import com.seucaixa.caixacombo.ui.components.KeyboardType
import com.seucaixa.caixacombo.ui.components.toDoubleSafe

// ==================== POS DIALOG ====================

@Composable
fun EscolhaFormaPagamentoDialogPOS(
    onFormaSelecionada: (FormaPagamento) -> Unit,
    onCancelar: () -> Unit
) {
    Dialog(onDismissRequest = onCancelar) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                Text(
                    "💳 FORMA DE PAGAMENTO",
                    fontSize = FontDimensions.precoMedio(),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grid de botões de pagamento
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Linha 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.DINHEIRO,
                            icone = Icons.Default.Money,
                            texto = "DINHEIRO",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.DINHEIRO) },
                            modifier = Modifier.weight(1f)
                        )

                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.CARTAO_CREDITO,
                            icone = Icons.Default.CreditCard,
                            texto = "CRÉDITO",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.CARTAO_CREDITO) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Linha 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.CARTAO_DEBITO,
                            icone = Icons.Default.CreditCard,
                            texto = "DÉBITO",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.CARTAO_DEBITO) },
                            modifier = Modifier.weight(1f)
                        )

                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.PIX,
                            icone = Icons.Default.QrCode,
                            texto = "PIX",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.PIX) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botão cancelar
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CheckoutDimensions.botaoFinalizarHeight()),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "CANCELAR",
                        fontSize = FontDimensions.botaoTexto()
                    )
                }
            }
        }
    }
}

@Composable
fun ValorPagamentoDialogPOS(
    total: Double,
    formaPagamento: FormaPagamento,
    valorRecebido: String,
    onValorRecebidoChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    Dialog(onDismissRequest = onCancelar) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título com forma selecionada
                Text(
                    "💰 ${formaPagamento.name.replace("_", " ")}",
                    fontSize = FontDimensions.precoMedio(),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "TOTAL A PAGAR",
                            fontSize = FontDimensions.subtituloProduto()
                        )
                        Text(
                            "R$ %.2f".format(total),
                            fontSize = FontDimensions.precoGrande(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Valor recebido (só para dinheiro)
                if (formaPagamento == FormaPagamento.DINHEIRO) {
                    OutlinedTextField(
                        value = valorRecebido,
                        onValueChange = { newValue ->
                            // Máscara de moeda
                            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                            val normalized = filtered.replace(',', '.')
                            val parts = normalized.split('.')
                            val limited = if (parts.size > 2) {
                                parts[0] + "." + parts[1]
                            } else {
                                normalized
                            }
                            val finalValue = limited.let { text ->
                                val dotIndex = text.indexOf('.')
                                if (dotIndex >= 0 && text.length > dotIndex + 3) {
                                    text.substring(0, dotIndex + 3)
                                } else {
                                    text
                                }
                            }
                            onValorRecebidoChange(finalValue)
                        },
                        label = {
                            Text(
                                "Valor Recebido",
                                fontSize = FontDimensions.subtituloProduto()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = FontDimensions.tituloProduto()
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = ComposeKeyboardType.Decimal)
                    )

                    val troco = (valorRecebido.toDoubleSafe()) - total
                    if (troco > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "TROCO:",
                                    fontSize = FontDimensions.subtituloProduto()
                                )
                                Text(
                                    "R$ %.2f".format(troco),
                                    fontSize = FontDimensions.precoMedio(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelar,
                        modifier = Modifier
                            .weight(1f)
                            .height(CheckoutDimensions.botaoFinalizarHeight()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "VOLTAR",
                            fontSize = FontDimensions.botaoTexto()
                        )
                    }

                    Button(
                        onClick = onConfirmar,
                        modifier = Modifier
                            .weight(2f)
                            .height(CheckoutDimensions.botaoFinalizarHeight()),
                        shape = RoundedCornerShape(12.dp),
                        enabled = formaPagamento != FormaPagamento.DINHEIRO ||
                                (valorRecebido.toDoubleSafe()) >= total
                    ) {
                        Text(
                            "FINALIZAR",
                            fontSize = FontDimensions.botaoTexto(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinalizarVendaDialogPOS(
    total: Double,
    formaPagamento: FormaPagamento,
    valorRecebido: String,
    onFormaPagamentoChange: (FormaPagamento) -> Unit,
    onValorRecebidoChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    Dialog(onDismissRequest = onCancelar) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                // Título
                Text(
                    "💰 FINALIZAR VENDA",
                    fontSize = FontDimensions.precoMedio(),
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "TOTAL A PAGAR",
                            fontSize = FontDimensions.subtituloProduto()
                        )
                        Text(
                            "R$ %.2f".format(total),
                            fontSize = FontDimensions.precoGrande(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Formas de pagamento
                Text(
                    "FORMA DE PAGAMENTO:",
                    fontSize = FontDimensions.subtituloProduto(),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Grid de botões de pagamento
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Linha 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.DINHEIRO,
                            icone = Icons.Default.Money,
                            texto = "DINHEIRO",
                            selecionada = formaPagamento == FormaPagamento.DINHEIRO,
                            onClick = { onFormaPagamentoChange(FormaPagamento.DINHEIRO) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.CARTAO_CREDITO,
                            icone = Icons.Default.CreditCard,
                            texto = "CRÉDITO",
                            selecionada = formaPagamento == FormaPagamento.CARTAO_CREDITO,
                            onClick = { onFormaPagamentoChange(FormaPagamento.CARTAO_CREDITO) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Linha 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.CARTAO_DEBITO,
                            icone = Icons.Default.CreditCard,
                            texto = "DÉBITO",
                            selecionada = formaPagamento == FormaPagamento.CARTAO_DEBITO,
                            onClick = { onFormaPagamentoChange(FormaPagamento.CARTAO_DEBITO) },
                            modifier = Modifier.weight(1f)
                        )
                        
                        FormaPagamentoButtonPOS(
                            forma = FormaPagamento.PIX,
                            icone = Icons.Default.QrCode,
                            texto = "PIX",
                            selecionada = formaPagamento == FormaPagamento.PIX,
                            onClick = { onFormaPagamentoChange(FormaPagamento.PIX) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Valor recebido (só para dinheiro)
                if (formaPagamento == FormaPagamento.DINHEIRO) {
                    OutlinedTextField(
                        value = valorRecebido,
                        onValueChange = { newValue ->
                            // Máscara de moeda
                            val filtered = newValue.filter { it.isDigit() || it == '.' || it == ',' }
                            val normalized = filtered.replace(',', '.')
                            val parts = normalized.split('.')
                            val limited = if (parts.size > 2) {
                                parts[0] + "." + parts[1]
                            } else {
                                normalized
                            }
                            val finalValue = limited.let { text ->
                                val dotIndex = text.indexOf('.')
                                if (dotIndex >= 0 && text.length > dotIndex + 3) {
                                    text.substring(0, dotIndex + 3)
                                } else {
                                    text
                                }
                            }
                            onValorRecebidoChange(finalValue)
                        },
                        label = {
                            Text(
                                "Valor Recebido",
                                fontSize = FontDimensions.subtituloProduto()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = FontDimensions.tituloProduto()
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = ComposeKeyboardType.Decimal)
                    )
                    
                    val troco = (valorRecebido.toDoubleSafe()) - total
                    if (troco > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "TROCO:",
                                    fontSize = FontDimensions.subtituloProduto()
                                )
                                Text(
                                    "R$ %.2f".format(troco),
                                    fontSize = FontDimensions.precoMedio(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelar,
                        modifier = Modifier
                            .weight(1f)
                            .height(CheckoutDimensions.botaoFinalizarHeight()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "CANCELAR",
                            fontSize = FontDimensions.botaoTexto()
                        )
                    }
                    
                    Button(
                        onClick = onConfirmar,
                        modifier = Modifier
                            .weight(2f)
                            .height(CheckoutDimensions.botaoFinalizarHeight()),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "CONFIRMAR",
                            fontSize = FontDimensions.botaoTexto(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormaPagamentoButtonPOS(
    forma: FormaPagamento,
    icone: androidx.compose.ui.graphics.vector.ImageVector,
    texto: String,
    selecionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = if (selecionada) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icone,
                null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                texto,
                fontSize = FontDimensions.subtituloProduto(),
                fontWeight = if (selecionada) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

// ==================== MOBILE DIALOG ====================

@Composable
fun EscolhaFormaPagamentoDialogMobile(
    onFormaSelecionada: (FormaPagamento) -> Unit,
    onCancelar: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancelar) {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título
                Text(
                    "💳 Forma de Pagamento",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Formas de pagamento em grade de 2 colunas
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Linha 1: Dinheiro e Cartão Crédito
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.DINHEIRO,
                            texto = "💵 Dinheiro",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.DINHEIRO) },
                            modifier = Modifier.weight(1f)
                        )
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.CARTAO_CREDITO,
                            texto = "💳 Crédito",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.CARTAO_CREDITO) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Linha 2: Cartão Débito e PIX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.CARTAO_DEBITO,
                            texto = "💳 Débito",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.CARTAO_DEBITO) },
                            modifier = Modifier.weight(1f)
                        )
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.PIX,
                            texto = "📱 PIX",
                            selecionada = false,
                            onClick = { onFormaSelecionada(FormaPagamento.PIX) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Botão cancelar
                OutlinedButton(
                    onClick = onCancelar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun ValorPagamentoDialogMobile(
    total: Double,
    formaPagamento: FormaPagamento,
    valorRecebido: String,
    onValorRecebidoChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancelar) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Título com forma selecionada
                Text(
                    "💰 ${formaPagamento.name.replace("_", " ")}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Total compacto
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total:", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "R$ %.2f".format(total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Valor recebido com teclado customizado (só para dinheiro)
                if (formaPagamento == FormaPagamento.DINHEIRO) {
                    Text(
                        "Valor Recebido:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // Display do valor compacto
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = valorRecebido.ifEmpty { "0,00" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            textAlign = TextAlign.End,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Teclado numérico customizado com máscara automática
                    CustomKeyboard(
                        value = valorRecebido,
                        onValueChange = { newValue ->
                            // Máscara de moeda automática: digita 1500 -> mostra 15.00
                            val digitsOnly = newValue.filter { it.isDigit() }
                            val valorCentavos = digitsOnly.toLongOrNull() ?: 0L
                            val reais = valorCentavos / 100
                            val centavos = valorCentavos % 100
                            val formatado = String.format("%d.%02d", reais, centavos)
                            onValorRecebidoChange(formatado)
                        },
                        onDone = {},
                        keyboardType = KeyboardType.NUMERIC,
                        allowDecimal = false, // Não permitir ponto, máscara faz automaticamente
                        maxLength = 10,
                        showConfirmButton = false, // Ocultar botão confirmar
                        showDisplay = false // Ocultar display interno (já mostrado no Card acima)
                    )

                    // Troco
                    val troco = (valorRecebido.toDoubleSafe()) - total
                    if (troco > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Troco:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "R$ %.2f".format(troco),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Botões de ação compactos
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelar,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("Voltar", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    // Verificar se pode confirmar
                    val podeConfirmar = formaPagamento != FormaPagamento.DINHEIRO ||
                            (valorRecebido.toDoubleSafe()) >= total

                    Button(
                        onClick = onConfirmar,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(6.dp),
                        enabled = podeConfirmar,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (podeConfirmar) "Finalizar" else "Insuficiente",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Mostrar mensagem se valor insuficiente em dinheiro
                if (formaPagamento == FormaPagamento.DINHEIRO && 
                    (valorRecebido.toDoubleSafe()) < total) {
                    Text(
                        text = "Digite o valor recebido",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun FinalizarVendaDialogMobile(
    total: Double,
    formaPagamento: FormaPagamento,
    valorRecebido: String,
    onFormaPagamentoChange: (FormaPagamento) -> Unit,
    onValorRecebidoChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    // Usar Dialog em vez de AlertDialog para ter mais controle
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancelar) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Título
                Text(
                    "Finalizar Venda",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                // Total
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "R$ %.2f".format(total),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Formas de pagamento em grade de 2 colunas
                Text("Forma de Pagamento:", style = MaterialTheme.typography.titleSmall)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Linha 1: Dinheiro e Cartão Crédito
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.DINHEIRO,
                            texto = "💵 Dinheiro",
                            selecionada = formaPagamento == FormaPagamento.DINHEIRO,
                            onClick = { onFormaPagamentoChange(FormaPagamento.DINHEIRO) },
                            modifier = Modifier.weight(1f)
                        )
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.CARTAO_CREDITO,
                            texto = "💳 Crédito",
                            selecionada = formaPagamento == FormaPagamento.CARTAO_CREDITO,
                            onClick = { onFormaPagamentoChange(FormaPagamento.CARTAO_CREDITO) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Linha 2: Cartão Débito e PIX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.CARTAO_DEBITO,
                            texto = "💳 Débito",
                            selecionada = formaPagamento == FormaPagamento.CARTAO_DEBITO,
                            onClick = { onFormaPagamentoChange(FormaPagamento.CARTAO_DEBITO) },
                            modifier = Modifier.weight(1f)
                        )
                        FormaPagamentoChipMobile(
                            forma = FormaPagamento.PIX,
                            texto = "📱 PIX",
                            selecionada = formaPagamento == FormaPagamento.PIX,
                            onClick = { onFormaPagamentoChange(FormaPagamento.PIX) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Valor recebido com teclado customizado (só para dinheiro)
                if (formaPagamento == FormaPagamento.DINHEIRO) {
                    Text(
                        "Valor Recebido:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // Display do valor
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = valorRecebido.ifEmpty { "0,00" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            textAlign = TextAlign.End,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Teclado numérico customizado com máscara automática
                    com.seucaixa.caixacombo.ui.components.CustomKeyboard(
                        value = valorRecebido,
                        onValueChange = { newValue ->
                            // Máscara de moeda automática: digita 1500 -> mostra 15.00
                            val digitsOnly = newValue.filter { it.isDigit() }
                            val valorCentavos = digitsOnly.toLongOrNull() ?: 0L
                            val reais = valorCentavos / 100
                            val centavos = valorCentavos % 100
                            val formatado = String.format("%d.%02d", reais, centavos)
                            onValorRecebidoChange(formatado)
                        },
                        onDone = { },
                        keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                        allowDecimal = false, // Não permitir ponto, máscara faz automaticamente
                        maxLength = 10,
                        showConfirmButton = false, // Ocultar botão confirmar
                        showDisplay = false // Ocultar display interno (já mostrado no Card acima)
                    )

                    // Troco
                    val troco = (valorRecebido.toDoubleSafe()) - total
                    if (troco > 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Troco:",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "R$ %.2f".format(troco),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                // Botões de ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelar,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancelar")
                    }
                    // Verificar se pode confirmar
                    val podeConfirmar = formaPagamento != FormaPagamento.DINHEIRO ||
                            (valorRecebido.toDoubleSafe()) >= total

                    Button(
                        onClick = onConfirmar,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(8.dp),
                        enabled = podeConfirmar
                    ) {
                        Text(if (podeConfirmar) "Confirmar" else "Valor Insuficiente")
                    }

                    // Mostrar mensagem se valor insuficiente em dinheiro
                    if (formaPagamento == FormaPagamento.DINHEIRO && !podeConfirmar) {
                        Text(
                            text = "Digite o valor recebido para habilitar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormaPagamentoChipMobile(
    forma: FormaPagamento,
    texto: String,
    selecionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selecionada,
        onClick = onClick,
        label = { 
            Text(
                texto, 
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            ) 
        },
        leadingIcon = if (selecionada) {
            {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else null,
        modifier = modifier
            .fillMaxWidth(0.9f)
            .height(100.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}
