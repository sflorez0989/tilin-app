package com.tilin.app

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BarrasSel(
    c: Context,
    private val vals: List<Long>,
    private val etiquetas: List<String>,
    private val sel: Int,
    private val alSeleccionar: (Int) -> Unit
) : View(c) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var ultimoX = 0f

    init {
        isClickable = true
        setOnClickListener {
            val n = vals.size
            if (n > 0 && width > 0) {
                val i = (ultimoX / (width.toFloat() / n)).toInt().coerceIn(0, n - 1)
                alSeleccionar(i)
            }
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        ultimoX = e.x
        return super.onTouchEvent(e)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val base = height.toFloat() - 22f * d
        val tope = 6f * d
        val max = (vals.maxOrNull() ?: 0L).coerceAtLeast(1L).toFloat()
        val n = vals.size
        if (n == 0) return
        val slot = w / n
        val bw = slot * 0.5f
        for (i in 0 until n) {
            val x = slot * i + (slot - bw) / 2f
            var alto = (base - tope) * vals[i].toFloat() / max
            if (alto < 3f * d) alto = 3f * d
            val dest = if (sel >= 0) i == sel else i == n - 1
            p.shader = LinearGradient(
                0f, base - alto, 0f, base,
                Color.parseColor(if (dest) "#22D3EE" else "#2F6BFF"),
                Color.parseColor(if (dest) "#2F6BFF" else "#1B2F66"),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(RectF(x, base - alto, x + bw, base), 6f * d, 6f * d, p)
            p.shader = null
            p.color = Color.parseColor(if (dest) "#22D3EE" else "#8A97B0")
            p.textSize = 11f * d
            p.textAlign = Paint.Align.CENTER
            p.typeface = if (dest) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(etiquetas[i], x + bw / 2f, height.toFloat() - 5f * d, p)
        }
    }
}

object Dia {
    var sel = -1
    private val diaFmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    private val gris = Color.parseColor("#8A97B0")
    private val verde = Color.parseColor("#2EE59D")
    private val rojo = Color.parseColor("#FF6B6B")
    private val cian = Color.parseColor("#22D3EE")

    private fun t(a: Context, s: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val v = TextView(a)
        v.text = s
        v.textSize = size
        v.setTextColor(color)
        if (bold) v.setTypeface(null, Typeface.BOLD)
        return v
    }

    fun detalle(a: Activity, card: LinearLayout) {
        val dens = a.resources.displayMetrics.density
        if (sel < 0) {
            val h = t(a, "Toca una barra para ver los pagos de ese día", 12f, gris)
            h.setPadding(0, (8 * dens).toInt(), 0, 0)
            card.addView(h)
            return
        }
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -(6 - sel))
        val clave = diaFmt.format(cal.time)
        val etq = if (sel == 6) "Hoy"
        else if (sel == 5) "Ayer"
        else {
            val s = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "CO")).format(cal.time)
            s.substring(0, 1).uppercase() + s.substring(1)
        }
        val prefs = a.getSharedPreferences("tilin", Context.MODE_PRIVATE)
        val pagos = mutableListOf<Triple<Long, Long, String>>()
        for (linea in (prefs.getString("pagos", "") ?: "").split("\n")) {
            if (linea.isBlank()) continue
            val p = linea.split("|")
            if (p.size < 3) continue
            val ts = p[0].toLongOrNull() ?: continue
            if (diaFmt.format(Date(ts)) != clave) continue
            val banco = if (p.size > 4) p[4] else p[2]
            pagos.add(Triple(ts, p[1].toLongOrNull() ?: 0L, banco))
        }
        pagos.sortByDescending { it.first }
        var gastos = 0L
        for (linea in (prefs.getString("gastos", "") ?: "").split("\n")) {
            if (linea.isBlank()) continue
            val p = linea.split("|")
            if (p.size < 3) continue
            val ts = p[0].toLongOrNull() ?: continue
            if (diaFmt.format(Date(ts)) == clave) gastos += (p[1].toLongOrNull() ?: 0L)
        }
        var total = 0L
        for (x in pagos) total += x.second

        val sep = View(a)
        sep.setBackgroundColor(Color.parseColor("#1F2F4D"))
        val sl = LinearLayout.LayoutParams(-1, (1 * dens).toInt().coerceAtLeast(1))
        sl.setMargins(0, (12 * dens).toInt(), 0, (12 * dens).toInt())
        sep.layoutParams = sl
        card.addView(sep)

        card.addView(t(a, etq, 16f, Color.WHITE, true))
        card.addView(t(a, "$ " + Aviso.fmt(total), 26f, cian, true))
        card.addView(t(a, pagos.size.toString() + (if (pagos.size == 1) " pago" else " pagos"), 13f, verde, true))
        if (gastos > 0L) {
            val neto = total - gastos
            val g = t(
                a,
                "Gastos $ " + Aviso.fmt(gastos) + "  ·  Neto " + (if (neto < 0L) "- $ " + Aviso.fmt(-neto) else "$ " + Aviso.fmt(neto)),
                13f, rojo, true
            )
            g.setPadding(0, (2 * dens).toInt(), 0, 0)
            card.addView(g)
        }
        if (pagos.isEmpty()) {
            val v = t(a, "Sin pagos este día.", 13f, gris)
            v.setPadding(0, (8 * dens).toInt(), 0, 0)
            card.addView(v)
        } else {
            val hora = SimpleDateFormat("h:mm a", Locale("es", "CO"))
            for (x in pagos.take(15)) {
                val fila = LinearLayout(a)
                fila.orientation = LinearLayout.HORIZONTAL
                fila.setPadding(0, (7 * dens).toInt(), 0, 0)
                val b = t(a, x.third, 14f, Color.WHITE, true)
                b.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                fila.addView(b)
                val hr = t(a, hora.format(Date(x.first)), 12f, gris)
                hr.setPadding(0, 0, (10 * dens).toInt(), 0)
                fila.addView(hr)
                fila.addView(t(a, "+ $ " + Aviso.fmt(x.second), 15f, verde, true))
                card.addView(fila)
            }
            if (pagos.size > 15) {
                val m = t(a, "y " + (pagos.size - 15) + " más: míralos en Movimientos", 12f, gris)
                m.setPadding(0, (8 * dens).toInt(), 0, 0)
                card.addView(m)
            }
        }
        val c = t(a, "Toca la misma barra otra vez para cerrar", 11f, gris)
        c.setPadding(0, (10 * dens).toInt(), 0, 0)
        card.addView(c)
    }
}
