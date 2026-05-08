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
    val logoCheckoutPDV: Boolean = false,
    
    // Configurações de tamanho e espaçamento da logo
    val logoAltura: Float = 80f,
    val logoLargura: Float = 300f,
    val logoEspacamentoAcima: Float = 16f,
    val logoEspacamentoAbaixo: Float = 16f
)

// Configurações de logo sem o base64 (evita CursorWindow overflow)
data class LogoConfig(
    val logoHomeScreen: Boolean,
    val logoAltura: Float,
    val logoLargura: Float,
    val logoEspacamentoAcima: Float,
    val logoEspacamentoAbaixo: Float
)

// Configuração completa sem o logoBase64 (evita CursorWindow overflow)
data class ConfiguracaoImpressaoSemLogo(
    val id: Long,
    val titulo: String,
    val cnpj: String,
    val razaoSocial: String,
    val inscricaoEstadual: String,
    val telefone: String,
    val email: String,
    val endereco: String,
    val cidade: String,
    val cep: String,
    val rodapeLinha1: String,
    val rodapeLinha2: String,
    val rodapeLinha3: String,
    val rodapeLinha4: String,
    val logoHomeScreen: Boolean,
    val logoAbertura: Boolean,
    val logoFechamento: Boolean,
    val logoVenda: Boolean,
    val logoSangria: Boolean,
    val logoSuprimento: Boolean,
    val logoFicha: Boolean,
    val logoCheckoutPDV: Boolean,
    val logoAltura: Float,
    val logoLargura: Float,
    val logoEspacamentoAcima: Float,
    val logoEspacamentoAbaixo: Float
) {
    fun toConfiguracaoImpressao(logoBase64: String = ""): ConfiguracaoImpressao = ConfiguracaoImpressao(
        id = id, titulo = titulo, cnpj = cnpj, razaoSocial = razaoSocial,
        inscricaoEstadual = inscricaoEstadual, telefone = telefone, email = email,
        endereco = endereco, cidade = cidade, cep = cep,
        rodapeLinha1 = rodapeLinha1, rodapeLinha2 = rodapeLinha2,
        rodapeLinha3 = rodapeLinha3, rodapeLinha4 = rodapeLinha4,
        logoBase64 = logoBase64,
        logoHomeScreen = logoHomeScreen, logoAbertura = logoAbertura,
        logoFechamento = logoFechamento, logoVenda = logoVenda,
        logoSangria = logoSangria, logoSuprimento = logoSuprimento,
        logoFicha = logoFicha, logoCheckoutPDV = logoCheckoutPDV, logoAltura = logoAltura, logoLargura = logoLargura,
        logoEspacamentoAcima = logoEspacamentoAcima, logoEspacamentoAbaixo = logoEspacamentoAbaixo
    )
}
