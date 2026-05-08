package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "clientes",
    indices = [Index(value = ["cpfCnpj"], unique = true)]
)
data class Cliente(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nome: String,
    val cpfCnpj: String = "",
    val telefone: String = "",
    val email: String = "",
    val endereco: String = "",
    val cidade: String = "",
    val cep: String = "",
    val observacao: String = "",
    val ativo: Boolean = true,
    val dataCriacao: Long = System.currentTimeMillis()
)
