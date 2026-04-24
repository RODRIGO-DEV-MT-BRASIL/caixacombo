package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val nome: String,
    val cor: String? = null, // Hex color ex: #FF5722
    val icone: String? = null,
    val ordem: Int = 0,
    val ativa: Boolean = true
)

// Categorias padrão
object CategoriasPadrao {
    val TODAS = Categoria(
        id = -1,
        nome = "Todos os Produtos",
        ordem = -1
    )
    
    val PADRAO = Categoria(
        id = 0,
        nome = "Categoria Padrão",
        ordem = 0
    )
    
    val FAVORITOS = Categoria(
        id = -2,
        nome = "Favoritos",
        ordem = 1
    )
}
