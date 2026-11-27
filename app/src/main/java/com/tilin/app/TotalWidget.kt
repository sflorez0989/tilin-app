package com.tilin.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TotalWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            manager.updateAppWidget(id, armar(context))
        }
    }

    companion object {
        fun armar(c: Context): RemoteViews {
            val v = RemoteViews(c.packageName, R.layout.widget_total)
            val dia = SimpleDateFormat("yyyyMMdd", Locale.US)
            val hoy = dia.format(Date())
            val prefs = c.getSharedPreferences("tilin", Context.MODE_PRIVATE)
            var total = 0L
            var n = 0
            for (linea in (prefs.getString("pagos", "") ?: "").split("\n")) {
                if (linea.isBlank()) continue
                val p = linea.split("|")
                if (p.size < 3) continue
                val ts = p[0].toLongOrNull() ?: continue
                if (dia.format(Date(ts)) == hoy) {
                    total += (p[1].toLongOrNull() ?: 0L)
                    n += 1
                }
            }
            v.setTextViewText(R.id.w_total, "$ " + Aviso.fmt(total))
            v.setTextViewText(R.id.w_detalle, n.toString() + (if (n == 1) " pago hoy" else " pagos hoy"))
            val abrir = PendingIntent.getActivity(
                c, 2, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            v.setOnClickPendingIntent(R.id.w_raiz, abrir)
            return v
        }

        fun refrescar(c: Context) {
            try {
                val app = c.applicationContext
                val m = AppWidgetManager.getInstance(app)
                val ids = m.getAppWidgetIds(ComponentName(app, TotalWidget::class.java))
                if (ids.isNotEmpty()) {
                    val v = armar(app)
                    for (id in ids) {
                        m.updateAppWidget(id, v)
                    }
                }
            } catch (e: Exception) { }
        }
    }
}
