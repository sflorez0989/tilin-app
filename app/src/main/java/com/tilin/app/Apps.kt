package com.tilin.app

import android.content.Context

object Apps {
    private val reg = Regex(
        "nequi|bancolombia|daviplata|davivienda|nubank|\\bnu\\b|banco|bbva|ita[uú]|lulo|movii|pibank|rappipay|tpaga|breb|bancoomeva|colpatria|scotiabank|villas|ual[aá]|powwi",
        RegexOption.IGNORE_CASE
    )

    private fun prefs(c: Context) =
        c.applicationContext.getSharedPreferences("tilin", Context.MODE_PRIVATE)

    fun esAuto(pkg: String, label: String): Boolean =
        reg.containsMatchIn(pkg + " " + label)

    fun extras(c: Context): Set<String> =
        (prefs(c).getString("extra", "") ?: "").split("\n").filter { it.isNotBlank() }.toSet()

    fun permitida(c: Context, pkg: String, label: String): Boolean {
        if (esAuto(pkg, label)) return true
        if (extras(c).contains(pkg)) return true
        return prefs(c).getBoolean("todas", false)
    }

    fun alternar(c: Context, pkg: String) {
        val s = extras(c).toMutableSet()
        if (s.contains(pkg)) s.remove(pkg) else s.add(pkg)
        prefs(c).edit().putString("extra", s.joinToString("\n")).apply()
    }

    fun registrarVista(c: Context, pkg: String, label: String) {
        try {
            val p = prefs(c)
            val lineas = (p.getString("vistas", "") ?: "").split("\n").filter { it.isNotBlank() }.toMutableList()
            lineas.removeAll { it.startsWith(pkg + "|") }
            lineas.add(0, pkg + "|" + label.replace("|", " ").replace("\n", " ") + "|" + System.currentTimeMillis())
            p.edit().putString("vistas", lineas.take(40).joinToString("\n")).apply()
        } catch (e: Exception) { }
    }

    fun vistas(c: Context): List<Pair<String, String>> {
        val r = mutableListOf<Pair<String, String>>()
        for (l in (prefs(c).getString("vistas", "") ?: "").split("\n")) {
            val p = l.split("|")
            if (p.size >= 2 && p[0].isNotBlank()) r.add(Pair(p[0], p[1]))
        }
        return r
    }
}
