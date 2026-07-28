package com.example.hapticball.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object HapticEngine {
    fun triggerCollision(context: Context, impactVelocity: Float) {
        // 1. OBTENER EL SERVICIO DE VIBRACIÓN SEGÚN LA VERSIÓN
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31) en adelante (Tu S24 Ultra entra aquí)
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            // Android 11 o inferior
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // 2. EJECUTAR LA VIBRACIÓN SEGÚN LA VERSIÓN
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) en adelante: Haptics de alta definición
            val effect = if (impactVelocity > 300f) {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            } else {
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            }
            vibrator.vibrate(effect)

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8 (API 26) a Android 9: Efectos básicos controlando amplitud
            val effect = VibrationEffect.createOneShot(
                if (impactVelocity > 300f) 50L else 15L, // Duración en milisegundos
                VibrationEffect.DEFAULT_AMPLITUDE
            )
            vibrator.vibrate(effect)

        } else {
            // Android 7 (API 24 y 25): Vibración a la vieja escuela (solo encendido/apagado)
            val duration = if (impactVelocity > 300f) 50L else 15L
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }
}