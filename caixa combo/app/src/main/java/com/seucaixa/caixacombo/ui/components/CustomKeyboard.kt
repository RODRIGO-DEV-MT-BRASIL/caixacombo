package com.seucaixa.caixacombo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KeyboardType {
    NUMERIC,
    ALPHANUMERIC
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
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
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
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (showDisplay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = value.ifEmpty { "0" },
                    textAlign = TextAlign.End,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f) else primaryColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(if (allowDecimal) "." else "00", "0", "⌫")
        )

        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { key ->
                    when (key) {
                        "⌫" -> ModernKey(
                            icon = Icons.Default.Backspace,
                            onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
                            modifier = Modifier.weight(1f),
                            isAction = true,
                            isDelete = true
                        )
                        else -> ModernKey(
                            text = key,
                            onClick = {
                                if (value.length < maxLength) {
                                    if (key == ".") {
                                        if (allowDecimal && !value.contains(".")) onValueChange(value + key)
                                    } else if (key == "00") {
                                        if (value.length + 2 <= maxLength) onValueChange(value + key)
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

        if (showConfirmButton) {
            Spacer(modifier = Modifier.height(2.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Confirmar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
    var showNumbers by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (showDisplay) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = value.ifEmpty { "Digite..." },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Backspace, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }

        if (showNumbers) {
            val numRows = listOf(
                listOf("1", "2", "3", "4", "5", "6"),
                listOf("7", "8", "9", "0", ".", ","),
                listOf("ABC", "⎵", "⌫")
            )
            numRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        when (key) {
                            "⌫" -> ModernSmallKey(
                                icon = Icons.Default.Backspace,
                                onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
                                modifier = Modifier.weight(1.5f),
                                isAction = true,
                                isDelete = true
                            )
                            "ABC" -> ModernSmallKey(
                                text = "ABC",
                                onClick = { showNumbers = false },
                                modifier = Modifier.weight(1.2f),
                                isAction = true
                            )
                            "⎵" -> ModernSmallKey(
                                text = "⎵",
                                onClick = { if (value.length < maxLength) onValueChange(value + " ") },
                                modifier = Modifier.weight(3f)
                            )
                            else -> ModernSmallKey(
                                text = key,
                                onClick = { if (value.length < maxLength) onValueChange(value + key) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        } else {
            val letterRows = listOf(
                listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                listOf("⇧", "Z", "X", "C", "V", "B", "N", "M", "⌫"),
                listOf("123", "⎵", "✓")
            )
            letterRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        when (key) {
                            "⌫" -> ModernSmallKey(
                                icon = Icons.Default.Backspace,
                                onClick = { if (value.isNotEmpty()) onValueChange(value.dropLast(1)) },
                                modifier = Modifier.weight(1.3f),
                                isAction = true,
                                isDelete = true
                            )
                            "⇧" -> ModernSmallKey(
                                icon = Icons.Default.ArrowUpward,
                                onClick = { isUpperCase = !isUpperCase },
                                modifier = Modifier.weight(1.3f),
                                isAction = true,
                                isActive = isUpperCase
                            )
                            "123" -> ModernSmallKey(
                                text = "123",
                                onClick = { showNumbers = true },
                                modifier = Modifier.weight(1.5f),
                                isAction = true
                            )
                            "⎵" -> ModernSmallKey(
                                text = "⎵",
                                onClick = { if (value.length < maxLength) onValueChange(value + " ") },
                                modifier = Modifier.weight(4f)
                            )
                            "✓" -> ModernSmallKey(
                                icon = Icons.Default.Check,
                                onClick = onDone,
                                modifier = Modifier.weight(1.5f),
                                isAction = true,
                                isActive = true,
                                activeColor = primaryColor
                            )
                            else -> ModernSmallKey(
                                text = if (isUpperCase) key else key.lowercase(),
                                onClick = {
                                    if (value.length < maxLength) {
                                        val char = if (isUpperCase) key else key.lowercase()
                                        onValueChange(value + char)
                                        if (isUpperCase) isUpperCase = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        if (showConfirmButton) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(
                    onClick = { isUpperCase = !isUpperCase },
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isUpperCase) "ABC" else "abc", fontSize = 13.sp)
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(2f).height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    Text("Confirmar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ModernKey(
    text: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    isDelete: Boolean = false
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var pressed by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            pressed -> primaryColor.copy(alpha = 0.2f)
            isDelete -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            isAction -> primaryColor.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(100)
    )
    val contentColor = when {
        isDelete -> MaterialTheme.colorScheme.error
        isAction -> primaryColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(20.dp), tint = contentColor)
        } else {
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ModernSmallKey(
    text: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    isDelete: Boolean = false,
    isActive: Boolean = false,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    var pressed by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = when {
            pressed -> primaryColor.copy(alpha = 0.15f)
            isDelete -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            isActive -> activeColor.copy(alpha = 0.15f)
            isAction -> primaryColor.copy(alpha = 0.06f)
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(100)
    )
    val contentColor = when {
        isDelete -> MaterialTheme.colorScheme.error
        isActive -> activeColor
        isAction -> primaryColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(icon, null, modifier = Modifier.size(16.dp), tint = contentColor)
        } else {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isAction) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

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
