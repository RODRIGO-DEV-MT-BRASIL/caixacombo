package com.seucaixa.caixacombo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.data.repository.CategoriaRepository
import com.seucaixa.caixacombo.data.repository.ProdutoRepository
import com.seucaixa.caixacombo.data.repository.VendaRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class CheckoutViewModel(
    private val produtoRepository: ProdutoRepository,
    private val vendaRepository: VendaRepository,
    private val categoriaRepository: CategoriaRepository
) : ViewModel() {
    
    // StateFlow para UI reativa
    private val _produtos = MutableStateFlow<List<Produto>>(emptyList())
    val produtos: StateFlow<List<Produto>> = _produtos.asStateFlow()
    
    private val _carrinho = MutableStateFlow<List<ItemCarrinho>>(emptyList())
    val carrinho: StateFlow<List<ItemCarrinho>> = _carrinho.asStateFlow()
    
    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()
    
    private val _busca = MutableStateFlow("")
    val busca: StateFlow<String> = _busca.asStateFlow()
    
    private val _vendaFinalizada = MutableStateFlow(false)
    val vendaFinalizada: StateFlow<Boolean> = _vendaFinalizada.asStateFlow()

    private val _ultimaVenda = MutableStateFlow<Venda?>(null)
    val ultimaVenda: StateFlow<Venda?> = _ultimaVenda.asStateFlow()

    // Quantidade vendida por produto (hoje)
    private val _vendidosPorProduto = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val vendidosPorProduto: StateFlow<Map<Long, Int>> = _vendidosPorProduto.asStateFlow()

    // Categorias para abas
    private val _categorias = MutableStateFlow<List<Categoria>>(emptyList())
    val categorias: StateFlow<List<Categoria>> = _categorias.asStateFlow()

    private val _categoriaSelecionada = MutableStateFlow<Categoria?>(null)
    val categoriaSelecionada: StateFlow<Categoria?> = _categoriaSelecionada.asStateFlow()

    init {
        carregarProdutos()
        carregarVendasHoje()
        carregarCategorias()
    }

    private fun carregarCategorias() {
        viewModelScope.launch {
            categoriaRepository.allCategorias.collect { lista ->
                _categorias.value = lista
            }
        }
    }

    fun selecionarCategoria(categoria: Categoria?) {
        _categoriaSelecionada.value = categoria
        // Recarregar produtos com filtro
        viewModelScope.launch {
            if (categoria == null) {
                produtoRepository.allProdutos.collect { lista ->
                    _produtos.value = lista
                }
            } else {
                produtoRepository.getProdutosByCategoria(categoria.id).collect { lista ->
                    _produtos.value = lista
                }
            }
        }
    }

    private fun carregarVendasHoje() {
        viewModelScope.launch {
            // Buscar vendas do dia atual
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

            // Calcular quantidade vendida por produto
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
    
    private fun carregarProdutos() {
        viewModelScope.launch {
            produtoRepository.allProdutos.collect { lista ->
                _produtos.value = lista
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
    
    fun adicionarAoCarrinho(produto: Produto, quantidade: Double = 1.0) {
        val itemExistente = _carrinho.value.find { it.produtoId == produto.id }
        
        if (itemExistente != null) {
            // Atualizar quantidade
            val novoCarrinho = _carrinho.value.map { item ->
                if (item.produtoId == produto.id) {
                    val novaQuantidade = item.quantidade + quantidade
                    item.copy(
                        quantidade = novaQuantidade,
                        total = novaQuantidade * item.precoUnitario
                    )
                } else item
            }
            _carrinho.value = novoCarrinho
        } else {
            // Novo item
            val novoItem = ItemCarrinho(
                produtoId = produto.id,
                produtoNome = produto.nome,
                quantidade = quantidade,
                unidade = produto.unidade,
                precoUnitario = produto.precoVenda,
                total = quantidade * produto.precoVenda
            )
            _carrinho.value = _carrinho.value + novoItem
        }
        
        calcularTotal()
    }
    
    fun removerDoCarrinho(produtoId: Long) {
        _carrinho.value = _carrinho.value.filter { it.produtoId != produtoId }
        calcularTotal()
    }
    
    fun atualizarQuantidade(produtoId: Long, novaQuantidade: Double) {
        if (novaQuantidade <= 0) {
            removerDoCarrinho(produtoId)
            return
        }
        
        val novoCarrinho = _carrinho.value.map { item ->
            if (item.produtoId == produtoId) {
                item.copy(
                    quantidade = novaQuantidade,
                    total = novaQuantidade * item.precoUnitario
                )
            } else item
        }
        _carrinho.value = novoCarrinho
        calcularTotal()
    }
    
    fun limparCarrinho() {
        _carrinho.value = emptyList()
        _total.value = 0.0
    }
    
    private fun calcularTotal() {
        _total.value = _carrinho.value.sumOf { it.total }
    }
    
    fun finalizarVenda(formaPagamento: FormaPagamento, valorRecebido: Double): Boolean {
        if (_carrinho.value.isEmpty()) return false
        
        viewModelScope.launch {
            val troco = if (valorRecebido > _total.value) valorRecebido - _total.value else 0.0
            
            val itensVenda = _carrinho.value.map { itemCarrinho ->
                ItemVenda(
                    produtoId = itemCarrinho.produtoId,
                    produtoNome = itemCarrinho.produtoNome,
                    quantidade = itemCarrinho.quantidade,
                    unidade = itemCarrinho.unidade,
                    precoUnitario = itemCarrinho.precoUnitario,
                    desconto = 0.0,
                    total = itemCarrinho.total
                )
            }
            
            val venda = Venda(
                numero = gerarNumeroVenda(),
                itens = itensVenda,
                subtotal = _total.value,
                desconto = 0.0,
                total = _total.value,
                formaPagamento = formaPagamento,
                valorRecebido = valorRecebido,
                troco = troco
            )
            
            // Salvar venda
            vendaRepository.insert(venda)

            // Atualizar estoque
            _carrinho.value.forEach { item ->
                produtoRepository.decrementarEstoque(item.produtoId, item.quantidade)
            }

            // Guardar referência da última venda para impressão
            _ultimaVenda.value = venda

            // Atualizar contador de vendidos
            val vendidosAtual = _vendidosPorProduto.value.toMutableMap()
            itensVenda.forEach { item ->
                val atual = vendidosAtual[item.produtoId] ?: 0
                vendidosAtual[item.produtoId] = atual + item.quantidade.toInt()
            }
            _vendidosPorProduto.value = vendidosAtual

            _vendaFinalizada.value = true
            limparCarrinho()
        }
        
        return true
    }
    
    private fun gerarNumeroVenda(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "V$timestamp$random"
    }
    
    fun resetVendaFinalizada() {
        _vendaFinalizada.value = false
        _ultimaVenda.value = null
    }
    
    // Factory para criar ViewModel com repositórios
    class Factory(
        private val produtoRepository: ProdutoRepository,
        private val vendaRepository: VendaRepository,
        private val categoriaRepository: CategoriaRepository
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return CheckoutViewModel(produtoRepository, vendaRepository, categoriaRepository) as T
        }
    }
}

// Modelo auxiliar para o carrinho
data class ItemCarrinho(
    val produtoId: Long,
    val produtoNome: String,
    val quantidade: Double,
    val unidade: String,
    val precoUnitario: Double,
    val total: Double
)
