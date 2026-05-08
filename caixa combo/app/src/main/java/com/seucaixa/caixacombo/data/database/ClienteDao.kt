package com.seucaixa.caixacombo.data.database

import androidx.room.*
import com.seucaixa.caixacombo.data.model.Cliente
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {

    @Query("SELECT * FROM clientes WHERE ativo = 1 ORDER BY nome ASC")
    fun getAllClientes(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes ORDER BY nome ASC")
    fun getAllClientesIncludingInactive(): Flow<List<Cliente>>

    @Query("SELECT * FROM clientes WHERE id = :id")
    suspend fun getClienteById(id: Long): Cliente?

    @Query("SELECT * FROM clientes WHERE cpfCnpj = :cpfCnpj LIMIT 1")
    suspend fun getClienteByCpfCnpj(cpfCnpj: String): Cliente?

    @Query("SELECT * FROM clientes WHERE nome LIKE '%' || :query || '%' AND ativo = 1 ORDER BY nome ASC")
    fun searchClientes(query: String): Flow<List<Cliente>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cliente: Cliente): Long

    @Update
    suspend fun update(cliente: Cliente)

    @Delete
    suspend fun delete(cliente: Cliente)

    @Query("DELETE FROM clientes")
    suspend fun deleteAll()
}
