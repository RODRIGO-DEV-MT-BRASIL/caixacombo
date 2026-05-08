package com.seucaixa.caixacombo.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressaoSemLogo
import com.seucaixa.caixacombo.data.model.LogoConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfiguracaoImpressaoDao {
    @Query("SELECT * FROM configuracao_impressao WHERE id = 1")
    fun getConfiguracao(): Flow<ConfiguracaoImpressao?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(configuracao: ConfiguracaoImpressao): Long

    @Update
    suspend fun update(configuracao: ConfiguracaoImpressao)

    @Query("DELETE FROM configuracao_impressao")
    suspend fun deleteAll()

    @Query("SELECT logoBase64 FROM configuracao_impressao WHERE id = 1")
    suspend fun getLogoBase64(): String?

    @Query("SELECT logoHomeScreen, logoAltura, logoLargura, logoEspacamentoAcima, logoEspacamentoAbaixo FROM configuracao_impressao WHERE id = 1")
    fun getLogoConfig(): Flow<LogoConfig?>

    @Query("SELECT id, titulo, cnpj, razaoSocial, inscricaoEstadual, telefone, email, endereco, cidade, cep, rodapeLinha1, rodapeLinha2, rodapeLinha3, rodapeLinha4, logoHomeScreen, logoAbertura, logoFechamento, logoVenda, logoSangria, logoSuprimento, logoFicha, logoCheckoutPDV, logoAltura, logoLargura, logoEspacamentoAcima, logoEspacamentoAbaixo FROM configuracao_impressao WHERE id = 1")
    suspend fun getConfiguracaoSemLogo(): ConfiguracaoImpressaoSemLogo?
}
