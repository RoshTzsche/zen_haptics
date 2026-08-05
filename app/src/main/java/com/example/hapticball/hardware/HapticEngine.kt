package com.example.hapticball.hardware

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class CollisionType {
    WALL, BALL, OBSTACLE, LETHAL
}

object HapticEngine {
    fun triggerCollision(
        context: Context, 
        impactVelocity: Float, 
        normalizedX: Float = 0.5f, 
        normalizedY: Float = 0.5f,
        type: CollisionType = CollisionType.WALL
    ) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Intensity based on velocity (clamped 0.0 to 1.0)
        val intensity = (impactVelocity / 2400f).coerceIn(0.1f, 1.0f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Advanced Haptic Compositions (API 30+)
            val composition = VibrationEffect.startComposition()
            
            when (type) {
                CollisionType.WALL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, intensity)
                    } else {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                    }
                }
                CollisionType.BALL -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                }
                CollisionType.OBSTACLE -> {
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, intensity)
                }
                CollisionType.LETHAL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.9f)
                    } else {
                        composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.9f)
                    }
                    composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.9f)
                }
            }
            
            // Spatial "Pitch" Variation
            // If at the bottom (Y > 0.8), add a secondary low-frequency thud on API 31+
            if (normalizedY > 0.8f && type != CollisionType.LETHAL && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                composition.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, intensity * 0.5f)
            }

            vibrator.vibrate(composition.compose())

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Fallback for API 29
            val effectId = when {
                normalizedY < 0.4f -> {
                    if (normalizedX < 0.5f) VibrationEffect.EFFECT_TICK else VibrationEffect.EFFECT_CLICK
                }
                normalizedY > 0.6f -> {
                    if (normalizedX < 0.5f) VibrationEffect.EFFECT_CLICK else VibrationEffect.EFFECT_HEAVY_CLICK
                }
                else -> VibrationEffect.EFFECT_CLICK
            }
            val finalEffectId = if (impactVelocity > 1500f) VibrationEffect.EFFECT_HEAVY_CLICK else effectId
            vibrator.vibrate(VibrationEffect.createPredefined(finalEffectId))

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val duration = when {
                impactVelocity > 1000f -> 60L
                impactVelocity > 300f -> 30L
                else -> 10L
            }
            val effect = VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)

        } else {
            val duration = when {
                impactVelocity > 1000f -> 60L
                impactVelocity > 300f -> 30L
                else -> 10L
            }
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    fun triggerTick(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10L)
        }
    }
}