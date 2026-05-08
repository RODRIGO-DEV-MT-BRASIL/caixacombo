package com.seucaixa.caixacombo.data.database

import androidx.room.*
import com.seucaixa.caixacombo.data.model.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Query("SELECT * FROM usuarios WHERE ativo = 1 ORDER BY nome ASC")
    fun getAllUsuarios(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios ORDER BY nome ASC")
    fun getAllUsuariosIncludingInactive(): Flow<List<Usuario>>

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getUsuarioById(id: Long): Usuario?

    @Query("SELECT * FROM usuarios WHERE codigo = :codigo AND ativo = 1 LIMIT 1")
    suspend fun getUsuarioByCodigo(codigo: String): Usuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usuario: Usuario): Long

    @Update
    suspend fun update(usuario: Usuario)

    @Delete
    suspend fun delete(usuario: Usuario)

    @Query("DELETE FROM usuarios")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM usuarios WHERE ativo = 1")
    suspend fun countUsuarios(): Int
}
