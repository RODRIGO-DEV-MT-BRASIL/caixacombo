package com.seucaixa.caixacombo.data.remote

import com.seucaixa.caixacombo.BuildConfig

/**
 * Provider de credenciais Supabase.
 * As chaves ficam no .env e sao injetadas no BuildConfig pelo Gradle.
 *
 * Para ativar o cliente Supabase real:
 * 1. Atualizar Kotlin para 2.3+ (Supabase 3.6+ requer)
 * 2. Descomentar as dependencias em app/build.gradle.kts
 * 3. Descomentar o bloco abaixo e usar createSupabaseClient()
 *
 * Por enquanto, expomos apenas as configuracoes (URL/keys) para uso
 * via REST/HTTP direto com OkHttp/Retrofit.
 */
object SupabaseConfig {

    val url: String get() = BuildConfig.SUPABASE_URL
    val anonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    val serviceRoleKey: String get() = BuildConfig.SUPABASE_SERVICE_ROLE_KEY

    val isConfigured: Boolean
        get() = url.isNotBlank() &&
                anonKey.isNotBlank() &&
                !url.contains("SEU-PROJETO")

    /**
     * Headers padrao para chamadas REST ao Supabase (PostgREST).
     * Use com OkHttp/Retrofit.
     */
    val defaultHeaders: Map<String, String>
        get() = mapOf(
            "apikey" to anonKey,
            "Authorization" to "Bearer $anonKey",
            "Content-Type" to "application/json"
        )
}
