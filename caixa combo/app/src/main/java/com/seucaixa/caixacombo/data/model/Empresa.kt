package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "empresa")
data class Empresa(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1,
    val razaoSocial: String = "",
    val nomeFantasia: String = "",
    val cnpj: String = "",
    val inscricaoEstadual: String = "",
    val telefone: String = "",
    val email: String = "",
    val endereco: String = "",
    val cidade: String = "",
    val cep: String = "",
    val estado: String = ""
)
