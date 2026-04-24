package com.seucaixa.caixacombo.data.repository

import com.seucaixa.caixacombo.data.dao.OperacaoCaixaDao
import com.seucaixa.caixacombo.data.model.OperacaoCaixa
import com.seucaixa.caixacombo.data.model.TipoOperacaoCaixa
import kotlinx.coroutines.flow.Flow

class OperacaoCaixaRepository(private val operacaoDao: OperacaoCaixaDao) {

    fun getAllOperacoes(): Flow<List<OperacaoCaixa>> = operacaoDao.getAll()

    fun getUltimaAbertura(): Flow<OperacaoCaixa?> = operacaoDao.getUltimaAbertura()

    suspend fun getUltimaAberturaSuspend(): OperacaoCaixa? {
        return operacaoDao.getUltimaPorTipo(TipoOperacaoCaixa.ABERTURA)
    }

    fun getOperacoesPorTipo(tipo: TipoOperacaoCaixa): Flow<List<OperacaoCaixa>> =
        operacaoDao.getPorTipo(tipo)

    suspend fun isCaixaAberto(): Boolean = operacaoDao.isCaixaAberto()

    fun getOperacoesPorPeriodo(inicio: Long, fim: Long): Flow<List<OperacaoCaixa>> =
        operacaoDao.getPorPeriodo(inicio, fim)

    suspend fun getOperacoesPorPeriodoList(inicio: Long, fim: Long): List<OperacaoCaixa> =
        operacaoDao.getPorPeriodoList(inicio, fim)

    suspend fun insert(operacao: OperacaoCaixa): Long {
        return operacaoDao.insert(operacao)
    }

    suspend fun update(operacao: OperacaoCaixa) {
        operacaoDao.update(operacao)
    }

    suspend fun delete(operacao: OperacaoCaixa) {
        operacaoDao.delete(operacao)
    }
}
