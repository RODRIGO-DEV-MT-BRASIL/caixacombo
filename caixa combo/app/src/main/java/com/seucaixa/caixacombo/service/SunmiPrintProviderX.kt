package com.seucaixa.caixacombo.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.sunmi.printerx.PrinterSdk
import com.sunmi.printerx.PrinterSdk.Printer
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Motor de impressão Sunmi usando PrinterSdk (API moderna)
 * Melhor compatibilidade com dispositivos V1/V2/P2B
 * Baseado em SunmiPrinterXSample
 */
class SunmiPrintProviderX(private val context: Context) {

    companion object {
        private const val TAG = "SunmiPrintProviderX"
        
        @Volatile
        private var instance: SunmiPrintProviderX? = null
        private var selectedPrinter: Printer? = null

        fun getInstance(context: Context): SunmiPrintProviderX {
            return instance ?: synchronized(this) {
                instance ?: SunmiPrintProviderX(context.applicationContext).also {
                    instance = it
                }
            }
        }

        fun init(context: Context) {
            getInstance(context)
        }
    }

    private var printer: Printer? = null
    private var isInitialized = false

    init {
        initPrinter()
    }

    /**
     * Inicializa a impressora Sunmi usando PrinterSdk
     * Obtém a impressora padrão do dispositivo e a configura para uso
     */
    private fun initPrinter() {
        try {
            PrinterSdk.getInstance().getPrinter(context, object : PrinterSdk.PrinterListen {
                override fun onDefPrinter(printer: Printer?) {
                    if (selectedPrinter == null) {
                        selectedPrinter = printer
                        Log.d(TAG, "Impressora padrão definida")
                    }
                }

                override fun onPrinters(printers: MutableList<Printer>?) {
                    Log.d(TAG, "Impressoras disponíveis: ${printers?.size ?: 0}")
                    if (selectedPrinter == null && !printers.isNullOrEmpty()) {
                        selectedPrinter = printers[0]
                        Log.d(TAG, "Primeira impressora selecionada")
                    }
                }
            })
            isInitialized = true
            Log.d(TAG, "PrinterSdk inicializado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inicializar PrinterSdk", e)
        }
    }

    private fun getPrinter(): Printer? {
        if (selectedPrinter != null) return selectedPrinter
        
        // Tentar obter novamente
        initPrinter()
        return selectedPrinter
    }

    /**
     * Imprime logo se configurado
     */
    private fun imprimirLogoSeConfigurado(
        configuracao: com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao?,
        usarLogo: Boolean,
        lineApi: com.sunmi.printerx.api.LineApi
    ) {
        Log.d(TAG, "imprimirLogoSeConfigurado - usarLogo: $usarLogo, logoBase64: ${configuracao?.logoBase64?.take(20)}...")
        
        if (!usarLogo || configuracao?.logoBase64.isNullOrEmpty()) {
            Log.d(TAG, "Logo não será impressa - usarLogo: $usarLogo, logoBase64.isNullOrEmpty: ${configuracao?.logoBase64.isNullOrEmpty()}")
            return
        }

        try {
            val bytes = android.util.Base64.decode(configuracao!!.logoBase64, android.util.Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            
            if (bitmap != null) {
                Log.d(TAG, "Bitmap decodificado com sucesso - width: ${bitmap.width}, height: ${bitmap.height}")
                
                // Converter para escala de cinza
                val grayscaleBitmap = convertToGrayscale(bitmap)
                
                // Redimensionar para 120x120 pixels
                val resizedBitmap = Bitmap.createScaledBitmap(grayscaleBitmap, 120, 120, true)
                Log.d(TAG, "Bitmap redimensionado para 120x120")
                
                lineApi.initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                lineApi.printBitmap(resizedBitmap, com.sunmi.printerx.style.BitmapStyle.getStyle())
                lineApi.printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                Log.d(TAG, "Logo impressa com sucesso")
            } else {
                Log.e(TAG, "Bitmap decodificado é null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao imprimir logo", e)
        }
    }

    /**
     * Converte bitmap para escala de cinza
     */
    private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val red = android.graphics.Color.red(color)
            val green = android.graphics.Color.green(color)
            val blue = android.graphics.Color.blue(color)
            val alpha = android.graphics.Color.alpha(color)
            
            // Converter para escala de cinza usando fórmula de luminosidade
            val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
            
            // Se o pixel for muito escuro (quase preto), torna transparente
            if (gray < 50) {
                pixels[i] = android.graphics.Color.TRANSPARENT
            } else {
                pixels[i] = android.graphics.Color.argb(alpha, gray, gray, gray)
            }
        }
        
        grayscaleBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return grayscaleBitmap
    }

