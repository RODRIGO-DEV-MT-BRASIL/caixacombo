package com.seucaixa.caixacombo.data.database

import androidx.room.*
import com.seucaixa.caixacombo.data.model.Empresa
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {

    @Query("SELECT * FROM empresa WHERE id = 1")
    fun getEmpresa(): Flow<Empresa?>

    @Query("SELECT * FROM empresa WHERE id = 1")
    suspend fun getEmpresaOnce(): Empresa?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(empresa: Empresa): Long

    @Update
    suspend fun update(empresa: Empresa)

    @Delete
    suspend fun delete(empresa: Empresa)
}
