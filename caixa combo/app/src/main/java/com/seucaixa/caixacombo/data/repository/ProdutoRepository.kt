package com.seucaixa.caixacombo.data.repository

import com.seucaixa.caixacombo.data.database.ProdutoDao
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.TipoPreco
import kotlinx.coroutines.flow.Flow

class ProdutoRepository(private val produtoDao: ProdutoDao) {
    
    val allProdutos: Flow<List<Produto>> = produtoDao.getAllProdutos()
    
    suspend fun getAllProdutosList(): List<Produto> = produtoDao.getAllProdutosList()
    
    suspend fun getProdutoById(id: Long): Produto? = produtoDao.getProdutoById(id)
    
    suspend fun getProdutoByCodigoBarras(codigo: String): Produto? = 
        produtoDao.getProdutoByCodigoBarras(codigo)
    
    fun getProdutosByCategoria(categoriaId: Long): Flow<List<Produto>> = 
        produtoDao.getProdutosByCategoria(categoriaId)
    
    fun searchProdutos(query: String): Flow<List<Produto>> = produtoDao.searchProdutos(query)
    
    fun getProdutosEstoqueBaixo(): Flow<List<Produto>> = produtoDao.getProdutosEstoqueBaixo()
    
    suspend fun insert(produto: Produto): Long = produtoDao.insert(produto)
    
    suspend fun insertAll(produtos: List<Produto>) = produtoDao.insertAll(produtos)
    
    suspend fun update(produto: Produto) = produtoDao.update(produto)
    
    suspend fun delete(produto: Produto) = produtoDao.delete(produto)
    
    suspend fun decrementarEstoque(produtoId: Long, quantidade: Double) = 
        produtoDao.decrementarEstoque(produtoId, quantidade)
    
    suspend fun incrementarEstoque(produtoId: Long, quantidade: Double) = 
        produtoDao.incrementarEstoque(produtoId, quantidade)
    
    suspend fun countProdutos(): Int = produtoDao.countProdutos()
    
    fun getProdutosByTipo(tipo: TipoPreco): Flow<List<Produto>> = 
        produtoDao.getProdutosByTipo(tipo)
}
