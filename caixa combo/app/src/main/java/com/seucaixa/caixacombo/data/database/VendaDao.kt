package com.seucaixa.caixacombo.data.database

import androidx.room.*
import com.seucaixa.caixacombo.data.model.StatusVenda
import com.seucaixa.caixacombo.data.model.Venda
import kotlinx.coroutines.flow.Flow

@Dao
interface VendaDao {
    
    @Query("SELECT * FROM vendas ORDER BY dataHora DESC")
    fun getAllVendas(): Flow<List<Venda>>
    
    @Query("SELECT * FROM vendas ORDER BY dataHora DESC LIMIT :limit")
    suspend fun getRecentVendas(limit: Int = 100): List<Venda>
    
    @Query("SELECT * FROM vendas WHERE id = :id")
    suspend fun getVendaById(id: Long): Venda?
    
    @Query("SELECT * FROM vendas WHERE dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC")
    fun getVendasByPeriodo(inicio: Long, fim: Long): Flow<List<Venda>>

    @Query("SELECT * FROM vendas WHERE dataHora BETWEEN :inicio AND :fim ORDER BY dataHora DESC")
    suspend fun getVendasByPeriodoList(inicio: Long, fim: Long): List<Venda>

    @Query("SELECT * FROM vendas WHERE status = :status ORDER BY dataHora DESC")
    fun getVendasByStatus(status: StatusVenda): Flow<List<Venda>>
    
    @Query("SELECT * FROM vendas WHERE sincronizado = 0")
    suspend fun getVendasNaoSincronizadas(): List<Venda>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venda: Venda): Long
    
    @Update
    suspend fun update(venda: Venda)
    
    @Query("UPDATE vendas SET status = :status WHERE id = :vendaId")
    suspend fun updateStatus(vendaId: Long, status: StatusVenda)
    
    @Query("UPDATE vendas SET sincronizado = 1 WHERE id = :vendaId")
    suspend fun marcarSincronizado(vendaId: Long)
    
    @Delete
    suspend fun delete(venda: Venda)
    
    @Query("SELECT SUM(total) FROM vendas WHERE status = 'FINALIZADA' AND dataHora BETWEEN :inicio AND :fim")
    suspend fun getTotalVendasPeriodo(inicio: Long, fim: Long): Double?
    
    @Query("SELECT COUNT(*) FROM vendas WHERE status = 'FINALIZADA' AND dataHora BETWEEN :inicio AND :fim")
    suspend fun countVendasPeriodo(inicio: Long, fim: Long): Int
    
    @Query("SELECT SUM(total) FROM vendas WHERE status = 'FINALIZADA'")
    suspend fun getTotalVendas(): Double?
    
    @Query("SELECT COUNT(*) FROM vendas")
    suspend fun countVendas(): Int
}
