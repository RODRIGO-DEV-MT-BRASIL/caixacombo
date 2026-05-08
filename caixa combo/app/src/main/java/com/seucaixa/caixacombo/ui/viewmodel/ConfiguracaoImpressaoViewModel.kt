package com.seucaixa.caixacombo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao
import com.seucaixa.caixacombo.data.repository.ConfiguracaoImpressaoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConfiguracaoImpressaoViewModel(
    private val repository: ConfiguracaoImpressaoRepository
) : ViewModel() {

    class Factory(
        private val repository: ConfiguracaoImpressaoRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ConfiguracaoImpressaoViewModel::class.java)) {
                return ConfiguracaoImpressaoViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
    
    private val _configuracao = MutableStateFlow<ConfiguracaoImpressao?>(
        ConfiguracaoImpressao(
            id = 1,
            titulo = "",
            cnpj = "",
            razaoSocial = "",
            inscricaoEstadual = "",
            telefone = "",
            email = "",
            endereco = "",
            cidade = "",
            cep = "",
            rodapeLinha1 = "Agradecemos sua vinda",
            rodapeLinha2 = "Volte sempre",
            rodapeLinha3 = "Rodrigo Dev MT",
            rodapeLinha4 = "whatsapp(45)99104-6021",
            logoBase64 = "",
            logoHomeScreen = false,
            logoAbertura = false,
            logoFechamento = false,
            logoVenda = false,
            logoSangria = false,
            logoSuprimento = false,
            logoFicha = false,
            logoCheckoutPDV = false,
            logoAltura = 80f,
            logoLargura = 300f,
            logoEspacamentoAcima = 16f,
            logoEspacamentoAbaixo = 16f
        )
    )
    val configuracao: StateFlow<ConfiguracaoImpressao?> = _configuracao.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadConfiguracao()
    }
    
    private fun loadConfiguracao() {
        viewModelScope.launch {
            try {
                val semLogo = repository.getConfiguracaoSemLogo()
                if (semLogo != null) {
                    val logo = repository.getLogoBase64() ?: ""
                    _configuracao.value = semLogo.toConfiguracaoImpressao(logo)
                }
            } catch (_: Exception) {}
        }
    }
    
    fun updateTitulo(titulo: String) {
        _configuracao.value = _configuracao.value?.copy(titulo = titulo)
    }
    
    fun updateCnpj(cnpj: String) {
        _configuracao.value = _configuracao.value?.copy(cnpj = cnpj)
    }
    
    fun updateRazaoSocial(razaoSocial: String) {
        _configuracao.value = _configuracao.value?.copy(razaoSocial = razaoSocial)
    }
    
    fun updateInscricaoEstadual(ie: String) {
        _configuracao.value = _configuracao.value?.copy(inscricaoEstadual = ie)
    }
    
    fun updateTelefone(telefone: String) {
        _configuracao.value = _configuracao.value?.copy(telefone = telefone)
    }
    
    fun updateEmail(email: String) {
        _configuracao.value = _configuracao.value?.copy(email = email)
    }
    
    fun updateEndereco(endereco: String) {
        _configuracao.value = _configuracao.value?.copy(endereco = endereco)
    }
    
    fun updateCidade(cidade: String) {
        _configuracao.value = _configuracao.value?.copy(cidade = cidade)
    }
    
    fun updateCep(cep: String) {
        _configuracao.value = _configuracao.value?.copy(cep = cep)
    }
    
    fun updateRodapeLinha1(linha: String) {
        _configuracao.value = _configuracao.value?.copy(rodapeLinha1 = linha)
    }
    
    fun updateRodapeLinha2(linha: String) {
        _configuracao.value = _configuracao.value?.copy(rodapeLinha2 = linha)
    }
    
    fun updateRodapeLinha3(linha: String) {
        _configuracao.value = _configuracao.value?.copy(rodapeLinha3 = linha)
    }
    
    fun updateRodapeLinha4(linha: String) {
        _configuracao.value = _configuracao.value?.copy(rodapeLinha4 = linha)
    }
    
    fun updateLogoBase64(base64: String) {
        _configuracao.value = _configuracao.value?.copy(logoBase64 = base64)
    }
    
    fun updateLogoHomeScreen(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoHomeScreen = enabled)
    }
    
    fun updateLogoAbertura(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoAbertura = enabled)
    }
    
    fun updateLogoFechamento(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoFechamento = enabled)
    }
    
    fun updateLogoVenda(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoVenda = enabled)
    }
    
    fun updateLogoSangria(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoSangria = enabled)
    }
    
    fun updateLogoSuprimento(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoSuprimento = enabled)
    }
    
    fun updateLogoFicha(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoFicha = enabled)
    }
    
    fun updateLogoCheckoutPDV(enabled: Boolean) {
        _configuracao.value = _configuracao.value?.copy(logoCheckoutPDV = enabled)
    }
    
    fun updateLogoAltura(altura: Float) {
        _configuracao.value = _configuracao.value?.copy(logoAltura = altura)
    }
    
    fun updateLogoLargura(largura: Float) {
        _configuracao.value = _configuracao.value?.copy(logoLargura = largura)
    }
    
    fun updateLogoEspacamentoAcima(espacamento: Float) {
        _configuracao.value = _configuracao.value?.copy(logoEspacamentoAcima = espacamento)
    }
    
    fun updateLogoEspacamentoAbaixo(espacamento: Float) {
        _configuracao.value = _configuracao.value?.copy(logoEspacamentoAbaixo = espacamento)
    }
    
    fun salvarConfiguracao(onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val config = _configuracao.value ?: return@launch
                repository.saveConfiguracao(config)
                _saveSuccess.value = true
                _isLoading.value = false
                _errorMessage.value = null
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                val errorMsg = "Erro ao salvar configuração: ${e.message}"
                _errorMessage.value = errorMsg
                onError(errorMsg)
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
