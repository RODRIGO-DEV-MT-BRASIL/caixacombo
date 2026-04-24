package com.seucaixa.caixacombo.ui.components

import java.text.NumberFormat
import java.util.*

// Função para formatar valor como moeda brasileira
fun formatCurrency(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
    return formatter.format(value)
}

// Função para converter string em valor monetário
fun parseCurrency(value: String): Double {
    if (value.isBlank()) return 0.0
    return value.replace("R$", "")
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")
        .toDoubleOrNull() ?: 0.0
}

// Função para formatar entrada do usuário
fun formatCurrencyInput(input: String): String {
    // Remove tudo exceto números e separadores
    val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
    // Normaliza para ponto decimal
    val normalized = filtered.replace(',', '.')
    // Garante apenas um ponto decimal
    val parts = normalized.split('.')
    val limited = if (parts.size > 2) {
        parts[0] + "." + parts[1]
    } else {
        normalized
    }
    // Limita a 2 casas decimais
    return limited.let { text ->
        val dotIndex = text.indexOf('.')
        if (dotIndex >= 0 && text.length > dotIndex + 3) {
            text.substring(0, dotIndex + 3)
        } else {
            text
        }
    }
}

// Função para formatar para exibição
fun formatDisplay(value: Double): String {
    return String.format("R$ %.2f", value).replace(".", ",")
}

// Função para formatar para banco de dados
fun formatForDatabase(value: String): Double {
    return parseCurrency(value)
}

// Função de extensão para conversão segura de String para Double
fun String.toDoubleSafe(): Double {
    return this.toDoubleOrNull() ?: 0.0
}

// Função de extensão para conversão segura de String para Double com valor padrão
fun String.toDoubleSafe(default: Double): Double {
    return this.toDoubleOrNull() ?: default
}
