package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracao_impressao")
data class ConfiguracaoImpressao(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1,
    
    // Cabeçalho
    val titulo: String = "",
    val cnpj: String = "",
    val razaoSocial: String = "",
    val inscricaoEstadual: String = "",
    val telefone: String = "",
    val email: String = "",
    val endereco: String = "",
    val cidade: String = "",
    val cep: String = "",
    
    // Rodapé
    val rodapeLinha1: String = "Agradecemos sua vinda",
    val rodapeLinha2: String = "Volte sempre",
    val rodapeLinha3: String = "Rodrigo Dev MT",
    val rodapeLinha4: String = "whatsapp(45)99104-6021",
    
    // Logo (armazenado como Base64)
    val logoBase64: String = "",
    
    // Configurações de onde usar logo
    val logoHomeScreen: Boolean = false,
    val logoAbertura: Boolean = false,
    val logoFechamento: Boolean = false,
    val logoVenda: Boolean = false,
    val logoSangria: Boolean = false,
    val logoSuprimento: Boolean = false,
    val logoFicha: Boolean = false,
    
    // Configurações de tamanho e espaçamento da logo
    val logoAltura: Float = 80f,
    val logoLargura: Float = 300f,
    val logoEspacamentoAcima: Float = 16f,
    val logoEspacamentoAbaixo: Float = 16f
)
