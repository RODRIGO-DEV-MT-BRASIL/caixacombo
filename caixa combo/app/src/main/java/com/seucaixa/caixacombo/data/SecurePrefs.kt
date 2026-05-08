package com.seucaixa.caixacombo.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Helper para EncryptedSharedPreferences.
 * Armazena dados sensíveis (PIN operador, senha admin) de forma criptografada.
 * Stone compliance: segurança da informação.
 */
object SecurePrefs {

    private const val FILE_NAME = "secure_prefs"
    private const val KEY_OPERATOR_NAME = "operador_nome"
    private const val KEY_OPERATOR_CARGO = "operador_cargo"
    private const val KEY_OPERATOR_ID = "operador_id"
    private const val KEY_ADMIN_PASSWORD = "admin_password"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // Operador logado
    fun saveOperator(context: Context, nome: String, cargo: String, id: Long) {
        getPrefs(context).edit()
            .putString(KEY_OPERATOR_NAME, nome)
            .putString(KEY_OPERATOR_CARGO, cargo)
            .putLong(KEY_OPERATOR_ID, id)
            .apply()
    }

    fun getOperatorName(context: Context): String? {
        return getPrefs(context).getString(KEY_OPERATOR_NAME, null)
    }

    fun getOperatorCargo(context: Context): String? {
        return getPrefs(context).getString(KEY_OPERATOR_CARGO, null)
    }

    fun getOperatorId(context: Context): Long {
        return getPrefs(context).getLong(KEY_OPERATOR_ID, -1)
    }

    fun clearOperator(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_OPERATOR_NAME)
            .remove(KEY_OPERATOR_CARGO)
            .remove(KEY_OPERATOR_ID)
            .apply()
    }

    // Senha admin
    fun saveAdminPassword(context: Context, password: String) {
        getPrefs(context).edit()
            .putString(KEY_ADMIN_PASSWORD, password)
            .apply()
    }

    fun getAdminPassword(context: Context): String? {
        return getPrefs(context).getString(KEY_ADMIN_PASSWORD, null)
    }
}
