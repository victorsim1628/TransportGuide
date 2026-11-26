package com.transportguide.app.logic

/** Turns a forecast into plain-language advice shown before every trip. */
object WeatherAdvice {

    fun advise(w: WeatherAtTrip, walkingKm: Double?): List<String> {
        val out = mutableListOf<String>()
        val wet = w.precipProbability >= 50 || w.precipitationMm >= 0.5 || w.weatherCode in RAIN_CODES

        when {
            w.weatherCode in THUNDER_CODES -> out += "⛈️ Thunderstorms expected — avoid long walks and open water; take a taxi or train."
            w.weatherCode in SNOW_CODES || w.weatherCode in FREEZING_CODES ->
                out += "❄️ Snow or ice possible — wear shoes with grip and allow extra time; trains may run late."
            w.precipitationMm >= 4 || w.weatherCode in HEAVY_RAIN_CODES ->
                out += "🌧️ Heavy rain — take a taxi or public transport; bring a waterproof jacket."
            wet -> out += "☂️ ${w.precipProbability}% chance of rain — pack umbrellas."
            else -> out += "🌤️ ${w.description}. Little or no rain expected."
        }

        when {
            w.apparentC <= 0 -> out += "🥶 Feels like ${w.apparentC.toInt()}°C — hats, gloves and scarves for everyone."
            w.apparentC <= 7 -> out += "🧥 Feels like ${w.apparentC.toInt()}°C — warm coats and layers."
            w.apparentC <= 14 -> out += "🧶 Cool (${w.apparentC.toInt()}°C) — a light jacket is enough."
        }

        if (w.windKmh >= 40) out += "💨 Strong wind (${w.windKmh.toInt()} km/h) — water-taxis and vaporetti may be suspended; hold on to umbrellas."
        else if (w.windKmh >= 25) out += "🌬️ Breezy (${w.windKmh.toInt()} km/h) — choppy water in the lagoon."

        if (w.weatherCode in FOG_CODES) out += "🌫️ Fog — boats and flights may be delayed."
        if (!w.isDay) out += "🌙 It will be dark — keep to well-lit streets when walking."

        if (walkingKm != null && walkingKm > 2.0 && (wet || w.apparentC <= 3)) {
            out += "🚶 Walking ${Geo.km(walkingKm)} in this weather is not recommended."
        }
        return out
    }

    fun isWet(w: WeatherAtTrip) =
        w.precipProbability >= 50 || w.precipitationMm >= 0.5 || w.weatherCode in RAIN_CODES

    fun isSevere(w: WeatherAtTrip) =
        w.weatherCode in THUNDER_CODES || w.weatherCode in HEAVY_RAIN_CODES ||
            w.weatherCode in FREEZING_CODES || w.precipitationMm >= 4 || w.windKmh >= 50

    private val THUNDER_CODES = setOf(95, 96, 99)
    private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)
    private val FREEZING_CODES = setOf(56, 57, 66, 67)
    private val HEAVY_RAIN_CODES = setOf(65, 82)
    private val FOG_CODES = setOf(45, 48)
    private val RAIN_CODES = setOf(51, 53, 55, 61, 63, 65, 80, 81, 82) + THUNDER_CODES + FREEZING_CODES
}
