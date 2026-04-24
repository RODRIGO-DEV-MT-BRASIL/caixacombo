package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "operacoes_caixa")
@TypeConverters(Converters::class)
data class OperacaoCaixa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipo: TipoOperacaoCaixa,
    val nomeOperador: String,
    val dataHora: Long = System.currentTimeMillis(),
    val valor: Double,
    val valorInicial: Double? = null, // Para abertura
    val observacao: String? = null,
    val sincronizado: Boolean = false
)

enum class TipoOperacaoCaixa {
    ABERTURA,
    FECHAMENTO,
    SANGRIA,
    SUPRIMENTO
}

data class FechamentoCaixaDetalhe(
    val operacao: OperacaoCaixa,
    val vendas: List<Venda>,
    val totalPorFormaPagamento: Map<FormaPagamento, Double>,
    val valoresInformados: Map<FormaPagamento, Double>,
    val diferencas: Map<FormaPagamento, Double>
)
