package com.seucaixa.caixacombo.service

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log

/**
 * Serviço de integração com Stone via Deeplink.
 * Permite enviar solicitações de pagamento e receber o resultado.
 *
 * Fluxo:
 * 1. App cria Intent com URI payment-app://pay?amount=...&installment_count=...&type=...
 * 2. Stone processa o pagamento no terminal POS
 * 3. Stone retorna resultado via onActivityResult com payment-app://pay-response
 */
object StoneDeeplinkService {

    private const val TAG = "StoneDeeplink"
    const val REQUEST_CODE_PAYMENT = 1001

    // Tipos de pagamento Stone
    object PaymentType {
        const val CREDIT = "CREDIT"
        const val DEBIT = "DEBIT"
        const val PIX = "PIX"
    }

    /**
     * Resultado do pagamento Stone
     */
    data class PaymentResult(
        val success: Boolean,
        val code: Int,
        val amount: Long = 0,
        val type: String = "",
        val installmentCount: Int = 0,
        val brand: String = "",
        val entryMode: String = "",
        val authorizationCode: String = "",
        val reason: String = "",
        val orderId: String = ""
    )

    /**
     * Mapeia FormaPagamento do app para tipo do Stone
     */
    fun mapFormaPagamentoToStone(forma: com.seucaixa.caixacombo.data.model.FormaPagamento): String? {
        return when (forma) {
            com.seucaixa.caixacombo.data.model.FormaPagamento.CARTAO_CREDITO -> PaymentType.CREDIT
            com.seucaixa.caixacombo.data.model.FormaPagamento.CARTAO_DEBITO -> PaymentType.DEBIT
            com.seucaixa.caixacombo.data.model.FormaPagamento.PIX -> PaymentType.PIX
            else -> null // DINHEIRO, BOLETO, FIADO não usam Stone
        }
    }

    /**
     * Verifica se a forma de pagamento deve usar Stone deeplink
     */
    fun shouldUseStone(forma: com.seucaixa.caixacombo.data.model.FormaPagamento): Boolean {
        return mapFormaPagamentoToStone(forma) != null
    }

    /**
     * Cria a Intent de pagamento para o Stone deeplink
     *
     * @param amount valor em centavos (ex: 1000 = R$ 10,00)
     * @param type CREDIT, DEBIT ou PIX
     * @param installmentCount número de parcelas (0 para débito/PIX, 1+ para crédito)
     * @param orderId ID do pedido (opcional)
     */
    fun createPaymentIntent(
        amount: Long,
        type: String,
        installmentCount: Int = 0,
        orderId: String = ""
    ): Intent {
        val uriBuilder = Uri.Builder().apply {
            authority("pay")
            scheme("payment-app")
            appendQueryParameter("amount", amount.toString())
            appendQueryParameter("installment_count", installmentCount.toString())
            appendQueryParameter("type", type)
            if (orderId.isNotBlank()) {
                appendQueryParameter("order_id", orderId)
            }
        }

        val uri = uriBuilder.build()
        Log.d(TAG, "Criando intent de pagamento: $uri")

        return Intent(Intent.ACTION_VIEW, uri)
    }

    /**
     * Converte valor Double para centavos (Long)
     * ex: 10.50 -> 1050
     */
    fun toCentavos(valor: Double): Long {
        return (valor * 100).toLong()
    }

    /**
     * Envia o pagamento via deeplink para o app da Stone
     */
    fun sendPayment(activity: Activity, amount: Long, type: String, installmentCount: Int = 0, orderId: String = "") {
        val intent = createPaymentIntent(amount, type, installmentCount, orderId)
        try {
            Log.d(TAG, "Enviando pagamento: amount=$amount, type=$type, installmentCount=$installmentCount")
            activity.startActivityForResult(intent, REQUEST_CODE_PAYMENT)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar pagamento via Stone deeplink. App Stone instalado?", e)
        }
    }

    /**
     * Parseia o resultado do pagamento retornado pelo Stone
     * na URI payment-app://pay-response?code=0&amount=1000&success=true&type=CRÉDITO...
     */
    fun parsePaymentResult(data: Intent?): PaymentResult? {
        if (data == null) {
            Log.e(TAG, "Intent de resultado é nula")
            return null
        }

        val uri = data.data ?: return null
        Log.d(TAG, "URI de resposta: $uri")

        // Verificar se é resposta do Stone
        if (uri.scheme != "payment-app" || uri.authority != "pay-response") {
            Log.w(TAG, "URI não é resposta do Stone: $uri")
            return null
        }

        val code = uri.getQueryParameter("code")?.toIntOrNull() ?: -1
        val success = uri.getQueryParameter("success")?.toBoolean() ?: (code == 0)
        val amount = uri.getQueryParameter("amount")?.toLongOrNull() ?: 0
        val type = uri.getQueryParameter("type") ?: ""
        val installmentCount = uri.getQueryParameter("installment_count")?.toIntOrNull() ?: 0
        val brand = uri.getQueryParameter("brand") ?: ""
        val entryMode = uri.getQueryParameter("entry_mode") ?: ""
        val authorizationCode = uri.getQueryParameter("authorization_code") ?: ""
        val reason = uri.getQueryParameter("reason") ?: ""
        val orderId = uri.getQueryParameter("order_id") ?: ""

        val result = PaymentResult(
            success = success,
            code = code,
            amount = amount,
            type = type,
            installmentCount = installmentCount,
            brand = brand,
            entryMode = entryMode,
            authorizationCode = authorizationCode,
            reason = reason,
            orderId = orderId
        )

        Log.d(TAG, "Resultado Stone: success=${result.success}, code=${result.code}, type=${result.type}, brand=${result.brand}, authCode=${result.authorizationCode}")
        return result
    }
}
