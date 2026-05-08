package com.seucaixa.caixacombo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.data.repository.CategoriaRepository
import com.seucaixa.caixacombo.data.repository.ProdutoRepository
import com.seucaixa.caixacombo.data.repository.VendaRepository
import com.seucaixa.caixacombo.service.PollingService
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
    
    // Flag para usar produtos do servidor
    private var _usandoProdutosServidor = false
    
    // Flag para controle de sincronização
    private val _precisaSincronizar = MutableStateFlow(false)
    val precisaSincronizar: StateFlow<Boolean> = _precisaSincronizar.asStateFlow()
    
    // Contador de produtos pendentes de sincronização
    private val _produtosPendentes = MutableStateFlow(0)
    val produtosPendentes: StateFlow<Int> = _produtosPendentes.asStateFlow()
    
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
        // Carregar produtos locais primeiro como fallback
        carregarProdutos()
        carregarCategorias()
        carregarVendasHoje()
        // Verificar se há produtos pendentes de sincronização
        verificarProdutosPendentes()
    }

    /**
     * Atualiza produtos com dados do servidor e salva localmente
     */
    fun atualizarProdutosServidor(produtos: List<Produto>) {
        // Salvar produtos localmente para uso offline
        viewModelScope.launch {
            produtos.forEach { produto ->
                produtoRepository.insert(produto)
            }
            _produtos.value = produtos
            _usandoProdutosServidor = true
            _precisaSincronizar.value = false
            _produtosPendentes.value = 0
        }
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
        
        // Se estiver usando produtos do servidor, filtrar localmente
        if (_usandoProdutosServidor) {
            val produtosAtuais = _produtos.value
            if (categoria == null) {
                // Já tem todos os produtos
                return
            } else {
                // Filtrar por categoria
                val filtrados = produtosAtuais.filter { it.categoriaId == categoria.id }
                _produtos.value = filtrados
            }
            return
        }
        
        // Se não estiver usando produtos do servidor, não fazer nada
        if (!_usandoProdutosServidor) {
            return
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
            // Carregar produtos locais como fallback
            produtoRepository.allProdutos.collect { lista ->
                // Se não estiver usando produtos do servidor, usar locais
                if (!_usandoProdutosServidor) {
                    _produtos.value = lista
                }
            }
        }
    }
    
    fun buscarProdutos(query: String) {
        _busca.value = query
        
        // Se estiver usando produtos do servidor, filtrar localmente
        if (_usandoProdutosServidor) {
            val produtosAtuais = if (_categoriaSelecionada.value != null) {
                // Se tiver categoria selecionada, buscar todos os produtos da categoria
                _produtos.value
            } else {
                // Senão, buscar todos os produtos já carregados
                _produtos.value
            }
            
            if (query.isEmpty()) {
                // Se busca vazia, manter produtos atuais
                return
            } else {
                // Filtrar por nome ou código de barras
                val filtrados = produtosAtuais.filter { produto ->
                    produto.nome.contains(query, ignoreCase = true) ||
                    produto.codigoBarras?.contains(query, ignoreCase = true) == true
                }
                _produtos.value = filtrados
            }
            return
        }
        
        // Se não estiver usando produtos do servidor, não fazer nada
        if (!_usandoProdutosServidor) {
            return
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
    
    fun finalizarVenda(formaPagamento: FormaPagamento, valorRecebido: Double, clienteId: Long? = null): Boolean {
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
                troco = troco,
                clienteId = clienteId
            )
            
            // Salvar venda
            vendaRepository.insert(venda)

            // Atualizar estoque local
            _carrinho.value.forEach { item ->
                produtoRepository.decrementarEstoque(item.produtoId, item.quantidade)
            }
            
            // Atualizar produtos do servidor com novo estoque
            if (_usandoProdutosServidor) {
                val produtosAtualizados = _produtos.value.map { produto ->
                    val itemVendido = _carrinho.value.find { it.produtoId == produto.id }
                    if (itemVendido != null) {
                        val novoEstoque = produto.estoque - itemVendido.quantidade
                        // Enviar atualização para o servidor
                        PollingService.sendEstoqueUpdate(produto.id, novoEstoque)
                        produto.copy(estoque = novoEstoque)
                    } else {
                        produto
                    }
                }
                _produtos.value = produtosAtualizados
            }

            // Guardar referência da última venda para impressão
            _ultimaVenda.value = venda

            // Enviar dados da venda para o servidor
            val vendaJson = org.json.JSONObject().apply {
                put("id", venda.numero)
                put("dataHora", venda.dataHora)
                put("total", venda.total)
                put("formaPagamento", venda.formaPagamento.name)
                put("valorRecebido", venda.valorRecebido)
                put("troco", venda.troco)
                
                val itensArray = org.json.JSONArray()
                venda.itens.forEach { item ->
                    itensArray.put(org.json.JSONObject().apply {
                        put("produtoId", item.produtoId)
                        put("produtoNome", item.produtoNome)
                        put("quantidade", item.quantidade)
                        put("precoUnitario", item.precoUnitario)
                        put("total", item.total)
                    })
                }
                put("itens", itensArray)
            }
            PollingService.sendSaleData(vendaJson)

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
    
    /**
     * Verifica se há produtos locais que precisam ser sincronizados
     */
    private fun verificarProdutosPendentes() {
        viewModelScope.launch {
            val produtosLocais = produtoRepository.getAllProdutosList()
            if (produtosLocais.isNotEmpty() && !_usandoProdutosServidor) {
                _produtosPendentes.value = produtosLocais.size
                _precisaSincronizar.value = true
            } else {
                _produtosPendentes.value = 0
                _precisaSincronizar.value = false
            }
        }
    }
    
    /**
     * Força sincronização dos produtos locais com o servidor
     */
    fun sincronizarProdutos() {
        viewModelScope.launch {
            val produtosLocais = produtoRepository.getAllProdutosList()
            if (produtosLocais.isNotEmpty()) {
                // Enviar produtos para sincronização via WebSocket
                val produtosJson = org.json.JSONArray()
                produtosLocais.forEach { produto ->
                    produtosJson.put(org.json.JSONObject().apply {
                        put("id", produto.id)
                        put("nome", produto.nome)
                        put("descricao", produto.descricao)
                        put("precoVenda", produto.precoVenda)
                        put("estoque", produto.estoque)
                        put("unidade", produto.unidade)
                        put("codigoBarras", produto.codigoBarras)
                        put("categoriaId", produto.categoriaId)
                        put("ativo", produto.ativo)
                    })
                }
                
                PollingService.sendProdutosSync(produtosJson)
                android.util.Log.d("CheckoutViewModel", "Enviados ${produtosLocais.size} produtos para sincronização")
            }
        }
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
