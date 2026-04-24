package com.seucaixa.caixacombo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seucaixa.caixacombo.data.model.Categoria
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao
import com.seucaixa.caixacombo.data.model.Converters
import com.seucaixa.caixacombo.data.model.OperacaoCaixa
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.Venda
import com.seucaixa.caixacombo.data.dao.OperacaoCaixaDao

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Criar tabela configuracao_impressao se não existir
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS configuracao_impressao (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tipoImpressora TEXT NOT NULL,
                larguraPapel INTEGER NOT NULL,
                tamanhoFonte INTEGER NOT NULL,
                imprimirCabecalho INTEGER NOT NULL,
                imprimirRodape INTEGER NOT NULL,
                numeroCopias INTEGER NOT NULL
            )
        """)
    }
}

@Database(
    entities = [Produto::class, Categoria::class, Venda::class, OperacaoCaixa::class, ConfiguracaoImpressao::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun produtoDao(): ProdutoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun vendaDao(): VendaDao
    abstract fun operacaoCaixaDao(): OperacaoCaixaDao
    abstract fun configuracaoImpressaoDao(): ConfiguracaoImpressaoDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "caixa_combo_database"
                )
                .addMigrations(MIGRATION_2_3)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
