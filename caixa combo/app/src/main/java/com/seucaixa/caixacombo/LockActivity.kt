package com.seucaixa.caixacombo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.seucaixa.caixacombo.service.PollingService

class LockActivity : Activity() {

    private var lockPassword = ""
    private var lockReason = ""

    private var failedAttempts = 0
    private var lockoutEndTime = 0L
    private val MAX_ATTEMPTS = 5
    private val LOCKOUT_DURATION_MS = 300_000L
    private val handler = Handler(Looper.getMainLooper())
    private var countdownRunnable: Runnable? = null

    private var primaryColor = Color.parseColor("#3b82f6")
    private var bgColor = Color.parseColor("#1a1a2e")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val coresPrefs = getSharedPreferences("cores_sistema", MODE_PRIVATE)
        primaryColor = coresPrefs.getInt("primary_color", primaryColor)
        bgColor = coresPrefs.getInt("background_color", bgColor)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 0, 64, 0)
            setBackgroundColor(bgColor)
        }

        root.addView(ImageView(this).apply {
            setImageResource(android.R.drawable.ic_lock_lock)
            setColorFilter(primaryColor)
            layoutParams = LinearLayout.LayoutParams(120, 120)
        })

        root.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 40
        ))

        root.addView(TextView(this).apply {
            text = "DISPOSITIVO BLOQUEADO"
            setTextColor(Color.WHITE)
            textSize = 22f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })

        root.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 16
        ))

        root.addView(TextView(this).apply {
            text = lockReason
            setTextColor(Color.parseColor("#AAAAAA"))
            textSize = 14f
            gravity = Gravity.CENTER
        })

        root.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 32
        ))

        val input = EditText(this).apply {
            hint = "Digite a senha para desbloquear"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
            setHintTextColor(Color.parseColor("#666666"))
            background.setColorFilter(primaryColor, android.graphics.PorterDuff.Mode.SRC_IN)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        root.addView(input)

        root.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 16
        ))

        val errorText = TextView(this).apply {
            setTextColor(Color.RED)
            textSize = 12f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        root.addView(errorText)

        root.addView(View(this), LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 16
        ))

        root.addView(Button(this).apply {
            text = "DESBLOQUEAR"
            setTextColor(Color.WHITE)
            setBackgroundColor(primaryColor)
            setAllCaps(true)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
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

                    getSharedPreferences("lock_state", MODE_PRIVATE)
                        .edit()
                        .putBoolean("is_locked", false)
                        .remove("lock_reason")
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
        })

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

    override fun onResume() {
        super.onResume()
        instance = this
    }

    override fun onPause() {
        super.onPause()
        if (instance === this) instance = null
    }

    override fun onBackPressed() {}

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK ||
            event.keyCode == KeyEvent.KEYCODE_HOME ||
            event.keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    companion object {
        private var instance: LockActivity? = null

        fun start(context: Context, reason: String, lockPassword: String) {
            context.startActivity(
                Intent(context, LockActivity::class.java).apply {
                    putExtra("reason", reason)
                    putExtra("lockPassword", lockPassword)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }

        fun finishInstance() {
            instance?.finish()
        }
    }
}

