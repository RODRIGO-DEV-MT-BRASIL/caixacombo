package com.seucaixa.caixacombo.data.repository

import com.seucaixa.caixacombo.data.database.ConfiguracaoImpressaoDao
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao
import kotlinx.coroutines.flow.Flow

class ConfiguracaoImpressaoRepository(private val configuracaoDao: ConfiguracaoImpressaoDao) {
    
    fun getConfiguracao(): Flow<ConfiguracaoImpressao?> = configuracaoDao.getConfiguracao()
    
    suspend fun saveConfiguracao(configuracao: ConfiguracaoImpressao) {
        configuracaoDao.insert(configuracao)
    }
    
    suspend fun updateConfiguracao(configuracao: ConfiguracaoImpressao) {
        configuracaoDao.update(configuracao)
    }
}
