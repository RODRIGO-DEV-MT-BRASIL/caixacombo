package com.seucaixa.caixacombo

import android.app.Application
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.data.repository.CategoriaRepository
import com.seucaixa.caixacombo.data.repository.ConfiguracaoImpressaoRepository
import com.seucaixa.caixacombo.data.repository.OperacaoCaixaRepository
import com.seucaixa.caixacombo.data.repository.ProdutoRepository
import com.seucaixa.caixacombo.data.repository.VendaRepository
import com.seucaixa.caixacombo.service.SunmiPrintService
import com.seucaixa.caixacombo.service.SunmiPrintProviderX
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CaixaApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }

    val produtoRepository by lazy {
        ProdutoRepository(database.produtoDao())
    }

    val categoriaRepository by lazy {
        CategoriaRepository(database.categoriaDao())
    }

    val vendaRepository by lazy {
        VendaRepository(database.vendaDao())
    }

    val operacaoCaixaRepository by lazy {
        OperacaoCaixaRepository(database.operacaoCaixaDao())
    }

    val configuracaoImpressaoRepository by lazy {
        ConfiguracaoImpressaoRepository(database.configuracaoImpressaoDao())
    }

    val printService by lazy {
        SunmiPrintService(this).also { it.bind() }
    }
    
    private val applicationScope = CoroutineScope(Dispatchers.Default)
    
    override fun onCreate() {
        super.onCreate()

        // Inicializar serviço de impressão PrinterSdk para V1/V2 compatibilidade
        SunmiPrintProviderX.init(this)
    }
}
