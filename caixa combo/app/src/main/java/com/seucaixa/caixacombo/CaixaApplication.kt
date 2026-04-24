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

        // Inserir dados de exemplo (apenas primeira vez)
        applicationScope.launch {
            inserirDadosExemplo()
        }
    }
    
    private suspend fun inserirDadosExemplo() {
        // Verificar se já tem dados
        val count = produtoRepository.countProdutos()
        if (count > 0) return
        
        // Categorias padrão
        val categorias = listOf(
            com.seucaixa.caixacombo.data.model.Categoria(
                nome = "Bebidas",
                cor = "#2196F3",
                ordem = 1
            ),
            com.seucaixa.caixacombo.data.model.Categoria(
                nome = "Alimentos",
                cor = "#FF9800",
                ordem = 2
            ),
            com.seucaixa.caixacombo.data.model.Categoria(
                nome = "Limpeza",
                cor = "#4CAF50",
                ordem = 3
            ),
            com.seucaixa.caixacombo.data.model.Categoria(
                nome = "Padaria",
                cor = "#795548",
                ordem = 4
            )
        )
        
        categorias.forEach { categoriaRepository.insert(it) }
        
        // Produtos de exemplo
        val produtos = listOf(
            com.seucaixa.caixacombo.data.model.Produto(
                nome = "Coca-Cola 2L",
                precoVenda = 8.99,
                estoque = 50.0,
                unidade = "UN",
                categoriaId = 1,
                codigoBarras = "7894900011517"
            ),
            com.seucaixa.caixacombo.data.model.Produto(
                nome = "Pão Francês",
                precoVenda = 0.50,
                estoque = 100.0,
                unidade = "UN",
                categoriaId = 4,
                tipoPreco = com.seucaixa.caixacombo.data.model.TipoPreco.POR_UNIDADE
            ),
            com.seucaixa.caixacombo.data.model.Produto(
                nome = "Arroz 5kg",
                precoVenda = 22.90,
                estoque = 30.0,
                unidade = "UN",
                categoriaId = 2,
                codigoBarras = "7896019200101"
            ),
            com.seucaixa.caixacombo.data.model.Produto(
                nome = "Sabão em Pó",
                precoVenda = 15.99,
                estoque = 25.0,
                unidade = "UN",
                categoriaId = 3
            )
        )
        
        produtos.forEach { produtoRepository.insert(it) }
    }
}
