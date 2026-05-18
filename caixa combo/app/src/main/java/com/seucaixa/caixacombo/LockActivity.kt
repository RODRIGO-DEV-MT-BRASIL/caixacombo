package com.seucaixa.caixacombo

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.seucaixa.caixacombo.service.PollingService

/**
 * Activity fullscreen para bloqueio de tela.
 * Substitui o diálogo overlay (SYSTEM_ALERT_WINDOW - proibido pela Stone).
 *
 * SECURITY: Implementa rate limiting para prevenir ataques de força bruta.
 */
class LockActivity : Activity() {

    private var lockPassword = ""
    private var lockReason = ""

    private var failedAttempts = 0
    private var lockoutEndTime = 0L
    private val MAX_ATTEMPTS = 5
    private val LOCKOUT_DURATION_MS = 300_000L // 5 minutos
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

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
                val currentTime = System.currentTimeMillis()
                if (currentTime < lockoutEndTime) {
                    val remainingMinutes = ((lockoutEndTime - currentTime) / 60000) + 1
                    errorText.text = "Tentativas esgotadas. Tente novamente em ${remainingMinutes} minutos."
                    errorText.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                val password = input.text.toString()
                if (password.isBlank()) {
                    errorText.text = "Digite a senha"
                    errorText.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                if (lockPassword.isNotEmpty() && password == lockPassword) {
                    failedAttempts = 0
                    lockoutEndTime = 0L

                    val prefs = getSharedPreferences("lock_prefs", MODE_PRIVATE)
                    prefs.edit()
                        .putBoolean("is_locked", false)
                        .apply()

                    PollingService.sendUnlockConfirmed()
                    PollingService.sendDeviceStatus("online")

                    finish()
                } else {
                    failedAttempts++
                    if (failedAttempts >= MAX_ATTEMPTS) {
                        lockoutEndTime = System.currentTimeMillis() + LOCKOUT_DURATION_MS
                        errorText.text = "Tentativas esgotadas. Tente novamente em 5 minutos."
                        errorText.visibility = View.VISIBLE
                        input.isEnabled = false
                        isEnabled = false
                        startCountdown(errorText, input, this)
                    } else {
                        PollingService.sendUnlockAttempt(password)
                        errorText.text = "Senha incorreta (${failedAttempts}/$MAX_ATTEMPTS tentativas)"
                        errorText.visibility = View.VISIBLE
                        input.text.clear()
                    }
                }
            }
        }
        root.addView(btnUnlock)

        setContentView(root)
    }

    private fun startCountdown(errorText: TextView, input: EditText, btnUnlock: Button) {
        countdownRunnable?.let { handler.removeCallbacks(it) }

        countdownRunnable = object : Runnable {
            override fun run() {
                val remaining = lockoutEndTime - System.currentTimeMillis()
                if (remaining <= 0) {
                    failedAttempts = 0
                    lockoutEndTime = 0L
                    errorText.text = ""
                    errorText.visibility = View.GONE
                    input.isEnabled = true
                    btnUnlock.isEnabled = true
                } else {
                    val minutes = (remaining / 60000) + 1
                    errorText.text = "Bloqueado. Tente novamente em ${minutes} minutos."
                    handler.postDelayed(this, 60000)
                }
            }
        }
        handler.post(countdownRunnable!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onBackPressed() {
        // Não faz nada - não permite sair
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK ||
            event.keyCode == KeyEvent.KEYCODE_HOME ||
            event.keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true
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

