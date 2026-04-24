package com.seucaixa.caixacombo.data.dao

import androidx.room.*
import com.seucaixa.caixacombo.data.model.OperacaoCaixa
import com.seucaixa.caixacombo.data.model.TipoOperacaoCaixa
import kotlinx.coroutines.flow.Flow

@Dao
interface OperacaoCaixaDao {
    @Query("SELECT * FROM operacoes_caixa ORDER BY dataHora DESC")
    fun getAll(): Flow<List<OperacaoCaixa>>

    @Query("SELECT * FROM operacoes_caixa WHERE tipo = :tipo ORDER BY dataHora DESC LIMIT 1")
    suspend fun getUltimaPorTipo(tipo: TipoOperacaoCaixa): OperacaoCaixa?

    @Query("SELECT * FROM operacoes_caixa WHERE tipo = :tipo ORDER BY dataHora DESC")
    fun getPorTipo(tipo: TipoOperacaoCaixa): Flow<List<OperacaoCaixa>>

    @Query("SELECT * FROM operacoes_caixa WHERE tipo = 'ABERTURA' ORDER BY dataHora DESC LIMIT 1")
    fun getUltimaAbertura(): Flow<OperacaoCaixa?>

    @Query("SELECT * FROM operacoes_caixa WHERE dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC")
    fun getPorPeriodo(inicio: Long, fim: Long): Flow<List<OperacaoCaixa>>

    @Query("SELECT * FROM operacoes_caixa WHERE dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC")
    suspend fun getPorPeriodoList(inicio: Long, fim: Long): List<OperacaoCaixa>

    @Insert
    suspend fun insert(operacao: OperacaoCaixa): Long

    @Update
    suspend fun update(operacao: OperacaoCaixa)

    @Delete
    suspend fun delete(operacao: OperacaoCaixa)

    @Query("SELECT EXISTS(SELECT 1 FROM operacoes_caixa WHERE tipo = 'ABERTURA' AND id > (SELECT COALESCE(MAX(id), 0) FROM operacoes_caixa WHERE tipo = 'FECHAMENTO'))")
    suspend fun isCaixaAberto(): Boolean
}
