package com.seucaixa.caixacombo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.seucaixa.caixacombo.data.model.Categoria
import com.seucaixa.caixacombo.data.model.Cliente
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao
import com.seucaixa.caixacombo.data.model.Converters
import com.seucaixa.caixacombo.data.model.Empresa
import com.seucaixa.caixacombo.data.model.OperacaoCaixa
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.Usuario
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

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Limpar todas as tabelas para remover dados mockados
        database.execSQL("DELETE FROM produtos")
        database.execSQL("DELETE FROM categorias")
        database.execSQL("DELETE FROM vendas")
        database.execSQL("DELETE FROM operacoes_caixa")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS usuarios (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nome TEXT NOT NULL,
                codigo TEXT NOT NULL,
                cargo TEXT NOT NULL,
                ativo INTEGER NOT NULL,
                dataCriacao INTEGER NOT NULL,
                permVender INTEGER NOT NULL,
                permCaixa INTEGER NOT NULL,
                permProdutos INTEGER NOT NULL,
                permVendas INTEGER NOT NULL,
                permRelatorios INTEGER NOT NULL,
                permConfiguracoes INTEGER NOT NULL,
                permSangria INTEGER NOT NULL,
                permSuprimento INTEGER NOT NULL,
                permFechamento INTEGER NOT NULL,
                permAcessos INTEGER NOT NULL
            )
        """.trimIndent())
        // Inserir usuário admin padrão (código 1234)
        database.execSQL("INSERT INTO usuarios (nome, codigo, cargo, ativo, dataCriacao, permVender, permCaixa, permProdutos, permVendas, permRelatorios, permConfiguracoes, permSangria, permSuprimento, permFechamento, permAcessos) VALUES ('Admin', '1234', 'ADMIN', 1, ${System.currentTimeMillis()}, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE usuarios ADD COLUMN cpf TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE usuarios ADD COLUMN telefone TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE usuarios ADD COLUMN email TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE configuracao_impressao ADD COLUMN logoCheckoutPDV INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                nome TEXT NOT NULL,
                cpfCnpj TEXT NOT NULL DEFAULT '',
                telefone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                endereco TEXT NOT NULL DEFAULT '',
                cidade TEXT NOT NULL DEFAULT '',
                cep TEXT NOT NULL DEFAULT '',
                observacao TEXT NOT NULL DEFAULT '',
                ativo INTEGER NOT NULL DEFAULT 1,
                dataCriacao INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_clientes_cpfCnpj ON clientes(cpfCnpj)")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS empresa (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL DEFAULT 1,
                razaoSocial TEXT NOT NULL DEFAULT '',
                nomeFantasia TEXT NOT NULL DEFAULT '',
                cnpj TEXT NOT NULL DEFAULT '',
                inscricaoEstadual TEXT NOT NULL DEFAULT '',
                telefone TEXT NOT NULL DEFAULT '',
                email TEXT NOT NULL DEFAULT '',
                endereco TEXT NOT NULL DEFAULT '',
                cidade TEXT NOT NULL DEFAULT '',
                cep TEXT NOT NULL DEFAULT '',
                estado TEXT NOT NULL DEFAULT ''
            )
        """.trimIndent())
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE vendas ADD COLUMN stoneAtk TEXT")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Limpar produtos e categorias para re-sincronizar com IDs do servidor
        // (antes autoGenerate gerava IDs locais que não correspondiam aos do servidor)
        database.execSQL("DELETE FROM produtos")
        database.execSQL("DELETE FROM categorias")
    }
}

@Database(
    entities = [Produto::class, Categoria::class, Venda::class, OperacaoCaixa::class, ConfiguracaoImpressao::class, Usuario::class, Cliente::class, Empresa::class],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun produtoDao(): ProdutoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun vendaDao(): VendaDao
    abstract fun operacaoCaixaDao(): OperacaoCaixaDao
    abstract fun configuracaoImpressaoDao(): ConfiguracaoImpressaoDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun clienteDao(): ClienteDao
    abstract fun empresaDao(): EmpresaDao
    
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
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Inserir usuário admin padrão
                        db.execSQL("INSERT INTO usuarios (nome, codigo, cpf, telefone, email, cargo, ativo, dataCriacao, permVender, permCaixa, permProdutos, permVendas, permRelatorios, permConfiguracoes, permSangria, permSuprimento, permFechamento, permAcessos) VALUES ('Admin', '1234', '', '', '', 'ADMIN', 1, ${System.currentTimeMillis()}, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
