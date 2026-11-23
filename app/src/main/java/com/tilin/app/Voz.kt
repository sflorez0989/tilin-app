package com.tilin.app

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object Voz {
    fun aplicar(tts: TextToSpeech?, c: Context) {
        if (tts == null) return
        try {
            val p = c.getSharedPreferences("tilin", Context.MODE_PRIVATE)
            tts.setLanguage(Locale("es", "CO"))
            val nombre = p.getString("vozNombre", "") ?: ""
            if (nombre.isNotEmpty()) {
                val v = tts.voices?.firstOrNull { it.name == nombre }
                if (v != null) tts.setVoice(v)
            }
            tts.setPitch(p.getInt("tono", 100) / 100f)
            tts.setSpeechRate(p.getInt("vel", 100) / 100f)
        } catch (e: Exception) { }
    }

    fun frase(c: Context, monto: Long, origen: String): String {
        val f = c.getSharedPreferences("tilin", Context.MODE_PRIVATE).getInt("frase", 0)
        return when (f) {
            1 -> "¡Tilín! " + monto + " pesos por " + origen
            2 -> monto.toString() + " pesos"
            3 -> "¡Tienes plata nueva! " + monto + " pesos por " + origen
            else -> "Te llegaron " + monto + " pesos por " + origen
        }
    }
}
