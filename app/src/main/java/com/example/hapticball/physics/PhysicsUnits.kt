package com.example.hapticball.physics

import java.util.Locale

object PhysicsUnits {
    // Standard Earth Gravity in m/s^2
    const val EARTH_GRAVITY = 9.81f

    /**
     * Formats a mass value realistically.
     * Uses grams for < 1000g, and kilograms otherwise.
     */
    fun formatMass(grams: Float): String {
        return if (grams < 1000f) {
            String.format(Locale.getDefault(), "%.0fg", grams)
        } else {
            String.format(Locale.getDefault(), "%.2fkg", grams / 1000f)
        }
    }

    /**
     * Formats gravity in m/s^2
     */
    fun formatGravity(ms2: Float): String {
        return String.format(Locale.getDefault(), "%.1f m/s²", ms2)
    }

    /**
     * Formats scale in pixels per meter
     */
    fun formatScale(pxPerMeter: Float): String {
        return String.format(Locale.getDefault(), "%.0f px/m", pxPerMeter)
    }
}
