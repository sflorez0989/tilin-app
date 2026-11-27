package com.tilin.app

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class PasoNov(
    val icono: String,
    val titulo: String,
    val texto: String,
    val boton: String,
    val pestana: Int,
    val seccion: Int
)

object Novedades {
    const val ID = "2.9"
    var nav: ((Int, Int) -> Unit)? = null

    private val pasos = listOf(
        PasoNov("🆕", "Novedades de Rovika", "Esta versión trae cosas nuevas. Te las muestro en menos de un minuto. Puedes volver a verlas cuando quieras en Más → Qué hay de nuevo.", "", -1, -1),
        PasoNov("📈", "Toca las barras", "En Inicio, toca cualquier barra de la gráfica de 7 días y abajo verás el total y los pagos de ese día. Toca la misma barra otra vez para cerrar.", "Probarlo en Inicio", 0, -1),
        PasoNov("🏠", "Inicio más limpio", "Inicio ahora muestra tus últimos 3 pagos. Para ver todos está el botón Ver todos los movimientos, que te lleva a Movimientos.", "Ir a Movimientos", 1, -1),
        PasoNov("📕", "PDF y Excel", "En Más → Exportar puedes sacar un PDF bonito del mes, para mostrar o imprimir, o un archivo de Excel para tu contabilidad. Los dos se guardan en Descargas.", "Abrir Más", 3, 0),
        PasoNov("💳", "Salidas por confirmar", "Rovika detecta compras con tarjeta, envíos y retiros. Tú decides con un toque cuáles cuentan como gasto.", "Ver salidas", 3, 5),
        PasoNov("🔔", "Sonido y widget", "Ahora suena una caja registradora antes de la voz, y puedes poner un widget con tu total del día en la pantalla de inicio.", "Ver opciones", 3, 6)
    )

    fun mostrarSiToca(a: Activity) {
        try {
            val p = a.getSharedPreferences("tilin", Context.MODE_PRIVATE)
            if (p.getString("novVisto", "") == ID) return
            if (!p.getBoolean("tutVisto", false)) {
                p.edit().putString("novVisto", ID).apply()
                return
            }
            mostrar(a)
        } catch (e: Exception) { }
    }

    fun mostrar(a: Activity) {
        if (a.isFinishing || a.isDestroyed) return
        val dens = a.resources.displayMetrics.density
        val dp = { v: Int -> (v * dens).toInt() }
        val cian = Color.parseColor("#22D3EE")
        val azul = Color.parseColor("#2F6BFF")
        val gris = Color.parseColor("#8A97B0")

        val d = Dialog(a)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setCancelable(true)
        d.setOnDismissListener {
            a.getSharedPreferences("tilin", Context.MODE_PRIVATE).edit().putString("novVisto", ID).apply()
        }

        val raiz = LinearLayout(a)
        raiz.orientation = LinearLayout.VERTICAL
        val fondo = GradientDrawable()
        fondo.setColor(Color.parseColor("#0F1626"))
        fondo.cornerRadius = dp(24).toFloat()
        fondo.setStroke(dp(1), cian)
        raiz.background = fondo
        raiz.setPadding(dp(22), dp(18), dp(22), dp(22))
        val scroll = ScrollView(a)
        scroll.addView(raiz)
        d.setContentView(scroll)

        var paso = 0

        fun boton(t: String, lleno: Boolean, accion: () -> Unit): Button {
            val b = Button(a)
            b.text = t
            b.isAllCaps = false
            b.textSize = 15f
            b.setTypeface(null, Typeface.BOLD)
            b.setTextColor(if (lleno) Color.WHITE else cian)
            val g = if (lleno) GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(azul, cian)) else GradientDrawable()
            if (!lleno) {
                g.setColor(Color.TRANSPARENT)
                g.setStroke(dp(1), cian)
            }
            g.cornerRadius = dp(16).toFloat()
            b.background = g
            b.setOnClickListener { accion() }
            return b
        }

        fun pintar() {
            raiz.removeAllViews()
            val s = pasos[paso]

            val top = LinearLayout(a)
            top.orientation = LinearLayout.HORIZONTAL
            top.gravity = Gravity.CENTER_VERTICAL
            val pts = StringBuilder()
            for (k in pasos.indices) pts.append(if (k == paso) "● " else "○ ")
            val tp = TextView(a)
            tp.text = pts.toString().trim()
            tp.textSize = 13f
            tp.setTextColor(cian)
            tp.setTypeface(null, Typeface.BOLD)
            tp.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            top.addView(tp)
            val cerrar = TextView(a)
            cerrar.text = "Cerrar ✕"
            cerrar.textSize = 13f
            cerrar.setTextColor(gris)
            cerrar.setTypeface(null, Typeface.BOLD)
            cerrar.setPadding(dp(10), dp(6), dp(4), dp(6))
            cerrar.setOnClickListener { d.dismiss() }
            top.addView(cerrar)
            raiz.addView(top)

            val ic = TextView(a)
            ic.text = s.icono
            ic.textSize = 46f
            ic.setPadding(0, dp(14), 0, dp(4))
            raiz.addView(ic)

            val ti = TextView(a)
            ti.text = s.titulo
            ti.textSize = 22f
            ti.setTextColor(Color.WHITE)
            ti.setTypeface(null, Typeface.BOLD)
            ti.setPadding(0, 0, 0, dp(8))
            raiz.addView(ti)

            val tx = TextView(a)
            tx.text = s.texto
            tx.textSize = 15f
            tx.setTextColor(Color.parseColor("#C9D3E8"))
            tx.setLineSpacing(0f, 1.15f)
            tx.setPadding(0, 0, 0, dp(16))
            raiz.addView(tx)

            if (s.boton.isNotEmpty()) {
                val b = boton(s.boton, false) {
                    d.dismiss()
                    nav?.invoke(s.pestana, s.seccion)
                }
                val lp = LinearLayout.LayoutParams(-1, dp(48))
                lp.setMargins(0, 0, 0, dp(10))
                b.layoutParams = lp
                raiz.addView(b)
            }

            val fila = LinearLayout(a)
            fila.orientation = LinearLayout.HORIZONTAL
            if (paso > 0) {
                val atras = boton("◀ Atrás", false) {
                    paso -= 1
                    pintar()
                }
                val lp = LinearLayout.LayoutParams(0, dp(48), 1f)
                lp.setMargins(0, 0, dp(6), 0)
                atras.layoutParams = lp
                fila.addView(atras)
            }
            val ultimo = paso == pasos.size - 1
            val sig = boton(if (ultimo) "Terminar ✔" else "Siguiente ▶", true) {
                if (ultimo) {
                    d.dismiss()
                } else {
                    paso += 1
                    pintar()
                }
            }
            val lp2 = LinearLayout.LayoutParams(0, dp(48), 1f)
            sig.layoutParams = lp2
            fila.addView(sig)
            raiz.addView(fila)
        }

        pintar()
        d.show()
        d.window?.setBackgroundDrawableResource(android.R.color.transparent)
        val ancho = minOf((a.resources.displayMetrics.widthPixels * 0.92f).toInt(), dp(520))
        d.window?.setLayout(ancho, WindowManager.LayoutParams.WRAP_CONTENT)
    }
}
