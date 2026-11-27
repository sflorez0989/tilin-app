package com.tilin.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object Sonido {
    fun activo(c: Context): Boolean =
        c.applicationContext.getSharedPreferences("tilin", Context.MODE_PRIVATE).getBoolean("caja", true)

    fun caja(c: Context) {
        if (activo(c)) tocar()
    }

    fun tocar() {
        Thread {
            var track: AudioTrack? = null
            try {
                val rate = 22050
                val total = (rate * 0.6).toInt()
                val buf = ShortArray(total)
                for (i in 0 until total) {
                    val t = i.toDouble() / rate
                    var s = 0.0
                    if (t < 0.09) {
                        s = sin(2.0 * PI * 1318.5 * t) * exp(-t * 12.0)
                    } else if (t >= 0.11) {
                        val u = t - 0.11
                        s = (sin(2.0 * PI * 1975.5 * u) * 0.7 + sin(2.0 * PI * 3951.0 * u) * 0.3) * exp(-u * 6.0)
                    }
                    buf[i] = (s * 0.5 * 32767.0).toInt().toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(rate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buf.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buf, 0, buf.size)
                track.play()
                Thread.sleep(800)
            } catch (e: Exception) {
            } finally {
                try {
                    track?.release()
                } catch (e: Exception) { }
            }
        }.start()
    }
}
