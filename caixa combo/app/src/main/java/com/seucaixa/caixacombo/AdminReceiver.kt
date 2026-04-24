package com.seucaixa.caixacombo

import android.content.Context
import android.content.Intent
import android.app.admin.DeviceAdminReceiver

/**
 * AdminReceiver necessário para modo quiosque (lock task mode)
 * Permite que o app seja bloqueado na tela em dispositivos Android
 */
class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
    }
}
