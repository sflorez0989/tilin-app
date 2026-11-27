package com.tilin.app

import android.app.Activity
import android.graphics.Color
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Mas2 {
    private val gris = Color.parseColor("#8A97B0")
    private val rojo = Color.parseColor("#FF6B6B")

    fun textoPendientes(a: Activity): String {
        val n = Salidas.pendientes(a).size
        return if (n == 0) "Compras, envíos y retiros detectados" else n.toString() + " por confirmar"
    }

    fun salidas(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        Mas.volver(a, cont, refrescar)
        cont.addView(Mas.texto(a, "Salidas por confirmar", 24f, Color.WHITE, true))
        val sub = Mas.texto(a, "Rovika detecta compras con tarjeta, envíos, pagos y retiros. Tú decides cuáles cuentan como gasto.", 13f, gris)
        sub.setPadding(0, Mas.dp(a, 2), 0, Mas.dp(a, 14))
        cont.addView(sub)

        val lista = Salidas.pendientes(a)
        if (lista.isEmpty()) {
            val t = Mas.tarjeta(a)
            t.addView(Mas.texto(a, "No hay salidas pendientes.", 15f, Color.WHITE, true))
            val d = Mas.texto(a, "Cuando pagues con tu tarjeta o envíes plata, aparecerán aquí si tu banco manda notificación a este teléfono.", 13f, gris)
            d.setPadding(0, Mas.dp(a, 6), 0, 0)
            t.addView(d)
            cont.addView(t)
            return
        }

        Mas.filaBotones(
            a, cont,
            Mas.botonPeq(a, "✔ Contar todas", true) {
                Salidas.confirmarTodas(a)
                refrescar()
            },
            Mas.botonPeq(a, "🗑 Ignorar todas", false) {
                Salidas.descartarTodas(a)
                refrescar()
            }
        )

        val fecha = SimpleDateFormat("d MMM, h:mm a", Locale("es", "CO"))
        for (s in lista) {
            val t = Mas.tarjeta(a)
            t.addView(Mas.texto(a, s.tipo, 16f, Color.WHITE, true))
            t.addView(Mas.texto(a, "- $ " + Aviso.fmt(s.monto), 22f, rojo, true))
            val d = Mas.texto(a, fecha.format(Date(s.ts)) + " · " + s.detalle, 12f, gris)
            d.setPadding(0, 0, 0, Mas.dp(a, 10))
            t.addView(d)
            Mas.filaBotones(
                a, t,
                Mas.botonPeq(a, "Contar", true) {
                    Salidas.confirmar(a, s.ts)
                    refrescar()
                },
                Mas.botonPeq(a, "Ignorar", false) {
                    Salidas.descartar(a, s.ts)
                    refrescar()
                }
            )
            cont.addView(t)
        }
    }

    private fun alternador(a: Activity, cont: LinearLayout, titulo: String, desc: String, clave: String, def: Boolean, refrescar: () -> Unit) {
        val p = Mas.prefs(a)
        val on = p.getBoolean(clave, def)
        val t = Mas.tarjeta(a)
        t.addView(Mas.texto(a, titulo, 16f, Color.WHITE, true))
        val d = Mas.texto(a, desc, 13f, gris)
        d.setPadding(0, Mas.dp(a, 4), 0, Mas.dp(a, 10))
        t.addView(d)
        t.addView(Mas.boton(a, if (on) "✔ Activado (toca para apagar)" else "Apagado (toca para activar)", on) {
            p.edit().putBoolean(clave, !on).apply()
            refrescar()
        })
        cont.addView(t)
    }

    fun opciones(a: Activity, cont: LinearLayout, refrescar: () -> Unit) {
        Mas.volver(a, cont, refrescar)
        cont.addView(Mas.texto(a, "Opciones extra", 24f, Color.WHITE, true))
        val sub = Mas.texto(a, "Sonido, salidas y widget", 14f, Color.parseColor("#22D3EE"))
        sub.setPadding(0, Mas.dp(a, 2), 0, Mas.dp(a, 14))
        cont.addView(sub)

        alternador(
            a, cont, "🔔 Sonido de caja registradora",
            "Suena un cha-ching justo antes de que Rovika diga el pago.",
            "caja", true, refrescar
        )
        cont.addView(Mas.boton(a, "▶ Probar sonido", false) { Sonido.tocar() })

        alternador(
            a, cont, "💸 Contar salidas automáticamente",
            "Apagado: las compras con tarjeta, envíos, pagos y retiros esperan tu confirmación. Encendido: se suman solos a tus gastos.",
            "salidaAuto", false, refrescar
        )

        val w = Mas.tarjeta(a)
        w.addView(Mas.texto(a, "📱 Widget en tu pantalla de inicio", 16f, Color.WHITE, true))
        val d = Mas.texto(
            a,
            "Mantén presionado un espacio vacío de la pantalla de inicio, toca Widgets, busca Rovika y arrastra el widget. Muestra tu total del día y cuántos pagos llevas.",
            13f, gris
        )
        d.setPadding(0, Mas.dp(a, 4), 0, 0)
        w.addView(d)
        cont.addView(w)
    }
}
