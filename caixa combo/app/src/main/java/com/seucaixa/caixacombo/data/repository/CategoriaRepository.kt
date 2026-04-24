package com.seucaixa.caixacombo.data.repository

import com.seucaixa.caixacombo.data.database.CategoriaDao
import com.seucaixa.caixacombo.data.model.Categoria
import kotlinx.coroutines.flow.Flow

class CategoriaRepository(private val categoriaDao: CategoriaDao) {
    
    val allCategorias: Flow<List<Categoria>> = categoriaDao.getAllCategorias()
    
    suspend fun getAllCategoriasList(): List<Categoria> = categoriaDao.getAllCategoriasList()
    
    suspend fun getCategoriaById(id: Long): Categoria? = categoriaDao.getCategoriaById(id)
    
    suspend fun insert(categoria: Categoria): Long = categoriaDao.insert(categoria)
    
    suspend fun insertAll(categorias: List<Categoria>) = categoriaDao.insertAll(categorias)
    
    suspend fun update(categoria: Categoria) = categoriaDao.update(categoria)
    
    suspend fun delete(categoria: Categoria) = categoriaDao.delete(categoria)
    
    suspend fun countCategorias(): Int = categoriaDao.countCategorias()
}
