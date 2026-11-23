package com.tilin.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class Nueva(val nombre: String, val codigo: Long)

object Actualizar {
    const val API = "https://api.github.com/repos/sflorez0989/tilin-app/releases/latest"
    const val APK = "https://github.com/sflorez0989/tilin-app/releases/latest/download/Rovika.apk"
    private const val CANAL = "actualizaciones"
    private const val CADA = 12L * 3600L * 1000L

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences("tilin", Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    fun codigoActual(c: Context): Long {
        return try {
            val i = c.packageManager.getPackageInfo(c.packageName, 0)
            if (Build.VERSION.SDK_INT >= 28) i.longVersionCode else i.versionCode.toLong()
        } catch (e: Exception) {
            0L
        }
    }

    fun hayNueva(c: Context): Boolean =
        prefs(c).getLong("updCodigo", 0L) > codigoActual(c)

    private fun consultar(): Nueva? {
        var con: HttpURLConnection? = null
        try {
            con = URL(API).openConnection() as HttpURLConnection
            con.connectTimeout = 8000
            con.readTimeout = 8000
            con.setRequestProperty("Accept", "application/vnd.github+json")
            con.setRequestProperty("User-Agent", "Rovika")
            if (con.responseCode != 200) return null
            val txt = con.inputStream.bufferedReader().use { it.readText() }
            val j = JSONObject(txt)
            val tag = j.optString("tag_name", "")
            val nombre = j.optString("name", tag)
            val m = Regex("-b(\\d+)$").find(tag) ?: return null
            val codigo = 100L + m.groupValues[1].toLong()
            return Nueva(nombre, codigo)
        } catch (e: Exception) {
            return null
        } finally {
            try {
                con?.disconnect()
            } catch (e: Exception) { }
        }
    }

    fun revisarSiToca(c: Context, alTerminar: (() -> Unit)? = null) {
        val app = c.applicationContext
        val p = prefs(app)
        val ahora = System.currentTimeMillis()
        if (ahora - p.getLong("updUltimo", 0L) < CADA) return
        p.edit().putLong("updUltimo", ahora).apply()
        Thread {
            revisarAhora(app, true)
            alTerminar?.invoke()
        }.start()
    }

    fun revisarAhora(c: Context, avisar: Boolean): Int {
        val app = c.applicationContext
        val n = consultar() ?: return -1
        val p = prefs(app)
        p.edit()
            .putLong("updUltimo", System.currentTimeMillis())
            .putLong("updCodigo", n.codigo)
            .putString("updNombre", n.nombre)
            .apply()
        if (n.codigo > codigoActual(app)) {
            if (avisar && p.getLong("updAvisado", 0L) < n.codigo) {
                notificar(app, n.nombre)
                p.edit().putLong("updAvisado", n.codigo).apply()
            }
            return 1
        }
        return 0
    }

    private fun notificar(c: Context, nombre: String) {
        try {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CANAL) == null) {
                val ch = NotificationChannel(CANAL, "Actualizaciones", NotificationManager.IMPORTANCE_DEFAULT)
                ch.description = "Avisa cuando hay una nueva versión de Rovika"
                nm.createNotificationChannel(ch)
            }
            if (!nm.areNotificationsEnabled()) return
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(APK))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val abrir = PendingIntent.getActivity(
                c, 1, i,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val n = Notification.Builder(c, CANAL)
                .setSmallIcon(R.drawable.ic_notif)
                .setContentTitle("Hay una nueva versión de Rovika")
                .setContentText(nombre + " · toca para descargar e instalar")
                .setColor(Color.parseColor("#22D3EE"))
                .setAutoCancel(true)
                .setContentIntent(abrir)
                .build()
            nm.notify(1002, n)
        } catch (e: Exception) { }
    }
}
