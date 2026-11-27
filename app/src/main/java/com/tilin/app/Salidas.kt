package com.tilin.app

import android.content.Context

class Salida(val ts: Long, val monto: Long, val tipo: String, val detalle: String)

object Salidas {
    private var ultimaClave = ""
    private var ultimaHora = 0L

    private const val NUM = "(\\d{1,3}(?:[.,]\\d{3})+(?:,\\d{1,2})?|\\d+(?:,\\d{1,2})?)"
    private val conSigno = Regex("(?:[$]|COP)\\s?" + NUM, RegexOption.IGNORE_CASE)
    private val sinSigno = Regex("(?:enviaste|transferiste|pagaste|compraste|retiraste|giraste)\\s+" + NUM, RegexOption.IGNORE_CASE)

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences("tilin", Context.MODE_PRIVATE)

    private fun lineas(c: Context, clave: String): List<String> =
        (prefs(c).getString(clave, "") ?: "").split("\n").filter { it.isNotBlank() }

    private fun limpio(s: String): String = s.replace("|", " ").replace("\n", " ")

    private fun parse(s: String): Long? {
        var n = s
        val k = n.lastIndexOf(',')
        if (k >= 0 && n.length - k - 1 <= 2) n = n.substring(0, k)
        n = n.replace(".", "").replace(",", "")
        return n.toLongOrNull()
    }

    private fun tipoDe(m: String): String? {
        if (m.contains("gmf") || m.contains("impuesto") || m.contains("4x1000") || m.contains("4 x 1000")) return null
        if (m.contains("retir")) return "🏧 Retiro"
        if (m.contains("enviaste") || m.contains("transferiste") || m.contains("giraste")) return "📤 Envío a otra persona"
        if (m.contains("compra") || m.contains("tarjeta")) return "💳 Compra con tarjeta"
        if (m.contains("pagaste") || m.contains("pago a ") || m.contains("pagó a") || m.contains("has pagado") || m.contains("pago de")) return "🧾 Pago"
        if (m.contains("débito") || m.contains("debito") || m.contains("cobro") || m.contains("se descontó") || m.contains("se descontaron")) return "💳 Cobro"
        return null
    }

    private fun escribirGasto(c: Context, ts: Long, monto: Long, cat: String, nota: String) {
        val l = lineas(c, "gastos").toMutableList()
        l.add(0, ts.toString() + "|" + monto + "|" + limpio(cat) + "|" + limpio(nota))
        prefs(c).edit().putString("gastos", l.take(2000).joinToString("\n")).apply()
    }

    fun analizar(c: Context, app: String, texto: String) {
        try {
            val t = texto.lowercase()
            val tipo = tipoDe(t) ?: return
            val m = conSigno.find(texto) ?: sinSigno.find(texto) ?: return
            val monto = parse(m.groupValues[1]) ?: return
            if (monto <= 0L) return
            val clave = monto.toString() + "|" + tipo
            val ahora = System.currentTimeMillis()
            if (clave == ultimaClave && ahora - ultimaHora < 45000L) return
            ultimaClave = clave
            ultimaHora = ahora
            val detalle = limpio(app)
            if (prefs(c).getBoolean("salidaAuto", false)) {
                escribirGasto(c, ahora, monto, tipo, detalle)
                Aviso.diag(c, "salida contada como gasto: " + tipo)
            } else {
                val l = lineas(c, "salidas").toMutableList()
                l.add(0, ahora.toString() + "|" + monto + "|" + tipo + "|" + detalle)
                prefs(c).edit().putString("salidas", l.take(50).joinToString("\n")).apply()
                Aviso.diag(c, "salida por confirmar: " + tipo)
            }
        } catch (e: Exception) { }
    }

    fun pendientes(c: Context): List<Salida> {
        val r = mutableListOf<Salida>()
        for (l in lineas(c, "salidas")) {
            val p = l.split("|")
            if (p.size < 3) continue
            val ts = p[0].toLongOrNull() ?: continue
            r.add(Salida(ts, p[1].toLongOrNull() ?: 0L, p[2], if (p.size > 3) p[3] else ""))
        }
        return r
    }

    fun descartar(c: Context, ts: Long) {
        val l = lineas(c, "salidas").filter { it.split("|")[0] != ts.toString() }
        prefs(c).edit().putString("salidas", l.joinToString("\n")).apply()
    }

    fun confirmar(c: Context, ts: Long) {
        val s = pendientes(c).firstOrNull { it.ts == ts } ?: return
        escribirGasto(c, s.ts, s.monto, s.tipo, s.detalle)
        descartar(c, ts)
    }

    fun confirmarTodas(c: Context) {
        for (s in pendientes(c)) {
            escribirGasto(c, s.ts, s.monto, s.tipo, s.detalle)
        }
        prefs(c).edit().putString("salidas", "").apply()
    }

    fun descartarTodas(c: Context) {
        prefs(c).edit().putString("salidas", "").apply()
    }
}
