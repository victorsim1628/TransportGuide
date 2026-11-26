package com.transportguide.app.logic

import com.transportguide.app.data.City
import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    /** Great-circle distance in km. */
    fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(a))
    }

    fun nearestCity(lat: Double, lon: Double): City {
        val best = City.entries.filter { it != City.OTHER }
            .minBy { haversineKm(lat, lon, it.lat, it.lon) }
        return if (haversineKm(lat, lon, best.lat, best.lon) <= 40.0) best else City.OTHER
    }

    /** Venice lagoon box: historic centre, Giudecca, Lido, Murano, Burano, Tronchetto. */
    fun inVeniceLagoon(lat: Double, lon: Double): Boolean =
        lat in 45.37..45.50 && lon in 12.29..12.45

    /** Always kilometres, never metres or miles. */
    fun km(value: Double): String = when {
        value < 10 -> String.format(Locale.US, "%.1f km", value)
        else -> String.format(Locale.US, "%.0f km", value)
    }

    fun minutes(min: Int): String = if (min < 60) "$min min" else "${min / 60} h ${min % 60} min"
}
