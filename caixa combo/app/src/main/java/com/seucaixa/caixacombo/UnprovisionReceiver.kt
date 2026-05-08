package com.seucaixa.caixacombo

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver para desprovisionar Device Owner via ADB.
 * Uso: adb shell am broadcast -a com.seucaixa.caixacombo.UNPROVISION
 * Após executar, o app pode ser desinstalado normalmente.
 */
class UnprovisionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.seucaixa.caixacombo.UNPROVISION") {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = ComponentName(context, AdminReceiver::class.java)

                if (dpm.isDeviceOwnerApp(context.packageName)) {
                    dpm.clearDeviceOwnerApp(context.packageName)
                    Log.d("UnprovisionReceiver", "Device Owner removido com sucesso")
                } else if (dpm.isAdminActive(admin)) {
                    dpm.removeActiveAdmin(admin)
                    Log.d("UnprovisionReceiver", "Device Admin removido com sucesso")
                }
            } catch (e: Exception) {
                Log.e("UnprovisionReceiver", "Erro ao desprovisionar: ${e.message}", e)
            }
        }
    }
}
