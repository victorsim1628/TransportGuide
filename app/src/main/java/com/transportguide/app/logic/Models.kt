package com.transportguide.app.logic

enum class Mode(val label: String, val emoji: String) {
    WALK("Walking", "🚶"),
    TRAIN("Train / Metro / Bus", "🚆"),
    TAXI("Taxi", "🚕"),
    WATER_TAXI("Water-taxi / Vaporetto", "🛥️"),
}

data class WeatherAtTrip(
    val timeLabel: String,
    val temperatureC: Double,
    val apparentC: Double,
    val precipProbability: Int,
    val precipitationMm: Double,
    val windKmh: Double,
    val weatherCode: Int,
    val isDay: Boolean,
) {
    val description: String get() = describe(weatherCode)

    companion object {
        /** WMO weather interpretation codes as used by Open-Meteo. */
        fun describe(code: Int): String = when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61 -> "Light rain"
            63 -> "Rain"
            65 -> "Heavy rain"
            66, 67 -> "Freezing rain"
            71, 73, 75, 77 -> "Snow"
            80, 81 -> "Rain showers"
            82 -> "Violent rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown"
        }
    }
}

enum class Severity { INFO, WATCH, WARNING }

data class Disruption(
    val title: String,
    val source: String,
    val link: String,
    val publishedMillis: Long,
    val modes: Set<Mode>,
    val severity: Severity,
    /** True when the headline mentions one of the trip dates for that city. */
    val mentionsTripDate: Boolean,
)

data class RouteOption(
    val mode: Mode,
    val km: Double,
    val minutes: Int,
    val available: Boolean,
    val isEstimate: Boolean,
    val notes: List<String>,
    val warnings: List<String>,
    val score: Double = 0.0,
    val best: Boolean = false,
)
