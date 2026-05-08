package com.seucaixa.caixacombo

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.seucaixa.caixacombo.service.PollingService

/**
 * Activity fullscreen para bloqueio de tela.
 * Substitui o diálogo overlay (SYSTEM_ALERT_WINDOW - proibido pela Stone).
 */
class LockActivity : Activity() {

    private var lockPassword = ""
    private var lockReason = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar janela fullscreen sobre a tela de bloqueio
        window.apply {
            addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
        }

        lockReason = intent.getStringExtra("reason") ?: "Dispositivo bloqueado pelo administrador"
        lockPassword = intent.getStringExtra("lockPassword") ?: ""

        // Layout programático (sem dependência de XML)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(64, 0, 64, 0)
            setBackgroundColor(android.graphics.Color.parseColor("#FF1a1a2e"))
        }

        val icon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            setColorFilter(android.graphics.Color.parseColor("#FF6200EE"))
            layoutParams = LinearLayout.LayoutParams(120, 120)
        }
        root.addView(icon)

        val spacer1 = View(this)
        root.addView(spacer1, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 40))

        val title = TextView(this).apply {
            text = "DISPOSITIVO BLOQUEADO"
            setTextColor(android.graphics.Color.WHITE)
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
        }
        root.addView(title)

        val spacer2 = View(this)
        root.addView(spacer2, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16))

        val reasonText = TextView(this).apply {
            text = lockReason
            setTextColor(android.graphics.Color.parseColor("#AAAAAA"))
            textSize = 14f
            gravity = android.view.Gravity.CENTER
        }
        root.addView(reasonText)

        val spacer3 = View(this)
        root.addView(spacer3, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 32))

        val input = EditText(this).apply {
            hint = "Digite a senha para desbloquear"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTextColor(android.graphics.Color.WHITE)
            setHintTextColor(android.graphics.Color.parseColor("#666666"))
            background.setColorFilter(android.graphics.Color.parseColor("#FF6200EE"), android.graphics.PorterDuff.Mode.SRC_IN)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(input)

        val spacer4 = View(this)
        root.addView(spacer4, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16))

        val errorText = TextView(this).apply {
            text = ""
            setTextColor(android.graphics.Color.RED)
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            visibility = View.GONE
        }
        root.addView(errorText)

        val spacer5 = View(this)
        root.addView(spacer5, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 16))

        val btnUnlock = Button(this).apply {
            text = "DESBLOQUEAR"
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#FF6200EE"))
            setAllCaps(true)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setOnClickListener {
                val password = input.text.toString()
                if (password.isBlank()) {
                    errorText.text = "Digite a senha"
                    errorText.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                // Verificar senha localmente (senha recebida do servidor)
                if (lockPassword.isNotEmpty() && password == lockPassword) {
                    // Salvar desbloqueio
                    val prefs = getSharedPreferences("lock_prefs", MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("is_locked", false)
                        .apply()

                    // Notificar servidor via PollingService
                    PollingService.sendUnlockConfirmed()
                    PollingService.sendDeviceStatus("online")

                    // Fechar activity
                    finish()
                } else {
                    // Enviar tentativa para servidor validar
                    PollingService.sendUnlockAttempt(password)
                    errorText.text = "Senha incorreta"
                    errorText.visibility = View.VISIBLE
                    input.text.clear()
                }
            }
        }
        root.addView(btnUnlock)

        setContentView(root)
    }

    // Bloquear botão voltar
    override fun onBackPressed() {
        // Não faz nada - não permite sair
    }

    // Bloquear botões de volume e outros
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK ||
            event.keyCode == KeyEvent.KEYCODE_HOME ||
            event.keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true // Consumir evento
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        fun start(context: android.content.Context, reason: String, lockPassword: String) {
            val intent = Intent(context, LockActivity::class.java).apply {
                putExtra("reason", reason)
                putExtra("lockPassword", lockPassword)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(intent)
        }
    }
}
