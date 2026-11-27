package com.tilin.app

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.speech.tts.TextToSpeech
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PagoListenerService : NotificationListenerService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ultimaClave = ""
    private var ultimaHora = 0L

    private val palabras = listOf(
        "te enviaron", "te envió", "te envio", "recibiste",
        "te llegó", "te llego", "te pagaron", "te transfirieron",
        "has recibido", "te mandaron", "te mandó", "te mando",
        "te depositaron", "te transfirió", "te transfirio",
        "te consignaron", "llegó dinero", "llego dinero",
        "dinero recibido", "ha llegado", "te enviaron plata",
        "recarga", "recepción", "recepcion", "bre-b", "bre b",
        "abono", "ingreso", "depósito", "deposito",
        "recibió", "recibio", "recibido", "transferencia recibida",
        "te acreditaron", "acreditado", "te abonaron",
        "consignación", "consignacion", "pago recibido", "te giraron"
    )

    private val negativas = listOf(
        "enviaste", "pagaste", "compraste", "compra por", "compra en",
        "compra de", "retiro", "retiraste", "gmf", "impuesto", "cobro",
        "débito", "debito", "transferiste", "pago a ", "pagó a",
        "has pagado", "se descontó", "se descontaron"
    )

    private val montoRegex =
        Regex("""(?:\$|COP)\s?(\d{1,3}(?:[.,]\d{3})+(?:,\d{1,2})?|\d+(?:,\d{1,2})?)""", RegexOption.IGNORE_CASE)

    private val montoSinSigno =
        Regex(
            """(?:te\s+envi[oó]|te\s+mand[oó]|te\s+transfiri[oó]|te\s+pag[oó]|te\s+deposit[oó]|te\s+consign[oó])\s+(\d{1,3}(?:[.,]\d{3})+(?:,\d{1,2})?|\d+(?:,\d{1,2})?)""",
            RegexOption.IGNORE_CASE
        )

    private val bancos = listOf(
        "Nequi" to Regex("nequi", RegexOption.IGNORE_CASE),
        "Bancolombia" to Regex("bancolombia", RegexOption.IGNORE_CASE),
        "Daviplata" to Regex("daviplata", RegexOption.IGNORE_CASE),
        "Davivienda" to Regex("davivienda", RegexOption.IGNORE_CASE),
        "Nu" to Regex("\\bnu\\b|nubank", RegexOption.IGNORE_CASE),
        "BBVA" to Regex("bbva", RegexOption.IGNORE_CASE),
        "Banco de Bogotá" to Regex("banco de bogot|bancodebogota", RegexOption.IGNORE_CASE),
        "Itaú" to Regex("ita[uú]", RegexOption.IGNORE_CASE),
        "Banco Popular" to Regex("banco popular", RegexOption.IGNORE_CASE),
        "AV Villas" to Regex("av villas|avvillas", RegexOption.IGNORE_CASE),
        "Banco de Occidente" to Regex("occidente", RegexOption.IGNORE_CASE),
        "Scotiabank" to Regex("scotiabank|colpatria", RegexOption.IGNORE_CASE),
        "Banco Falabella" to Regex("falabella", RegexOption.IGNORE_CASE),
        "Banco Caja Social" to Regex("caja social|cajasocial", RegexOption.IGNORE_CASE),
        "Bancoomeva" to Regex("bancoomeva", RegexOption.IGNORE_CASE),
        "Ualá" to Regex("ual[aá]", RegexOption.IGNORE_CASE),
        "Powwi" to Regex("powwi", RegexOption.IGNORE_CASE),
        "Lulo" to Regex("\\blulo\\b", RegexOption.IGNORE_CASE),
        "Movii" to Regex("\\bmovii\\b", RegexOption.IGNORE_CASE)
    )

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Actualizar.revisarSiToca(this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Voz.aplicar(tts, this)
        }
    }

    private fun parseMonto(s: String): Long? {
        var n = s
        val c = n.lastIndexOf(',')
        if (c >= 0 && n.length - c - 1 <= 2) n = n.substring(0, c)
        n = n.replace(".", "").replace(",", "")
        return n.toLongOrNull()
    }

    private fun totalHoy(): Long {
        val dia = SimpleDateFormat("yyyyMMdd", Locale.US)
        val hoy = dia.format(Date())
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        var suma = 0L
        val lineas = (prefs.getString("pagos", "") ?: "").split("\n")
        for (linea in lineas) {
            if (linea.isBlank()) continue
            val c = linea.split("|")
            if (c.size < 3) continue
            val ts = c[0].toLongOrNull() ?: continue
            if (dia.format(Date(ts)) == hoy) {
                suma += (c[1].toLongOrNull() ?: 0L)
            }
        }
        return suma
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return

        Actualizar.revisarSiToca(this)

        val pkg = sbn.packageName
        val extras = sbn.notification.extras
        val titulo = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val cuerpo = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val completo = titulo + " " + cuerpo

        val app = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        } catch (e: Exception) {
            pkg
        }

        val minuscula = completo.lowercase()
        val relevante = montoRegex.containsMatchIn(completo) || palabras.any { minuscula.contains(it) }
        if (relevante) Apps.registrarVista(this, pkg, app)

        if (!Apps.permitida(this, pkg, app)) return

        val todas = getSharedPreferences("tilin", MODE_PRIVATE).getBoolean("todas", false)
        if (!todas || relevante) guardarLog(pkg + ": " + completo)

        if (negativas.any { minuscula.contains(it) }) {
            Salidas.analizar(this, app, completo)
            return
        }
        if (palabras.none { minuscula.contains(it) }) return

        val m1 = montoRegex.find(completo)
        val m2 = if (m1 == null) montoSinSigno.find(completo) else null
        val crudo = (m1 ?: m2)?.groupValues?.get(1) ?: return
        val monto = parseMonto(crudo) ?: return

        val origen = sacarOrigen(completo, app, pkg)

        val clave = monto.toString() + "|" + origen
        val ahora = System.currentTimeMillis()
        if (clave == ultimaClave && ahora - ultimaHora < 45000L) {
            Aviso.diag(this, "pago repetido ignorado")
            return
        }
        ultimaClave = clave
        ultimaHora = ahora

        val antes = totalHoy()
        guardarPago(monto, app, "", origen)
        TotalWidget.refrescar(this)
        val meta = getSharedPreferences("tilin", MODE_PRIVATE).getLong("meta", 150000L)
        val cumplida = antes < meta && antes + monto >= meta

        Voz.aplicar(tts, this)
        val dicho = Voz.frase(this, monto, origen)
        if (Sonido.activo(this)) {
            Sonido.tocar()
            Handler(Looper.getMainLooper()).postDelayed({
                tts?.speak(dicho, TextToSpeech.QUEUE_ADD, null, "pago")
                if (cumplida) {
                    tts?.speak("¡Meta del día cumplida! ¡Felicidades!", TextToSpeech.QUEUE_ADD, null, "meta")
                }
            }, 650L)
        } else {
            tts?.speak(dicho, TextToSpeech.QUEUE_ADD, null, "pago")
            if (cumplida) {
                tts?.speak("¡Meta del día cumplida! ¡Felicidades!", TextToSpeech.QUEUE_ADD, null, "meta")
            }
        }
        try {
            Aviso.mostrar(
                this,
                origen,
                "$ " + Aviso.fmt(monto),
                if (cumplida) "¡Meta cumplida! 🎉" else app
            )
        } catch (e: Exception) {
            Aviso.diag(this, "ERROR servicio: " + e.javaClass.simpleName + " " + (e.message ?: ""))
        }
    }

    private fun sacarOrigen(texto: String, app: String, pkg: String): String {
        val t = texto.lowercase()
        if (t.contains("bre-b") || t.contains("bre b")) return "Bre-B"
        var recibidor = ""
        for (b in bancos) {
            if (b.second.containsMatchIn(app + " " + pkg)) {
                recibidor = b.first
                break
            }
        }
        if (recibidor.isNotEmpty() && recibidor != "Nequi") return recibidor
        var otro = ""
        for (b in bancos) {
            if (b.first != "Nequi" && b.second.containsMatchIn(texto)) {
                otro = b.first
                break
            }
        }
        if (t.contains("recarga")) {
            return if (otro.isNotEmpty()) "Recarga " + otro else "Recarga"
        }
        if (otro.isNotEmpty()) return otro
        if (recibidor.isNotEmpty()) return recibidor
        for (b in bancos) {
            if (b.second.containsMatchIn(texto)) return b.first
        }
        return app
    }

    private fun guardarPago(monto: Long, app: String, nombre: String, banco: String) {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val actuales = prefs.getString("pagos", "") ?: ""
        val lista = actuales.split("\n").filter { it.isNotBlank() }.toMutableList()
        val limpio = { s: String -> s.replace("|", " ").replace("\n", " ") }
        lista.add(0, System.currentTimeMillis().toString() + "|" + monto + "|" + limpio(app) + "|" + limpio(nombre) + "|" + limpio(banco))
        prefs.edit().putString("pagos", lista.take(3000).joinToString("\n")).apply()
    }

    private fun guardarLog(linea: String) {
        val prefs = getSharedPreferences("tilin", MODE_PRIVATE)
        val actuales = prefs.getString("log", "") ?: ""
        val lista = actuales.split("\n---\n").filter { it.isNotBlank() }.toMutableList()
        lista.add(0, linea.take(300))
        val recortada = lista.take(25).joinToString("\n---\n")
        prefs.edit().putString("log", recortada).apply()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }
}
