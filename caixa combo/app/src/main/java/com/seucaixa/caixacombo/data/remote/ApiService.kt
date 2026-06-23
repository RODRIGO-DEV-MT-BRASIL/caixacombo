package com.seucaixa.caixacombo.data.remote

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body

/**
 * Health check do backend (Vercel).
 * Use para validar que o app esta se comunicando com a API.
 */
data class HealthResponse(
    val status: String,
    val version: String? = null,
    val timestamp: String? = null
)

/**
 * Sync request - envio de vendas offline para o backend.
 */
data class SyncVendaRequest(
    val localId: Long,
    val numero: String,
    val dataHora: Long,
    val total: Double,
    val formaPagamento: String,
    val itens: List<SyncVendaItem>
)

data class SyncVendaItem(
    val produtoId: Long,
    val produtoNome: String,
    val quantidade: Double,
    val precoUnitario: Double,
    val total: Double
)

data class SyncVendaResponse(
    val ok: Boolean,
    val serverId: String? = null,
    val message: String? = null
)

interface ApiService {
    @GET("api/health")
    suspend fun health(): HealthResponse

    @POST("api/vendas/sync")
    suspend fun syncVenda(@Body venda: SyncVendaRequest): SyncVendaResponse
}
