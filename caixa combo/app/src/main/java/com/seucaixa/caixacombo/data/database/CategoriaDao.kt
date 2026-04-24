package com.seucaixa.caixacombo.data.database

import androidx.room.*
import com.seucaixa.caixacombo.data.model.Categoria
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {
    
    @Query("SELECT * FROM categorias WHERE ativa = 1 ORDER BY ordem ASC, nome ASC")
    fun getAllCategorias(): Flow<List<Categoria>>
    
    @Query("SELECT * FROM categorias WHERE ativa = 1 ORDER BY ordem ASC, nome ASC")
    suspend fun getAllCategoriasList(): List<Categoria>
    
    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun getCategoriaById(id: Long): Categoria?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(categoria: Categoria): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categorias: List<Categoria>)
    
    @Update
    suspend fun update(categoria: Categoria)
    
    @Delete
    suspend fun delete(categoria: Categoria)
    
    @Query("SELECT COUNT(*) FROM categorias WHERE ativa = 1")
    suspend fun countCategorias(): Int
}
