package com.transportguide.app.logic

import java.util.Locale

/** Google Maps direction links. They open the Google Maps app if installed, otherwise the browser. */
object MapsLinks {
    /** Up to this straight-line distance a sight is suggested on foot; beyond it, public transport. */
    const val WALK_LIMIT_KM = 2.0

    /**
     * Uses the saddr/daddr form: the Android Maps app ignored `origin` in the newer
     * `maps/dir/?api=1` form and showed "Choose start location", but fills in saddr.
     * dirflg: w = walking, r = public transport, d = driving.
     */
    fun directions(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, travelMode: String): String {
        val flag = when (travelMode) { "walking" -> "w"; "transit" -> "r"; else -> "d" }
        return "https://maps.google.com/maps?saddr=${coord(fromLat, fromLon)}" +
            "&daddr=${coord(toLat, toLon)}&dirflg=$flag"
    }

    /** Directions from the trip's Destination Point to a suggested sight. */
    fun toSuggestion(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): String {
        val km = Geo.haversineKm(fromLat, fromLon, toLat, toLon)
        return directions(fromLat, fromLon, toLat, toLon, if (km <= WALK_LIMIT_KM) "walking" else "transit")
    }

    fun modeFor(mode: Mode) = when (mode) { Mode.WALK -> "walking"; Mode.TAXI -> "driving"; else -> "transit" }

    private fun coord(lat: Double, lon: Double) = String.format(Locale.US, "%.6f,%.6f", lat, lon)
}
