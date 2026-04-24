package com.seucaixa.caixacombo.data.repository

import com.seucaixa.caixacombo.data.database.VendaDao
import com.seucaixa.caixacombo.data.model.StatusVenda
import com.seucaixa.caixacombo.data.model.Venda
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class VendaRepository(private val vendaDao: VendaDao) {
    
    val allVendas: Flow<List<Venda>> = vendaDao.getAllVendas()
    
    suspend fun getRecentVendas(limit: Int = 100): List<Venda> = 
        vendaDao.getRecentVendas(limit)
    
    suspend fun getVendaById(id: Long): Venda? = vendaDao.getVendaById(id)
    
    fun getVendasByPeriodo(inicio: Long, fim: Long): Flow<List<Venda>> =
        vendaDao.getVendasByPeriodo(inicio, fim)

    suspend fun getVendasByPeriodoList(inicio: Long, fim: Long): List<Venda> =
        vendaDao.getVendasByPeriodo(inicio, fim).first()
    
    fun getVendasByStatus(status: StatusVenda): Flow<List<Venda>> = 
        vendaDao.getVendasByStatus(status)
    
    suspend fun getVendasNaoSincronizadas(): List<Venda> = 
        vendaDao.getVendasNaoSincronizadas()
    
    suspend fun insert(venda: Venda): Long = vendaDao.insert(venda)
    
    suspend fun update(venda: Venda) = vendaDao.update(venda)
    
    suspend fun updateStatus(vendaId: Long, status: StatusVenda) = 
        vendaDao.updateStatus(vendaId, status)
    
    suspend fun marcarSincronizado(vendaId: Long) = 
        vendaDao.marcarSincronizado(vendaId)
    
    suspend fun delete(venda: Venda) = vendaDao.delete(venda)
    
    suspend fun getTotalVendasPeriodo(inicio: Long, fim: Long): Double = 
        vendaDao.getTotalVendasPeriodo(inicio, fim) ?: 0.0
    
    suspend fun countVendasPeriodo(inicio: Long, fim: Long): Int = 
        vendaDao.countVendasPeriodo(inicio, fim)
    
    suspend fun getTotalVendas(): Double = vendaDao.getTotalVendas() ?: 0.0
    
    suspend fun countVendas(): Int = vendaDao.countVendas()
}
