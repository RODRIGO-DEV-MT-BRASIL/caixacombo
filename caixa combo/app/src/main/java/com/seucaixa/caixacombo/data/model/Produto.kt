package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "produtos",
    indices = [Index(value = ["codigoBarras"], unique = true)]
)
data class Produto(
    @PrimaryKey
    val id: Long = 0,
    
    val nome: String,
    val codigoBarras: String? = null,
    val precoVenda: Double,
    val precoCusto: Double? = null,
    val estoque: Double = 0.0,
    val unidade: String = "UN", // UN, KG, LT, etc
    val categoriaId: Long? = null,
    val imagem: String? = null,
    val ativo: Boolean = true,
    val tipoPreco: TipoPreco = TipoPreco.POR_UNIDADE,
    val codigoPLU: String? = null,
    val descricao: String? = null,
    val dataCriacao: Long = System.currentTimeMillis(),
    val dataAtualizacao: Long = System.currentTimeMillis()
)

enum class TipoPreco {
    POR_UNIDADE,
    POR_PESO,
    POR_VOLUME
}

// Extension para formatação
fun Produto.precoFormatado(): String {
    return "R$ %.2f".format(precoVenda)
}

fun Produto.estoqueFormatado(): String {
    return when (tipoPreco) {
        TipoPreco.POR_UNIDADE -> "%.0f $unidade".format(estoque)
        TipoPreco.POR_PESO -> "%.3f $unidade".format(estoque)
        TipoPreco.POR_VOLUME -> "%.2f $unidade".format(estoque)
    }
}
