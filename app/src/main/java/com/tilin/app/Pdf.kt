package com.tilin.app

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Pdf {
    private const val W = 595
    private const val H = 842
    private const val M = 40f

    private class Fila(val ts: Long, val monto: Long, val etiqueta: String)

    private class Hoja(val doc: PdfDocument) {
        var n = 0
        var pagina: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)

        fun nueva() {
            cerrar()
            n += 1
            val info = PdfDocument.PageInfo.Builder(W, H, n).create()
            val pg = doc.startPage(info)
            pagina = pg
            canvas = pg.canvas
            y = M + 6f
        }

        fun cerrar() {
            val pg = pagina ?: return
            val cv = canvas
            if (cv != null) {
                p.reset()
                p.isAntiAlias = true
                p.textSize = 9f
                p.color = Color.GRAY
                p.textAlign = Paint.Align.CENTER
                cv.drawText("Rovika · página " + n, W / 2f, H - 18f, p)
            }
            doc.finishPage(pg)
            pagina = null
            canvas = null
        }

        fun asegurar(alto: Float) {
            if (y + alto > H - 50f) nueva()
        }
    }

    private fun leer(c: Context, clave: String, banco: Boolean): List<Fila> {
        val r = mutableListOf<Fila>()
        val prefs = c.getSharedPreferences("tilin", Context.MODE_PRIVATE)
        for (linea in (prefs.getString(clave, "") ?: "").split("\n")) {
            if (linea.isBlank()) continue
            val p = linea.split("|")
            if (p.size < 3) continue
            val ts = p[0].toLongOrNull() ?: continue
            val monto = p[1].toLongOrNull() ?: 0L
            val et = if (banco) (if (p.size > 4) p[4] else p[2]) else p[2]
            r.add(Fila(ts, monto, et))
        }
        return r
    }

    private fun limpiar(s: String): String {
        val r = s.filter { it.code in 32..126 || it.code in 160..255 }.trim()
        return if (r.isEmpty()) "Otro" else r
    }

    private fun dinero(n: Long): String =
        (if (n < 0L) "- $ " else "$ ") + Aviso.fmt(if (n < 0L) -n else n)

    private fun texto(
        h: Hoja, t: String, x: Float, size: Float, color: Int,
        bold: Boolean = false, derecha: Boolean = false, yy: Float = h.y
    ) {
        val p = h.p
        p.reset()
        p.isAntiAlias = true
        p.textSize = size
        p.color = color
        p.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        p.textAlign = if (derecha) Paint.Align.RIGHT else Paint.Align.LEFT
        h.canvas?.drawText(t, x, yy, p)
    }

    private fun seccion(h: Hoja, titulo: String) {
        h.asegurar(50f)
        h.y += 22f
        texto(h, titulo, M, 13f, Color.parseColor("#0B1230"), true)
        h.y += 6f
        h.p.reset()
        h.p.color = Color.parseColor("#22D3EE")
        h.p.strokeWidth = 1.5f
        h.canvas?.drawLine(M, h.y, W - M, h.y, h.p)
        h.y += 16f
    }

    private fun fila(h: Hoja, izq: String, medio: String, der: String, colorDer: Int) {
        h.asegurar(18f)
        texto(h, izq, M, 11f, Color.parseColor("#222222"))
        if (medio.isNotEmpty()) texto(h, medio, 230f, 11f, Color.parseColor("#666666"))
        texto(h, der, W - M, 11f, colorDer, true, true)
        h.y += 17f
    }

    fun mes(a: Activity, ini: Long, fin: Long, nombre: String) {
        try {
            if (Build.VERSION.SDK_INT < 29) {
                Toast.makeText(a, "El PDF necesita Android 10 o más nuevo. Usa Excel.", Toast.LENGTH_LONG).show()
                return
            }
            val pagos = leer(a, "pagos", true).filter { it.ts >= ini && it.ts < fin }.sortedBy { it.ts }
            val gastos = leer(a, "gastos", false).filter { it.ts >= ini && it.ts < fin }.sortedBy { it.ts }
            if (pagos.isEmpty() && gastos.isEmpty()) {
                Toast.makeText(a, "No hay movimientos en ese mes", Toast.LENGTH_LONG).show()
                return
            }
            val esCO = Locale("es", "CO")
            val diaF = SimpleDateFormat("yyyyMMdd", Locale.US)
            val fFecha = SimpleDateFormat("EEE d MMM", esCO)
            val fHora = SimpleDateFormat("d MMM · h:mm a", esCO)
            val mesRaw = SimpleDateFormat("MMMM yyyy", esCO).format(Date(ini))
            val mesTxt = mesRaw.substring(0, 1).uppercase() + mesRaw.substring(1)

            var ingresos = 0L
            for (p in pagos) ingresos += p.monto
            var gastosT = 0L
            for (g in gastos) gastosT += g.monto
            val neto = ingresos - gastosT

            val porDia = java.util.TreeMap<String, LongArray>()
            val etqDia = HashMap<String, String>()
            for (p in pagos) {
                val k = diaF.format(Date(p.ts))
                val arr = porDia.getOrPut(k) { LongArray(2) }
                arr[0] += p.monto
                arr[1] += 1L
                if (!etqDia.containsKey(k)) etqDia[k] = fFecha.format(Date(p.ts))
            }
            var mejorK = ""
            var mejorV = 0L
            for (e in porDia.entries) {
                if (e.value[0] > mejorV) {
                    mejorV = e.value[0]
                    mejorK = e.key
                }
            }

            val doc = PdfDocument()
            val h = Hoja(doc)
            h.nueva()
            h.p.reset()
            h.p.color = Color.parseColor("#0B1230")
            h.canvas?.drawRect(0f, 0f, W.toFloat(), 100f, h.p)
            h.y = 46f
            texto(h, "ROVIKA", M, 26f, Color.parseColor("#22D3EE"), true)
            h.y = 72f
            texto(h, "Resumen de " + mesTxt, M, 14f, Color.WHITE)
            h.y = 140f

            val cols = listOf(
                Triple("INGRESOS", ingresos, Color.parseColor("#0E9F6E")),
                Triple("GASTOS", gastosT, Color.parseColor("#D64545")),
                Triple("GANANCIA NETA", neto, Color.parseColor("#0B1230"))
            )
            for (i in cols.indices) {
                val x = M + i * 175f
                texto(h, cols[i].first, x, 9f, Color.parseColor("#777777"), true, false, h.y)
                texto(h, dinero(cols[i].second), x, 17f, cols[i].third, true, false, h.y + 22f)
            }
            h.y += 52f

            texto(h, pagos.size.toString() + " pagos en " + porDia.size + " días trabajados", M, 11f, Color.parseColor("#333333"))
            h.y += 16f
            if (porDia.size > 0) {
                texto(h, "Promedio por día: " + dinero(ingresos / porDia.size), M, 11f, Color.parseColor("#333333"))
                h.y += 16f
            }
            if (mejorV > 0L) {
                texto(h, "Mejor día: " + (etqDia[mejorK] ?: mejorK) + " con " + dinero(mejorV), M, 11f, Color.parseColor("#0E9F6E"), true)
                h.y += 16f
            }

            if (porDia.isNotEmpty()) {
                seccion(h, "Ingresos por día")
                for (e in porDia.entries) {
                    val n = e.value[1]
                    fila(h, etqDia[e.key] ?: e.key, n.toString() + (if (n == 1L) " pago" else " pagos"), dinero(e.value[0]), Color.parseColor("#0E9F6E"))
                }
            }
            if (pagos.isNotEmpty()) {
                seccion(h, "Detalle de ingresos")
                for (p in pagos) {
                    fila(h, fHora.format(Date(p.ts)), limpiar(p.etiqueta), "+ " + dinero(p.monto), Color.parseColor("#0E9F6E"))
                }
            }
            if (gastos.isNotEmpty()) {
                seccion(h, "Gastos")
                for (g in gastos) {
                    fila(h, fHora.format(Date(g.ts)), limpiar(g.etiqueta), "- " + dinero(g.monto), Color.parseColor("#D64545"))
                }
            }
            h.cerrar()

            val archivo = "Rovika_" + nombre + ".pdf"
            val v = ContentValues()
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, archivo)
            v.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            val uri = a.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
            var ok = false
            if (uri != null) {
                val os = a.contentResolver.openOutputStream(uri)
                if (os != null) {
                    doc.writeTo(os)
                    os.close()
                    ok = true
                }
            }
            doc.close()
            if (ok && uri != null) {
                Toast.makeText(a, "PDF guardado en Descargas: " + archivo, Toast.LENGTH_LONG).show()
                try {
                    val i = Intent(Intent.ACTION_VIEW)
                    i.setDataAndType(uri, "application/pdf")
                    i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    a.startActivity(i)
                } catch (e: Exception) { }
            } else {
                Toast.makeText(a, "No pude guardar el PDF. Intenta de nuevo.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(a, "No pude crear el PDF. Intenta de nuevo.", Toast.LENGTH_LONG).show()
        }
    }
}
