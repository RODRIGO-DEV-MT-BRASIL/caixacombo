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
    const val REQUEST_CODE_CANCEL = 1002
    const val REQUEST_CODE_REPRINT = 1003

    // Return scheme configurado no AndroidManifest
    private const val RETURN_SCHEME = "caixacombo"

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
     * Resultado do cancelamento Stone
     */
    data class CancelResult(
        val success: Boolean,
        val atk: String = "",
        val canceledAmount: Long = 0,
        val transactionAmount: Long = 0,
        val paymentType: Int = 0,
        val authorizationCode: String = "",
        val reason: String = "",
        val responseCode: String = ""
    )

    /**
     * Resultado da reimpressão Stone
     */
    data class ReprintResult(
        val success: Boolean,
        val reason: String = "",
        val responseCode: String = ""
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
     * Verifica se os apps da Stone estão instalados no dispositivo.
     * O deeplink só funciona se o Stone Payment App estiver presente.
     * D2s usa SUNMI Payment, não tem Stone - deeplink não funciona.
     */
    fun isStoneInstalled(context: android.content.Context): Boolean {
        val pm = context.packageManager
        return try {
            // Verificar se o app de pagamento da Stone está instalado
            pm.getPackageInfo("br.com.stone.posandroid.paymentapp", 0)
            true
        } catch (e: Exception) {
            try {
                // Fallback: verificar se resolve o scheme payment-app
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("payment-app://pay"))
                val resolved = pm.queryIntentActivities(intent, 0)
                resolved.isNotEmpty()
            } catch (e2: Exception) {
                false
            }
        }
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
            appendQueryParameter("returnscheme", RETURN_SCHEME)
            appendQueryParameter("third_party_theme_enabled", "true")
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
            // Usar startActivity em vez de startActivityForResult pois launchMode=singleTask
            // O retorno vem via onNewIntent com scheme caixacombo
            activity.startActivity(intent)
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

        // Verificar se é resposta do Stone - pode vir via returnscheme (caixacombo) ou payment-app
        val isPayResponse = uri.authority == "pay-response" &&
            (uri.scheme == "payment-app" || uri.scheme == RETURN_SCHEME)
        if (!isPayResponse) {
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

    // ==================== CANCELAMENTO ====================

    /**
     * Cria a Intent de cancelamento para o Stone deeplink
     * Documentação: cancel-app://cancel?atk=...&amount=...&editable_amount=...&returnscheme=...
     *
     * @param atk Código único da transação gerado pelo autorizador da Stone
     * @param amount Valor do cancelamento em centavos (opcional, 0 = valor total)
     * @param editableAmount Permite editar o valor no app de cancelamento
     */
    fun createCancelIntent(atk: String, amount: Long? = null, editableAmount: Boolean = false): Intent {
        val uriBuilder = Uri.Builder().apply {
            authority("cancel")
            scheme("cancel-app")
            appendQueryParameter("returnscheme", RETURN_SCHEME)
            appendQueryParameter("atk", atk)
            if (amount != null) {
                appendQueryParameter("amount", amount.toString())
            }
            appendQueryParameter("editable_amount", editableAmount.toString())
            appendQueryParameter("third_party_theme_enabled", "true")
        }

        val uri = uriBuilder.build()
        Log.d(TAG, "Criando intent de cancelamento: $uri")

        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Envia o cancelamento via deeplink para o app da Stone
     */
    fun sendCancel(activity: Activity, atk: String, amount: Long? = null, editableAmount: Boolean = false) {
        val intent = createCancelIntent(atk, amount, editableAmount)
        try {
            Log.d(TAG, "Enviando cancelamento: atk=$atk, amount=$amount")
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar cancelamento via Stone deeplink. App Stone instalado?", e)
        }
    }

    /**
     * Parseia o resultado do cancelamento retornado pelo Stone
     * Retorno: caixacombo://cancel?success=true&atk=...&canceledamount=...&paymenttype=...&authorizationcode=...&reason=APPROVED&responsecode=0000
     */
    fun parseCancelResult(data: Intent?): CancelResult? {
        if (data == null) {
            Log.e(TAG, "Intent de resultado cancelamento é nula")
            return null
        }

        val uri = data.data ?: return null
        Log.d(TAG, "URI de resposta cancelamento: $uri")

        val success = uri.getQueryParameter("success")?.toBoolean() ?: false
        val atk = uri.getQueryParameter("atk") ?: ""
        val canceledAmount = uri.getQueryParameter("canceledamount")?.toLongOrNull() ?: 0
        val transactionAmount = uri.getQueryParameter("transactionamount")?.toLongOrNull() ?: 0
        val paymentType = uri.getQueryParameter("paymenttype")?.toIntOrNull() ?: 0
        val authorizationCode = uri.getQueryParameter("authorizationcode") ?: ""
        val reason = uri.getQueryParameter("reason") ?: ""
        val responseCode = uri.getQueryParameter("responsecode") ?: ""

        val result = CancelResult(
            success = success,
            atk = atk,
            canceledAmount = canceledAmount,
            transactionAmount = transactionAmount,
            paymentType = paymentType,
            authorizationCode = authorizationCode,
            reason = reason,
            responseCode = responseCode
        )

        Log.d(TAG, "Resultado cancelamento: success=${result.success}, atk=${result.atk}, reason=${result.reason}")
        return result
    }

    // ==================== REIMPRESSÃO ====================

    /**
     * Cria a Intent de reimpressão para o Stone deeplink
     * Documentação: reprinter-app://reprint?ATK=...&SCHEME_RETURN=...&SHOW_FEEDBACK_SCREEN=...
     *
     * @param atk Código único da transação (opcional - se não enviado, será solicitado no app)
     */
    fun createReprintIntent(atk: String? = null): Intent {
        val uriBuilder = Uri.Builder().apply {
            authority("reprint")
            scheme("reprinter-app")
            appendQueryParameter("SHOW_FEEDBACK_SCREEN", "true")
            appendQueryParameter("SCHEME_RETURN", RETURN_SCHEME)
            if (!atk.isNullOrEmpty()) {
                appendQueryParameter("ATK", atk)
            }
        }

        val uri = uriBuilder.build()
        Log.d(TAG, "Criando intent de reimpressão: $uri")

        return Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Envia a reimpressão via deeplink para o app da Stone
     */
    fun sendReprint(activity: Activity, atk: String? = null) {
        val intent = createReprintIntent(atk)
        try {
            Log.d(TAG, "Enviando reimpressão: atk=$atk")
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao enviar reimpressão via Stone deeplink. App Stone instalado?", e)
        }
    }

    /**
     * Parseia o resultado da reimpressão retornado pelo Stone
     * Retorno: caixacombo://reprint?success=true&reason=...&responsecode=...
     */
    fun parseReprintResult(data: Intent?): ReprintResult? {
        if (data == null) {
            Log.e(TAG, "Intent de resultado reimpressão é nula")
            return null
        }

        val uri = data.data ?: return null
        Log.d(TAG, "URI de resposta reimpressão: $uri")

        val success = uri.getQueryParameter("success")?.toBoolean() ?: false
        val reason = uri.getQueryParameter("reason") ?: ""
        val responseCode = uri.getQueryParameter("responsecode") ?: ""

        val result = ReprintResult(
            success = success,
            reason = reason,
            responseCode = responseCode
        )

        Log.d(TAG, "Resultado reimpressão: success=${result.success}, reason=${result.reason}")
        return result
    }
}
