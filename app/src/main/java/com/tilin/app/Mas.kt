package com.tilin.app

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
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

class Reg(val ts: Long, val monto: Long, val etiqueta: String, val nota: String)

object Mas {
    private var seccion = 0
    private var mesDesp = 0
    private var catSel = 0
    private var borrador = ""
    private var scrollRef: ScrollView? = null
    private val h = Handler(Looper.getMainLooper())
    private var lazo: Runnable? = null

    private val gris = Color.parseColor("#8A97B0")
    private val verde = Color.parseColor("#2EE59D")
    private val rojo = Color.parseColor("#FF6B6B")
    private val cian = Color.parseColor("#22D3EE")
    private val azul = Color.parseColor("#2F6BFF")
    private val panel = Color.parseColor("#0F1626")
    private val borde = Color.parseColor("#1F2F4D")

    private val categorias = listOf("⛽ Gasolina", "🛣️ Peaje", "🧽 Lavada", "🍔 Comida", "🅿️ Parqueo", "🔧 Otro")
    private val diaFmt = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun caja(c: Context, color: Int, radio: Int, bordeColor: Int = 0): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(c, radio).toFloat()
        if (bordeColor != 0) g.setStroke(dp(c, 1), bordeColor)
        return g
    }

    fun degradado(c: Context): GradientDrawable {
        val g = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(azul, cian))
        g.cornerRadius = dp(c, 16).toFloat()
        return g
    }

    fun texto(c: Context, t: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val v = TextView(c)
        v.text = t
        v.textSize = size
        v.setTextColor(color)
        if (bold) v.setTypeface(null, Typeface.BOLD)
        return v
    }

    fun tarjeta(c: Context, color: Int = panel, bordeColor: Int = borde): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.background = caja(c, color, 22, bordeColor)
        l.setPadding(dp(c, 18), dp(c, 16), dp(c, 18), dp(c, 16))
        val p = LinearLayout.LayoutParams(-1, -2)
        p.setMargins(0, 0, 0, dp(c, 12))
        l.layoutParams = p
        return l
    }

    fun hero(c: Context): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        val g = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.parseColor("#14327A"), Color.parseColor("#0B2A33"))
        )
        g.cornerRadius = dp(c, 26).toFloat()
        g.setStroke(dp(c, 1), azul)
        l.background = g
        l.setPadding(dp(c, 22), dp(c, 20), dp(c, 22), dp(c, 20))
        val p = LinearLayout.LayoutParams(-1, -2)
        p.setMargins(0, 0, 0, dp(c, 12))
        l.layoutParams = p
        return l
    }

    fun boton(c: Context, t: String, lleno: Boolean, accion: () -> Unit): Button {
        val b = Button(c)
        b.text = t
        b.isAllCaps = false
        b.textSize = 16f
        b.setTypeface(null, Typeface.BOLD)
        b.setTextColor(if (lleno) Color.WHITE else cian)
        b.background = if (lleno) degradado(c) else caja(c, Color.TRANSPARENT, 16, cian)
        b.setOnClickListener { accion() }
        val p = LinearLayout.LayoutParams(-1, dp(c, 52))
        p.setMargins(0, 0, 0, dp(c, 10))
        b.layoutParams = p
        return b
    }

    fun botonPeq(c: Context, t: String, lleno: Boolean, accion: () -> Unit): Button {
        val b = Button(c)
        b.text = t
        b.isAllCaps = false
        b.textSize = 14f
        b.setTypeface(null, Typeface.BOLD)
        b.setTextColor(if (lleno) Color.WHITE else cian)
        b.background = if (lleno) degradado(c) else caja(c, Color.TRANSPARENT, 16, cian)
        b.setOnClickListener { accion() }
        val p = LinearLayout.LayoutParams(0, dp(c, 50), 1f)
        p.setMargins(dp(c, 4), 0, dp(c, 4), 0)
        b.layoutParams = p
        return b
    }

    fun filaBotones(c: Context, cont: LinearLayout, vararg bs: Button) {
        val f = LinearLayout(c)
        f.orientation = LinearLayout.HORIZONTAL
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.setMargins(0, 0, 0, dp(c, 10))
        f.layoutParams = lp
        for (b in bs) f.addView(b)
        cont.addView(f)
    }

    fun prefs(c: Context) = c.getSharedPreferences("tilin", Context.MODE_PRIVATE)

    fun diaDe(ts: Long): String = diaFmt.format(Date(ts))

    fun suma(l: List<Reg>): Long {
        var s = 0L
        for (r in l) s += r.monto
        return s
    }

    fun leerPagos(c: Context): List<Reg> {
        val r = mutableListOf<Reg>()
        for (linea in (prefs(c).getString("pagos", "") ?: "").split("\n")) {
            if (linea.isBlank()) continue
            val p = linea.split("|")
            if (p.size < 3) continue
            val ts = p[0].toLongOrNull() ?: continue
            val monto = p[1].toLongOrNull() ?: 0L
            val banco = if (p.size > 4) p[4] else p[2]
            r.add(Reg(ts, monto, banco, p[2]))
        }
        return r
    }

    fun leerGastos(c: Context): List<Reg> {
        val r = mutableListOf<Reg>()
        for (linea in (prefs(c).getString("gastos", "") ?: "").split("\n")) {
            if (linea.isBlank()) continue
            val p = linea.split("|")
            if (p.size < 3) continue
            val ts = p[0].toLongOrNull() ?: continue
            val monto = p[1].toLongOrNull() ?: 0L
            val nota = if (p.size > 3) p[3] else ""
            r.add(Reg(ts, monto, p[2], nota))
        }
        return r
    }

    fun guardarGasto(c: Context, monto: Long, cat: String, nota: String) {
        val limpio = { s: String -> s.replace("|", " ").replace("\n", " ") }
        val actuales = (prefs(c).getString("gastos", "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
        actuales.add(0, System.currentTimeMillis().toString() + "|" + monto + "|" + limpio(cat) + "|" + limpio(nota))
        prefs(c).edit().putString("gastos", actuales.take(2000).joinToString("\n")).apply()
    }

    fun borrarGasto(c: Context, ts: Long) {
        val lineas = (prefs(c).getString("gastos", "") ?: "").split("\n")
            .filter { it.isNotBlank() && it.split("|")[0] != ts.toString() }
        prefs(c).edit().putString("gastos", lineas.joinToString("\n")).apply()
    }

    fun rangoMes(desp: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MONTH, desp)
        val ini = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return Pair(ini, cal.timeInMillis)
    }

    fun nombreMes(ts: Long): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(ts))

    fun hace(ts: Long): String {
        val m = (System.currentTimeMillis() - ts) / 60000L
        return if (m < 1L) "ahora mismo"
        else if (m < 60L) "hace " + m + " min"
        else if (m < 1440L) "hace " + (m / 60L) + " h"
        else SimpleDateFormat("d MMM, h:mm a", Locale("es", "CO")).format(Date(ts))
    }

    fun irA(n: Int, refrescar: () -> Unit) {
        seccion = n
        if (n == 2) mesDesp = 0
        scrollRef?.scrollTo(0, 0)
        refrescar()
    }

    fun dibujar(a: Activity, cont: LinearLayout, scroll: ScrollView, barra: View, refrescar: () -> Unit) {
        scrollRef = scroll
        if (seccion == 3) {
            barra.visibility = View.GONE
            a.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            manejo(a, cont, refrescar)
            iniciarLazo(a, refrescar)
            return
        }
        a.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (seccion == 1) gastos(a, cont, refrescar)
        else if (seccion == 2) resumen(a, cont, refrescar)
        else if (seccion == 4) acerca(a, cont, refrescar)
        else if (seccion == 5) Mas2.salidas(a, cont, refrescar)
        else if (seccion == 6) Mas2.opciones(a, cont, refrescar)
        else menu(a, cont, refrescar)
    }

    fun iniciarLazo(a: Activity, refrescar: () -> Unit) {
        if (lazo != null) return
        val r = object : Runnable {
            override fun run() {
                if (seccion != 3 || a.isFinishing || a.isDestroyed) {
                    lazo = null
                    return
                }
                if (a.hasWindowFocus()) refrescar()
                h.postDelayed(this, 3000)
            }
        }
        lazo = r
        h.postDelayed(r, 3000)
    }

    fun volver(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        val t = texto(a, "◀ Volver", 15f, cian, true)
        t.setPadding(0, 0, dp(a, 10), dp(a, 12))
        t.setOnClickListener { irA(0, refrescar) }
        cont.addView(t)
    }

    fun item(c: Context, cont: LinearLayout, icono: String, titulo: String, desc: String, alTocar: () -> Unit) {
        val t = tarjeta(c)
        t.setPadding(dp(c, 18), dp(c, 14), dp(c, 18), dp(c, 14))
        val fila = LinearLayout(c)
        fila.orientation = LinearLayout.HORIZONTAL
        fila.gravity = Gravity.CENTER_VERTICAL
        val ic = texto(c, icono, 26f, Color.WHITE)
        ic.setPadding(0, 0, dp(c, 14), 0)
        fila.addView(ic)
        val col = LinearLayout(c)
        col.orientation = LinearLayout.VERTICAL
        col.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        col.addView(texto(c, titulo, 17f, Color.WHITE, true))
        col.addView(texto(c, desc, 12f, gris))
        fila.addView(col)
        fila.addView(texto(c, "›", 24f, cian, true))
        t.addView(fila)
        t.setOnClickListener { alTocar() }
        cont.addView(t)
    }

    fun menu(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        cont.addView(texto(a, "Más", 28f, Color.WHITE, true))
        val sub = texto(a, "Herramientas para tu negocio", 14f, cian)
        sub.setPadding(0, dp(a, 2), 0, dp(a, 14))
        cont.addView(sub)
        item(a, cont, "🚗", "Modo manejo", "Letras gigantes con tu último pago") { irA(3, refrescar) }
        item(a, cont, "💸", "Gastos y ganancia neta", "Anota gasolina, peajes y más") { irA(1, refrescar) }
        item(a, cont, "📊", "Resumen del mes", "Totales, comparación y por banco") { irA(2, refrescar) }
        item(a, cont, "📤", "Exportar a Excel", "Guarda tus movimientos en un archivo") { dialogoExportar(a) }
        item(a, cont, "🔔", "Salidas por confirmar", Mas2.textoPendientes(a)) { irA(5, refrescar) }
        item(a, cont, "⚙️", "Opciones extra", "Sonido de caja y widget") { irA(6, refrescar) }
        item(a, cont, "ℹ️", "Acerca de Rovika", "Versión, privacidad y recomendar") { irA(4, refrescar) }
    }

    fun dialogoExportar(a: Activity) {
        val opciones = arrayOf("Este mes", "Mes anterior", "Todo el historial")
        AlertDialog.Builder(a)
            .setTitle("Exportar a Excel")
            .setItems(opciones) { _, i ->
                if (i == 2) {
                    exportar(a, 0L, Long.MAX_VALUE, "historial")
                } else {
                    val r = rangoMes(if (i == 0) 0 else -1)
                    exportar(a, r.first, r.second, nombreMes(r.first))
                }
            }
            .show()
    }

    fun compartir(a: Activity, txt: String) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TEXT, txt)
        a.startActivity(Intent.createChooser(i, "Compartir con"))
    }

    fun exportar(a: Activity, ini: Long, fin: Long, nombre: String) {
        try {
            val fFecha = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val fHora = SimpleDateFormat("HH:mm", Locale.US)
            val limpio = { s: String -> s.replace(";", ",").replace("\n", " ") }
            val pagos = leerPagos(a).filter { it.ts >= ini && it.ts < fin }.sortedBy { it.ts }
            val gastos = leerGastos(a).filter { it.ts >= ini && it.ts < fin }.sortedBy { it.ts }
            if (pagos.isEmpty() && gastos.isEmpty()) {
                Toast.makeText(a, "No hay movimientos para exportar", Toast.LENGTH_LONG).show()
                return
            }
            val sb = StringBuilder()
            sb.append("Tipo;Fecha;Hora;Banco o categoría;Detalle;Valor\n")
            for (p in pagos) {
                sb.append("Ingreso;").append(fFecha.format(Date(p.ts))).append(";")
                    .append(fHora.format(Date(p.ts))).append(";")
                    .append(limpio(p.etiqueta)).append(";")
                    .append(limpio(p.nota)).append(";")
                    .append(p.monto).append("\n")
            }
            for (g in gastos) {
                sb.append("Gasto;").append(fFecha.format(Date(g.ts))).append(";")
                    .append(fHora.format(Date(g.ts))).append(";")
                    .append(limpio(g.etiqueta)).append(";")
                    .append(limpio(g.nota)).append(";")
                    .append(-g.monto).append("\n")
            }
            val csv = sb.toString()
            val archivo = "Rovika_" + nombre + ".csv"
            var guardado = false
            if (Build.VERSION.SDK_INT >= 29) {
                val v = ContentValues()
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, archivo)
                v.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                val uri = a.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                if (uri != null) {
                    val os = a.contentResolver.openOutputStream(uri)
                    if (os != null) {
                        os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        os.write(csv.toByteArray(Charsets.UTF_8))
                        os.close()
                        guardado = true
                    }
                }
            }
            if (guardado) {
                Toast.makeText(a, "Guardado en Descargas: " + archivo + ". Ábrelo con Excel o Google Sheets.", Toast.LENGTH_LONG).show()
            } else {
                compartir(a, csv)
            }
        } catch (e: Exception) {
            Toast.makeText(a, "No pude guardar el archivo. Intenta de nuevo.", Toast.LENGTH_LONG).show()
        }
    }

    fun filaBarra(c: Context, cont: LinearLayout, etiqueta: String, valor: Long, max: Long) {
        val f = LinearLayout(c)
        f.orientation = LinearLayout.VERTICAL
        val lf = LinearLayout.LayoutParams(-1, -2)
        lf.setMargins(0, dp(c, 8), 0, 0)
        f.layoutParams = lf
        val l = LinearLayout(c)
        l.orientation = LinearLayout.HORIZONTAL
        val et = texto(c, etiqueta, 14f, Color.WHITE)
        et.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        l.addView(et)
        l.addView(texto(c, "$ " + Aviso.fmt(valor), 14f, cian, true))
        f.addView(l)
        val pista = LinearLayout(c)
        pista.orientation = LinearLayout.HORIZONTAL
        pista.weightSum = 100f
        pista.background = caja(c, Color.parseColor("#17233B"), 8)
        val pl = LinearLayout.LayoutParams(-1, dp(c, 8))
        pl.setMargins(0, dp(c, 4), 0, 0)
        pista.layoutParams = pl
        val pct = if (max > 0L) (valor * 100L / max).toInt().coerceIn(0, 100) else 0
        if (pct > 0) {
            val r = View(c)
            r.background = degradado(c)
            r.layoutParams = LinearLayout.LayoutParams(0, -1, pct.toFloat())
            pista.addView(r)
        }
        f.addView(pista)
        cont.addView(f)
    }

    fun resumen(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        volver(a, cont, refrescar)
        val rango = rangoMes(mesDesp)
        val ini = rango.first
        val fin = rango.second
        val todos = leerPagos(a)
        val pagos = todos.filter { it.ts >= ini && it.ts < fin }
        val gastos = leerGastos(a).filter { it.ts >= ini && it.ts < fin }
        val total = suma(pagos)
        val totalGastos = suma(gastos)
        val neto = total - totalGastos
        val prev = rangoMes(mesDesp - 1)
        val prevTotal = suma(todos.filter { it.ts >= prev.first && it.ts < prev.second })

        val nombre = SimpleDateFormat("MMMM yyyy", Locale("es", "CO")).format(Date(ini))
        cont.addView(texto(a, nombre.substring(0, 1).uppercase() + nombre.substring(1), 26f, Color.WHITE, true))
        val sub = texto(a, "Resumen del mes", 14f, cian)
        sub.setPadding(0, dp(a, 2), 0, dp(a, 12))
        cont.addView(sub)

        filaBotones(
            a, cont,
            botonPeq(a, "◀ Anterior", false) {
                mesDesp -= 1
                refrescar()
            },
            botonPeq(a, "Siguiente ▶", false) {
                if (mesDesp < 0) {
                    mesDesp += 1
                    refrescar()
                }
            }
        )

        val hr = hero(a)
        hr.addView(texto(a, "Ingresos del mes", 14f, Color.parseColor("#B8C7E6")))
        hr.addView(texto(a, "$ " + Aviso.fmt(total), 40f, Color.WHITE, true))
        if (prevTotal > 0L) {
            val d = (total - prevTotal) * 100L / prevTotal
            if (d >= 0L) hr.addView(texto(a, "▲ " + d + "% más que el mes anterior", 14f, verde, true))
            else hr.addView(texto(a, "▼ " + (-d) + "% menos que el mes anterior", 14f, rojo, true))
        } else {
            hr.addView(texto(a, "Sin datos del mes anterior para comparar", 13f, gris))
        }
        cont.addView(hr)

        val porDia = HashMap<String, Long>()
        for (p in pagos) {
            val k = diaDe(p.ts)
            porDia[k] = (porDia[k] ?: 0L) + p.monto
        }
        val dias = porDia.size
        val prom = if (dias > 0) total / dias else 0L
        var mejorK = ""
        var mejorV = 0L
        for (e in porDia.entries) {
            if (e.value > mejorV) {
                mejorV = e.value
                mejorK = e.key
            }
        }

        val est = tarjeta(a)
        est.addView(texto(a, "Tu mes en números", 14f, gris))
        est.addView(texto(a, pagos.size.toString() + " pagos en " + dias + " días trabajados", 16f, Color.WHITE, true))
        est.addView(texto(a, "Promedio por día: $ " + Aviso.fmt(prom), 15f, Color.parseColor("#C9D3E8")))
        if (mejorV > 0L) {
            val numDia = mejorK.substring(6).toInt()
            est.addView(texto(a, "Mejor día: el " + numDia + " con $ " + Aviso.fmt(mejorV), 15f, verde, true))
        }
        cont.addView(est)

        val semanas = LongArray(5)
        for (p in pagos) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = p.ts
            val idx = ((cal.get(Calendar.DAY_OF_MONTH) - 1) / 7).coerceIn(0, 4)
            semanas[idx] += p.monto
        }
        var maxSem = 0L
        for (v in semanas) if (v > maxSem) maxSem = v
        val etiquetasSem = listOf("Días 1 al 7", "Días 8 al 14", "Días 15 al 21", "Días 22 al 28", "Días 29 al 31")
        val cs = tarjeta(a)
        cs.addView(texto(a, "Por semana", 14f, gris))
        for (i in 0..4) filaBarra(a, cs, etiquetasSem[i], semanas[i], maxSem)
        cont.addView(cs)

        val porBanco = HashMap<String, Long>()
        for (p in pagos) porBanco[p.etiqueta] = (porBanco[p.etiqueta] ?: 0L) + p.monto
        val ordenados = porBanco.entries.sortedByDescending { it.value }.take(6)
        val cb = tarjeta(a)
        cb.addView(texto(a, "Por banco o medio de pago", 14f, gris))
        if (ordenados.isEmpty()) {
            cb.addView(texto(a, "Aún no hay pagos en este mes.", 13f, gris))
        } else {
            val maxB = ordenados[0].value
            for (e in ordenados) filaBarra(a, cb, e.key, e.value, maxB)
        }
        cont.addView(cb)

        val cg = tarjeta(a)
        cg.addView(texto(a, "Gastos y ganancia neta", 14f, gris))
        cg.addView(texto(a, "Gastos del mes: $ " + Aviso.fmt(totalGastos), 16f, rojo, true))
        val netoTxt = if (neto < 0L) "- $ " + Aviso.fmt(-neto) else "$ " + Aviso.fmt(neto)
        cg.addView(texto(a, "Ganancia neta: " + netoTxt, 18f, if (neto >= 0L) verde else rojo, true))
        cont.addView(cg)

        cont.addView(boton(a, "📤 Exportar este mes a Excel", true) {
            exportar(a, ini, fin, nombreMes(ini))
        })
    }

    fun confirmarBorrar(a: Activity, r: Reg, refrescar: () -> Unit) {
        AlertDialog.Builder(a)
            .setTitle("¿Borrar este gasto?")
            .setMessage(r.etiqueta + " · $ " + Aviso.fmt(r.monto))
            .setPositiveButton("Borrar") { _, _ ->
                borrarGasto(a, r.ts)
                refrescar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun gastos(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        volver(a, cont, refrescar)
        cont.addView(texto(a, "Gastos y ganancia neta", 24f, Color.WHITE, true))
        val sub = texto(a, "Anota lo que gastas y Rovika te dice cuánto ganaste de verdad", 13f, gris)
        sub.setPadding(0, dp(a, 2), 0, dp(a, 14))
        cont.addView(sub)

        val hoy = diaDe(System.currentTimeMillis())
        val pagosHoy = leerPagos(a).filter { diaDe(it.ts) == hoy }
        val gastosHoy = leerGastos(a).filter { diaDe(it.ts) == hoy }
        val ing = suma(pagosHoy)
        val gas = suma(gastosHoy)
        val neto = ing - gas

        val hr = hero(a)
        hr.addView(texto(a, "Ganancia neta de hoy", 14f, Color.parseColor("#B8C7E6")))
        val netoTxt = if (neto < 0L) "- $ " + Aviso.fmt(-neto) else "$ " + Aviso.fmt(neto)
        hr.addView(texto(a, netoTxt, 40f, if (neto >= 0L) Color.WHITE else rojo, true))
        hr.addView(texto(a, "Ingresos $ " + Aviso.fmt(ing) + "  ·  Gastos $ " + Aviso.fmt(gas), 14f, verde, true))
        cont.addView(hr)

        val form = tarjeta(a)
        form.addView(texto(a, "Nuevo gasto", 14f, gris))
        for (fila in 0..1) {
            val f = LinearLayout(a)
            f.orientation = LinearLayout.HORIZONTAL
            val lp = LinearLayout.LayoutParams(-1, -2)
            lp.setMargins(0, dp(a, 8), 0, 0)
            f.layoutParams = lp
            for (j in 0..2) {
                val idx = fila * 3 + j
                val b = Button(a)
                b.text = categorias[idx]
                b.isAllCaps = false
                b.textSize = 13f
                b.setTypeface(null, Typeface.BOLD)
                val sel = idx == catSel
                b.setTextColor(if (sel) Color.WHITE else cian)
                b.background = if (sel) degradado(a) else caja(a, Color.TRANSPARENT, 14, borde)
                val p = LinearLayout.LayoutParams(0, dp(a, 46), 1f)
                p.setMargins(dp(a, 3), 0, dp(a, 3), 0)
                b.layoutParams = p
                b.setOnClickListener {
                    catSel = idx
                    refrescar()
                }
                f.addView(b)
            }
            form.addView(f)
        }

        val et = EditText(a)
        et.hint = "Valor del gasto (ej: 30000)"
        et.setHintTextColor(Color.parseColor("#5B6781"))
        et.setTextColor(Color.WHITE)
        et.textSize = 18f
        et.inputType = InputType.TYPE_CLASS_NUMBER
        et.background = caja(a, panel, 16, borde)
        et.setPadding(dp(a, 16), dp(a, 12), dp(a, 16), dp(a, 12))
        val elp = LinearLayout.LayoutParams(-1, -2)
        elp.setMargins(0, dp(a, 12), 0, dp(a, 12))
        et.layoutParams = elp
        et.setText(borrador)
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                borrador = s?.toString() ?: ""
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, cn: Int, af: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, cn: Int) {}
        })
        form.addView(et)

        form.addView(boton(a, "➕ Agregar gasto", true) {
            val v = borrador.filter { it.isDigit() }.toLongOrNull() ?: 0L
            if (v <= 0L) {
                Toast.makeText(a, "Escribe el valor del gasto", Toast.LENGTH_SHORT).show()
            } else {
                guardarGasto(a, v, categorias[catSel], "")
                borrador = ""
                Toast.makeText(a, "Gasto anotado ✔", Toast.LENGTH_SHORT).show()
                refrescar()
            }
        })
        cont.addView(form)

        val h1 = texto(a, "Gastos de hoy", 18f, Color.WHITE, true)
        h1.setPadding(0, dp(a, 6), 0, dp(a, 10))
        cont.addView(h1)
        if (gastosHoy.isEmpty()) {
            cont.addView(texto(a, "Aún no anotas gastos hoy.", 14f, gris))
        } else {
            val fHora = SimpleDateFormat("h:mm a", Locale("es", "CO"))
            for (g in gastosHoy) {
                val f = tarjeta(a)
                val fila = LinearLayout(a)
                fila.orientation = LinearLayout.HORIZONTAL
                fila.gravity = Gravity.CENTER_VERTICAL
                val col = LinearLayout(a)
                col.orientation = LinearLayout.VERTICAL
                col.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                col.addView(texto(a, g.etiqueta, 16f, Color.WHITE, true))
                col.addView(texto(a, fHora.format(Date(g.ts)), 12f, gris))
                fila.addView(col)
                fila.addView(texto(a, "- $ " + Aviso.fmt(g.monto), 18f, rojo, true))
                val x = texto(a, "✕", 20f, gris, true)
                x.setPadding(dp(a, 14), dp(a, 4), dp(a, 4), dp(a, 4))
                x.setOnClickListener { confirmarBorrar(a, g, refrescar) }
                fila.addView(x)
                f.addView(fila)
                cont.addView(f)
            }
        }
    }

    fun manejo(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        val top = LinearLayout(a)
        top.orientation = LinearLayout.HORIZONTAL
        top.gravity = Gravity.CENTER_VERTICAL
        val tl = LinearLayout.LayoutParams(-1, -2)
        tl.setMargins(0, 0, 0, dp(a, 14))
        top.layoutParams = tl
        val tt = texto(a, "MODO MANEJO", 14f, cian, true)
        tt.letterSpacing = 0.2f
        tt.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        top.addView(tt)
        val salir = texto(a, "✕ Salir", 18f, Color.WHITE, true)
        salir.setPadding(dp(a, 14), dp(a, 8), dp(a, 4), dp(a, 8))
        salir.setOnClickListener { irA(0, refrescar) }
        top.addView(salir)
        cont.addView(top)

        val hoy = diaDe(System.currentTimeMillis())
        val todos = leerPagos(a)
        val pagosHoy = todos.filter { diaDe(it.ts) == hoy }
        val gastosHoy = leerGastos(a).filter { diaDe(it.ts) == hoy }
        val total = suma(pagosHoy)
        val neto = total - suma(gastosHoy)

        cont.addView(texto(a, "Hoy llevas", 20f, Color.parseColor("#B8C7E6")))
        val tTxt = "$ " + Aviso.fmt(total)
        cont.addView(texto(a, tTxt, if (tTxt.length > 9) 46f else 64f, Color.WHITE, true))
        val resumenTxt = pagosHoy.size.toString() + (if (pagosHoy.size == 1) " pago" else " pagos") +
            "  ·  neto $ " + Aviso.fmt(neto)
        val rt = texto(a, resumenTxt, 18f, verde, true)
        rt.setPadding(0, dp(a, 4), 0, dp(a, 14))
        cont.addView(rt)

        val meta = prefs(a).getLong("meta", 150000L)
        val pct = if (meta > 0L) (total * 100L / meta).toInt().coerceIn(0, 100) else 0
        val pista = LinearLayout(a)
        pista.orientation = LinearLayout.HORIZONTAL
        pista.weightSum = 100f
        pista.background = caja(a, Color.parseColor("#17233B"), 10)
        val pl = LinearLayout.LayoutParams(-1, dp(a, 14))
        pl.setMargins(0, 0, 0, dp(a, 6))
        pista.layoutParams = pl
        if (pct > 0) {
            val r = View(a)
            r.background = degradado(a)
            r.layoutParams = LinearLayout.LayoutParams(0, -1, pct.toFloat())
            pista.addView(r)
        }
        cont.addView(pista)
        val mt = texto(a, if (pct >= 100) "🎉 ¡Meta cumplida!" else pct.toString() + "% de tu meta", 16f, if (pct >= 100) verde else cian, true)
        mt.setPadding(0, 0, 0, dp(a, 18))
        cont.addView(mt)

        val ultimo = todos.maxByOrNull { it.ts }
        val t = tarjeta(a, Color.parseColor("#12392A"), verde)
        t.setPadding(dp(a, 22), dp(a, 22), dp(a, 22), dp(a, 22))
        if (ultimo == null) {
            t.addView(texto(a, "Esperando tu primer pago…", 22f, Color.WHITE, true))
        } else {
            t.addView(texto(a, "ÚLTIMO PAGO", 14f, gris, true))
            val mTxt = "+ $ " + Aviso.fmt(ultimo.monto)
            t.addView(texto(a, mTxt, if (mTxt.length > 10) 40f else 52f, verde, true))
            t.addView(texto(a, ultimo.etiqueta, 26f, Color.WHITE, true))
            t.addView(texto(a, hace(ultimo.ts), 18f, gris))
        }
        cont.addView(t)
        cont.addView(texto(a, "La pantalla se queda encendida y se actualiza sola.", 12f, gris))
    }

    fun acerca(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        volver(a, cont, refrescar)
        val ver = try {
            a.packageManager.getPackageInfo(a.packageName, 0).versionName
        } catch (e: Exception) {
            "?"
        }
        val hr = hero(a)
        hr.gravity = Gravity.CENTER_HORIZONTAL
        hr.addView(texto(a, "🔔", 52f, Color.WHITE))
        val nom = texto(a, "ROVIKA", 28f, Color.WHITE, true)
        nom.letterSpacing = 0.25f
        hr.addView(nom)
        hr.addView(texto(a, "Tus pagos, en voz alta", 15f, cian))
        hr.addView(texto(a, "Versión " + ver, 12f, gris))
        cont.addView(hr)

        val pv = tarjeta(a)
        pv.addView(texto(a, "🔒 Tu privacidad", 17f, Color.WHITE, true))
        val cuerpo = texto(
            a,
            "• Rovika solo guarda las notificaciones de apps de pagos (y las que tú actives).\n\n" +
                "• Tus pagos y gastos se guardan únicamente en este teléfono.\n\n" +
                "• No pedimos cuentas, claves ni datos personales.\n\n" +
                "• Internet solo se usa para buscar nuevas versiones de Rovika.",
            14f, Color.parseColor("#C9D3E8")
        )
        cuerpo.setLineSpacing(0f, 1.1f)
        cuerpo.setPadding(0, dp(a, 8), 0, 0)
        pv.addView(cuerpo)
        cont.addView(pv)

        cont.addView(boton(a, "📲 Recomendar Rovika", true) {
            compartir(
                a,
                "Mira Rovika 🔔: la app que te dice en voz alta cada pago que te llega por Nequi, Bancolombia y más, y lleva la cuenta de tu día. Descárgala aquí: " + Actualizar.APK
            )
        })
        val fin = texto(a, "Hecha en Colombia 🇨🇴", 13f, gris)
        fin.gravity = Gravity.CENTER
        fin.setPadding(0, dp(a, 10), 0, 0)
        cont.addView(fin)
    }
}
