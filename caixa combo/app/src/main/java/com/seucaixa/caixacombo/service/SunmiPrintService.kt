package com.seucaixa.caixacombo.service

import android.content.Context
import android.util.Log
import com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao
import com.seucaixa.caixacombo.data.model.FormaPagamento
import com.seucaixa.caixacombo.data.model.Venda

/**
 * Serviço de impressão usando PrinterSdk (API moderna para V1/V2 compatibilidade)
 * Utiliza SunmiPrintProviderX para comunicação com impressoras Sunmi
 */
class SunmiPrintService(private val context: Context) {

    companion object {
        private const val TAG = "SunmiPrintService"
        // Formas de pagamento que NÃO devem aparecer no fechamento de caixa
        private val FORMAS_EXCLUIR_FECHAMENTO = setOf(FormaPagamento.FIADO, FormaPagamento.BOLETO)
    }

    private val printProvider: SunmiPrintProviderX = SunmiPrintProviderX.getInstance(context)

    fun bind() {
        Log.d(TAG, "Serviço de impressão inicializado (PrinterSdk)")
    }

    fun unbind() {
        Log.d(TAG, "Serviço de impressão finalizado")
        printProvider.destroy()
    }

    fun imprimirAberturaCaixa(nomeOperador: String, dataHora: Long, valorInicial: Double, configuracao: ConfiguracaoImpressao? = null) {
        Log.d(TAG, "Iniciando impressão de abertura (PrinterSdk) - Operador: $nomeOperador, Valor: R$ $valorInicial")

        printProvider.imprimirComprovanteAberturaCaixa(
            nomeOperador = nomeOperador,
            dataHora = dataHora,
            valorInicial = valorInicial,
            configuracao = configuracao
        ) { success ->
            if (success) {
                Log.d(TAG, "Comprovante de abertura impresso com sucesso")
            } else {
                Log.e(TAG, "Falha ao imprimir comprovante de abertura")
            }
        }
    }

    fun imprimirFechamentoCaixa(
        nomeOperador: String,
        dataAbertura: Long,
        dataFechamento: Long,
        valorInicial: Double,
        totalVendas: Double,
        totalSangrias: Double,
        vendas: List<Venda>,
        valoresInformados: Map<FormaPagamento, Double>,
        sangrias: List<com.seucaixa.caixacombo.data.model.OperacaoCaixa> = emptyList(),
        configuracao: ConfiguracaoImpressao? = null,
        valorContado: Double = 0.0
    ) {
        Log.d(TAG, "Iniciando impressão de fechamento (PrinterSdk) - Operador: $nomeOperador, Valor Inicial: R$ $valorInicial, Vendas: R$ $totalVendas, Sangrias: R$ $totalSangrias")

        // Calcular totais por forma de pagamento das vendas reais
        val valoresPorForma = mutableMapOf<String, Double>()
        FormaPagamento.values()
            .filter { it !in FORMAS_EXCLUIR_FECHAMENTO }
            .forEach { forma ->
                val valor = vendas.sumOf { venda ->
                    if (venda.formaPagamento == forma) venda.total else 0.0
                }
                valoresPorForma[forma.name] = valor
            }

        // Calcular produtos vendidos (agrupar por nome)
        val produtosVendidos = mutableMapOf<String, Pair<Int, Double>>()
        vendas.forEach { venda ->
            venda.itens.forEach { item ->
                val (qtd, total) = produtosVendidos[item.produtoNome] ?: Pair(0, 0.0)
                produtosVendidos[item.produtoNome] = Pair(qtd + item.quantidade.toInt(), total + item.total)
            }
        }

        // Converter para lista de Triples
        val produtosLista = produtosVendidos.map { (nome, dados) ->
            Triple(nome, dados.first, dados.second)
        }

        // Converter sangrias para lista de Triples (motivo, valor, saldo)
        val sangriasDetalhadas = sangrias.map { sangria ->
            Triple(sangria.observacao ?: "Sangria", sangria.valor, 0.0)
        }

        printProvider.imprimirComprovanteFechamentoCaixa(
            nomeOperador = nomeOperador,
            dataAbertura = dataAbertura,
            dataFechamento = dataFechamento,
            valorInicial = valorInicial,
            totalVendas = totalVendas,
            totalSangrias = totalSangrias,
            valoresPorForma = valoresPorForma,
            produtosVendidos = produtosLista,
            sangriasDetalhadas = sangriasDetalhadas,
            configuracao = configuracao,
            valorContado = valorContado
        ) { success ->
            if (success) {
                Log.d(TAG, "Comprovante de fechamento impresso com sucesso")
            } else {
                Log.e(TAG, "Falha ao imprimir comprovante de fechamento")
            }
        }
    }

