package com.seucaixa.caixacombo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seucaixa.caixacombo.BuildConfig
import com.seucaixa.caixacombo.data.model.*
import com.seucaixa.caixacombo.data.repository.ConfiguracaoImpressaoRepository
import com.seucaixa.caixacombo.data.repository.OperacaoCaixaRepository
import com.seucaixa.caixacombo.data.repository.VendaRepository
import com.seucaixa.caixacombo.service.SunmiPrintService
import com.seucaixa.caixacombo.service.WebSocketService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class CaixaViewModel(
    private val operacaoRepository: OperacaoCaixaRepository,
    private val vendaRepository: VendaRepository,
    private val printService: SunmiPrintService,
    private val configuracaoRepository: ConfiguracaoImpressaoRepository
) : ViewModel() {

    private val _caixaAberto = MutableStateFlow(false)
    val caixaAberto: StateFlow<Boolean> = _caixaAberto.asStateFlow()

    private val _ultimaAbertura = MutableStateFlow<OperacaoCaixa?>(null)
    val ultimaAbertura: StateFlow<OperacaoCaixa?> = _ultimaAbertura.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Saldos calculados
    private val _saldoAtual = MutableStateFlow(0.0)
    val saldoAtual: StateFlow<Double> = _saldoAtual.asStateFlow()

    private val _totalVendas = MutableStateFlow(0.0)
    val totalVendas: StateFlow<Double> = _totalVendas.asStateFlow()

    private val _totalSangrias = MutableStateFlow(0.0)
    val totalSangrias: StateFlow<Double> = _totalSangrias.asStateFlow()

    private val _totalSuprimentos = MutableStateFlow(0.0)
    val totalSuprimentos: StateFlow<Double> = _totalSuprimentos.asStateFlow()

    // Vendas por forma de pagamento
    private val _vendasDinheiro = MutableStateFlow(0.0)
    val vendasDinheiro: StateFlow<Double> = _vendasDinheiro.asStateFlow()

    private val _vendasCredito = MutableStateFlow(0.0)
    val vendasCredito: StateFlow<Double> = _vendasCredito.asStateFlow()

    // Produtos do servidor
    private val _produtosServidor = MutableStateFlow<List<Produto>>(emptyList())
    val produtosServidor: StateFlow<List<Produto>> = _produtosServidor.asStateFlow()

    private val _vendasDebito = MutableStateFlow(0.0)
    val vendasDebito: StateFlow<Double> = _vendasDebito.asStateFlow()

    private val _vendasPix = MutableStateFlow(0.0)
    val vendasPix: StateFlow<Double> = _vendasPix.asStateFlow()

    private val _caixaAbertoDiaAnterior = MutableStateFlow(false)
    val caixaAbertoDiaAnterior: StateFlow<Boolean> = _caixaAbertoDiaAnterior.asStateFlow()

    private val _aberturas = MutableStateFlow<List<OperacaoCaixa>>(emptyList())
    val aberturas: StateFlow<List<OperacaoCaixa>> = _aberturas.asStateFlow()

    private val _fechamentos = MutableStateFlow<List<OperacaoCaixa>>(emptyList())
    val fechamentos: StateFlow<List<OperacaoCaixa>> = _fechamentos.asStateFlow()

    private val _sangrias = MutableStateFlow<List<OperacaoCaixa>>(emptyList())
    val sangrias: StateFlow<List<OperacaoCaixa>> = _sangrias.asStateFlow()

    private val _suprimentos = MutableStateFlow<List<OperacaoCaixa>>(emptyList())
    val suprimentos: StateFlow<List<OperacaoCaixa>> = _suprimentos.asStateFlow()

    // Listas de vendas por forma de pagamento
    private val _vendasDinheiroList = MutableStateFlow<List<com.seucaixa.caixacombo.data.model.Venda>>(emptyList())
    val vendasDinheiroList: StateFlow<List<com.seucaixa.caixacombo.data.model.Venda>> = _vendasDinheiroList.asStateFlow()

    private val _vendasCreditoList = MutableStateFlow<List<com.seucaixa.caixacombo.data.model.Venda>>(emptyList())
    val vendasCreditoList: StateFlow<List<com.seucaixa.caixacombo.data.model.Venda>> = _vendasCreditoList.asStateFlow()

    private val _vendasDebitoList = MutableStateFlow<List<com.seucaixa.caixacombo.data.model.Venda>>(emptyList())
    val vendasDebitoList: StateFlow<List<com.seucaixa.caixacombo.data.model.Venda>> = _vendasDebitoList.asStateFlow()

    private val _vendasPixList = MutableStateFlow<List<com.seucaixa.caixacombo.data.model.Venda>>(emptyList())
    val vendasPixList: StateFlow<List<com.seucaixa.caixacombo.data.model.Venda>> = _vendasPixList.asStateFlow()

    init {
        verificarStatusCaixa()
        carregarUltimaAbertura()
        calcularSaldos()
        verificarCaixaDiaAnterior()
        carregarRegistros()
    }

    private fun carregarRegistros() {
        viewModelScope.launch {
            // Carregar aberturas
            operacaoRepository.getOperacoesPorTipo(TipoOperacaoCaixa.ABERTURA).collect { ops ->
                _aberturas.value = ops
            }
        }
        viewModelScope.launch {
            // Carregar fechamentos
            operacaoRepository.getOperacoesPorTipo(TipoOperacaoCaixa.FECHAMENTO).collect { ops ->
                _fechamentos.value = ops
            }
        }
        viewModelScope.launch {
            // Carregar sangrias
            operacaoRepository.getOperacoesPorTipo(TipoOperacaoCaixa.SANGRIA).collect { ops ->
                _sangrias.value = ops
            }
        }
        viewModelScope.launch {
            // Carregar suprimentos
            operacaoRepository.getOperacoesPorTipo(TipoOperacaoCaixa.SUPRIMENTO).collect { ops ->
                _suprimentos.value = ops
            }
        }
        viewModelScope.launch {
            // Carregar vendas por forma de pagamento (filtrando pelo período do caixa atual)
            val abertura = operacaoRepository.getUltimaAberturaSuspend()
            if (abertura != null) {
                val dataAbertura = abertura.dataHora
                vendaRepository.getVendasByPeriodo(dataAbertura, Long.MAX_VALUE).collect { vendas ->
                    _vendasDinheiroList.value = vendas.filter { it.formaPagamento == FormaPagamento.DINHEIRO }
                    _vendasCreditoList.value = vendas.filter { it.formaPagamento == FormaPagamento.CARTAO_CREDITO }
                    _vendasDebitoList.value = vendas.filter { it.formaPagamento == FormaPagamento.CARTAO_DEBITO }
                    _vendasPixList.value = vendas.filter { it.formaPagamento == FormaPagamento.PIX }
                }
            } else {
                // Se não houver caixa aberto, lista vazia
                _vendasDinheiroList.value = emptyList()
                _vendasCreditoList.value = emptyList()
                _vendasDebitoList.value = emptyList()
                _vendasPixList.value = emptyList()
            }
        }
    }

    private fun calcularSaldos() {
        viewModelScope.launch {
            val abertura = operacaoRepository.getUltimaAberturaSuspend()
            if (abertura != null) {
                val dataAbertura = abertura.dataHora

                // COMBINE: Escutar operações, vendas e ticker para atualização em tempo real
                kotlinx.coroutines.flow.combine(
                    operacaoRepository.getOperacoesPorPeriodo(dataAbertura, Long.MAX_VALUE),
                    vendaRepository.getVendasByPeriodo(dataAbertura, Long.MAX_VALUE),
                    kotlinx.coroutines.flow.flow {
                        while (true) {
                            emit(Unit)
                            kotlinx.coroutines.delay(1000) // Atualiza a cada segundo
                        }
                    }
                ) { operacoes, vendas, _ ->
                    val sangrias = operacoes.filter { it.tipo == TipoOperacaoCaixa.SANGRIA }
                    val suprimentos = operacoes.filter { it.tipo == TipoOperacaoCaixa.SUPRIMENTO }
                    val vendasDinheiro = vendas.filter { it.formaPagamento == FormaPagamento.DINHEIRO }
                    val vendasCredito = vendas.filter { it.formaPagamento == FormaPagamento.CARTAO_CREDITO }
                    val vendasDebito = vendas.filter { it.formaPagamento == FormaPagamento.CARTAO_DEBITO }
                    val vendasPix = vendas.filter { it.formaPagamento == FormaPagamento.PIX }

                    val totalSangria = sangrias.sumOf { it.valor }
                    val totalSuprimento = suprimentos.sumOf { it.valor }
                    val totalVenda = vendas.sumOf { it.total }

                    val vendasDinheiroList = vendasDinheiro
                    val vendasCreditoList = vendasCredito
                    val vendasDebitoList = vendasDebito
                    val vendasPixList = vendasPix

                    _totalVendas.value = totalVenda
                    _totalSangrias.value = totalSangria
                    _totalSuprimentos.value = totalSuprimento
                    _vendasDinheiro.value = vendasDinheiro.sumOf { it.total }
                    _vendasCredito.value = vendasCredito.sumOf { it.total }
                    _vendasDebito.value = vendasDebito.sumOf { it.total }
                    _vendasPix.value = vendasPix.sumOf { it.total }
                    _saldoAtual.value = (abertura.valorInicial ?: 0.0) + totalVenda - totalSangria + totalSuprimento
                }.collect()
            } else {
                // Resetar valores quando não há caixa aberto
                _totalVendas.value = 0.0
                _totalSangrias.value = 0.0
                _totalSuprimentos.value = 0.0
                _vendasDinheiro.value = 0.0
                _vendasCredito.value = 0.0
                _vendasDebito.value = 0.0
                _vendasPix.value = 0.0
                _saldoAtual.value = 0.0
            }
        }
    }

    private fun verificarStatusCaixa() {
        viewModelScope.launch {
            _caixaAberto.value = operacaoRepository.isCaixaAberto()
        }
    }

    private fun carregarUltimaAbertura() {
        viewModelScope.launch {
            operacaoRepository.getUltimaAbertura().collect { abertura ->
                _ultimaAbertura.value = abertura
            }
        }
    }

    private fun verificarCaixaDiaAnterior() {
        viewModelScope.launch {
            val abertura = operacaoRepository.getUltimaAberturaSuspend()
            if (abertura != null && _caixaAberto.value) {
                val dataAbertura = abertura.dataHora
                val dataAtual = System.currentTimeMillis()
                val diasDiferenca = ((dataAtual - dataAbertura) / (1000 * 60 * 60 * 24)).toInt()
                
                _caixaAbertoDiaAnterior.value = diasDiferenca >= 1
            } else {
                _caixaAbertoDiaAnterior.value = false
            }
        }
    }

    fun abrirCaixa(
        nomeOperador: String,
        valorInicial: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        android.util.Log.d("CaixaViewModel", "🔓 abrirCaixa chamado: nome=$nomeOperador, valor=$valorInicial")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("CaixaViewModel", "🔓 Iniciando abertura de caixa...")

                val operacao = OperacaoCaixa(
                    tipo = TipoOperacaoCaixa.ABERTURA,
                    nomeOperador = nomeOperador,
                    valor = valorInicial,
                    valorInicial = valorInicial
                )

                operacaoRepository.insert(operacao)
                
                // Enviar operação para o servidor/dashboard
                android.util.Log.d("CaixaViewModel", "🔓 Enviando abertura para servidor...")
                WebSocketService.sendOperacaoCaixa(
                    tipo = "abertura",
                    valor = valorInicial,
                    nomeOperador = nomeOperador
                )
                android.util.Log.d("CaixaViewModel", "🔓 Abertura enviada com sucesso!")

                // Imprimir comprovante
                val configuracaoAbertura = configuracaoRepository.getConfiguracao().firstOrNull() ?: ConfiguracaoImpressao()
                printService.imprimirAberturaCaixa(
                    nomeOperador = nomeOperador,
                    dataHora = System.currentTimeMillis(),
                    valorInicial = valorInicial,
                    configuracao = configuracaoAbertura
                )

                _caixaAberto.value = true
                _isLoading.value = false

                // Recalcular saldos e recarregar registros após abertura
                calcularSaldos()
                carregarRegistros()

                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError("Erro ao abrir caixa: ${e.message}")
            }
        }
    }

    fun fecharCaixa(
        nomeOperador: String,
        valoresInformados: Map<FormaPagamento, Double>,
        valorContado: Double = 0.0,
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val abertura = operacaoRepository.getUltimaAberturaSuspend()
                if (abertura == null) {
                    _isLoading.value = false
                    onError("Nenhuma abertura de caixa encontrada!")
                    return@launch
                }

                val dataAbertura = abertura.dataHora
                val dataFechamento = System.currentTimeMillis()

                val vendas = vendaRepository.getVendasByPeriodoList(dataAbertura, dataFechamento)
                val totalVendas = vendas.sumOf { it.total }

                val sangrias = operacaoRepository.getOperacoesPorPeriodoList(dataAbertura, dataFechamento)
                    .filter { it.tipo == TipoOperacaoCaixa.SANGRIA }

                val operacao = OperacaoCaixa(
                    tipo = TipoOperacaoCaixa.FECHAMENTO,
                    nomeOperador = nomeOperador,
                    valor = totalVendas
                )

                operacaoRepository.insert(operacao)

                // Enviar operação para o servidor/dashboard
                android.util.Log.d("CaixaViewModel", "🔒 Enviando fechamento para servidor...")
                WebSocketService.sendOperacaoCaixa(
                    tipo = "fechamento",
                    valor = totalVendas,
                    nomeOperador = nomeOperador
                )
                android.util.Log.d("CaixaViewModel", "🔒 Fechamento enviado com sucesso!")

                val valorInicial = abertura.valorInicial ?: 0.0
                val totalSangrias = _totalSangrias.value

                val configuracao = configuracaoRepository.getConfiguracao().firstOrNull() ?: ConfiguracaoImpressao()
                printService.imprimirFechamentoCaixa(
                    nomeOperador = nomeOperador,
                    dataAbertura = dataAbertura,
                    dataFechamento = dataFechamento,
                    valorInicial = valorInicial,
                    totalVendas = totalVendas,
                    totalSangrias = totalSangrias,
                    vendas = vendas,
                    valoresInformados = valoresInformados,
                    sangrias = sangrias,
                    configuracao = configuracao,
                    valorContado = valorContado
                )

                _caixaAberto.value = false
                _isLoading.value = false
                
                // Zerar todos os totais para começar novo período
                _saldoAtual.value = 0.0
                _totalVendas.value = 0.0
                _totalSangrias.value = 0.0
                _vendasDinheiro.value = 0.0
                _vendasCredito.value = 0.0
                _vendasDebito.value = 0.0
                _vendasPix.value = 0.0
                _ultimaAbertura.value = null
                
                // Zerar listas
                _vendasDinheiroList.value = emptyList()
                _vendasCreditoList.value = emptyList()
                _vendasDebitoList.value = emptyList()
                _vendasPixList.value = emptyList()
                
                // Recalcular saldos para garantir que o flow pare de usar dados antigos
                calcularSaldos()
                
                onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError("Erro ao fechar caixa: ${e.message}")
            }
        }
    }

    fun registrarSangria(nomeOperador: String, valor: Double, motivo: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

            // Verificar se há saldo suficiente
            if (_saldoAtual.value < valor) {
                _isLoading.value = false
                onError("Saldo insuficiente!")
                return@launch
            }

            val operacao = OperacaoCaixa(
                tipo = TipoOperacaoCaixa.SANGRIA,
                nomeOperador = nomeOperador,
                valor = valor,
                observacao = motivo
            )

            operacaoRepository.insert(operacao)

            // Enviar operação para o servidor/dashboard
            android.util.Log.d("CaixaViewModel", "💸 Enviando sangria para servidor...")
            WebSocketService.sendOperacaoCaixa(
                tipo = "sangria",
                valor = valor,
                nomeOperador = nomeOperador,
                observacao = motivo
            )
            android.util.Log.d("CaixaViewModel", "💸 Sangria enviada com sucesso!")

            // Calcular novo saldo antes de imprimir
            val novoSaldo = _saldoAtual.value - valor

            // Atualizar saldo local
            _totalSangrias.value += valor
            _saldoAtual.value = novoSaldo

            // Imprimir comprovante com o saldo restante
            val configuracaoSangria = configuracaoRepository.getConfiguracao().firstOrNull() ?: ConfiguracaoImpressao()
            printService.imprimirSangria(
                nomeOperador = nomeOperador,
                valor = valor,
                motivo = motivo,
                dataHora = System.currentTimeMillis(),
                saldoRestante = novoSaldo,
                configuracao = configuracaoSangria
            )

            _isLoading.value = false
            onSuccess()
            } catch (e: Exception) {
                _isLoading.value = false
                onError("Erro ao registrar sangria: ${e.message}")
            }
        }
    }

    fun registrarSuprimento(
        nomeOperador: String,
        valor: Double,
        motivo: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {

            val operacao = OperacaoCaixa(
                tipo = TipoOperacaoCaixa.SUPRIMENTO,
                nomeOperador = nomeOperador,
                valor = valor,
                dataHora = System.currentTimeMillis(),
                observacao = motivo
            )

            operacaoRepository.insert(operacao)
            
            // Enviar operação para o servidor/dashboard
            WebSocketService.sendOperacaoCaixa(
                tipo = "suprimento",
                valor = valor,
                nomeOperador = nomeOperador,
                observacao = motivo
            )

            // Calcular novo saldo antes de imprimir
            val novoSaldo = _saldoAtual.value + valor
            _saldoAtual.value = novoSaldo

            // Imprimir comprovante com o novo saldo
            val configuracaoSuprimento = configuracaoRepository.getConfiguracao().firstOrNull() ?: ConfiguracaoImpressao()
            printService.imprimirSuprimento(
                nomeOperador = nomeOperador,
                valor = valor,
                motivo = motivo,
                dataHora = System.currentTimeMillis(),
                saldoAtual = novoSaldo,
                configuracao = configuracaoSuprimento
            )

            // Recalcular saldos após suprimento para garantir atualização correta
            calcularSaldos()

            _isLoading.value = false
            onSuccess()
        }
    }

    /**
     * Atualiza produtos recebidos do servidor
     */
    fun atualizarProdutos(produtos: List<Produto>) {
        _produtosServidor.value = produtos
    }

    // Factory
    class Factory(
        private val operacaoRepository: OperacaoCaixaRepository,
        private val vendaRepository: VendaRepository,
        private val printService: SunmiPrintService,
        private val configuracaoRepository: ConfiguracaoImpressaoRepository
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return CaixaViewModel(operacaoRepository, vendaRepository, printService, configuracaoRepository) as T
        }
    }
}
