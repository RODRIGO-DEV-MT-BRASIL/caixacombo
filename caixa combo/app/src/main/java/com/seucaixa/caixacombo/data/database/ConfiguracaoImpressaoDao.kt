package com.seucaixa.caixacombo.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao
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
}
