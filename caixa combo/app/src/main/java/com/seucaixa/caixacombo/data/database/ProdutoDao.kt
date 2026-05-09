package com.seucaixa.caixacombo.data.database

import androidx.room.*
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.TipoPreco
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdutoDao {
    
    @Query("SELECT * FROM produtos WHERE ativo = 1 ORDER BY nome ASC")
    fun getAllProdutos(): Flow<List<Produto>>
    
    @Query("SELECT * FROM produtos WHERE ativo = 1 ORDER BY nome ASC")
    suspend fun getAllProdutosList(): List<Produto>
    
    @Query("SELECT * FROM produtos WHERE id = :id")
    suspend fun getProdutoById(id: Long): Produto?
    
    @Query("SELECT * FROM produtos WHERE codigoBarras = :codigo LIMIT 1")
    suspend fun getProdutoByCodigoBarras(codigo: String): Produto?
    
    @Query("SELECT * FROM produtos WHERE categoriaId = :categoriaId AND ativo = 1")
    fun getProdutosByCategoria(categoriaId: Long): Flow<List<Produto>>
    
    @Query("SELECT * FROM produtos WHERE nome LIKE '%' || :query || '%' AND ativo = 1")
    fun searchProdutos(query: String): Flow<List<Produto>>
    
    @Query("SELECT * FROM produtos WHERE estoque <= 10 AND ativo = 1")
    fun getProdutosEstoqueBaixo(): Flow<List<Produto>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(produto: Produto): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(produtos: List<Produto>)
    
    @Update
    suspend fun update(produto: Produto)
    
    @Delete
    suspend fun delete(produto: Produto)
    
    @Query("UPDATE produtos SET estoque = estoque - :quantidade WHERE id = :produtoId")
    suspend fun decrementarEstoque(produtoId: Long, quantidade: Double)
    
    @Query("UPDATE produtos SET estoque = estoque + :quantidade WHERE id = :produtoId")
    suspend fun incrementarEstoque(produtoId: Long, quantidade: Double)
    
    @Query("SELECT COUNT(*) FROM produtos WHERE ativo = 1")
    suspend fun countProdutos(): Int

    @Query("DELETE FROM produtos")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM produtos WHERE tipoPreco = :tipo AND ativo = 1")
    fun getProdutosByTipo(tipo: TipoPreco): Flow<List<Produto>>
}