    fun imprimirSangria(nomeOperador: String, valor: Double, motivo: String, dataHora: Long, saldoRestante: Double = 0.0, configuracao: ConfiguracaoImpressao? = null) {
        Log.d(TAG, "Iniciando impressão de sangria (PrinterSdk) - Operador: $nomeOperador, Valor: R$ $valor")

        printProvider.imprimirComprovanteSangria(
            nomeOperador = nomeOperador,
            valor = valor,
            motivo = motivo,
            dataHora = dataHora,
            saldoRestante = saldoRestante,
            configuracao = configuracao
        ) { success ->
            if (success) {
                Log.d(TAG, "Comprovante de sangria impresso com sucesso")
            } else {
                Log.e(TAG, "Falha ao imprimir comprovante de sangria")
            }
        }
    }

    fun imprimirSuprimento(nomeOperador: String, valor: Double, motivo: String, dataHora: Long, saldoAtual: Double, configuracao: ConfiguracaoImpressao? = null) {
        Log.d(TAG, "Iniciando impressão de suprimento (PrinterSdk) - Operador: $nomeOperador, Valor: R$ $valor")

        printProvider.imprimirComprovanteSuprimento(
            nomeOperador = nomeOperador,
            valor = valor,
            motivo = motivo,
            dataHora = dataHora,
            saldoAtual = saldoAtual,
            configuracao = configuracao
        ) { success ->
            if (success) {
                Log.d(TAG, "Comprovante de suprimento impresso com sucesso")
            } else {
                Log.e(TAG, "Falha ao imprimir comprovante de suprimento")
            }
        }
    }

    fun imprimirVenda(venda: com.seucaixa.caixacombo.data.model.Venda, configuracao: ConfiguracaoImpressao? = null, nomeCliente: String? = null) {
        Log.d(TAG, "Iniciando impressão de venda - Nº: ${venda.numero}")

        val itens = venda.itens.map { item ->
            Triple(item.produtoNome, item.quantidade, item.total)
        }

        printProvider.imprimirComprovanteVenda(
            numeroVenda = venda.numero,
            dataHora = venda.dataHora,
            itens = itens,
            subtotal = venda.subtotal,
            desconto = venda.desconto,
            total = venda.total,
            formaPagamento = venda.formaPagamento.name.replace("_", " "),
            valorRecebido = venda.valorRecebido,
            troco = venda.troco,
            configuracao = configuracao,
            nomeCliente = nomeCliente
        ) { success ->
            if (success) {
                Log.d(TAG, "Comprovante de venda impresso com sucesso")
            } else {
                Log.e(TAG, "Falha ao imprimir comprovante de venda")
            }
        }
    }

    fun imprimirFichaProducao(
        item: com.seucaixa.caixacombo.data.model.ItemVenda,
        numeroVenda: String,
        dataHora: Long,
        formaPagamento: String,
        quantidadeUnidade: Int = 1,
        configuracao: ConfiguracaoImpressao? = null
    ) {
        Log.d(TAG, "Iniciando impressão de ficha de produção (PrinterSdk) - Produto: ${item.produtoNome}, Qtd: $quantidadeUnidade")

        printProvider.imprimirFichaProducao(
            item = item,
            numeroVenda = numeroVenda,
            dataHora = dataHora,
            formaPagamento = formaPagamento,
            quantidadeUnidade = quantidadeUnidade,
            configuracao = configuracao
        ) { success ->
            if (success) {
                Log.d(TAG, "Ficha de produção impressa com sucesso")
            } else {
                Log.e(TAG, "Falha ao imprimir ficha de produção")
            }
        }
    }
}
