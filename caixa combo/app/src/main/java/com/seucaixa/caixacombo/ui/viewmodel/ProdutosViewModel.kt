package com.seucaixa.caixacombo.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seucaixa.caixacombo.ui.components.toDoubleSafe
import com.seucaixa.caixacombo.data.model.Categoria
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.TipoPreco
import com.seucaixa.caixacombo.data.repository.CategoriaRepository
import com.seucaixa.caixacombo.data.repository.ProdutoRepository
import com.seucaixa.caixacombo.data.repository.VendaRepository
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Calendar
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProdutosViewModel(
    private val produtoRepository: ProdutoRepository,
    private val categoriaRepository: CategoriaRepository,
    private val vendaRepository: VendaRepository,
    private val context: Context
) : ViewModel() {
    
    private val _produtos = MutableStateFlow<List<Produto>>(emptyList())
    val produtos: StateFlow<List<Produto>> = _produtos.asStateFlow()
    
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()
    
    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca.asStateFlow()
    
    private val _categoriaSelecionada = MutableStateFlow<Long?>(null)
    val categoriaSelecionada: StateFlow<Long?> = _categoriaSelecionada.asStateFlow()
    
    private val _produtoEditando = MutableStateFlow<Produto?>(null)
    val produtoEditando: StateFlow<Produto?> = _produtoEditando.asStateFlow()

    // Quantidade vendida por produto (hoje)
    private val _vendidosPorProduto = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val vendidosPorProduto: StateFlow<Map<Long, Int>> = _vendidosPorProduto.asStateFlow()

    init {
        carregarDados()
        carregarVendasHoje()
    }

    private fun carregarVendasHoje() {
        viewModelScope.launch {
            val inicioDia = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val fimDia = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val vendas = vendaRepository.getVendasByPeriodoList(inicioDia, fimDia)

            val vendidos = mutableMapOf<Long, Int>()
            vendas.forEach { venda ->
                venda.itens.forEach { item ->
                    val atual = vendidos[item.produtoId] ?: 0
                    vendidos[item.produtoId] = atual + item.quantidade.toInt()
                }
            }
            _vendidosPorProduto.value = vendidos
        }
    }

    // Gerar código de barras EAN-13 automático
    fun gerarCodigoBarrasAutomatico(): String {
        // Gera um EAN-13 válido baseado no timestamp
        val prefixo = "789" // Código do Brasil
        val random = (10000000..99999999).random()
        val base = "$prefixo$random"

        // Calcular dígito verificador
        var soma = 0
        for (i in base.indices) {
            val digito = base[i].digitToInt()
            soma += if (i % 2 == 0) digito else digito * 3
        }
        val digitoVerificador = (10 - (soma % 10)) % 10

        return "$base$digitoVerificador"
    }
    
    private fun carregarDados() {
        viewModelScope.launch {
            produtoRepository.allProdutos.collect { lista ->
                _produtos.value = lista
            }
        }
        
        viewModelScope.launch {
            categoriaRepository.allCategorias.collect { lista ->
                _categorias.value = lista
            }
        }
    }
    
    fun buscarProdutos(query: String) {
        _busca.value = query
        viewModelScope.launch {
            if (query.isEmpty()) {
                produtoRepository.allProdutos.collect { lista ->
                    _produtos.value = lista
                }
            } else {
                produtoRepository.searchProdutos(query).collect { lista ->
                    _produtos.value = lista
                }
            }
        }
    }
    
    fun filtrarPorCategoria(categoriaId: Long?) {
        _categoriaSelecionada.value = categoriaId
        viewModelScope.launch {
            if (categoriaId == null) {
                produtoRepository.allProdutos.collect { lista ->
                    _produtos.value = lista
                }
            } else {
                produtoRepository.getProdutosByCategoria(categoriaId).collect { lista ->
                    _produtos.value = lista
                }
            }
        }
    }
    
    fun salvarProduto(produto: Produto) {
        viewModelScope.launch {
            if (produto.id == 0L) {
                produtoRepository.insert(produto)
            } else {
                produtoRepository.update(produto)
            }
            _produtoEditando.value = null
        }
    }
    
    fun editarProduto(produto: Produto) {
        _produtoEditando.value = produto
    }
    
    fun novoProduto() {
        _produtoEditando.value = Produto(
            nome = "",
            precoVenda = 0.0,
            estoque = 0.0,
            unidade = "UN"
        )
    }
    
    fun cancelarEdicao() {
        _produtoEditando.value = null
    }
    
    fun excluirProduto(produto: Produto) {
        viewModelScope.launch {
            produtoRepository.delete(produto)
        }
    }
    
    fun atualizarEstoque(produtoId: Long, novoEstoque: Double) {
        viewModelScope.launch {
            val produto = produtoRepository.getProdutoById(produtoId)
            produto?.let {
                val atualizado = it.copy(estoque = novoEstoque)
                produtoRepository.update(atualizado)
            }
        }
    }
    
    fun salvarCategoria(categoria: Categoria) {
        viewModelScope.launch {
            categoriaRepository.insert(categoria)
        }
    }
    
    fun excluirCategoria(categoria: Categoria) {
        viewModelScope.launch {
            categoriaRepository.delete(categoria)
        }
    }

    fun exportarProdutosCSV(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val produtos = produtoRepository.getAllProdutosList()
                val csv = StringBuilder()

                // Cabeçalho com todos os campos
                csv.append("ID,Nome,CodigoBarras,PrecoVenda,PrecoCusto,Estoque,Unidade,CategoriaID,Imagem,Ativo,TipoPreco,CodigoPLU,Descricao\n")
                produtos.forEach { produto ->
                    val linha = "${produto.id}," +
                            "${produto.nome}," +
                            "${produto.codigoBarras ?: ""}," +
                            "${produto.precoVenda}," +
                            "${produto.precoCusto ?: ""}," +
                            "${produto.estoque}," +
                            "${produto.unidade}," +
                            "${produto.categoriaId ?: ""}," +
                            "${produto.imagem ?: ""}," +
                            "${produto.ativo}," +
                            "${produto.tipoPreco.name}," +
                            "${produto.codigoPLU ?: ""}," +
                            "${produto.descricao ?: ""}\n"
                    csv.append(linha)
                }

                val outputStream = context.contentResolver.openOutputStream(uri)
                val writer = OutputStreamWriter(outputStream)
                writer.write(csv.toString())
                writer.close()
                outputStream?.close()
            } catch (e: Exception) {
                // Erro ao exportar CSV
            }
        }
    }

    fun importarProdutosCSV(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    return@launch
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val linhas = reader.readLines()
                reader.close()
                inputStream.close()

                var produtosImportados = 0

                // Pular a linha de cabeçalho
                linhas.drop(1).forEachIndexed { index, linha ->
                    val colunas = linha.split(",")
                    if (colunas.size >= 13) {
                        val produto = Produto(
                            id = colunas[0].toLongOrNull() ?: 0L,
                            nome = colunas[1],
                            codigoBarras = colunas[2].ifBlank { null },
                            precoVenda = colunas[3].toDoubleSafe(),
                            precoCusto = colunas[4].toDoubleSafe(0.0),
                            estoque = colunas[5].toDoubleSafe(),
                            unidade = colunas[6],
                            categoriaId = colunas[7].toLongOrNull(),
                            imagem = colunas[8].ifBlank { null },
                            ativo = colunas[9].toBoolean(),
                            tipoPreco = try {
                                TipoPreco.valueOf(colunas[10].uppercase())
                            } catch (e: Exception) {
                                TipoPreco.POR_UNIDADE
                            },
                            codigoPLU = colunas[11].ifBlank { null },
                            descricao = colunas[12].ifBlank { null }
                        )
                        if (produto.id == 0L) {
                            produtoRepository.insert(produto)
                        } else {
                            produtoRepository.update(produto)
                        }
                        produtosImportados++
                    }
                }
            } catch (e: Exception) {
                // Erro ao importar CSV
            }
        }
    }

    // Factory para criar ViewModel com repositórios
    class Factory(
        private val produtoRepository: ProdutoRepository,
        private val categoriaRepository: CategoriaRepository,
        private val vendaRepository: VendaRepository,
        private val context: Context
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ProdutosViewModel(produtoRepository, categoriaRepository, vendaRepository, context) as T
        }
    }
}
