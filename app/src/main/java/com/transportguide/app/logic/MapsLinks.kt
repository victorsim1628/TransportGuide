package com.transportguide.app.logic

import java.util.Locale

/** Google Maps URLs (https://developers.google.com/maps/documentation/urls). They open the Maps app if installed. */
object MapsLinks {
    /** Up to this straight-line distance a sight is suggested on foot; beyond it, public transport. */
    const val WALK_LIMIT_KM = 2.0

    fun directions(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, travelMode: String): String =
        "https://www.google.com/maps/dir/?api=1" +
            "&origin=${coord(fromLat, fromLon)}&destination=${coord(toLat, toLon)}&travelmode=$travelMode"

    /** Directions from the trip's Destination Point to a suggested sight. */
    fun toSuggestion(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): String {
        val km = Geo.haversineKm(fromLat, fromLon, toLat, toLon)
        return directions(fromLat, fromLon, toLat, toLon, if (km <= WALK_LIMIT_KM) "walking" else "transit")
    }

    fun modeFor(mode: Mode) = when (mode) { Mode.WALK -> "walking"; Mode.TAXI -> "driving"; else -> "transit" }

    private fun coord(lat: Double, lon: Double) = String.format(Locale.US, "%.6f,%.6f", lat, lon)
}