    /**
     * Remove fundo preto do bitmap e torna transparente
     */
    private fun removeBlackBackground(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val transparentBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in pixels.indices) {
            val color = pixels[i]
            val red = android.graphics.Color.red(color)
            val green = android.graphics.Color.green(color)
            val blue = android.graphics.Color.blue(color)
            val alpha = android.graphics.Color.alpha(color)
            
            // Se o pixel for preto ou muito escuro, torna transparente
            if (red < 30 && green < 30 && blue < 30) {
                pixels[i] = android.graphics.Color.TRANSPARENT
            } else {
                pixels[i] = color
            }
        }
        
        transparentBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return transparentBitmap
    }

    /**
     * Imprime comprovante de abertura de caixa
     * @param nomeOperador Nome do operador que abriu o caixa
     * @param dataHora Timestamp da abertura do caixa
     * @param valorInicial Valor inicial em dinheiro
     * @param configuracao Configuração de impressão personalizada (opcional)
     * @param callback Callback chamado após conclusão da impressão (true = sucesso, false = falha)
     */
    fun imprimirComprovanteAberturaCaixa(
        nomeOperador: String,
        dataHora: Long,
        valorInicial: Double,
        configuracao: com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao? = null,
        callback: (Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Iniciando impressão de abertura de caixa (PrinterSdk)...")

        try {
            val printer = getPrinter()
            if (printer == null) {
                Log.e(TAG, "Nenhuma impressora disponível")
                callback(false)
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt-BR"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("pt-BR"))

            printer.lineApi()?.run {
                // Cabeçalho
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                
                // Logo se configurado para abertura
                imprimirLogoSeConfigurado(configuracao, configuracao?.logoAbertura == true, this)
                
                // Título ou configuracao.titulo
                val titulo = configuracao?.titulo?.takeIf { it.isNotBlank() } ?: "Rodrigo Dev MT"
                addText(titulo, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // CNPJ
                configuracao?.cnpj?.takeIf { it.isNotBlank() }?.let {
                    addText("CNPJ $it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(24).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                addText("ABERTURA DE CAIXA", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("DATA: ${dateFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Informações
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))
                addText("CAIXA: 01", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("OPERADOR: $nomeOperador", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("HORA: ${timeFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Valor Inicial
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("VALOR INICIAL", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("DINHEIRO: R$ ${String.format("%.2f", valorInicial)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Observação
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("OBSERVACAO:", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("Fundo de troco", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)

                // Status
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("STATUS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("CAIXA ABERTO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Footer
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("Sistema: Rodrigo Dev MT", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("WhatsApp: (45)99104-6021", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)

                autoOut()
            }

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na impressão de abertura", e)
            callback(false)
        }
    }

    /**
     * Imprime comprovante de fechamento de caixa
     * @param nomeOperador Nome do operador que fechou o caixa
     * @param dataAbertura Timestamp da abertura do caixa
     * @param dataFechamento Timestamp do fechamento do caixa
     * @param valorInicial Valor inicial do caixa
     * @param totalVendas Total de vendas no período
     * @param totalSangrias Total de sangrias no período
     * @param valoresPorForma Mapa com valores por forma de pagamento (DINHEIRO, CARTAO_CREDITO, etc)
     * @param produtosVendidos Lista de produtos vendidos (nome, quantidade, total)
     * @param sangriasDetalhadas Lista de sangrias detalhadas (motivo, valor, data)
     * @param configuracao Configuração de impressão personalizada (opcional)
     * @param valorContado Valor contado manualmente no fechamento
     * @param callback Callback chamado após conclusão da impressão (true = sucesso, false = falha)
     */
    fun imprimirComprovanteFechamentoCaixa(
        nomeOperador: String,
        dataAbertura: Long,
        dataFechamento: Long,
        valorInicial: Double,
        totalVendas: Double,
        totalSangrias: Double,
        totalSuprimentos: Double = 0.0,
        valoresPorForma: Map<String, Double>,
        produtosVendidos: List<Triple<String, Int, Double>> = emptyList(),
        sangriasDetalhadas: List<Triple<String, Double, Double>> = emptyList(),
        suprimentosDetalhados: List<Triple<String, Double, Double>> = emptyList(),
        configuracao: com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao? = null,
        valorContado: Double = 0.0,
        callback: (Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Iniciando impressão de fechamento de caixa (PrinterSdk)...")

        try {
            val printer = getPrinter()
            if (printer == null) {
                Log.e(TAG, "Nenhuma impressora disponível")
                callback(false)
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale("pt", "BR"))
            val diaFormat = SimpleDateFormat("EEEE", Locale("pt", "BR"))
            val saldoFinal = valorInicial + totalVendas - totalSangrias + totalSuprimentos

            printer.lineApi()?.run {
                // Cabeçalho com ConfiguracaoImpressao
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                
                // Logo se configurado para fechamento
                imprimirLogoSeConfigurado(configuracao, configuracao?.logoFechamento == true, this)
                
                // Título ou configuracao.titulo
                val titulo = configuracao?.titulo?.takeIf { it.isNotBlank() } ?: "Rodrigo Dev MT"
                addText(titulo, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // CNPJ
                configuracao?.cnpj?.takeIf { it.isNotBlank() }?.let {
                    addText("CNPJ $it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                // IE
                configuracao?.inscricaoEstadual?.takeIf { it.isNotBlank() }?.let {
                    addText("IE:$it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                // Endereço
                configuracao?.endereco?.takeIf { it.isNotBlank() }?.let {
                    addText("END:$it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                // Cidade
                configuracao?.cidade?.takeIf { it.isNotBlank() }?.let {
                    addText("CIDADE:$it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                // CEP
                configuracao?.cep?.takeIf { it.isNotBlank() }?.let {
                    addText("CEP:$it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                // Telefone
                configuracao?.telefone?.takeIf { it.isNotBlank() }?.let {
                    addText("TEL:$it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // Título FECHAMENTO DE CAIXA
                addText("FECHAMENTO DE CAIXA", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(35).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                
                // Data na primeira linha, hora na segunda
                addText("DATA: ${dateFormat.format(dataFechamento)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("HORA: ${timeFormat.format(dataFechamento)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)

                // Informações
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))
                addText("CAIXA: 01", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(35).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("OPERADOR: $nomeOperador", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(35).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("ABERTURA: ${dateFormat.format(dataAbertura)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                val diaSemana = diaFormat.format(dataAbertura).replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("pt", "BR")) else it.toString() }
                addText("DIA: $diaSemana", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("HORA ABERTURA: ${timeFormat.format(dataAbertura)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // Fechamento data e hora separados
                addText("FECHAMENTO: ${dateFormat.format(dataFechamento)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("HORA FECHAMENTO: ${timeFormat.format(dataFechamento)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Produtos vendidos (agora vem primeiro)
                if (produtosVendidos.isNotEmpty()) {
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                    addText("PRODUTOS VENDIDOS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(36).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))

                    produtosVendidos.forEach { (nome, qtd, total) ->
                        addText("$nome", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                        printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                        addText("QTD: $qtd   TOTAL: R$ ${String.format("%.2f", total)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                        printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    }

                    val totalItens = produtosVendidos.sumOf { it.second }
                    addText("ITENS VENDIDOS: $totalItens", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(35).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                }

                // Resumo de vendas por forma de pagamento
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("RESUMO DE VENDAS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))

                valoresPorForma.forEach { (forma, valor) ->
                    val label = when (forma) {
                        "CARTAO_CREDITO" -> "CRÉDITO"
                        "CARTAO_DEBITO" -> "DÉBITO"
                        else -> forma.replace("_", " ")
                    }
                    addText("$label : R$ ${String.format("%.2f", valor)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(35).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }

                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("TOTAL DE VENDAS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("R$ ${String.format("%.2f", totalVendas)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // FATURAMENTO
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("FATURAMENTO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("R$ ${String.format("%.2f", totalVendas)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)

                // Sangrias (seção separada com motivo)
                if (sangriasDetalhadas.isNotEmpty()) {
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                    addText("SANGRIAS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))

                    sangriasDetalhadas.forEachIndexed { index, (motivo, valor, saldo) ->
                        addText("${motivo} : R$ ${String.format("%.2f", valor)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                        printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    }

                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                    addText("TOTAL SANGRIAS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    addText("-R$ ${String.format("%.2f", totalSangrias)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                }

                // Suprimentos (seção separada com motivo)
                if (suprimentosDetalhados.isNotEmpty()) {
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                    addText("SUPRIMENTOS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))

                    suprimentosDetalhados.forEachIndexed { index, (motivo, valor, saldo) ->
                        addText("${motivo} : R$ ${String.format("%.2f", valor)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                        printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    }

                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                    addText("TOTAL SUPRIMENTOS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(32).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    addText("+R$ ${String.format("%.2f", totalSuprimentos)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                }

                // Caixa final
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("CAIXA FINAL", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(36).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                addText("DINHEIRO ESPERADO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)

                addText("R$ ${String.format("%.2f", saldoFinal)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(42).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // VALOR EM CAIXA
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)
                addText("VALOR EM CAIXA", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                val valorEmCaixa = if (valorContado > 0) valorContado else saldoFinal
                addText("R$ ${String.format("%.2f", valorEmCaixa)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(42).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // VALOR SOBRANDO OU FALTANDO
                val diferenca = valorEmCaixa - saldoFinal
                if (diferenca != 0.0) {
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                    if (diferenca > 0) {
                        addText("sobra caixa", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                        printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                        addText("R$ ${String.format("%.2f", diferenca)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(42).enableBold(true))
                    } else {
                        addText("falta", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                        printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                        addText("R$ ${String.format("%.2f", Math.abs(diferenca))}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(42).enableBold(true))
                    }
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                }

                // Footer - usa configuracao ou padrão
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                
                // Linha 1 do rodapé
                configuracao?.rodapeLinha1?.takeIf { it.isNotBlank() }?.let {
                    addText(it, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                } ?: addText("Sistema: Rodrigo Dev MT", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // Linha 2 do rodapé (WhatsApp na segunda linha como solicitado)
                configuracao?.rodapeLinha2?.takeIf { it.isNotBlank() }?.let {
                    addText(it, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                configuracao?.rodapeLinha3?.takeIf { it.isNotBlank() }?.let {
                    addText(it, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                configuracao?.rodapeLinha4?.takeIf { it.isNotBlank() }?.let {
                    addText(it, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                } ?: run {
                    addText("WhatsApp: (45)99104-6021", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)

                autoOut()
            }

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na impressão de fechamento", e)
            callback(false)
        }
    }

    /**
     * Imprime comprovante de sangria
     */
    fun imprimirComprovanteSangria(
        nomeOperador: String,
        valor: Double,
        motivo: String,
        dataHora: Long,
        saldoRestante: Double,
        configuracao: com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao? = null,
        callback: (Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Iniciando impressão de sangria (PrinterSdk)...")

        try {
            val printer = getPrinter()
            if (printer == null) {
                Log.e(TAG, "Nenhuma impressora disponível")
                callback(false)
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt-BR"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("pt-BR"))

            printer.lineApi()?.run {
                // Cabeçalho
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                
                // Logo se configurado para sangria
                imprimirLogoSeConfigurado(configuracao, configuracao?.logoSangria == true, this)
                
                // Título ou configuracao.titulo
                val titulo = configuracao?.titulo?.takeIf { it.isNotBlank() } ?: "Rodrigo Dev MT"
                addText(titulo, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // CNPJ
                configuracao?.cnpj?.takeIf { it.isNotBlank() }?.let {
                    addText("CNPJ $it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(24).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                addText("SANGRIA", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Informações
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))
                addText("CAIXA: 01", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("OPERADOR: $nomeOperador", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("DATA: ${dateFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("HORA: ${timeFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Valor retirado
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("VALOR RETIRADO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("R$ ${String.format("%.2f", valor)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Motivo
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("MOTIVO:", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("$motivo", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Saldo após sangria
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("SALDO APOS SANGRIA", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("R$ ${String.format("%.2f", saldoRestante)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Footer
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("Sistema: Rodrigo Dev MT", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("WhatsApp: (45)99104-6021", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)

                autoOut()
            }

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na impressão de sangria", e)
            callback(false)
        }
    }

    /**
     * Imprime comprovante de suprimento
     */
    fun imprimirComprovanteSuprimento(
        nomeOperador: String,
        valor: Double,
        motivo: String,
        dataHora: Long,
        saldoAtual: Double,
        configuracao: com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao? = null,
        callback: (Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Iniciando impressão de suprimento (PrinterSdk)...")

        try {
            val printer = getPrinter()
            if (printer == null) {
                Log.e(TAG, "Nenhuma impressora disponível")
                callback(false)
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt-BR"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("pt-BR"))

            printer.lineApi()?.run {
                // Cabeçalho
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                
                // Logo se configurado para suprimento
                imprimirLogoSeConfigurado(configuracao, configuracao?.logoSuprimento == true, this)
                
                // Título ou configuracao.titulo
                val titulo = configuracao?.titulo?.takeIf { it.isNotBlank() } ?: "Rodrigo Dev MT"
                addText(titulo, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // CNPJ
                configuracao?.cnpj?.takeIf { it.isNotBlank() }?.let {
                    addText("CNPJ $it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(24).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                addText("SUPRIMENTO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Informações
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))
                addText("CAIXA: 01", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("OPERADOR: $nomeOperador", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("DATA: ${dateFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("HORA: ${timeFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Valor adicionado
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("VALOR ADICIONADO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("R$ ${String.format("%.2f", valor)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Motivo
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("MOTIVO:", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("$motivo", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(20))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Saldo atual
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("SALDO ATUAL", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("R$ ${String.format("%.2f", saldoAtual)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Footer
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("Sistema: Rodrigo Dev MT", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("WhatsApp: (45)99104-6021", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)

                autoOut()
            }

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na impressão de suprimento", e)
            callback(false)
        }
    }

    fun destroy() {
        try {
            PrinterSdk.getInstance().destroy()
            Log.d(TAG, "PrinterSdk destruído")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao destruir PrinterSdk", e)
        }
    }

    /**
     * Imprime ficha de produção com QR Code
     */
    fun imprimirFichaProducao(
        item: com.seucaixa.caixacombo.data.model.ItemVenda,
        numeroVenda: String,
        dataHora: Long,
        formaPagamento: String,
        quantidadeUnidade: Int = 1,
        configuracao: com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao? = null,
        callback: (Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Iniciando impressão de ficha de produção (PrinterSdk)...")

        try {
            val printer = getPrinter()
            if (printer == null) {
                Log.e(TAG, "Nenhuma impressora disponível")
                callback(false)
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt-BR"))
            val timeFormat = SimpleDateFormat("HH:mm", Locale("pt-BR"))

            // Gerar número aleatório para ficha (01-99)
            val numeroFicha = (1..99).random().toString().padStart(2, '0')

            printer.lineApi()?.run {
                // Cabeçalho
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                
                // Logo se configurado para ficha
                imprimirLogoSeConfigurado(configuracao, configuracao?.logoFicha == true, this)
                
                // Título ou configuracao.titulo
                val titulo = configuracao?.titulo?.takeIf { it.isNotBlank() } ?: "Rodrigo Dev MT"
                addText(titulo, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // CNPJ
                configuracao?.cnpj?.takeIf { it.isNotBlank() }?.let {
                    addText("CNPJ $it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(24).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                addText("Sistema de Fichas", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Ticket ID e Ficha
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("TICKET ID", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("$numeroVenda", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("FICHA: $numeroFicha", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("DATA: ${dateFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("HORA: ${timeFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("PAGAMENTO: $formaPagamento", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Produto
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("${item.produtoNome}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("QTD: $quantidadeUnidade", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("TOTAL: R$ ${String.format("%.2f", item.total)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(36).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // QR Code
                val qrData = "TICKET:$numeroVenda|PROD:${item.produtoNome}|QTD:$quantidadeUnidade"
                printQrCode(qrData, com.sunmi.printerx.style.QrStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER).setDot(5).setErrorLevel(com.sunmi.printerx.enums.ErrorLevel.M))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)

                // Instruções
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("APRESENTE O QR CODE", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("PARA RETIRADA DO PRODUTO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Footer
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("Sistema: Rodrigo Dev MT", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("WhatsApp: (45)99104-6021", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)

                autoOut()
            }

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na impressão de ficha", e)
            callback(false)
        }
    }

    /**
     * Imprime comprovante de venda
     */
    fun imprimirComprovanteVenda(
        numeroVenda: String,
        dataHora: Long,
        itens: List<Triple<String, Double, Double>>, // nome, qtd, total
        subtotal: Double,
        desconto: Double,
        total: Double,
        formaPagamento: String,
        valorRecebido: Double,
        troco: Double,
        configuracao: com.seucaixa.caixacombo.data.model.ConfiguracaoImpressao? = null,
        nomeCliente: String? = null,
        callback: (Boolean) -> Unit = {}
    ) {
        Log.d(TAG, "Iniciando impressão de comprovante de venda (PrinterSdk)...")

        try {
            val printer = getPrinter()
            if (printer == null) {
                Log.e(TAG, "Nenhuma impressora disponível")
                callback(false)
                return
            }

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt-BR"))

            printer.lineApi()?.run {
                // Cabeçalho
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                
                // Logo se configurado para venda
                imprimirLogoSeConfigurado(configuracao, configuracao?.logoVenda == true, this)
                
                // Título ou configuracao.titulo
                val titulo = configuracao?.titulo?.takeIf { it.isNotBlank() } ?: "Rodrigo Dev MT"
                addText(titulo, com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                
                // CNPJ
                configuracao?.cnpj?.takeIf { it.isNotBlank() }?.let {
                    addText("CNPJ $it", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(28).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("COMPROVANTE DE VENDA", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Cliente identificado
                if (!nomeCliente.isNullOrBlank()) {
                    addText("CLIENTE: $nomeCliente", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }

                // Informações
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("VENDA", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("$numeroVenda", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("DATA: ${dateFormat.format(dataHora)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Itens
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("LISTA DE ITENS", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))

                itens.forEachIndexed { index, (nome, qtd, total) ->
                    val numeroItem = (index + 1).toString().padStart(2, '0')
                    addText("$numeroItem $nome", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(34).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                    addText("  Qtd: $qtd   Total: R$ ${String.format("%.2f", total)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(25).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }

                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Totais
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))
                addText("Subtotal: R$ ${String.format("%.2f", subtotal)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                if (desconto > 0) {
                    addText("Desconto: -R$ ${String.format("%.2f", desconto)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                addText("Pagamento: $formaPagamento", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                // Valor recebido centralizado com título
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("VALOR RECEBIDO", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("R$ ${String.format("%.2f", valorRecebido)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                if (troco > 0) {
                    initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.LEFT))
                    addText("Troco: R$ ${String.format("%.2f", troco)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                    printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                }
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("TOTAL: R$ ${String.format("%.2f", total)}", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(38).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)

                // Footer
                initLine(com.sunmi.printerx.style.BaseStyle.getStyle().setAlign(com.sunmi.printerx.enums.Align.CENTER))
                addText("Sistema: Rodrigo Dev MT", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 1)
                addText("WhatsApp: (45)99104-6021", com.sunmi.printerx.style.TextStyle.getStyle().setTextSize(30).enableBold(true))
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 2)
                printDividingLine(com.sunmi.printerx.enums.DividingLine.EMPTY, 3)

                autoOut()
            }

            callback(true)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na impressão de venda", e)
            callback(false)
        }
    }
}
