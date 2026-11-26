package com.transportguide.app.logic

import java.net.URLEncoder
import java.util.Locale

/** Google Maps direction links. They open the Google Maps app if installed, otherwise the browser. */
object MapsLinks {
    /** Up to this straight-line distance a sight is suggested on foot; beyond it, public transport. */
    const val WALK_LIMIT_KM = 2.0

    /**
     * Uses the saddr/daddr form: the Android Maps app ignored `origin` in the newer
     * `maps/dir/?api=1` form and showed "Choose start location", but fills in saddr.
     * Each end is a place name when one is given (Maps then shows "Pantheon", not the nearest
     * business), otherwise exact coordinates. dirflg: w = walking, r = public transport, d = driving.
     */
    fun directions(
        fromLat: Double, fromLon: Double, toLat: Double, toLon: Double, travelMode: String,
        fromName: String? = null, toName: String? = null,
    ): String {
        val flag = when (travelMode) { "walking" -> "w"; "transit" -> "r"; else -> "d" }
        return "https://maps.google.com/maps?saddr=${end(fromName, fromLat, fromLon)}" +
            "&daddr=${end(toName, toLat, toLon)}&dirflg=$flag"
    }

    /** Directions from the trip's Destination Point to a suggested sight. */
    fun toSuggestion(
        fromLat: Double, fromLon: Double, toLat: Double, toLon: Double,
        fromName: String? = null, toName: String? = null,
    ): String {
        val km = Geo.haversineKm(fromLat, fromLon, toLat, toLon)
        return directions(fromLat, fromLon, toLat, toLon, if (km <= WALK_LIMIT_KM) "walking" else "transit", fromName, toName)
    }

    private fun end(name: String?, lat: Double, lon: Double) =
        if (name.isNullOrBlank()) coord(lat, lon) else URLEncoder.encode(name, "UTF-8")

    fun modeFor(mode: Mode) = when (mode) { Mode.WALK -> "walking"; Mode.TAXI -> "driving"; else -> "transit" }

    private fun coord(lat: Double, lon: Double) = String.format(Locale.US, "%.6f,%.6f", lat, lon)
}
