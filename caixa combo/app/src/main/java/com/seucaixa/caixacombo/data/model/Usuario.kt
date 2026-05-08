package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val codigo: String = "", // Código de acesso (PIN)
    val cpf: String = "",
    val telefone: String = "",
    val email: String = "",
    val cargo: CargoUsuario = CargoUsuario.FUNCIONARIO,
    val ativo: Boolean = true,
    val dataCriacao: Long = System.currentTimeMillis(),

    // Permissões
    val permVender: Boolean = true,
    val permCaixa: Boolean = false,
    val permProdutos: Boolean = false,
    val permVendas: Boolean = false,
    val permRelatorios: Boolean = false,
    val permConfiguracoes: Boolean = false,
    val permSangria: Boolean = false,
    val permSuprimento: Boolean = false,
    val permFechamento: Boolean = false,
    val permAcessos: Boolean = false
)

enum class CargoUsuario {
    FUNCIONARIO,
    GERENTE,
    ADMIN
}
