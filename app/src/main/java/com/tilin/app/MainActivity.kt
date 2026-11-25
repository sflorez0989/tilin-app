package com.tilin.app

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Pago(val ts: Long, val monto: Long, val app: String, val nombre: String, val banco: String)

class Barras(c: Context, private val vals: List<Long>, private val etiquetas: List<String>) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val base = height.toFloat() - 22f * d
        val tope = 6f * d
        val max = (vals.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val n = vals.size
        val slot = w / n
        val bw = slot * 0.5f
        for (i in 0 until n) {
            val x = slot * i + (slot - bw) / 2f
            var alto = (base - tope) * vals[i].toFloat() / max
            if (alto < 3f * d) alto = 3f * d
            val hoy = i == n - 1
            p.shader = LinearGradient(
                0f, base - alto, 0f, base,
                Color.parseColor(if (hoy) "#22D3EE" else "#2F6BFF"),
                Color.parseColor(if (hoy) "#2F6BFF" else "#1B2F66"),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(RectF(x, base - alto, x + bw, base), 6f * d, 6f * d, p)
            p.shader = null
            p.color = Color.parseColor(if (hoy) "#22D3EE" else "#8A97B0")
            p.textSize = 11f * d
            p.textAlign = Paint.Align.CENTER
            canvas.drawText(etiquetas[i], x + bw / 2f, height.toFloat() - 5f * d, p)
        }
    }
}

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private lateinit var contenido: LinearLayout
    private lateinit var listaMov: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var barraNav: LinearLayout
    private lateinit var tabInicio: TextView
    private lateinit var tabMov: TextView
    private lateinit var tabAj: TextView
    private lateinit var tabMas: TextView
    private var pestana = 0
    private var abierta = -1
    private var logAbierto = false
    private var tutPaso = -1
    private val tutTotal = 7
    private var diaPintado = ""
    private var ultimoTotal = 0L
    private var filtro = ""
    private val h = Handler(Looper.getMainLooper())
    private val tic = object : Runnable {
        override fun run() {
            if (hoyStr() != diaPintado) dibujar()
            h.postDelayed(this, 30000)
        }
    }

    private val gris = Color.parseColor("#8A97B0")
    private val verde = Color.parseColor("#2EE59D")
    private val cian = Color.parseColor("#22D3EE")
    private val azul = Color.parseColor("#2F6BFF")
    private val ambar = Color.parseColor("#FFC107")
    private val fondo = Color.parseColor("#070B14")
    private val panel = Color.parseColor("#0F1626")
    private val borde = Color.parseColor("#1F2F4D")

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun hoyStr(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    private fun caja(color: Int, radio: Int, bordeColor: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(radio).toFloat()
        if (bordeColor != 0) g.setStroke(dp(1), bordeColor)
        return g
    }

    private fun degradado(): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(azul, cian))
        g.cornerRadius = dp(16).toFloat()
        return g
    }

    private fun heroFondo(): GradientDrawable {
        val g = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#14327A"), Color.parseColor("#0B2A33"))
        )
        g.cornerRadius = dp(26).toFloat()
        g.setStroke(dp(1), azul)
        return g
    }

    private fun texto(t: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val v = TextView(this)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        if (bold) v.setTypeface(null, Typeface.BOLD)
        return v
    }

    private fun tarjeta(color: Int, bordeColor: Int = borde): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.background = caja(color, 22, bordeColor)
        l.setPadding(dp(18), dp(16), dp(18), dp(16))
        val p = LinearLayout.LayoutParams(-1, -2)
        p.setMargins(0, 0, 0, dp(12))
        l.layoutParams = p
        return l
    }

    private fun boton(t: String, lleno: Boolean, accion: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.isAllCaps = false
        b.textSize = 16f
        b.setTypeface(null, Typeface.BOLD)
        b.setTextColor(if (lleno) Color.WHITE else cian)
        b.background = if (lleno) degradado() else caja(Color.TRANSPARENT, 16, cian)
        b.setOnClickListener { accion() }
        val p = LinearLayout.LayoutParams(-1, dp(52))
        p.setMargins(0, 0, 0, dp(10))
        b.layoutParams = p
        return b
    }

    private fun botonPeq(t: String, lleno: Boolean, accion: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.isAllCaps = false
        b.textSize = 14f
        b.setTypeface(null, Typeface.BOLD)
        b.setTextColor(if (lleno) Color.WHITE else cian)
        b.background = if (lleno) degradado() else caja(Color.TRANSPARENT, 16, cian)
        b.setOnClickListener { accion() }
        val p = LinearLayout.LayoutParams(0, dp(50), 1f)
        p.setMargins(dp(4), 0, dp(4), 0)
        b.layoutParams = p
        return b
    }

    private fun filaBotones(vararg bs: Button) {
        val f = LinearLayout(this)
        f.orientation = LinearLayout.HORIZONTAL
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, dp(10))
        f.layoutParams = lp
        for (b in bs) f.addView(b)
        contenido.addView(f)
    }

    private fun botonMini(t: String, accion: () -> Unit): Button {
        val b = Button(this)
        b.text = t
        b.isAllCaps = false
        b.textSize = 22f
        b.setTypeface(null, Typeface.BOLD)
        b.setTextColor(Color.WHITE)
        b.background = degradado()
        b.setOnClickListener { accion() }
        b.layoutParams = LinearLayout.LayoutParams(dp(64), dp(48))
        return b
    }

    private fun encabezado(icono: String, titulo: String, resumen: String, abiertoAhora: Boolean, alTocar: () -> Unit) {
        val t = tarjeta(panel, if (abiertoAhora) cian else borde)
        t.setPadding(dp(18), dp(14), dp(18), dp(14))
        val fila = LinearLayout(this)
        fila.orientation = LinearLayout.HORIZONTAL
        fila.gravity = Gravity.CENTER_VERTICAL
        val ic = texto(icono, 24f, Color.WHITE)
        ic.setPadding(0, 0, dp(14), 0)
        fila.addView(ic)
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        col.addView(texto(titulo, 17f, Color.WHITE, true))
        col.addView(texto(resumen, 12f, gris))
        fila.addView(col)
        fila.addView(texto(if (abiertoAhora) "▴" else "▾", 20f, cian, true))
        t.addView(fila)
        t.setOnClickListener { alTocar() }
        contenido.addView(t)
    }

    private fun fila(titulo: String, etiquetas: List<String>, actual: Int, elegir: (Int) -> Unit) {
        val t = tarjeta(panel)
        t.addView(texto(titulo, 14f, gris))
        val f = LinearLayout(this)
        f.orientation = LinearLayout.HORIZONTAL
        val lp0 = LinearLayout.LayoutParams(-1, -2)
        lp0.setMargins(0, dp(10), 0, 0)
        f.layoutParams = lp0
        for (i in etiquetas.indices) {
            val b = Button(this)
            b.text = etiquetas[i]
            b.isAllCaps = false
            b.textSize = 13f
            b.setTypeface(null, Typeface.BOLD)
            val sel = i == actual
            b.setTextColor(if (sel) Color.WHITE else cian)
            b.background = if (sel) degradado() else caja(Color.TRANSPARENT, 14, borde)
            val p = LinearLayout.LayoutParams(0, dp(46), 1f)
            p.setMargins(dp(3), 0, dp(3), 0)
            b.layoutParams = p
            b.setOnClickListener { elegir(i) }
            f.addView(b)
        }
        t.addView(f)
        contenido.addView(t)
    }

    private fun ajusteNum(titulo: String, clave: String, def: Int, min: Int, max: Int, paso: Int, sufijo: String) {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val valor = prefs.getInt(clave, def)
        val t = tarjeta(panel)
        t.addView(texto(titulo, 14f, gris))
        val rd = LinearLayout(this)
        rd.orientation = LinearLayout.HORIZONTAL
        rd.gravity = Gravity.CENTER_VERTICAL
        val rl = LinearLayout.LayoutParams(-1, -2)
        rl.setMargins(0, dp(10), 0, 0)
        rd.layoutParams = rl
        rd.addView(botonMini("−") {
            prefs.edit().putInt(clave, (valor - paso).coerceAtLeast(min)).apply()
            dibujar()
        })
        val tv = texto(valor.toString() + sufijo, 20f, Color.WHITE, true)
        tv.gravity = Gravity.CENTER
        tv.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        rd.addView(tv)
        rd.addView(botonMini("+") {
            prefs.edit().putInt(clave, (valor + paso).coerceAtMost(max)).apply()
            dibujar()
        })
        t.addView(rd)
        contenido.addView(t)
    }

    private fun paleta(titulo: String, colores: List<String>, actual: Int, elegir: (Int) -> Unit) {
        val t = tarjeta(panel)
        t.addView(texto(titulo, 14f, gris))
        val f = LinearLayout(this)
        f.orientation = LinearLayout.HORIZONTAL
        val lp0 = LinearLayout.LayoutParams(-1, -2)
        lp0.setMargins(0, dp(10), 0, 0)
        f.layoutParams = lp0
        for (i in colores.indices) {
            val v = View(this)
            val g = GradientDrawable()
            g.setColor(Color.parseColor(colores[i]))
            g.cornerRadius = dp(12).toFloat()
            g.setStroke(dp(if (i == actual) 3 else 1), if (i == actual) cian else borde)
            v.background = g
            val p = LinearLayout.LayoutParams(0, dp(42), 1f)
            p.setMargins(dp(3), 0, dp(3), 0)
            v.layoutParams = p
            v.setOnClickListener { elegir(i) }
            f.addView(v)
        }
        t.addView(f)
        contenido.addView(t)
    }

    private fun emojiFila(actual: String, elegir: (String) -> Unit) {
        val t = tarjeta(panel)
        t.addView(texto("Emoji del letrero", 14f, gris))
        val lista = Paleta.emojis
        for (fila in 0..1) {
            val f = LinearLayout(this)
            f.orientation = LinearLayout.HORIZONTAL
            val lp0 = LinearLayout.LayoutParams(-1, -2)
            lp0.setMargins(0, dp(10), 0, 0)
            f.layoutParams = lp0
            for (j in 0..4) {
                val e = lista[fila * 5 + j]
                val b = TextView(this)
                b.text = e
                b.textSize = 26f
                b.gravity = Gravity.CENTER
                b.background = caja(Color.TRANSPARENT, 14, if (e == actual) cian else borde)
                val p = LinearLayout.LayoutParams(0, dp(52), 1f)
                p.setMargins(dp(3), 0, dp(3), 0)
                b.layoutParams = p
                b.setOnClickListener { elegir(e) }
                f.addView(b)
            }
            t.addView(f)
        }
        contenido.addView(t)
    }

    private fun vistaPrevia() {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val fi = prefs.getInt("cFondo", 0).coerceIn(0, Paleta.fondos.size - 1)
        val ai = prefs.getInt("cAcento", 0).coerceIn(0, Paleta.acentos.size - 1)
        val emoji = prefs.getString("emoji", "🔔") ?: "🔔"
        val colFondo = Color.parseColor(Paleta.fondos[fi])
        val colAcento = Color.parseColor(Paleta.acentos[ai])
        val claro = Color.luminance(colFondo) > 0.5f
        val t = tarjeta(panel)
        t.addView(texto("Vista previa", 14f, gris))

        val c = LinearLayout(this)
        c.orientation = LinearLayout.HORIZONTAL
        c.gravity = Gravity.CENTER_VERTICAL
        c.background = caja(colFondo, 40, colAcento)
        c.setPadding(dp(18), dp(10), dp(22), dp(10))
        val lp = LinearLayout.LayoutParams(-2, -2)
        lp.gravity = Gravity.CENTER_HORIZONTAL
        lp.setMargins(0, dp(10), 0, 0)
        c.layoutParams = lp

        if (emoji.isNotEmpty()) {
            c.addView(texto(emoji, 22f, Color.WHITE))
        }
        val centro = LinearLayout(this)
        centro.orientation = LinearLayout.VERTICAL
        centro.setPadding(dp(12), 0, dp(18), 0)
        centro.addView(texto("Bre-B", 15f, if (claro) Color.parseColor("#111111") else Color.WHITE, true))
        centro.addView(texto("Nequi", 12f, if (claro) Color.parseColor("#555555") else Color.parseColor("#9AA5B8")))
        c.addView(centro)
        c.addView(texto("$ 15.000", 22f, colAcento, true))
        t.addView(c)
        contenido.addView(t)
    }

    private fun hablarPrueba() {
        Voz.aplicar(tts, this)
        tts?.speak(Voz.frase(this, 15000L, "Bre-B"), TextToSpeech.QUEUE_FLUSH, null, "prueba")
    }

    private fun leerPagos(): List<Pago> {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val lineas = (prefs.getString("pagos", "") ?: "").split("\n").filter { it.isNotBlank() }
        val resultado = mutableListOf<Pago>()
        for (linea in lineas) {
            val c = linea.split("|")
            if (c.size < 3) continue
            val nombre = if (c.size > 3) c[3] else ""
            val banco = if (c.size > 4) c[4] else c[2]
            resultado.add(Pago(c[0].toLongOrNull() ?: 0L, c[1].toLongOrNull() ?: 0L, c[2], nombre, banco))
        }
        return resultado
    }

    private fun compartir(txt: String) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, txt)
        startActivity(Intent.createChooser(i, "Compartir con"))
    }

    private fun resumenHoy(): String {
        val dia = SimpleDateFormat("yyyyMMdd", Locale.US)
        val hora = SimpleDateFormat("h:mm a", Locale("es", "CO"))
        val fecha = SimpleDateFormat("d 'de' MMMM yyyy", Locale("es", "CO"))
        val hoy = hoyStr()
        val pagos = leerPagos().filter { dia.format(Date(it.ts)) == hoy }.reversed()
        val total = pagos.fold(0L) { a, p -> a + p.monto }
        val sb = StringBuilder()
        sb.append("Rovika - ").append(fecha.format(Date())).append("\n")
        sb.append("Total: $ ").append(Aviso.fmt(total)).append(" (").append(pagos.size).append(" pagos)\n\n")
        for (p in pagos) {
            sb.append("- ").append(p.banco).append(" $ ").append(Aviso.fmt(p.monto))
                .append(" · ").append(hora.format(Date(p.ts))).append("\n")
        }
        return sb.toString()
    }

    private fun restaurar(txt: String) {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val actuales = (prefs.getString("pagos", "") ?: "").split("\n").filter { it.isNotBlank() }
        val nuevas = txt.split("\n").map { it.trim() }.filter {
            val c = it.split("|")
            c.size >= 3 && c[0].toLongOrNull() != null
        }
        val union = (actuales + nuevas).distinct()
            .sortedByDescending { it.split("|")[0].toLongOrNull() ?: 0L }
        prefs.edit().putString("pagos", union.take(3000).joinToString("\n")).apply()
        Toast.makeText(this, "Se restauraron " + nuevas.size + " movimientos", Toast.LENGTH_LONG).show()
        dibujar()
    }

    private fun pedirRestaurar() {
        val et = EditText(this)
        et.hint = "Pega aquí la copia de seguridad"
        et.minLines = 4
        et.gravity = Gravity.TOP
        AlertDialog.Builder(this)
            .setTitle("Restaurar copia")
            .setView(et)
            .setPositiveButton("Restaurar") { _, _ -> restaurar(et.text.toString()) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        window.statusBarColor = fondo
        window.navigationBarColor = Color.parseColor("#0A1020")

        val vistoTutorial = getSharedPreferences("tilin", MODE_PRIVATE).getBoolean("tutVisto", false)
        if (!vistoTutorial) tutPaso = 0

        if (Build.VERSION.SDK_INT >= 33 && vistoTutorial) {
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 7)
        }

        val raiz = LinearLayout(this)
        raiz.orientation = LinearLayout.VERTICAL
        raiz.setBackgroundColor(fondo)

        scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        contenido = LinearLayout(this)
        contenido.orientation = LinearLayout.VERTICAL
        contenido.setPadding(dp(20), dp(24), dp(20), dp(32))
        scroll.addView(contenido)

        val barra = LinearLayout(this)
        barra.orientation = LinearLayout.HORIZONTAL
        barra.setBackgroundColor(Color.parseColor("#0A1020"))
        barra.layoutParams = LinearLayout.LayoutParams(-1, -2)
        barraNav = barra

        tabInicio = texto("🏠\nInicio", 12f, cian, true)
        tabMov = texto("📋\nMovimientos", 12f, gris, true)
        tabAj = texto("⚙️\nAjustes", 12f, gris, true)
        tabMas = texto("✨\nMás", 12f, gris, true)
        for (t in listOf(tabInicio, tabMov, tabAj, tabMas)) {
            t.gravity = Gravity.CENTER
            t.layoutParams = LinearLayout.LayoutParams(0, dp(60), 1f)
            barra.addView(t)
        }
        tabInicio.setOnClickListener { pestana = 0; scroll.scrollTo(0, 0); dibujar() }
        tabMov.setOnClickListener { pestana = 1; scroll.scrollTo(0, 0); dibujar() }
        tabAj.setOnClickListener { pestana = 2; scroll.scrollTo(0, 0); dibujar() }
        tabMas.setOnClickListener { pestana = 3; scroll.scrollTo(0, 0); dibujar() }

        raiz.addView(scroll)
        raiz.addView(barra)
        setContentView(raiz)
    }

    override fun onResume() {
        super.onResume()
        dibujar()
        h.removeCallbacks(tic)
        h.postDelayed(tic, 30000)
        Actualizar.revisarSiToca(this) {
            h.post {
                if (!isFinishing && !isDestroyed && tutPaso < 0 && Actualizar.hayNueva(this)) dibujar()
            }
        }
    }

    override fun onPause() {
        h.removeCallbacks(tic)
        super.onPause()
    }

    private fun dibujar() {
        diaPintado = hoyStr()
        barraNav.visibility = if (tutPaso >= 0) View.GONE else View.VISIBLE
        contenido.removeAllViews()
        if (tutPaso >= 0) {
            dibujarTutorial()
            return
        }
        val tabs = listOf(tabInicio, tabMov, tabAj, tabMas)
        for (i in tabs.indices) {
            tabs[i].setTextColor(if (pestana == i) cian else Color.parseColor("#5B6781"))
        }
        if (pestana == 0) dibujarInicio()
        else if (pestana == 1) dibujarMov()
        else if (pestana == 2) dibujarAjustes()
        else Mas.dibujar(this, contenido, scroll, barraNav) { dibujar() }
    }

    private fun abrirAjuste(i: Intent) {
        try {
            startActivity(i)
        } catch (e: Exception) {
            Toast.makeText(this, "No pude abrir esa pantalla. Búscala en los Ajustes del teléfono.", Toast.LENGTH_LONG).show()
        }
    }

    private fun estadoLinea(ok: Boolean, si: String, no: String): TextView {
        val v = texto(if (ok) "✅ " + si else "⚠️ " + no, 14f, if (ok) verde else ambar, true)
        v.setPadding(0, dp(10), 0, dp(2))
        return v
    }

    private fun tarjetaPaso(icono: String, titulo: String, cuerpo: String): LinearLayout {
        val t = tarjeta(panel, cian)
        t.addView(texto(icono, 46f, Color.WHITE))
        val tt = texto(titulo, 24f, Color.WHITE, true)
        tt.setPadding(0, dp(8), 0, dp(8))
        t.addView(tt)
        val cu = texto(cuerpo, 15f, Color.parseColor("#C9D3E8"))
        cu.setLineSpacing(0f, 1.15f)
        t.addView(cu)
        return t
    }

    private fun terminarTutorial() {
        getSharedPreferences("tilin", MODE_PRIVATE).edit().putBoolean("tutVisto", true).apply()
        tutPaso = -1
        pestana = 0
        scroll.scrollTo(0, 0)
        dibujar()
    }

    private fun irAAjustes(seccion: Int) {
        getSharedPreferences("tilin", MODE_PRIVATE).edit().putBoolean("tutVisto", true).apply()
        tutPaso = -1
        pestana = 2
        abierta = seccion
        scroll.scrollTo(0, 0)
        dibujar()
    }

    private fun dibujarTutorial() {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val activos = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val leyendo = activos.contains(packageName)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val avisosOk = nm.areNotificationsEnabled()
        val encimaOk = Settings.canDrawOverlays(this)
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val bateriaOk = pm.isIgnoringBatteryOptimizations(packageName)
        val usaLetrero = (prefs.getString("estilo", "letrero") ?: "letrero") == "letrero"
        val paso = tutPaso

        val top = LinearLayout(this)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val tl = LinearLayout.LayoutParams(-1, -2)
        tl.setMargins(0, 0, 0, dp(14))
        top.layoutParams = tl
        val puntos = StringBuilder()
        for (i in 0 until tutTotal) {
            puntos.append(if (i == paso) "●" else "○").append(" ")
        }
        val tp = texto(puntos.toString().trim(), 14f, cian, true)
        tp.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        top.addView(tp)
        val saltar = texto("Saltar ✕", 14f, gris, true)
        saltar.setPadding(dp(10), dp(6), dp(4), dp(6))
        saltar.setOnClickListener { terminarTutorial() }
        top.addView(saltar)
        contenido.addView(top)

        if (paso == 0) {
            contenido.addView(tarjetaPaso(
                "👋", "Bienvenido a Rovika",
                "Rovika te dice en voz alta cada pago que te llega por Nequi, Bancolombia, Daviplata y otras apps de pagos, y lleva la cuenta de tu día.\n\nEn menos de un minuto la dejamos lista."
            ))
        } else if (paso == 1) {
            val t = tarjetaPaso(
                "🔔", "Permiso para leer notificaciones",
                "Así se entera Rovika de tus pagos. Solo toma en cuenta las notificaciones de apps de pagos. Tus pagos se guardan únicamente en tu teléfono y nunca salen de él: Rovika solo usa internet para avisarte cuando hay una versión nueva."
            )
            t.addView(estadoLinea(leyendo, "Permiso activado", "Falta activar el permiso"))
            val tip = texto("Si el interruptor sale gris: Ajustes del teléfono → Aplicaciones → Rovika → ⋮ (arriba a la derecha) → Permitir ajustes restringidos. Luego vuelve y actívalo.", 13f, gris)
            tip.setPadding(0, dp(8), 0, 0)
            t.addView(tip)
            contenido.addView(t)
            contenido.addView(boton(if (leyendo) "Abrir permisos otra vez" else "Abrir permisos", !leyendo) {
                abrirAjuste(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
        } else if (paso == 2) {
            val t = tarjetaPaso(
                "💬", "Avisos en pantalla",
                "Cuando llega un pago, Rovika muestra un letrero con el valor y de dónde vino. Necesita estos permisos:"
            )
            t.addView(estadoLinea(encimaOk, "Aparecer encima de otras apps", "Falta: aparecer encima de otras apps"))
            t.addView(estadoLinea(avisosOk, "Notificaciones de Rovika", "Falta: notificaciones de Rovika"))
            val nota = texto("Si prefieres no dar el permiso de aparecer encima, en Ajustes puedes elegir la emergente del sistema.", 13f, gris)
            nota.setPadding(0, dp(8), 0, 0)
            t.addView(nota)
            contenido.addView(t)
            if (!encimaOk) {
                contenido.addView(boton("Permitir aparecer encima", true) {
                    abrirAjuste(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
                })
            }
            if (!avisosOk) {
                contenido.addView(boton("Permitir notificaciones de Rovika", true) {
                    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    abrirAjuste(i)
                })
            }
        } else if (paso == 3) {
            val t = tarjetaPaso(
                "🔋", "Que no se apague",
                "Para que Rovika te avise con la pantalla apagada mientras manejas, el teléfono no debe cerrarla:"
            )
            t.addView(estadoLinea(bateriaOk, "Batería sin restricciones", "Batería optimizada: el sistema puede apagar Rovika"))
            val lista = texto("1. Batería de Rovika: Sin restricciones.\n2. En Infinix, Xiaomi o Huawei, activa también Inicio automático.\n3. En las apps recientes, toca el ícono de Rovika y elige Mantener abierta (candado).", 14f, Color.parseColor("#C9D3E8"))
            lista.setLineSpacing(0f, 1.2f)
            lista.setPadding(0, dp(10), 0, 0)
            t.addView(lista)
            contenido.addView(t)
            contenido.addView(boton("Abrir ajustes de Rovika", true) {
                abrirAjuste(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)))
            })
            contenido.addView(boton("Ver apps sin optimizar", false) {
                abrirAjuste(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            })
        } else if (paso == 4) {
            contenido.addView(tarjetaPaso(
                "🏦", "Tus bancos",
                "Rovika reconoce sola Nequi, Bancolombia, Daviplata, Davivienda, Nu, BBVA y más.\n\nSolo suma lo que RECIBES: lo que envías, las compras y los impuestos no cuentan.\n\nSi tu banco no se reconoce, entra a Ajustes → Apps que Rovika lee y actívalo. Deja apagadas apps como WhatsApp."
            ))
            contenido.addView(boton("Ver apps que Rovika lee", false) { irAAjustes(2) })
        } else if (paso == 5) {
            contenido.addView(tarjetaPaso(
                "🎨", "Hazla tuya",
                "En Ajustes eliges los colores y el emoji del letrero, su tamaño y su lugar, cuánto dura, y la voz: normal, ardilla, gigante o robot.\n\nTambién puedes poner una meta del día desde Inicio y Rovika te felicita cuando la cumplas."
            ))
            contenido.addView(boton("🔊 Probar voz y aviso", true) {
                hablarPrueba()
                Aviso.mostrar(this, "Bre-B", "$ 15.000", "Nequi")
            })
            contenido.addView(boton("Abrir Ajustes", false) { irAAjustes(0) })
        } else {
            val t = tarjetaPaso(
                "✅", "¡Todo listo!",
                "Revisa que todo esté en verde. Cuando llegue tu próximo pago, Rovika lo dirá en voz alta."
            )
            t.addView(estadoLinea(leyendo, "Lee notificaciones", "Falta el permiso de leer notificaciones"))
            t.addView(estadoLinea(if (usaLetrero) encimaOk else avisosOk, "Avisos en pantalla", "Faltan los avisos en pantalla"))
            t.addView(estadoLinea(bateriaOk, "Batería sin restricciones", "Batería optimizada"))
            val fin = texto("Cuando haya una versión nueva te llegará un aviso para descargarla. Puedes volver a ver este tutorial en Ajustes → Ver tutorial.", 13f, gris)
            fin.setPadding(0, dp(10), 0, 0)
            t.addView(fin)
            contenido.addView(t)
        }

        if (paso == 0) {
            filaBotones(botonPeq("Empezar ▶", true) {
                tutPaso = 1
                scroll.scrollTo(0, 0)
                dibujar()
            })
        } else {
            val ultimo = paso == tutTotal - 1
            filaBotones(
                botonPeq("◀ Atrás", false) {
                    tutPaso = paso - 1
                    scroll.scrollTo(0, 0)
                    dibujar()
                },
                botonPeq(if (ultimo) "Terminar ✔" else "Siguiente ▶", true) {
                    if (ultimo) {
                        terminarTutorial()
                    } else {
                        tutPaso = paso + 1
                        scroll.scrollTo(0, 0)
                        dibujar()
                    }
                }
            )
        }
    }

    private fun filaPago(p: Pago, hora: SimpleDateFormat): LinearLayout {
        val f = tarjeta(panel)
        f.addView(texto(p.banco, 16f, Color.WHITE, true))
        f.addView(texto("+ $ " + Aviso.fmt(p.monto), 22f, verde, true))
        f.addView(texto(p.app + " · " + hora.format(Date(p.ts)), 13f, gris))
        return f
    }

    private fun pedirMeta() {
        val et = EditText(this)
        et.inputType = InputType.TYPE_CLASS_NUMBER
        et.hint = "Ej: 150000"
        AlertDialog.Builder(this)
            .setTitle("Meta del día")
            .setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                val v = et.text.toString().toLongOrNull()
                if (v != null && v > 0) {
                    getSharedPreferences("tilin", MODE_PRIVATE).edit().putLong("meta", v).apply()
                    dibujar()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun filaApp(pk: String, nom: String, activadas: Set<String>): LinearLayout {
        val f = LinearLayout(this)
        f.orientation = LinearLayout.HORIZONTAL
        f.gravity = Gravity.CENTER_VERTICAL
        f.setPadding(0, dp(8), 0, dp(8))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        col.addView(texto(nom, 15f, Color.WHITE, true))
        col.addView(texto(pk, 11f, gris))
        f.addView(col)
        if (Apps.esAuto(pk, nom)) {
            f.addView(texto("✔ Automática", 12f, verde, true))
        } else {
            val on = activadas.contains(pk)
            val b = Button(this)
            b.text = if (on) "Activada ✔" else "Activar"
            b.isAllCaps = false
            b.textSize = 13f
            b.setTypeface(null, Typeface.BOLD)
            b.setTextColor(if (on) Color.WHITE else cian)
            b.background = if (on) degradado() else caja(Color.TRANSPARENT, 14, cian)
            b.layoutParams = LinearLayout.LayoutParams(dp(112), dp(40))
            b.setOnClickListener {
                Apps.alternar(this, pk)
                dibujar()
            }
            f.addView(b)
        }
        return f
    }

    private fun bannerActualizacion() {
        if (!Actualizar.hayNueva(this)) return
        val nombre = getSharedPreferences("tilin", MODE_PRIVATE).getString("updNombre", "") ?: ""
        val t = tarjeta(Color.parseColor("#12392A"), verde)
        t.addView(texto("🚀 Nueva versión disponible", 17f, verde, true))
        val d = texto(nombre + ". Descárgala, toca el archivo y elige Instalar. No pierdes tus pagos ni tus ajustes.", 13f, gris)
        d.setPadding(0, dp(4), 0, 0)
        t.addView(d)
        val b = boton("⬇ Descargar actualización", true) {
            abrirAjuste(Intent(Intent.ACTION_VIEW, Uri.parse(Actualizar.APK)))
        }
        (b.layoutParams as LinearLayout.LayoutParams).setMargins(0, dp(12), 0, 0)
        t.addView(b)
        contenido.addView(t)
    }

    private fun dibujarAjustes() {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val estilo = prefs.getString("estilo", "letrero") ?: "letrero"
        val letrero = estilo == "letrero"

        contenido.addView(texto("Ajustes", 28f, Color.WHITE, true))
        val sub = texto("Toca una sección para abrirla", 14f, cian)
        sub.setPadding(0, dp(2), 0, dp(14))
        contenido.addView(sub)

        filaBotones(
            botonPeq("📖 Ver tutorial", false) {
                tutPaso = 0
                scroll.scrollTo(0, 0)
                dibujar()
            },
            botonPeq("🔄 Buscar actualizaciones", false) {
                Toast.makeText(this, "Buscando...", Toast.LENGTH_SHORT).show()
                Thread {
                    val r = Actualizar.revisarAhora(applicationContext, false)
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            val msg = if (r == 1) "¡Hay una versión nueva! Míralo en Inicio."
                            else if (r == 0) "Ya tienes la última versión ✔"
                            else "No pude consultar. Revisa tu internet."
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                            dibujar()
                        }
                    }
                }.start()
            }
        )

        val dur = prefs.getInt("dur", 6)
        encabezado(
            "🔔", "Aviso en pantalla",
            (if (letrero) "Letrero propio" else "Emergente del sistema") + " · " + dur + " seg",
            abierta == 0
        ) {
            abierta = if (abierta == 0) -1 else 0
            dibujar()
        }
        if (abierta == 0) ajusteAviso(letrero)

        val frases = listOf("Normal", "Tilín", "Corta", "Alegre")
        val fr = frases[prefs.getInt("frase", 0).coerceIn(0, 3)]
        encabezado(
            "🎙", "Voz",
            "Frase " + fr + " · tono " + prefs.getInt("tono", 100) + "% · velocidad " + prefs.getInt("vel", 100) + "%",
            abierta == 1
        ) {
            abierta = if (abierta == 1) -1 else 1
            dibujar()
        }
        if (abierta == 1) ajusteVoz()

        val todas = prefs.getBoolean("todas", false)
        val nExtra = Apps.extras(this).size
        encabezado(
            "🏦", "Apps que Rovika lee",
            if (todas) "Todas las apps" else "Bancos automáticos + " + nExtra + " activadas",
            abierta == 2
        ) {
            abierta = if (abierta == 2) -1 else 2
            dibujar()
        }
        if (abierta == 2) ajusteApps()

        val dg = prefs.getString("diag", "") ?: ""
        encabezado(
            "🛠", "Diagnóstico",
            if (dg.isBlank()) "Aún no hay avisos" else dg.take(46),
            abierta == 3
        ) {
            abierta = if (abierta == 3) -1 else 3
            dibujar()
        }
        if (abierta == 3) {
            val t = tarjeta(panel)
            t.addView(texto("Último aviso", 13f, gris, true))
            val dv = texto(if (dg.isBlank()) "Aún no se ha mostrado ningún aviso." else dg, 12f, ambar)
            dv.setTextIsSelectable(true)
            t.addView(dv)
            val ver = try {
                packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: Exception) {
                "?"
            }
            t.addView(texto("Rovika versión " + ver + " (código " + Actualizar.codigoActual(this) + ")", 12f, gris))
            contenido.addView(t)
        }
    }

    private fun ajusteAviso(letrero: Boolean) {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)

        fila("Estilo del aviso", listOf("Emergente del sistema", "Letrero propio"), if (letrero) 1 else 0) { i ->
            prefs.edit().putString("estilo", if (i == 1) "letrero" else "sistema").apply()
            dibujar()
        }

        ajusteNum("Duración en pantalla", "dur", 6, 2, 20, 1, " segundos")

        if (letrero) {
            vistaPrevia()
            paleta("Color del fondo", Paleta.fondos, prefs.getInt("cFondo", 0)) { i ->
                prefs.edit().putInt("cFondo", i).apply()
                dibujar()
            }
            paleta("Color del valor", Paleta.acentos, prefs.getInt("cAcento", 0)) { i ->
                prefs.edit().putInt("cAcento", i).apply()
                dibujar()
            }
            emojiFila(prefs.getString("emoji", "🔔") ?: "🔔") { e ->
                prefs.edit().putString("emoji", e).apply()
                dibujar()
            }
            fila("Tamaño del letrero", listOf("Pequeño", "Mediano", "Grande"), prefs.getInt("tam", 1)) { i ->
                prefs.edit().putInt("tam", i).apply()
                dibujar()
            }
            fila("Posición vertical", listOf("Arriba", "Centro", "Abajo"), prefs.getInt("posV", 0)) { i ->
                prefs.edit().putInt("posV", i).apply()
                dibujar()
            }
            fila("Posición horizontal", listOf("Izquierda", "Centro", "Derecha"), prefs.getInt("posH", 1)) { i ->
                prefs.edit().putInt("posH", i).apply()
                dibujar()
            }
            if (!Settings.canDrawOverlays(this)) {
                contenido.addView(boton("Activar permiso de aparecer encima", true) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)))
                })
            }
        } else {
            val nota = tarjeta(Color.parseColor("#0B2A33"), cian)
            nota.addView(texto("La emergente del sistema la maneja Android, por eso no se puede cambiar su tamaño, lugar ni colores. Para personalizarlos elige Letrero propio.", 13f, gris))
            contenido.addView(nota)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.areNotificationsEnabled()) {
                contenido.addView(boton("Activar avisos emergentes", true) {
                    val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    i.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    startActivity(i)
                })
            }
        }

        contenido.addView(boton("Probar aviso ahora", true) {
            Aviso.mostrar(this, "Bre-B", "$ 15.000", "Nequi")
        })
    }

    private fun ajusteVoz() {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)

        fila("Frase que dice", listOf("Normal", "Tilín", "Corta", "Alegre"), prefs.getInt("frase", 0)) { i ->
            prefs.edit().putInt("frase", i).apply()
            hablarPrueba()
            dibujar()
        }

        val tono = prefs.getInt("tono", 100)
        val vel = prefs.getInt("vel", 100)
        val presets = listOf(100 to 100, 190 to 125, 55 to 80, 70 to 95)
        val sel = presets.indexOfFirst { it.first == tono && it.second == vel }
        fila("Voces divertidas", listOf("Normal", "Ardilla", "Gigante", "Robot"), sel) { i ->
            prefs.edit().putInt("tono", presets[i].first).putInt("vel", presets[i].second).apply()
            hablarPrueba()
            dibujar()
        }

        ajusteNum("Tono de la voz", "tono", 100, 50, 200, 10, "%")
        ajusteNum("Velocidad de la voz", "vel", 100, 50, 200, 10, "%")

        val voces: List<Voice> = try {
            tts?.voices?.filter { it.locale.language == "es" }?.sortedBy { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val nombreActual = prefs.getString("vozNombre", "") ?: ""
        val tv = tarjeta(panel)
        tv.addView(texto("Voz del aviso", 14f, gris))
        val opciones = mutableListOf<Pair<String, String>>()
        opciones.add(Pair("Automática", ""))
        val contadores = HashMap<String, Int>()
        for (v in voces.take(8)) {
            val reg = when (v.locale.country) {
                "ES" -> "España"
                "US" -> "Latinoamérica"
                "MX" -> "México"
                "CO" -> "Colombia"
                "AR" -> "Argentina"
                else -> v.locale.country
            }
            val k = (contadores[reg] ?: 0) + 1
            contadores[reg] = k
            opciones.add(Pair("Voz de " + reg + " " + k, v.name))
        }
        for (op in opciones) {
            val b = Button(this)
            b.text = op.first
            b.isAllCaps = false
            b.textSize = 14f
            b.setTypeface(null, Typeface.BOLD)
            val seleccionada = op.second == nombreActual
            b.setTextColor(if (seleccionada) Color.WHITE else cian)
            b.background = if (seleccionada) degradado() else caja(Color.TRANSPARENT, 14, borde)
            val p = LinearLayout.LayoutParams(-1, dp(42))
            p.setMargins(0, dp(8), 0, 0)
            b.layoutParams = p
            b.setOnClickListener {
                prefs.edit().putString("vozNombre", op.second).apply()
                hablarPrueba()
                dibujar()
            }
            tv.addView(b)
        }
        if (voces.isEmpty()) {
            val aviso = texto("Aún no cargan las voces en español. Sal y vuelve a entrar a Ajustes en unos segundos.", 12f, ambar)
            aviso.setPadding(0, dp(8), 0, 0)
            tv.addView(aviso)
        }
        contenido.addView(tv)

        contenido.addView(boton("Escuchar voz", true) { hablarPrueba() })
        contenido.addView(boton("Instalar o cambiar motor de voz", false) {
            try {
                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            } catch (e: Exception) {
                Toast.makeText(this, "Busca Texto a voz en los Ajustes del sistema", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun ajusteApps() {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val todas = prefs.getBoolean("todas", false)
        val activos = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val activo = activos.contains(packageName)

        fila("Qué apps lee", listOf("Bancos y las que actives", "Todas las apps"), if (todas) 1 else 0) { i ->
            prefs.edit().putBoolean("todas", i == 1).apply()
            dibujar()
        }

        val nota = tarjeta(Color.parseColor("#0B2A33"), cian)
        nota.addView(texto("Aquí salen las apps que mandaron una notificación con pinta de pago. Los bancos (Nequi, Bancolombia, Daviplata, Davivienda, Nu, BBVA...) se leen solos. Si sale otra app, como WhatsApp, déjala apagada: Rovika no la contará como pago. Actívala solo si es tu banco y no se reconoció solo.", 13f, gris))
        contenido.addView(nota)

        val vistas = Apps.vistas(this)
        val activadas = Apps.extras(this)
        val lista = tarjeta(panel)
        lista.addView(texto("Apps vistas", 14f, gris))
        if (vistas.isEmpty()) {
            val vacio = texto("Aún no hay apps vistas. Cuando llegue una notificación de pago, aparecerá aquí.", 13f, gris)
            vacio.setPadding(0, dp(8), 0, 0)
            lista.addView(vacio)
        } else {
            for (vv in vistas) lista.addView(filaApp(vv.first, vv.second, activadas))
        }
        contenido.addView(lista)

        if (activo) {
            contenido.addView(boton("Permisos de lectura de notificaciones", false) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
        }
    }

    private fun dibujarInicio() {
        val activos = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
        val activo = activos.contains(packageName)
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)

        val cab = LinearLayout(this)
        cab.orientation = LinearLayout.HORIZONTAL
        cab.gravity = Gravity.CENTER_VERTICAL
        val cl = LinearLayout.LayoutParams(-1, -2)
        cl.setMargins(0, 0, 0, dp(18))
        cab.layoutParams = cl
        val colT = LinearLayout(this)
        colT.orientation = LinearLayout.VERTICAL
        colT.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        val titulo = texto("ROVIKA", 28f, Color.WHITE, true)
        titulo.letterSpacing = 0.25f
        colT.addView(titulo)
        colT.addView(texto("Tus pagos, en voz alta", 14f, cian))
        cab.addView(colT)
        val pill = texto(if (activo) "● Activo" else "● Sin permiso", 13f, if (activo) verde else ambar, true)
        pill.setPadding(dp(14), dp(8), dp(14), dp(8))
        pill.background = caja(
            if (activo) Color.parseColor("#0B2A33") else Color.parseColor("#3A2A12"),
            20,
            if (activo) cian else ambar
        )
        cab.addView(pill)
        contenido.addView(cab)

        bannerActualizacion()

        if (!activo) {
            contenido.addView(boton("Dar permiso de notificaciones", true) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
        }

        val hoy = hoyStr()
        val dia = SimpleDateFormat("yyyyMMdd", Locale.US)
        val hora = SimpleDateFormat("h:mm a", Locale("es", "CO"))
        val todos = leerPagos()
        val pagos = todos.filter { dia.format(Date(it.ts)) == hoy }
        val total = pagos.fold(0L) { a, p -> a + p.monto }
        val meta = prefs.getLong("meta", 150000L)
        val pct = (total * 100L / meta).coerceAtMost(100L).toInt()

        val hero = LinearLayout(this)
        hero.orientation = LinearLayout.VERTICAL
        hero.background = heroFondo()
        hero.setPadding(dp(22), dp(20), dp(22), dp(20))
        val hp = LinearLayout.LayoutParams(-1, -2)
        hp.setMargins(0, 0, 0, dp(12))
        hero.layoutParams = hp
        hero.addView(texto("Hoy llevas", 14f, Color.parseColor("#B8C7E6")))
        val tvTotal = texto("$ " + Aviso.fmt(total), 44f, Color.WHITE, true)
        hero.addView(tvTotal)
        val chips = LinearLayout(this)
        chips.orientation = LinearLayout.HORIZONTAL
        chips.addView(texto(pagos.size.toString() + (if (pagos.size == 1) " pago" else " pagos"), 14f, verde, true))
        chips.addView(texto("   ·   ", 14f, gris))
        chips.addView(texto(
            if (pct >= 100) "🎉 Meta cumplida" else pct.toString() + "% de la meta",
            14f, if (pct >= 100) verde else cian, true
        ))
        hero.addView(chips)
        contenido.addView(hero)

        val desde = ultimoTotal
        ultimoTotal = total
        if (desde != total) {
            val a = ValueAnimator.ofFloat(desde.toFloat(), total.toFloat())
            a.duration = 900
            a.addUpdateListener {
                tvTotal.text = "$ " + Aviso.fmt((it.animatedValue as Float).toLong())
            }
            a.start()
        }

        val tMeta = tarjeta(panel)
        tMeta.setOnClickListener { pedirMeta() }
        tMeta.addView(texto("Meta del día", 14f, gris))
        tMeta.addView(texto("$ " + Aviso.fmt(total) + " de $ " + Aviso.fmt(meta), 18f, Color.WHITE, true))
        val pista = LinearLayout(this)
        pista.orientation = LinearLayout.HORIZONTAL
        pista.weightSum = 100f
        pista.background = caja(Color.parseColor("#17233B"), 8)
        val pl = LinearLayout.LayoutParams(-1, dp(10))
        pl.setMargins(0, dp(10), 0, dp(8))
        pista.layoutParams = pl
        if (pct > 0) {
            val rell = View(this)
            rell.background = degradado()
            rell.layoutParams = LinearLayout.LayoutParams(0, -1, pct.toFloat())
            pista.addView(rell)
        }
        tMeta.addView(pista)
        tMeta.addView(texto("Toca para cambiar la meta", 12f, gris))
        contenido.addView(tMeta)

        val vals = mutableListOf<Long>()
        val etiquetas = mutableListOf<String>()
        val porDia = todos.groupBy { dia.format(Date(it.ts)) }
        val nombreDia = SimpleDateFormat("EEE", Locale("es", "CO"))
        var totalSemana = 0L
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val clave = dia.format(cal.time)
            val suma = (porDia[clave] ?: emptyList()).fold(0L) { a, p -> a + p.monto }
            vals.add(suma)
            totalSemana += suma
            val nd = nombreDia.format(cal.time)
            etiquetas.add(nd.substring(0, 1).uppercase())
        }
        val tSem = tarjeta(panel)
        tSem.addView(texto("Últimos 7 días", 14f, gris))
        tSem.addView(texto("$ " + Aviso.fmt(totalSemana), 24f, Color.WHITE, true))
        val grafica = Barras(this, vals, etiquetas)
        val gl = LinearLayout.LayoutParams(-1, dp(150))
        gl.setMargins(0, dp(10), 0, 0)
        grafica.layoutParams = gl
        tSem.addView(grafica)
        contenido.addView(tSem)

        filaBotones(
            botonPeq("🔊 Probar aviso", activo) {
                hablarPrueba()
                Aviso.mostrar(this, "Bre-B", "$ 15.000", "Nequi")
            },
            botonPeq("📤 Compartir resumen", false) {
                compartir(resumenHoy())
            }
        )

        val h1 = texto("Pagos de hoy", 18f, Color.WHITE, true)
        h1.setPadding(0, dp(10), 0, dp(10))
        contenido.addView(h1)

        if (pagos.isEmpty()) {
            val vacio = texto("Aún no hay pagos hoy.", 14f, gris)
            vacio.setPadding(0, 0, 0, dp(14))
            contenido.addView(vacio)
        } else {
            for (p in pagos) contenido.addView(filaPago(p, hora))
        }

        encabezado("🧾", "Registro de notificaciones", "Lo último que Rovika vio de tus bancos", logAbierto) {
            logAbierto = !logAbierto
            dibujar()
        }
        if (logAbierto) {
            val log = prefs.getString("log", "") ?: ""
            val t = tarjeta(panel)
            val lv = texto(if (log.isBlank()) "Todavía no hay notificaciones." else log, 11f, Color.parseColor("#7C89A3"))
            lv.setTextIsSelectable(true)
            t.addView(lv)
            contenido.addView(t)
        }
    }

    private fun dibujarMov() {
        contenido.addView(texto("Movimientos", 28f, Color.WHITE, true))
        val sub = texto("Pagos por día", 14f, cian)
        sub.setPadding(0, dp(2), 0, dp(14))
        contenido.addView(sub)

        filaBotones(
            botonPeq("💾 Guardar copia", false) {
                val datos = getSharedPreferences("tilin", MODE_PRIVATE).getString("pagos", "") ?: ""
                if (datos.isBlank()) {
                    Toast.makeText(this, "Aún no hay movimientos para guardar", Toast.LENGTH_LONG).show()
                } else {
                    compartir(datos)
                }
            },
            botonPeq("📥 Restaurar copia", false) { pedirRestaurar() }
        )

        val et = EditText(this)
        et.hint = "Buscar por banco o valor"
        et.setHintTextColor(Color.parseColor("#5B6781"))
        et.setTextColor(Color.WHITE)
        et.textSize = 15f
        et.isSingleLine = true
        et.background = caja(panel, 16, borde)
        et.setPadding(dp(16), dp(12), dp(16), dp(12))
        val el = LinearLayout.LayoutParams(-1, -2)
        el.setMargins(0, dp(6), 0, dp(14))
        et.layoutParams = el
        et.setText(filtro)
        contenido.addView(et)

        listaMov = LinearLayout(this)
        listaMov.orientation = LinearLayout.VERTICAL
        contenido.addView(listaMov)

        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filtro = s?.toString() ?: ""
                pintarLista()
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        pintarLista()
    }

    private fun pintarLista() {
        listaMov.removeAllViews()
        val dia = SimpleDateFormat("yyyyMMdd", Locale.US)
        val hora = SimpleDateFormat("h:mm a", Locale("es", "CO"))
        val larga = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "CO"))
        val hoy = hoyStr()
        val ayer = dia.format(Date(System.currentTimeMillis() - 86400000L))
        val f = filtro.trim().lowercase()

        val pagos = leerPagos().filter {
            f.isEmpty() ||
                it.banco.lowercase().contains(f) ||
                it.app.lowercase().contains(f) ||
                it.monto.toString().contains(f)
        }
        if (pagos.isEmpty()) {
            listaMov.addView(texto(if (f.isEmpty()) "Aún no hay movimientos." else "No hay resultados.", 14f, gris))
            return
        }

        val grupos = pagos.groupBy { dia.format(Date(it.ts)) }
        for (entrada in grupos.entries) {
            val clave = entrada.key
            val lista = entrada.value
            val etiqueta = if (clave == hoy) "Hoy"
            else if (clave == ayer) "Ayer"
            else {
                val txt = larga.format(Date(lista[0].ts))
                txt.substring(0, 1).uppercase() + txt.substring(1)
            }
            val total = lista.fold(0L) { a, p -> a + p.monto }

            val cab = tarjeta(Color.parseColor("#0B2A33"), cian)
            cab.addView(texto(etiqueta, 16f, Color.WHITE, true))
            cab.addView(texto("$ " + Aviso.fmt(total), 26f, cian, true))
            cab.addView(texto(lista.size.toString() + " pagos", 13f, gris))
            listaMov.addView(cab)

            for (p in lista) listaMov.addView(filaPago(p, hora))
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Voz.aplicar(tts, this)
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
