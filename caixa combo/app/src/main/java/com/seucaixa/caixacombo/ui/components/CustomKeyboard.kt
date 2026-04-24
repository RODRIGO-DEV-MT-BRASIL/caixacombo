package com.seucaixa.caixacombo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Teclado customizado do sistema para evitar teclado Android nativo
 * Usado em dispositivos P2B (modo quiosque)
 */

enum class KeyboardType {
    NUMERIC,    // Apenas números e decimal
    ALPHANUMERIC  // Letras e números
}

@Composable
fun CustomKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    keyboardType: KeyboardType = KeyboardType.NUMERIC,
    modifier: Modifier = Modifier,
    maxLength: Int = 20,
    allowDecimal: Boolean = true,
    showConfirmButton: Boolean = true,
    showDisplay: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        when (keyboardType) {
            KeyboardType.NUMERIC -> NumericKeyboard(
                value = value,
                onValueChange = onValueChange,
                onDone = onDone,
                allowDecimal = allowDecimal,
                maxLength = maxLength,
                showConfirmButton = showConfirmButton,
                showDisplay = showDisplay
            )
            KeyboardType.ALPHANUMERIC -> AlphanumericKeyboard(
                value = value,
                onValueChange = onValueChange,
                onDone = onDone,
                maxLength = maxLength,
                showConfirmButton = showConfirmButton,
                showDisplay = showDisplay
            )
        }
    }
}

@Composable
private fun NumericKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    allowDecimal: Boolean,
    maxLength: Int,
    showConfirmButton: Boolean,
    showDisplay: Boolean
) {
    val buttons = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (allowDecimal) "." else "", "0", "⌫")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Display do valor atual
        if (showDisplay) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = value.ifEmpty { "0" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    textAlign = TextAlign.End,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        // Grid de botões
        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "⌫" -> KeyboardButton(
                            text = "",
                            icon = Icons.Default.Backspace,
                            onClick = {
                                if (value.isNotEmpty()) {
                                    onValueChange(value.dropLast(1))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            isAction = true
                        )
                        "" -> Spacer(modifier = Modifier.weight(1f))
                        else -> KeyboardButton(
                            text = key,
                            onClick = {
                                if (value.length < maxLength) {
                                    // Validação para decimal
                                    if (key == ".") {
                                        if (allowDecimal && !value.contains(".")) {
                                            onValueChange(value + key)
                                        }
                                    } else {
                                        onValueChange(value + key)
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Botão confirmar
        if (showConfirmButton) {
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Check, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirmar", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AlphanumericKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    maxLength: Int,
    showConfirmButton: Boolean,
    showDisplay: Boolean
) {
    var isUpperCase by remember { mutableStateOf(true) }

    val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("Z", "X", "C", "V", "B", "N", "M", "⌫"),
        listOf("123", "SPACE", "ENTER")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Display do valor atual
        if (showDisplay) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value.ifEmpty { "Digite..." },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (value.isEmpty()) 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Backspace, null, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
        }

        // Grid de letras
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "⌫" -> SmallKeyboardButton(
                            icon = Icons.Default.Backspace,
                            onClick = {
                                if (value.isNotEmpty()) {
                                    onValueChange(value.dropLast(1))
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            isAction = true
                        )
                        "123" -> SmallKeyboardButton(
                            text = "123",
                            onClick = { /* Alternar para numérico - não implementado nesta versão */ },
                            modifier = Modifier.weight(1.5f),
                            isAction = true
                        )
                        "SPACE" -> SmallKeyboardButton(
                            text = "ESPAÇO",
                            onClick = {
                                if (value.length < maxLength) {
                                    onValueChange(value + " ")
                                }
                            },
                            modifier = Modifier.weight(4f)
                        )
                        "ENTER" -> SmallKeyboardButton(
                            icon = Icons.Default.Check,
                            onClick = onDone,
                            modifier = Modifier.weight(1.5f),
                            isAction = true,
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                        else -> SmallKeyboardButton(
                            text = if (isUpperCase) key else key.lowercase(),
                            onClick = {
                                if (value.length < maxLength) {
                                    val char = if (isUpperCase) key else key.lowercase()
                                    onValueChange(value + char)
                                    isUpperCase = false
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Botão case
        if (showConfirmButton) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { isUpperCase = !isUpperCase },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (isUpperCase) "ABC" else "abc", fontSize = 14.sp)
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Confirmar", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun KeyboardButton(
    text: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isAction) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isAction) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAction) 
                    MaterialTheme.colorScheme.onPrimaryContainer 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SmallKeyboardButton(
    text: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isAction) containerColor else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isAction && containerColor == MaterialTheme.colorScheme.primary) 
                    MaterialTheme.colorScheme.onPrimary 
                else if (isAction) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isAction) FontWeight.Medium else FontWeight.Normal,
                color = if (isAction && containerColor == MaterialTheme.colorScheme.primary) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Campo de texto com teclado customizado embutido
 */
@Composable
fun OutlinedTextFieldWithCustomKeyboard(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.NUMERIC,
    modifier: Modifier = Modifier,
    maxLength: Int = 20,
    allowDecimal: Boolean = true,
    prefix: String = ""
) {
    var showKeyboard by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Campo de texto (read-only para não abrir teclado Android)
        OutlinedTextField(
            value = value,
            onValueChange = { },
            label = { Text(label) },
            prefix = if (prefix.isNotEmpty()) { { Text(prefix) } } else null,
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showKeyboard = !showKeyboard }) {
                    Icon(
                        if (showKeyboard) Icons.Default.KeyboardArrowDown else Icons.Default.Check,
                        contentDescription = if (showKeyboard) "Fechar" else "Abrir teclado"
                    )
                }
            }
        )

        // Teclado customizado
        if (showKeyboard) {
            CustomKeyboard(
                value = value,
                onValueChange = onValueChange,
                onDone = { showKeyboard = false },
                keyboardType = keyboardType,
                maxLength = maxLength,
                allowDecimal = allowDecimal
            )
        }
    }
}
