package com.tilin.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Paleta {
    val fondos = listOf("#000000", "#0B2A33", "#12392A", "#2A1245", "#4A1230", "#3A2A12", "#0B1230", "#FFFFFF")
    val acentos = listOf("#2EE59D", "#22D3EE", "#FFC107", "#FF4D8D", "#A78BFA", "#FF7A1A", "#2F6BFF", "#FFFFFF")
    val emojis = listOf("🔔", "💰", "🤑", "💸", "🎉", "⭐", "🚀", "🔥", "💎", "😎")
}

object Aviso {
    private const val CANAL = "pagos_recibidos"
    private val h = Handler(Looper.getMainLooper())
    private var vista: LinearLayout? = null
    private var wmActual: WindowManager? = null
    private var tvTitulo: TextView? = null
    private var tvMonto: TextView? = null
    private var tvBanco: TextView? = null
    private var quitar: Runnable? = null
    private var desvanecer: Runnable? = null
    private var claveActual = ""

    fun fmt(n: Long): String =
        n.toString().reversed().chunked(3).joinToString(".").reversed()

    fun diag(ctx: Context, texto: String) {
        try {
            val hora = SimpleDateFormat("h:mm:ss a", Locale("es", "CO")).format(Date())
            ctx.applicationContext.getSharedPreferences("tilin", Context.MODE_PRIVATE)
                .edit().putString("diag", hora + " · " + texto).apply()
        } catch (e: Exception) { }
    }

    fun mostrar(ctx: Context, titulo: String, monto: String, banco: String) {
        try {
            val c = ctx.applicationContext
            val p = c.getSharedPreferences("tilin", Context.MODE_PRIVATE)
            val estilo = p.getString("estilo", "letrero") ?: "letrero"
            val dur = p.getInt("dur", 6).coerceIn(2, 20)
            if (estilo == "letrero" && Settings.canDrawOverlays(c)) {
                val fi = p.getInt("cFondo", 0).coerceIn(0, Paleta.fondos.size - 1)
                val ai = p.getInt("cAcento", 0).coerceIn(0, Paleta.acentos.size - 1)
                val emoji = p.getString("emoji", "🔔") ?: "🔔"
                mostrarLetrero(
                    c, titulo, monto, banco, dur,
                    p.getInt("tam", 1), p.getInt("posV", 0), p.getInt("posH", 1),
                    Paleta.fondos[fi], Paleta.acentos[ai], emoji
                )
            } else {
                if (estilo == "letrero") diag(c, "falta permiso de aparecer encima, uso emergente del sistema")
                mostrarSistema(c, titulo, monto, banco, dur)
            }
        } catch (e: Exception) {
            diag(ctx, "ERROR: " + e.javaClass.simpleName + " " + (e.message ?: ""))
        }
    }

    private fun mostrarSistema(c: Context, titulo: String, monto: String, banco: String, dur: Int) {
        try {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CANAL) == null) {
                val ch = NotificationChannel(CANAL, "Pagos recibidos", NotificationManager.IMPORTANCE_HIGH)
                ch.description = "Aviso emergente cuando llega un pago"
                ch.enableVibration(false)
                ch.setSound(null, null)
                nm.createNotificationChannel(ch)
            }
            if (!nm.areNotificationsEnabled()) {
                diag(c, "las notificaciones de Rovika están apagadas")
                return
            }
            val abrir = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = Notification.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("+ " + monto)
                .setContentText(titulo + " · " + banco)
                .setColor(Color.parseColor("#22D3EE"))
                .setAutoCancel(true)
                .setTimeoutAfter(dur * 1000L)
                .setContentIntent(abrir)
                .build()
            nm.notify(1001, n)
            diag(c, "aviso emergente del sistema enviado")
        } catch (e: Exception) {
            diag(c, "ERROR sistema: " + e.javaClass.simpleName + " " + (e.message ?: ""))
        }
    }

    private fun cancelarTemporizadores() {
        quitar?.let { h.removeCallbacks(it) }
        desvanecer?.let { h.removeCallbacks(it) }
        quitar = null
        desvanecer = null
    }

    private fun retirar() {
        cancelarTemporizadores()
        val v = vista
        val wm = wmActual
        vista = null
        wmActual = null
        tvTitulo = null
        tvMonto = null
        tvBanco = null
        claveActual = ""
        if (v != null && wm != null) {
            try {
                wm.removeViewImmediate(v)
            } catch (e: Exception) { }
        }
    }

    private fun programar(c: Context, dur: Int) {
        cancelarTemporizadores()
        val total = dur * 1000L
        val f = Runnable {
            try {
                vista?.animate()?.alpha(0f)?.setDuration(280)?.start()
            } catch (e: Exception) { }
        }
        val r = Runnable {
            retirar()
            diag(c, "letrero retirado después de " + dur + " segundos")
        }
        desvanecer = f
        quitar = r
        h.postDelayed(f, total - 300L)
        h.postDelayed(r, total)
    }

    private fun mostrarLetrero(
        c: Context, titulo: String, monto: String, banco: String,
        dur: Int, tam: Int, posV: Int, posH: Int,
        fondoHex: String, acentoHex: String, emoji: String
    ) {
        h.post {
            try {
                val clave = tam.toString() + "-" + posV + "-" + posH + "-" + fondoHex + "-" + acentoHex + "-" + emoji
                val actual = vista
                if (actual != null && clave == claveActual) {
                    actual.animate().cancel()
                    actual.alpha = 1f
                    actual.scaleX = 1f
                    actual.scaleY = 1f
                    tvTitulo?.text = titulo
                    tvMonto?.text = monto
                    tvBanco?.text = banco
                    programar(c, dur)
                    diag(c, "letrero actualizado, se queda " + dur + " segundos")
                } else {
                    if (actual != null) retirar()
                    val wm = c.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    val d = c.resources.displayMetrics.density
                    val e = if (tam == 0) 0.8f else if (tam == 2) 1.4f else 1f

                    val colFondo = Color.parseColor(fondoHex)
                    val colAcento = Color.parseColor(acentoHex)
                    val claro = Color.luminance(colFondo) > 0.5f
                    val colTitulo = if (claro) Color.parseColor("#111111") else Color.WHITE
                    val colBanco = if (claro) Color.parseColor("#555555") else Color.parseColor("#9AA5B8")

                    val fondo = GradientDrawable()
                    fondo.setColor(colFondo)
                    fondo.cornerRadius = 40f * d
                    fondo.setStroke((1.5f * d).toInt().coerceAtLeast(1), (colAcento and 0x00FFFFFF) or 0x77000000)

                    val caja = LinearLayout(c)
                    caja.orientation = LinearLayout.HORIZONTAL
                    caja.gravity = Gravity.CENTER_VERTICAL
                    caja.background = fondo
                    caja.setPadding(
                        (18 * d * e).toInt(), (10 * d * e).toInt(),
                        (22 * d * e).toInt(), (10 * d * e).toInt()
                    )

                    if (emoji.isNotEmpty()) {
                        val ico = TextView(c)
                        ico.text = emoji
                        ico.textSize = 22f * e
                        caja.addView(ico)
                    }

                    val centro = LinearLayout(c)
                    centro.orientation = LinearLayout.VERTICAL
                    centro.setPadding((12 * d * e).toInt(), 0, (18 * d * e).toInt(), 0)

                    val t1 = TextView(c)
                    t1.text = titulo
                    t1.textSize = 15f * e
                    t1.setTextColor(colTitulo)
                    t1.setTypeface(null, Typeface.BOLD)
                    t1.maxLines = 1
                    centro.addView(t1)

                    val t3 = TextView(c)
                    t3.text = banco
                    t3.textSize = 12f * e
                    t3.setTextColor(colBanco)
                    t3.maxLines = 1
                    centro.addView(t3)
                    caja.addView(centro)

                    val t2 = TextView(c)
                    t2.text = monto
                    t2.textSize = 22f * e
                    t2.setTextColor(colAcento)
                    t2.setTypeface(null, Typeface.BOLD)
                    t2.maxLines = 1
                    caja.addView(t2)

                    val lp = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    )
                    val v = if (posV == 0) Gravity.TOP else if (posV == 2) Gravity.BOTTOM else Gravity.CENTER_VERTICAL
                    val hz = if (posH == 0) Gravity.START else if (posH == 2) Gravity.END else Gravity.CENTER_HORIZONTAL
                    lp.gravity = v or hz
                    lp.y = if (posV == 1) 0 else (56 * d).toInt()
                    lp.x = if (posH == 1) 0 else (16 * d).toInt()

                    caja.alpha = 0f
                    caja.scaleX = 0.6f
                    caja.scaleY = 0.6f
                    wm.addView(caja, lp)
                    vista = caja
                    wmActual = wm
                    tvTitulo = t1
                    tvMonto = t2
                    tvBanco = t3
                    claveActual = clave
                    diag(c, "letrero dibujado, se queda " + dur + " segundos")
                    caja.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(350)
                        .setInterpolator(OvershootInterpolator(1.1f))
                        .start()
                    programar(c, dur)
                }
            } catch (ex: Exception) {
                diag(c, "ERROR letrero: " + ex.javaClass.simpleName + " " + (ex.message ?: ""))
            }
        }
    }
}
